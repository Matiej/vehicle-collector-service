package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.infra.AnnotationsInfo
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.CaptureInfo
import com.emat.vehicle_collector_service.assets.infra.FileInfo
import com.emat.vehicle_collector_service.assets.infra.Place
import com.emat.vehicle_collector_service.assets.infra.VehicleCandidate
import com.emat.vehicle_collector_service.assets.infra.VehicleRecognition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
