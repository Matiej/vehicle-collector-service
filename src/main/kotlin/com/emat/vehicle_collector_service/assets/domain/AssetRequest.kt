package com.emat.vehicle_collector_service.assets.domain

import org.springframework.http.codec.multipart.FilePart

data class AssetRequest(
    val sessionPublicId: String,
    val filePart: FilePart,
    val ownerId: String,
    val assetType: AssetType
) {
}