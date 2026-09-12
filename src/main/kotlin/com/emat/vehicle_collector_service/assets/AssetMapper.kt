package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.api.dto.AssetCaptureResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailAnnotationsResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailCameraResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailCaptureResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailExternalInfoResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailFileResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailPlaceResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailRagStatusResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailRagSyncResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailThumbnailResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailVehicleCandidateResponse
import com.emat.vehicle_collector_service.api.dto.AssetDetailVehicleRecognitionResponse
import com.emat.vehicle_collector_service.api.dto.AssetFileResponse
import com.emat.vehicle_collector_service.api.dto.AssetResponse
import com.emat.vehicle_collector_service.api.dto.GeoPointResponse
import com.emat.vehicle_collector_service.api.dto.PlaceResponse
import com.emat.vehicle_collector_service.assets.domain.*
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.CaptureInfo
import com.emat.vehicle_collector_service.assets.infra.VehicleCandidate
import java.time.Instant

object AssetMapper {

    fun toDomain(assetDocument: AssetDocument): Asset =
        Asset(
            id = assetDocument.id ?: "",
            assetPublicId = assetDocument.assetPublicId,
            ownerId = assetDocument.ownerId,
            sessionPublicId = assetDocument.sessionPublicId,
            type = assetDocument.assetType,
            status = assetDocument.file.status,
            thumbnails = assetDocument.file.thumbnails.map {
                ThumbnailInfo(size = it.size, storageKeyPath = it.storageKeyPath, createdAt = it.createdAt)
            },
            createdAt = assetDocument.createdAt,
            updatedAt = assetDocument.updatedAt
        )

    fun toAssetResponse(assetDocument: AssetDocument): AssetResponse =
        AssetResponse(
            assetPublicId = assetDocument.assetPublicId,
            ownerId = assetDocument.ownerId,
            sessionPublicId = assetDocument.sessionPublicId,
            assetType = assetDocument.assetType,
            file = AssetFileResponse(
                originalFilename = assetDocument.file.originalFilename,
                mimeType = assetDocument.file.mimeType,
                sizeBytes = assetDocument.file.sizeBytes,
                width = assetDocument.file.width,
                height = assetDocument.file.height,
                sha256 = assetDocument.file.sha256
            ),
            capture = AssetCaptureResponse(
                takenAt = assetDocument.capture.takenAt,
                gps = assetDocument.capture.activeGps()?.let { GeoPointResponse(it.lat, it.lng) },
                gpsSource = assetDocument.capture.gpsSource,
                place = assetDocument.capture.place?.let {
                    PlaceResponse(
                        countryCode = it.countryCode,
                        country = it.country,
                        city = it.city,
                        region = it.region
                    )
                }
            ),
            thumbnailSmallUrl = assetDocument.file.thumbnails
                .firstOrNull { it.size == ThumbnailSize.THUMB_320 }
                ?.let { "/api/app/assets/${assetDocument.assetPublicId}/thumbnail?size=THUMB_320" },
            thumbnailMediumUrl = assetDocument.file.thumbnails
                .firstOrNull { it.size == ThumbnailSize.THUMB_640 }
                ?.let { "/api/app/assets/${assetDocument.assetPublicId}/thumbnail?size=THUMB_640" },
            createdAt = assetDocument.createdAt ?: assetDocument.updatedAt ?: Instant.now()
        )

    fun toAssetDetailResponse(assetDocument: AssetDocument): AssetDetailResponse {
        val file = assetDocument.file
        val capture = assetDocument.capture
        val annotations = assetDocument.annotations
        val vehicleRecognition = assetDocument.vehicleRecognition

        return AssetDetailResponse(
            assetPublicId = assetDocument.assetPublicId,
            ownerId = assetDocument.ownerId,
            sessionPublicId = assetDocument.sessionPublicId,
            assetType = assetDocument.assetType,
            status = file.status,
            failureReason = file.failureReason,
            file = AssetDetailFileResponse(
                originalFilename = file.originalFilename,
                mimeType = file.mimeType,
                sizeBytes = file.sizeBytes,
                width = file.width,
                height = file.height,
                sha256 = file.sha256
            ),
            thumbnails = file.thumbnails.map {
                AssetDetailThumbnailResponse(
                    size = it.size,
                    url = "/api/app/assets/${assetDocument.assetPublicId}/thumbnail?size=${it.size}",
                    createdAt = it.createdAt
                )
            },
            capture = AssetDetailCaptureResponse(
                takenAt = capture.takenAt,
                gps = capture.activeGps()?.let { GeoPointResponse(it.lat, it.lng) },
                gpsSource = capture.gpsSource,
                exifGps = capture.exifGps?.let { GeoPointResponse(it.lat, it.lng) },
                userGps = capture.userGps?.let { GeoPointResponse(it.lat, it.lng) },
                place = capture.place?.let {
                    AssetDetailPlaceResponse(
                        countryCode = it.countryCode,
                        country = it.country,
                        city = it.city,
                        region = it.region,
                        geocodedAt = it.geocodedAt,
                        geocodedFrom = it.geocodedFrom?.let { g -> GeoPointResponse(g.lat, g.lng) }
                    )
                },
                camera = capture.camera?.let {
                    AssetDetailCameraResponse(
                        make = it.make,
                        model = it.model,
                        lens = it.lens,
                        iso = it.iso,
                        exposure = it.exposure,
                        fNumber = it.fNumber,
                        focalLength = it.focalLength
                    )
                }
            ),
            annotations = AssetDetailAnnotationsResponse(
                title = annotations.title,
                titleSource = annotations.titleSource,
                favorite = annotations.favorite,
                notes = annotations.notes,
                externalInfo = annotations.externalInfo.map {
                    AssetDetailExternalInfoResponse(
                        type = it.type,
                        label = it.label,
                        url = it.url,
                        text = it.text,
                        source = it.source
                    )
                },
                albumIds = annotations.albumIds
            ),
            vehicleRecognition = AssetDetailVehicleRecognitionResponse(
                badge = vehicleRecognition.badge,
                ragStatus = AssetDetailRagStatusResponse(
                    vectorStatus = vehicleRecognition.ragStatus.vectorStatus,
                    analysisStatus = vehicleRecognition.ragStatus.analysisStatus,
                    suggestionStatus = vehicleRecognition.ragStatus.suggestionStatus,
                    verificationStatus = vehicleRecognition.ragStatus.verificationStatus
                ),
                lastError = vehicleRecognition.lastError,
                best = vehicleRecognition.best?.let { toVehicleCandidateResponse(it) },
                candidates = vehicleRecognition.candidates.map { toVehicleCandidateResponse(it) },
                metaData = vehicleRecognition.metaData,
                sync = AssetDetailRagSyncResponse(
                    ingestState = vehicleRecognition.sync.ingestState,
                    ingestedAt = vehicleRecognition.sync.ingestedAt,
                    lastSyncAt = vehicleRecognition.sync.lastSyncAt,
                    lastEventAt = vehicleRecognition.sync.lastEventAt
                )
            ),
            createdAt = assetDocument.createdAt,
            updatedAt = assetDocument.updatedAt,
            version = assetDocument.version
        )
    }

    private fun toVehicleCandidateResponse(candidate: VehicleCandidate): AssetDetailVehicleCandidateResponse =
        AssetDetailVehicleCandidateResponse(
            ragCandidateId = candidate.ragCandidateId,
            createdAt = candidate.createdAt,
            selected = candidate.selected,
            source = candidate.source,
            brand = candidate.brand,
            model = candidate.model,
            variant = candidate.variant,
            vehicleType = candidate.vehicleType,
            bodyType = candidate.bodyType,
            colorPrimary = candidate.colorPrimary,
            colorSecondary = candidate.colorSecondary,
            country = candidate.country,
            doors = candidate.doors,
            drivetrain = candidate.drivetrain,
            fuelType = candidate.fuelType,
            engineBadge = candidate.engineBadge,
            year = candidate.year,
            yearFrom = candidate.yearFrom,
            yearTo = candidate.yearTo,
            generation = candidate.generation,
            tags = candidate.tags,
            confidence = candidate.confidence,
            humanEdited = candidate.humanEdited
        )
}
