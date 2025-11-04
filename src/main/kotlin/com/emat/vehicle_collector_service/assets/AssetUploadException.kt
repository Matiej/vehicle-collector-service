package com.emat.vehicle_collector_service.assets

import org.springframework.http.HttpStatus

class AssetUploadException(
    message: String?,
    val status: HttpStatus,
    val code: String = "ASSET_UPLOAD_ERROR"
) : Exception(message) {

}