package com.emat.vehicle_collector_service.assets.badge

import com.emat.vehicle_collector_service.assets.domain.AnalysisStatus
import com.emat.vehicle_collector_service.assets.domain.IngestState
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.domain.SuggestionStatus
import com.emat.vehicle_collector_service.assets.domain.VectorStatus
import com.emat.vehicle_collector_service.assets.domain.VerificationStatus
import com.emat.vehicle_collector_service.assets.infra.RagStatus

object BadgeResolver {

    fun resolve(axes: RagStatus, ingestState: IngestState): RecognitionBadge = when {
        axes.verificationStatus == VerificationStatus.VERIFIED -> RecognitionBadge.VERIFIED
        axes.vectorStatus == VectorStatus.FAILED || axes.analysisStatus == AnalysisStatus.FAILED ->
            RecognitionBadge.ERROR
        axes.suggestionStatus == SuggestionStatus.SUGGESTED -> RecognitionBadge.TO_REVIEW
        axes.suggestionStatus == SuggestionStatus.UNKNOWN -> RecognitionBadge.UNKNOWN
        ingestState != IngestState.INGESTED -> RecognitionBadge.RAW
        else -> RecognitionBadge.PROCESSING
    }
}
