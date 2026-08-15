package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.assets.domain.GpsSource
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.CaptureInfo
import com.emat.vehicle_collector_service.assets.infra.FileInfo
import com.emat.vehicle_collector_service.assets.infra.Place
import com.emat.vehicle_collector_service.assets.infra.Thumbnail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class AssetMapperTest {

    private val exifGps = GeoPoint(50.0614, 19.9383)
    private val userGps = GeoPoint(52.2297, 21.0122)

    @Test
    fun `maps file block from document`() {
        val document = document(
            file = FileInfo(
                storageKeyPath = "image/2026/8/asset.jpg",
                originalFilename = "IMG_0203.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 4661824,
                width = 4032,
                height = 3024,
                sha256 = "3f07839db92489db591"
            )
        )

        val response = AssetMapper.toAssetResponse(document)

        assertEquals("IMG_0203.jpg", response.file.originalFilename)
        assertEquals("image/jpeg", response.file.mimeType)
        assertEquals(4661824, response.file.sizeBytes)
        assertEquals(4032, response.file.width)
        assertEquals(3024, response.file.height)
        assertEquals("3f07839db92489db591", response.file.sha256)
    }

    @Test
    fun `maps capture block with place from document`() {
        val takenAt = Instant.parse("2026-02-21T14:30:00Z")
        val document = document(
            capture = CaptureInfo(
                takenAt = takenAt,
                exifGps = exifGps,
                place = Place(countryCode = "PL", country = "Polska", city = "Krakow", region = "malopolskie")
            )
        )

        val response = AssetMapper.toAssetResponse(document)

        assertEquals(takenAt, response.capture.takenAt)
        assertEquals("PL", response.capture.place?.countryCode)
        assertEquals("Krakow", response.capture.place?.city)
    }

    @Test
    fun `capture gps is exif gps when gps source is exif`() {
        val document = document(
            capture = CaptureInfo(exifGps = exifGps, userGps = userGps, gpsSource = GpsSource.EXIF)
        )

        val response = AssetMapper.toAssetResponse(document)

        assertEquals(exifGps.lat, response.capture.gps?.lat)
        assertEquals(exifGps.lng, response.capture.gps?.lng)
        assertEquals(GpsSource.EXIF, response.capture.gpsSource)
    }

    @Test
    fun `capture gps is user gps when gps source is user`() {
        val document = document(
            capture = CaptureInfo(exifGps = exifGps, userGps = userGps, gpsSource = GpsSource.USER)
        )

        val response = AssetMapper.toAssetResponse(document)

        assertEquals(userGps.lat, response.capture.gps?.lat)
        assertEquals(userGps.lng, response.capture.gps?.lng)
        assertEquals(GpsSource.USER, response.capture.gpsSource)
    }

    @Test
    fun `capture gps is null when neither source has coordinates`() {
        val response = AssetMapper.toAssetResponse(document(capture = CaptureInfo()))

        assertNull(response.capture.gps)
        assertNull(response.capture.place)
    }

    @Test
    fun `thumbnail urls come from the file block`() {
        val document = document(
            file = fileInfo(
                thumbnails = listOf(
                    Thumbnail(ThumbnailSize.THUMB_320, "thumbnails/asset_thumb_320.jpg")
                )
            )
        )

        val response = AssetMapper.toAssetResponse(document)

        assertEquals(
            "/api/public/assets/${document.assetPublicId}/thumbnail?size=THUMB_320",
            response.thumbnailSmallUrl
        )
        assertNull(response.thumbnailMediumUrl)
    }

    @Test
    fun `domain asset takes status and thumbnails from the file block`() {
        val document = document(
            file = fileInfo(
                status = AssetStatus.VECTORIZED,
                thumbnails = listOf(Thumbnail(ThumbnailSize.THUMB_640, "thumbnails/asset_thumb_640.jpg"))
            )
        )

        val asset = AssetMapper.toDomain(document)

        assertEquals(AssetStatus.VECTORIZED, asset.status)
        assertEquals(1, asset.thumbnails.size)
        assertEquals(ThumbnailSize.THUMB_640, asset.thumbnails.first().size)
    }

    private fun fileInfo(
        status: AssetStatus = AssetStatus.RAW,
        thumbnails: List<Thumbnail> = emptyList()
    ) = FileInfo(
        storageKeyPath = "image/2026/8/asset.jpg",
        originalFilename = "sample.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 509768,
        sha256 = "0153cfff78fc38bb7ec85879549f9c2bd4a6b3ff363c93927ad8cb85f1faabe6",
        status = status,
        thumbnails = thumbnails
    )

    private fun document(
        file: FileInfo = fileInfo(),
        capture: CaptureInfo = CaptureInfo()
    ) = AssetDocument(
        id = "65f000000000000000000001",
        assetPublicId = "asset_2026_2_original_abc12345",
        ownerId = "user-keycloak-uuid",
        sessionPublicId = "session_2026_2_xyz98765",
        assetType = AssetType.IMAGE,
        file = file,
        capture = capture,
        createdAt = Instant.parse("2026-02-21T14:32:00Z")
    )
}
