package com.emat.vehicle_collector_service.assets.badge

import com.emat.vehicle_collector_service.assets.domain.AnalysisStatus
import com.emat.vehicle_collector_service.assets.domain.IngestState
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.domain.SuggestionStatus
import com.emat.vehicle_collector_service.assets.domain.VectorStatus
import com.emat.vehicle_collector_service.assets.domain.VerificationStatus
import com.emat.vehicle_collector_service.assets.infra.RagStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BadgeResolverTest {

    @Test
    fun `not yet ingested with no axes set resolves to RAW`() {
        val badge = BadgeResolver.resolve(RagStatus(), IngestState.NOT_READY)

        assertEquals(RecognitionBadge.RAW, badge)
    }

    @Test
    fun `ingested with no result yet resolves to PROCESSING`() {
        val badge = BadgeResolver.resolve(RagStatus(), IngestState.INGESTED)

        assertEquals(RecognitionBadge.PROCESSING, badge)
    }

    @Test
    fun `pending ingest with no axes set still resolves to RAW`() {
        val badge = BadgeResolver.resolve(RagStatus(), IngestState.PENDING)

        assertEquals(RecognitionBadge.RAW, badge)
    }

    @Test
    fun `failed vector status resolves to ERROR`() {
        val badge = BadgeResolver.resolve(
            RagStatus(vectorStatus = VectorStatus.FAILED),
            IngestState.INGESTED
        )

        assertEquals(RecognitionBadge.ERROR, badge)
    }

    @Test
    fun `failed analysis status resolves to ERROR`() {
        val badge = BadgeResolver.resolve(
            RagStatus(analysisStatus = AnalysisStatus.FAILED),
            IngestState.INGESTED
        )

        assertEquals(RecognitionBadge.ERROR, badge)
    }

    @Test
    fun `verified verification status resolves to VERIFIED`() {
        val badge = BadgeResolver.resolve(
            RagStatus(verificationStatus = VerificationStatus.VERIFIED),
            IngestState.INGESTED
        )

        assertEquals(RecognitionBadge.VERIFIED, badge)
    }

    @Test
    fun `suggested suggestion status resolves to TO_REVIEW`() {
        val badge = BadgeResolver.resolve(
            RagStatus(suggestionStatus = SuggestionStatus.SUGGESTED),
            IngestState.INGESTED
        )

        assertEquals(RecognitionBadge.TO_REVIEW, badge)
    }

    @Test
    fun `unknown suggestion status resolves to UNKNOWN`() {
        val badge = BadgeResolver.resolve(
            RagStatus(suggestionStatus = SuggestionStatus.UNKNOWN),
            IngestState.INGESTED
        )

        assertEquals(RecognitionBadge.UNKNOWN, badge)
    }

    @Test
    fun `verified wins over a failed vector status`() {
        val badge = BadgeResolver.resolve(
            RagStatus(
                vectorStatus = VectorStatus.FAILED,
                verificationStatus = VerificationStatus.VERIFIED
            ),
            IngestState.INGESTED
        )

        assertEquals(RecognitionBadge.VERIFIED, badge)
    }

    @Test
    fun `verified wins over a failed analysis status`() {
        val badge = BadgeResolver.resolve(
            RagStatus(
                analysisStatus = AnalysisStatus.FAILED,
                verificationStatus = VerificationStatus.VERIFIED
            ),
            IngestState.INGESTED
        )

        assertEquals(RecognitionBadge.VERIFIED, badge)
    }

    @Test
    fun `a failed axis wins over a pending suggestion`() {
        val badge = BadgeResolver.resolve(
            RagStatus(
                vectorStatus = VectorStatus.FAILED,
                suggestionStatus = SuggestionStatus.SUGGESTED
            ),
            IngestState.INGESTED
        )

        assertEquals(RecognitionBadge.ERROR, badge)
    }

    @Test
    fun `verified wins over a stale suggestion status`() {
        val badge = BadgeResolver.resolve(
            RagStatus(
                suggestionStatus = SuggestionStatus.SUGGESTED,
                verificationStatus = VerificationStatus.VERIFIED
            ),
            IngestState.INGESTED
        )

        assertEquals(RecognitionBadge.VERIFIED, badge)
    }

    @Test
    fun `suggested wins over unknown when both are somehow set`() {
        val badge = BadgeResolver.resolve(
            RagStatus(suggestionStatus = SuggestionStatus.SUGGESTED),
            IngestState.INGESTED
        )

        assertEquals(RecognitionBadge.TO_REVIEW, badge)
    }
}
