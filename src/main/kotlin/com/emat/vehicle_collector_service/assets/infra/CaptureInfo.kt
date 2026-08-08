package com.emat.vehicle_collector_service.assets.infra

import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.assets.domain.GpsSource
import java.time.Instant

data class CaptureInfo(
    val takenAt: Instant? = null,
    val exifGps: GeoPoint? = null,
    val userGps: GeoPoint? = null,
    val gpsSource: GpsSource = GpsSource.EXIF,
    val place: Place? = null,
    val camera: CameraInfo? = null
)

data class Place(
    val countryCode: String? = null,
    val country: String? = null,
    val city: String? = null,
    val region: String? = null,
    val geocodedAt: Instant? = null,
    val geocodedFrom: GeoPoint? = null
)

data class CameraInfo(
    val make: String? = null,
    val model: String? = null,
    val lens: String? = null,
    val iso: Int? = null,
    val exposure: String? = null,
    val fNumber: Double? = null,
    val focalLength: Double? = null
)
