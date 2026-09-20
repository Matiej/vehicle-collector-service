package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.api.web.dto.SessionRefDto
import com.emat.vehicle_collector_service.assets.domain.AnalysisStatus
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.domain.VectorStatus
import com.emat.vehicle_collector_service.assets.infra.AnnotationsInfo
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.CameraInfo
import com.emat.vehicle_collector_service.assets.infra.CaptureInfo
import com.emat.vehicle_collector_service.assets.infra.FileInfo
import com.emat.vehicle_collector_service.assets.infra.Place
import com.emat.vehicle_collector_service.assets.infra.RagStatus
import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.assets.infra.VehicleCandidate
import com.emat.vehicle_collector_service.assets.infra.VehicleRecognition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
        val document = document(recognition = VehicleRecognition(candidates = listOf(best, other)))

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
        val document = document(recognition = VehicleRecognition(candidates = listOf(older, newerSameConfidence)))

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals("Opel", dto.recognition.bestCandidate?.brand)
        assertEquals(1, dto.recognition.candidates.size)
        assertEquals("Ford", dto.recognition.candidates[0].brand)
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
    fun `exif gps serializes lat and lon from the immutable exifGps, altitude is unknown`() {
        val document = document(
            capture = CaptureInfo(exifGps = GeoPoint(lat = 35.8989055, lng = 14.4936972))
        )

        val dto = PhotoWebMapper.toDetail(document, session = null)

        assertEquals(35.8989055, dto.exif.gps?.lat)
        assertEquals(14.4936972, dto.exif.gps?.lon)
        assertNull(dto.exif.gps?.altitude)
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
