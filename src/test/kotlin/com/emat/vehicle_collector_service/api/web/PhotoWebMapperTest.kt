package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.api.web.dto.AnalysisStatusDto
import com.emat.vehicle_collector_service.api.web.dto.SessionRefDto
import com.emat.vehicle_collector_service.api.web.dto.SuggestionStatusDto
import com.emat.vehicle_collector_service.api.web.dto.VectorStatusDto
import com.emat.vehicle_collector_service.api.web.dto.VerificationStatusDto
import com.emat.vehicle_collector_service.assets.domain.AnalysisStatus
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.domain.SuggestionStatus
import com.emat.vehicle_collector_service.assets.domain.VectorStatus
import com.emat.vehicle_collector_service.assets.domain.VerificationStatus
import com.emat.vehicle_collector_service.assets.infra.AnnotationsInfo
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.CameraInfo
import com.emat.vehicle_collector_service.assets.infra.CaptureInfo
import com.emat.vehicle_collector_service.assets.infra.ExternalInfo
import com.emat.vehicle_collector_service.assets.infra.FileInfo
import com.emat.vehicle_collector_service.assets.infra.Place
import com.emat.vehicle_collector_service.assets.infra.RagStatus
import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.assets.domain.GpsSource
import com.emat.vehicle_collector_service.assets.infra.VehicleCandidate
import com.emat.vehicle_collector_service.assets.infra.VehicleRecognition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant

class PhotoWebMapperTest {

    @Test
    fun `a fresh RAW asset maps to the contract-required fields, everything else null`() {
        val document = document()

        val dto = PhotoWebMapper.toListItem(document)

        assertEquals(document.assetPublicId, dto.assetPublicId)
        assertEquals("sample.jpg", dto.originalFilename)
        assertEquals("/api/web/assets/${document.assetPublicId}/thumb/640", dto.thumbnailUrl)
        assertEquals(RecognitionBadge.RAW, dto.badge)
        assertEquals(false, dto.favorite)
        assertNull(dto.title)
        assertNull(dto.brand)
        assertNull(dto.model)
        assertNull(dto.yearFrom)
        assertNull(dto.yearTo)
        assertNull(dto.takenAt)
        assertNull(dto.countryCode)
        assertNull(dto.city)
        assertNull(dto.errorMessage)
        assertNull(dto.sessionPublicId)
    }

    @Test
    fun `thumbnailUrl is built from the asset public id alone, regardless of recognition state`() {
        val document = document(recognition = VehicleRecognition(badge = RecognitionBadge.ERROR, lastError = "boom"))

        val dto = PhotoWebMapper.toListItem(document)

        assertEquals("/api/web/assets/${document.assetPublicId}/thumb/640", dto.thumbnailUrl)
    }

    @Test
    fun `brand, model and year range come from the best candidate snapshot`() {
        val best = VehicleCandidate(
            ragCandidateId = 1L,
            brand = "Ford",
            model = "Escort",
            yearFrom = 1980,
            yearTo = 1986
        )
        val document = document(recognition = VehicleRecognition(best = best))

        val dto = PhotoWebMapper.toListItem(document)

        assertEquals("Ford", dto.brand)
        assertEquals("Escort", dto.model)
        assertEquals(1980, dto.yearFrom)
        assertEquals(1986, dto.yearTo)
    }

    @Test
    fun `errorMessage is populated from the recognition lastError`() {
        val document = document(
            recognition = VehicleRecognition(badge = RecognitionBadge.ERROR, lastError = "AnalysisError: boom")
        )

        val dto = PhotoWebMapper.toListItem(document)

        assertEquals(RecognitionBadge.ERROR, dto.badge)
        assertEquals("AnalysisError: boom", dto.errorMessage)
    }

    @Test
    fun `title and favorite come from annotations, location from capture place`() {
        val document = document(
            annotations = AnnotationsInfo(title = "Ford Escort / Ta' Xbiex", favorite = true),
            capture = CaptureInfo(
                takenAt = Instant.parse("2026-07-14T10:00:00Z"),
                place = Place(countryCode = "MT", city = "Ta' Xbiex")
            )
        )

        val dto = PhotoWebMapper.toListItem(document)

        assertEquals("Ford Escort / Ta' Xbiex", dto.title)
        assertEquals(true, dto.favorite)
        assertEquals(Instant.parse("2026-07-14T10:00:00Z"), dto.takenAt)
        assertEquals("MT", dto.countryCode)
        assertEquals("Ta' Xbiex", dto.city)
    }

    @Test
    fun `sessionPublicId is passed through as-is`() {
        val document = document(sessionPublicId = "se_marina")

        val dto = PhotoWebMapper.toListItem(document)

        assertEquals("se_marina", dto.sessionPublicId)
    }

    @Test
    fun `candidates excludes the selected best entry, no duplicate`() {
        val best = VehicleCandidate(ragCandidateId = 5L, source = "external_llm", confidence = 0.90, selected = true)
        val other = VehicleCandidate(ragCandidateId = 7L, source = "similarity", confidence = 0.71)
        val document = document(
            recognition = VehicleRecognition(badge = RecognitionBadge.TO_REVIEW, candidates = listOf(best, other))
        )

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals("external_llm", dto.recognition.bestCandidate?.source)
        assertEquals(0.90, dto.recognition.bestCandidate?.confidence)
        assertEquals(1, dto.recognition.candidates.size)
        assertEquals("similarity", dto.recognition.candidates[0].source)
    }

    @Test
    fun `tie-break picks highest confidence, then newest, then highest ragCandidateId when nobody is selected`() {
        val older = VehicleCandidate(
            ragCandidateId = 1L, brand = "Ford", confidence = 0.71,
            createdAt = Instant.parse("2026-01-01T00:00:00Z")
        )
        val newerSameConfidence = VehicleCandidate(
            ragCandidateId = 2L, brand = "Opel", confidence = 0.71,
            createdAt = Instant.parse("2026-02-01T00:00:00Z")
        )
        val document = document(
            recognition = VehicleRecognition(
                badge = RecognitionBadge.TO_REVIEW,
                candidates = listOf(older, newerSameConfidence)
            )
        )

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals("Opel", dto.recognition.bestCandidate?.brand)
        assertEquals(1, dto.recognition.candidates.size)
        assertEquals("Ford", dto.recognition.candidates[0].brand)
    }

    @Test
    fun `a badge without a confident pick sends bestCandidate null and keeps the whole history`() {
        val stale = VehicleCandidate(ragCandidateId = 1L, brand = "Ford", confidence = 0.90, selected = true)
        val alsoStale = VehicleCandidate(ragCandidateId = 2L, brand = "Opel", confidence = 0.71)

        listOf(
            RecognitionBadge.RAW,
            RecognitionBadge.PROCESSING,
            RecognitionBadge.UNKNOWN,
            RecognitionBadge.ERROR
        ).forEach { badge ->
            val document = document(
                recognition = VehicleRecognition(badge = badge, candidates = listOf(stale, alsoStale))
            )

            val dto = PhotoWebMapper.toDetail(document, session = null)

            assertNull(dto.recognition.bestCandidate, "bestCandidate must be null for $badge")
            assertEquals(2, dto.recognition.candidates.size, "whole history must stay visible for $badge")
        }
    }

    @Test
    fun `VERIFIED keeps the confident pick`() {
        val selected = VehicleCandidate(ragCandidateId = 1L, brand = "Ford", selected = true, humanEdited = true)
        val document = document(
            recognition = VehicleRecognition(badge = RecognitionBadge.VERIFIED, candidates = listOf(selected))
        )

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals("Ford", dto.recognition.bestCandidate?.brand)
        assertTrue(dto.recognition.bestCandidate!!.isSelected)
        assertTrue(dto.recognition.candidates.isEmpty())
    }

    @ParameterizedTest
    @CsvSource(
        "NONE, PENDING",
        "PENDING, PENDING",
        "VECTORIZED, VECTORIZED",
        "FAILED, FAILED"
    )
    fun `vectorStatus is mapped onto the frozen contract values`(domain: VectorStatus, wire: VectorStatusDto) {
        val document = document(recognition = VehicleRecognition(ragStatus = RagStatus(vectorStatus = domain)))

        assertEquals(wire, PhotoWebMapper.toDetail(document, null).recognition.vectorStatus)
    }

    @ParameterizedTest
    @CsvSource(
        "NONE, RECEIVED",
        "RECEIVED, RECEIVED",
        "PROCESSING, PROCESSING",
        "DONE, DONE",
        "FAILED, FAILED"
    )
    fun `analysisStatus is mapped onto the frozen contract values`(domain: AnalysisStatus, wire: AnalysisStatusDto) {
        val document = document(recognition = VehicleRecognition(ragStatus = RagStatus(analysisStatus = domain)))

        assertEquals(wire, PhotoWebMapper.toDetail(document, null).recognition.analysisStatus)
    }

    @ParameterizedTest
    @CsvSource(
        "UNVERIFIED, NONE",
        "REJECTED, NONE",
        "PENDING_REVIEW, PENDING_REVIEW",
        "VERIFIED, VERIFIED"
    )
    fun `verificationStatus is mapped onto the frozen contract values`(
        domain: VerificationStatus,
        wire: VerificationStatusDto
    ) {
        val document = document(recognition = VehicleRecognition(ragStatus = RagStatus(verificationStatus = domain)))

        assertEquals(wire, PhotoWebMapper.toDetail(document, null).recognition.verificationStatus)
    }

    @ParameterizedTest
    @CsvSource(
        "NONE, NONE",
        "UNKNOWN, UNKNOWN",
        "SUGGESTED, SUGGESTED"
    )
    fun `suggestionStatus passes through, both sides share the same values`(
        domain: SuggestionStatus,
        wire: SuggestionStatusDto
    ) {
        val document = document(recognition = VehicleRecognition(ragStatus = RagStatus(suggestionStatus = domain)))

        assertEquals(wire, PhotoWebMapper.toDetail(document, null).recognition.suggestionStatus)
    }

    @Test
    fun `externalInfo drops entries without a url, the contract wants absolute links only`() {
        val document = document(
            annotations = AnnotationsInfo(
                externalInfo = listOf(
                    ExternalInfo(type = "link", label = "Ford Escort", url = "https://en.wikipedia.org/wiki/Ford_Escort", source = "wikipedia"),
                    ExternalInfo(type = "note", label = "Owner said Mk3", text = "numbers-matching"),
                    ExternalInfo(type = "link", label = "blank", url = "  ")
                )
            )
        )

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals(1, dto.externalInfo.size)
        assertEquals("wikipedia", dto.externalInfo[0].source)
        assertEquals("Ford Escort", dto.externalInfo[0].title)
        assertEquals("https://en.wikipedia.org/wiki/Ford_Escort", dto.externalInfo[0].url)
    }

    @Test
    fun `externalInfo falls back to the entry type when source is missing`() {
        val document = document(
            annotations = AnnotationsInfo(
                externalInfo = listOf(ExternalInfo(type = "manual", label = "Brochure", url = "https://example.com/x"))
            )
        )

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals("manual", dto.externalInfo[0].source)
    }

    @Test
    fun `recognition has no candidates when history is empty`() {
        val document = document()

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertNull(dto.recognition.bestCandidate)
        assertTrue(dto.recognition.candidates.isEmpty())
    }

    @Test
    fun `errorMessage surfaces only when a FAILED axis exists`() {
        val failed = document(
            recognition = VehicleRecognition(
                ragStatus = RagStatus(vectorStatus = VectorStatus.FAILED),
                lastError = "VectorError: boom"
            )
        )
        val notFailed = document(
            recognition = VehicleRecognition(
                ragStatus = RagStatus(analysisStatus = AnalysisStatus.DONE),
                lastError = "stale message from a previous run"
            )
        )

        assertEquals("VectorError: boom", PhotoWebMapper.toDetail(failed, null).recognition.errorMessage)
        assertNull(PhotoWebMapper.toDetail(notFailed, null).recognition.errorMessage)
    }

    @Test
    fun `exif gps serializes lat and lon, altitude is unknown`() {
        val document = document(
            capture = CaptureInfo(exifGps = GeoPoint(lat = 35.8989055, lng = 14.4936972))
        )

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals(35.8989055, dto.exif.gps?.lat)
        assertEquals(14.4936972, dto.exif.gps?.lon)
        assertNull(dto.exif.gps?.altitude)
    }

    @Test
    fun `gps follows the user correction, the same point capture place was geocoded from`() {
        val corrected = document(
            capture = CaptureInfo(
                exifGps = GeoPoint(lat = 50.0614, lng = 19.9366),
                userGps = GeoPoint(lat = 35.8989055, lng = 14.4936972),
                gpsSource = GpsSource.USER
            )
        )

        val dto = PhotoWebMapper.toDetail(corrected, session = null)

        assertEquals(35.8989055, dto.exif.gps?.lat)
        assertEquals(14.4936972, dto.exif.gps?.lon)
    }

    @Test
    fun `a photo positioned by hand without any EXIF GPS still reports a location`() {
        val handPlaced = document(
            capture = CaptureInfo(
                userGps = GeoPoint(lat = 35.8989055, lng = 14.4936972),
                gpsSource = GpsSource.USER
            )
        )

        val dto = PhotoWebMapper.toDetail(handPlaced, session = null)

        assertEquals(35.8989055, dto.exif.gps?.lat)
    }

    @Test
    fun `bestCandidate is the stored best, so the detail agrees with the list row and the search`() {
        val humanEdited = VehicleCandidate(ragCandidateId = 5L, brand = "Ford", humanEdited = true, confidence = 0.40)
        val preEditCopy = VehicleCandidate(ragCandidateId = 5L, brand = "Fnord", confidence = 0.40)
        val higherConfidence = VehicleCandidate(ragCandidateId = 9L, brand = "Opel", confidence = 0.95)
        val document = document(
            recognition = VehicleRecognition(
                badge = RecognitionBadge.VERIFIED,
                best = humanEdited,
                candidates = listOf(preEditCopy, higherConfidence)
            )
        )

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals("Ford", dto.recognition.bestCandidate?.brand)
        assertEquals(PhotoWebMapper.toListItem(document).brand, dto.recognition.bestCandidate?.brand)
        assertEquals(listOf("Opel"), dto.recognition.candidates.map { it.brand })
    }

    @Test
    fun `exif has no gps when the photo has none`() {
        val document = document(capture = CaptureInfo())

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertNull(dto.exif.gps)
    }

    @Test
    fun `camera combines make and model into a single label`() {
        val document = document(
            capture = CaptureInfo(camera = CameraInfo(make = "Apple", model = "iPhone 11", iso = 32))
        )

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals("Apple iPhone 11", dto.exif.camera)
        assertEquals(32, dto.exif.iso)
    }

    @Test
    fun `albums are always empty, session passes through as given`() {
        val document = document()
        val session = SessionRefDto(publicId = "se_marina", name = "Marina walk", hasAudio = false)

        val withSession = PhotoWebMapper.toDetail(document, session)
        val withoutSession = PhotoWebMapper.toDetail(document, session = null)

        assertTrue(withSession.albums.isEmpty())
        assertEquals(session, withSession.session)
        assertNull(withoutSession.session)
    }

    @Test
    fun `file hash carries the sha256 prefix expected by the contract`() {
        val document = document()

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals("sha256:${document.file.sha256}", dto.file.hash)
        assertEquals(document.file.sizeBytes, dto.file.size)
        assertFalse(dto.file.hash.startsWith("sha256:sha256:"))
    }

    private fun document(
        sessionPublicId: String? = null,
        annotations: AnnotationsInfo = AnnotationsInfo(),
        recognition: VehicleRecognition = VehicleRecognition(),
        capture: CaptureInfo = CaptureInfo()
    ) = AssetDocument(
        id = "65f000000000000000000001",
        assetPublicId = "asset_2026_2_original_abc12345",
        ownerId = "user-keycloak-uuid",
        sessionPublicId = sessionPublicId,
        assetType = AssetType.IMAGE,
        file = FileInfo(
            storageKeyPath = "image/2026/2/asset.jpg",
            originalFilename = "sample.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 509768,
            sha256 = "0153cfff78fc38bb7ec85879549f9c2bd4a6b3ff363c93927ad8cb85f1faabe6"
        ),
        capture = capture,
        annotations = annotations,
        vehicleRecognition = recognition,
        createdAt = Instant.parse("2026-02-21T14:32:00Z")
    )
}
