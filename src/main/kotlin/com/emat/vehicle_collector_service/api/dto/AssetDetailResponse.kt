package com.emat.vehicle_collector_service.api.dto

import com.emat.vehicle_collector_service.assets.domain.AnalysisStatus
import com.emat.vehicle_collector_service.assets.domain.AssetStatus
import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.assets.domain.GpsSource
import com.emat.vehicle_collector_service.assets.domain.IngestState
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.domain.SuggestionStatus
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.assets.domain.TitleSource
import com.emat.vehicle_collector_service.assets.domain.VectorStatus
import com.emat.vehicle_collector_service.assets.domain.VerificationStatus
import java.time.Instant

data class AssetDetailResponse(
    val assetPublicId: String,
    val ownerId: String,
    val sessionPublicId: String?,
    val assetType: AssetType,
    val status: AssetStatus,
    val failureReason: String?,
    val file: AssetDetailFileResponse,
    val thumbnails: List<AssetDetailThumbnailResponse>,
    val capture: AssetDetailCaptureResponse,
    val curation: AssetDetailCurationResponse,
    val vehicleRecognition: AssetDetailVehicleRecognitionResponse,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val version: Long?
)

data class AssetDetailFileResponse(
    val originalFilename: String?,
    val mimeType: String?,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
    val sha256: String
)

data class AssetDetailThumbnailResponse(
    val size: ThumbnailSize,
    val url: String,
    val createdAt: Instant
)

data class AssetDetailCaptureResponse(
    val takenAt: Instant?,
    val gps: GeoPointResponse?,
    val gpsSource: GpsSource,
    val exifGps: GeoPointResponse?,
    val userGps: GeoPointResponse?,
    val place: AssetDetailPlaceResponse?,
    val camera: AssetDetailCameraResponse?
)

data class AssetDetailPlaceResponse(
    val countryCode: String?,
    val country: String?,
    val city: String?,
    val region: String?,
    val geocodedAt: Instant?,
    val geocodedFrom: GeoPointResponse?
)

data class AssetDetailCameraResponse(
    val make: String?,
    val model: String?,
    val lens: String?,
    val iso: Int?,
    val exposure: String?,
    val fNumber: Double?,
    val focalLength: Double?
)

data class AssetDetailCurationResponse(
    val title: String?,
    val titleSource: TitleSource?,
    val favorite: Boolean,
    val notes: String?,
    val externalInfo: List<AssetDetailExternalInfoResponse>,
    val albumIds: List<String>
)

data class AssetDetailExternalInfoResponse(
    val type: String,
    val label: String,
    val url: String?,
    val text: String?,
    val source: String?
)

data class AssetDetailVehicleRecognitionResponse(
    val badge: RecognitionBadge,
    val ragStatus: AssetDetailRagStatusResponse,
    val lastError: String?,
    val best: AssetDetailVehicleCandidateResponse?,
    val candidates: List<AssetDetailVehicleCandidateResponse>,
    val metaData: Map<String, Any>,
    val sync: AssetDetailRagSyncResponse
)

data class AssetDetailRagStatusResponse(
    val vectorStatus: VectorStatus,
    val analysisStatus: AnalysisStatus,
    val suggestionStatus: SuggestionStatus,
    val verificationStatus: VerificationStatus
)

data class AssetDetailVehicleCandidateResponse(
    val ragCandidateId: Long,
    val createdAt: Instant?,
    val selected: Boolean,
    val source: String?,
    val brand: String?,
    val model: String?,
    val variant: String?,
    val vehicleType: String?,
    val bodyType: String?,
    val colorPrimary: String?,
    val colorSecondary: String?,
    val country: String?,
    val doors: Int?,
    val drivetrain: String?,
    val fuelType: String?,
    val engineBadge: String?,
    val year: Int?,
    val yearFrom: Int?,
    val yearTo: Int?,
    val generation: String?,
    val tags: List<String>,
    val confidence: Double?,
    val humanEdited: Boolean
)

data class AssetDetailRagSyncResponse(
    val ingestState: IngestState,
    val ingestedAt: Instant?,
    val lastSyncAt: Instant?,
    val lastEventAt: Instant?
)
