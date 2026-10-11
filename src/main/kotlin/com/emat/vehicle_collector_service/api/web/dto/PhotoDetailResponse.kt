package com.emat.vehicle_collector_service.api.web.dto

import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class PhotoDetailResponse(
    val assetPublicId: String,
    val title: String?,
    val originalFilename: String?,
    val badge: RecognitionBadge,
    val favorite: Boolean,
    val takenAt: Instant?,
    val createdAt: Instant,
    val countryCode: String?,
    val city: String?,
    val albums: List<AlbumRefResponse>,
    val vehicleRecognition: VehicleRecognitionResponse,
    val exif: ExifResponse,
    val session: SessionRefResponse?,
    val notes: String?,
    val externalInfo: List<ExternalInfoResponse>,
    val file: FileMetaResponse
)

data class AlbumRefResponse(
    val publicId: String,
    val name: String
)

data class VehicleRecognitionResponse(
    val vectorStatus: VectorStatusResponse,
    val analysisStatus: AnalysisStatusResponse,
    val suggestionStatus: SuggestionStatusResponse,
    val verificationStatus: VerificationStatusResponse,
    val errorMessage: String?,
    val bestCandidate: CandidateResponse?,
    val candidates: List<CandidateResponse>,
    val metaData: Map<String, Any>
)

enum class VectorStatusResponse { PENDING, VECTORIZED, FAILED }

enum class AnalysisStatusResponse { RECEIVED, PROCESSING, DONE, FAILED }

enum class SuggestionStatusResponse { NONE, UNKNOWN, SUGGESTED }

enum class VerificationStatusResponse { NONE, PENDING_REVIEW, VERIFIED }

data class CandidateResponse(
    val source: String?,
    val brand: String?,
    val model: String?,
    val variant: String?,
    val generation: String?,
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
    val tags: List<String>,
    val confidence: Double?,
    @get:JsonProperty("isSelected")
    val isSelected: Boolean,
    val humanEdited: Boolean
)

data class ExifResponse(
    val camera: String?,
    val lens: String?,
    val iso: Int?,
    val exposure: String?,
    val fNumber: Double?,
    val focalLength: Double?,
    val gps: GpsResponse?
)

data class GpsResponse(
    val lat: Double,
    val lng: Double,
    val altitude: Double?
)

data class SessionRefResponse(
    val publicId: String,
    val name: String,
    val hasAudio: Boolean
)

data class ExternalInfoResponse(
    val source: String,
    val title: String,
    val url: String
)

data class FileMetaResponse(
    val size: Long,
    val width: Int?,
    val height: Int?,
    val mimeType: String?,
    val hash: String
)
