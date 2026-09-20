package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.api.web.dto.PhotoListItemDto
import com.emat.vehicle_collector_service.assets.infra.AssetDocument

object PhotoWebMapper {

    fun toListItem(asset: AssetDocument): PhotoListItemDto {
        val recognition = asset.vehicleRecognition
        val best = recognition.best
        return PhotoListItemDto(
            assetPublicId = asset.assetPublicId,
            title = asset.annotations.title,
            originalFilename = asset.file.originalFilename,
            thumbnailUrl = "/api/web/assets/${asset.assetPublicId}/thumb/640",
            badge = recognition.badge,
            brand = best?.brand,
            model = best?.model,
            yearFrom = best?.yearFrom,
            yearTo = best?.yearTo,
            takenAt = asset.capture.takenAt,
            countryCode = asset.capture.place?.countryCode,
            city = asset.capture.place?.city,
            favorite = asset.annotations.favorite,
            sessionPublicId = asset.sessionPublicId,
            errorMessage = recognition.lastError,
            createdAt = requireNotNull(asset.createdAt) { "createdAt must be set by MongoDB auditing" }
        )
    }
}
