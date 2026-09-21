package com.emat.vehicle_collector_service.api.web.dto

import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import java.time.Instant

data class PhotoListItemResponse(
    val assetPublicId: String,
    val title: String?,
    val originalFilename: String?,
    val thumbnailUrl: String,
    val badge: RecognitionBadge,
    val brand: String?,
    val model: String?,
    val yearFrom: Int?,
    val yearTo: Int?,
    val takenAt: Instant?,
    val countryCode: String?,
    val city: String?,
    val favorite: Boolean,
    val sessionPublicId: String?,
    val errorMessage: String?,
    val createdAt: Instant
)
