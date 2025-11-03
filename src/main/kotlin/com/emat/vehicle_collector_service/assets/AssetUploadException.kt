package com.emat.vehicle_collector_service.assets

import org.springframework.http.HttpStatus

class AssetUploadException(
    message: String?,
    val status: HttpStatus?
) : Exception(message) {

}