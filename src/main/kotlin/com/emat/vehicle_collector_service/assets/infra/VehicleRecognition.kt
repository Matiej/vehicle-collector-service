package com.emat.vehicle_collector_service.assets.infra

import com.emat.vehicle_collector_service.assets.domain.AnalysisStatus
import com.emat.vehicle_collector_service.assets.domain.IngestState
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.domain.SuggestionStatus
import com.emat.vehicle_collector_service.assets.domain.VectorStatus
import com.emat.vehicle_collector_service.assets.domain.VerificationStatus
import java.time.Instant

data class VehicleRecognition(
    val badge: RecognitionBadge = RecognitionBadge.RAW,
    val ragStatus: RagStatus = RagStatus(),
    val lastError: String? = null,
    val best: VehicleCandidate? = null,
    val candidates: List<VehicleCandidate> = emptyList(),
    val metaData: Map<String, Any> = emptyMap(),
    val sync: RagSync = RagSync()
)

data class RagStatus(
    val vectorStatus: VectorStatus = VectorStatus.NONE,
    val analysisStatus: AnalysisStatus = AnalysisStatus.NONE,
    val suggestionStatus: SuggestionStatus = SuggestionStatus.NONE,
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED
)

data class VehicleCandidate(
    val ragCandidateId: Long,
    val createdAt: Instant? = null,
    val selected: Boolean = false,
    val source: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val variant: String? = null,
    val vehicleType: String? = null,
    val bodyType: String? = null,
    val colorPrimary: String? = null,
    val colorSecondary: String? = null,
    val country: String? = null,
    val doors: Int? = null,
    val drivetrain: String? = null,
    val fuelType: String? = null,
    val engineBadge: String? = null,
    val year: Int? = null,
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val generation: String? = null,
    val tags: List<String> = emptyList(),
    val confidence: Double? = null,
    val humanEdited: Boolean = false
)

data class RagSync(
    val ingestState: IngestState = IngestState.NOT_READY,
    val ingestedAt: Instant? = null,
    val lastSyncAt: Instant? = null,
    val lastEventAt: Instant? = null
)
