package com.emat.vehicle_collector_service.api.web.dto

import com.emat.vehicle_collector_service.assets.domain.AnalysisStatus
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.domain.SuggestionStatus
import com.emat.vehicle_collector_service.assets.domain.VectorStatus
import com.emat.vehicle_collector_service.assets.domain.VerificationStatus
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class PhotoDetailDto(
    val assetPublicId: String,
    val title: String?,
    val originalFilename: String?,
    val badge: RecognitionBadge,
    val favorite: Boolean,
    val takenAt: Instant?,
    val createdAt: Instant,
    val countryCode: String?,
    val city: String?,
    val albums: List<AlbumRefDto>,
    val recognition: RecognitionDto,
    val exif: ExifDto,
    val session: SessionRefDto?,
    val notes: String?,
    val externalInfo: List<ExternalInfoDto>,
    val file: FileMetaDto
)

data class AlbumRefDto(
    val publicId: String,
    val name: String
)

data class RecognitionDto(
    val vectorStatus: VectorStatus,
    val analysisStatus: AnalysisStatus,
    val suggestionStatus: SuggestionStatus,
    val verificationStatus: VerificationStatus,
    val errorMessage: String?,
    val bestCandidate: CandidateDto?,
    val candidates: List<CandidateDto>,
    val metaData: Map<String, Any>
)

data class CandidateDto(
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

data class ExifDto(
    val camera: String?,
    val lens: String?,
    val iso: Int?,
    val exposure: String?,
    val fNumber: Double?,
    val focalLength: Double?,
    val gps: GpsDto?
)

data class GpsDto(
    val lat: Double,
    val lon: Double,
    val altitude: Double?
)

data class SessionRefDto(
    val publicId: String,
    val name: String,
    val hasAudio: Boolean
)

data class ExternalInfoDto(
    val source: String,
    val title: String,
    val url: String
)

data class FileMetaDto(
    val size: Long,
    val width: Int?,
    val height: Int?,
    val mimeType: String?,
    val hash: String
)
