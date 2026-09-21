package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.api.web.dto.AnalysisStatusDto
import com.emat.vehicle_collector_service.api.web.dto.CandidateDto
import com.emat.vehicle_collector_service.api.web.dto.ExifDto
import com.emat.vehicle_collector_service.api.web.dto.ExternalInfoDto
import com.emat.vehicle_collector_service.api.web.dto.FileMetaDto
import com.emat.vehicle_collector_service.api.web.dto.GpsDto
import com.emat.vehicle_collector_service.api.web.dto.PhotoDetailDto
import com.emat.vehicle_collector_service.api.web.dto.PhotoListItemDto
import com.emat.vehicle_collector_service.api.web.dto.RecognitionDto
import com.emat.vehicle_collector_service.api.web.dto.SessionRefDto
import com.emat.vehicle_collector_service.api.web.dto.SuggestionStatusDto
import com.emat.vehicle_collector_service.api.web.dto.VectorStatusDto
import com.emat.vehicle_collector_service.api.web.dto.VerificationStatusDto
import com.emat.vehicle_collector_service.assets.domain.AnalysisStatus
import com.emat.vehicle_collector_service.assets.domain.RecognitionBadge
import com.emat.vehicle_collector_service.assets.domain.SuggestionStatus
import com.emat.vehicle_collector_service.assets.domain.VectorStatus
import com.emat.vehicle_collector_service.assets.domain.VerificationStatus
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.VehicleCandidate
import com.emat.vehicle_collector_service.assets.infra.VehicleRecognition
import java.time.Instant

object PhotoWebMapper {

    private val candidateTieBreak = compareByDescending<VehicleCandidate> { it.confidence ?: Double.NEGATIVE_INFINITY }
        .thenByDescending { it.createdAt ?: Instant.EPOCH }
        .thenByDescending { it.ragCandidateId }

    private val badgesWithConfidentPick = setOf(RecognitionBadge.VERIFIED, RecognitionBadge.TO_REVIEW)

    fun toListItem(asset: AssetDocument): PhotoListItemDto {
        val recognition = asset.vehicleRecognition
        val best = recognition.best
        return PhotoListItemDto(
            assetPublicId = asset.assetPublicId,
            title = asset.annotations.title,
            originalFilename = asset.file.originalFilename,
            thumbnailUrl = "/api/web/assets/${asset.assetPublicId}/thumb/640",
            badge = recognition.badge,
            brand = best?.brand,
            model = best?.model,
            yearFrom = best?.yearFrom,
            yearTo = best?.yearTo,
            takenAt = asset.capture.takenAt,
            countryCode = asset.capture.place?.countryCode,
            city = asset.capture.place?.city,
            favorite = asset.annotations.favorite,
            sessionPublicId = asset.sessionPublicId,
            errorMessage = recognition.lastError,
            createdAt = requireNotNull(asset.createdAt) { "createdAt must be set by MongoDB auditing" }
        )
    }

    fun toDetail(asset: AssetDocument, session: SessionRefDto?): PhotoDetailDto {
        val recognition = asset.vehicleRecognition
        val ragStatus = recognition.ragStatus
        val (best, otherCandidates) = splitBestAndCandidates(recognition)
        val hasFailedAxis = ragStatus.vectorStatus == VectorStatus.FAILED || ragStatus.analysisStatus == AnalysisStatus.FAILED
        val camera = asset.capture.camera
        // The frozen contract has one slot for coordinates, and capture.place (city/countryCode in the
        // same payload) is geocoded from the active point - sending raw exifGps would contradict it and
        // would drop the location of a photo positioned by hand.
        val gps = asset.capture.activeGps()

        return PhotoDetailDto(
            assetPublicId = asset.assetPublicId,
            title = asset.annotations.title,
            originalFilename = asset.file.originalFilename,
            badge = recognition.badge,
            favorite = asset.annotations.favorite,
            takenAt = asset.capture.takenAt,
            createdAt = requireNotNull(asset.createdAt) { "createdAt must be set by MongoDB auditing" },
            countryCode = asset.capture.place?.countryCode,
            city = asset.capture.place?.city,
            albums = emptyList(),
            recognition = RecognitionDto(
                vectorStatus = toVectorStatusDto(ragStatus.vectorStatus),
                analysisStatus = toAnalysisStatusDto(ragStatus.analysisStatus),
                suggestionStatus = toSuggestionStatusDto(ragStatus.suggestionStatus),
                verificationStatus = toVerificationStatusDto(ragStatus.verificationStatus),
                errorMessage = if (hasFailedAxis) recognition.lastError else null,
                bestCandidate = best?.let(::toCandidateDto),
                candidates = otherCandidates.map(::toCandidateDto),
                metaData = recognition.metaData
            ),
            exif = ExifDto(
                camera = listOfNotNull(camera?.make, camera?.model).joinToString(" ").ifBlank { null },
                lens = camera?.lens,
                iso = camera?.iso,
                exposure = camera?.exposure,
                fNumber = camera?.fNumber,
                focalLength = camera?.focalLength,
                gps = gps?.let { GpsDto(lat = it.lat, lon = it.lng, altitude = null) }
            ),
            session = session,
            notes = asset.annotations.notes,
            externalInfo = asset.annotations.externalInfo.mapNotNull { info ->
                info.url?.takeIf { it.isNotBlank() }
                    ?.let { ExternalInfoDto(source = info.source ?: info.type, title = info.label, url = it) }
            },
            file = FileMetaDto(
                size = asset.file.sizeBytes,
                width = asset.file.width,
                height = asset.file.height,
                mimeType = asset.file.mimeType,
                hash = "sha256:${asset.file.sha256}"
            )
        )
    }

    /**
     * `recognition.candidates` in Mongo holds the full candidate history (D-9); the wire contract
     * wants `bestCandidate` split out and `candidates` to hold only the rest, deduped by
     * `ragCandidateId`. Only VERIFIED and TO_REVIEW mean a confident pick exists — every other badge
     * sends `bestCandidate: null` with the whole history under `candidates`.
     *
     * `recognition.best` wins whenever it is set: the list row and the `q=` search both read it, and
     * the contract requires list->detail to show the same record. The `selected` flag and the
     * deterministic tie-break (confidence DESC -> createdAt DESC -> ragCandidateId DESC) only decide
     * for documents where `best` was never filled in.
     */
    private fun splitBestAndCandidates(recognition: VehicleRecognition): Pair<VehicleCandidate?, List<VehicleCandidate>> {
        val pool = LinkedHashMap<Long, VehicleCandidate>()
        recognition.best?.let { pool[it.ragCandidateId] = it }
        recognition.candidates.forEach { pool.putIfAbsent(it.ragCandidateId, it) }
        val all = pool.values.toList()
        if (all.isEmpty() || recognition.badge !in badgesWithConfidentPick) return null to all

        val best = recognition.best
            ?: all.firstOrNull { it.selected }
            ?: all.sortedWith(candidateTieBreak).first()
        val rest = all.filterNot { it.ragCandidateId == best.ragCandidateId }
        return best to rest
    }

    private fun toVectorStatusDto(status: VectorStatus): VectorStatusDto = when (status) {
        VectorStatus.NONE, VectorStatus.PENDING -> VectorStatusDto.PENDING
        VectorStatus.VECTORIZED -> VectorStatusDto.VECTORIZED
        VectorStatus.FAILED -> VectorStatusDto.FAILED
    }

    private fun toAnalysisStatusDto(status: AnalysisStatus): AnalysisStatusDto = when (status) {
        AnalysisStatus.NONE, AnalysisStatus.RECEIVED -> AnalysisStatusDto.RECEIVED
        AnalysisStatus.PROCESSING -> AnalysisStatusDto.PROCESSING
        AnalysisStatus.DONE -> AnalysisStatusDto.DONE
        AnalysisStatus.FAILED -> AnalysisStatusDto.FAILED
    }

    private fun toSuggestionStatusDto(status: SuggestionStatus): SuggestionStatusDto = when (status) {
        SuggestionStatus.NONE -> SuggestionStatusDto.NONE
        SuggestionStatus.UNKNOWN -> SuggestionStatusDto.UNKNOWN
        SuggestionStatus.SUGGESTED -> SuggestionStatusDto.SUGGESTED
    }

    private fun toVerificationStatusDto(status: VerificationStatus): VerificationStatusDto = when (status) {
        VerificationStatus.UNVERIFIED, VerificationStatus.REJECTED -> VerificationStatusDto.NONE
        VerificationStatus.PENDING_REVIEW -> VerificationStatusDto.PENDING_REVIEW
        VerificationStatus.VERIFIED -> VerificationStatusDto.VERIFIED
    }

    private fun toCandidateDto(candidate: VehicleCandidate): CandidateDto =
        CandidateDto(
            source = candidate.source,
            brand = candidate.brand,
            model = candidate.model,
            variant = candidate.variant,
            generation = candidate.generation,
            vehicleType = candidate.vehicleType,
            bodyType = candidate.bodyType,
            colorPrimary = candidate.colorPrimary,
            colorSecondary = candidate.colorSecondary,
            country = candidate.country,
            doors = candidate.doors,
            drivetrain = candidate.drivetrain,
            fuelType = candidate.fuelType,
            engineBadge = candidate.engineBadge,
            year = candidate.year,
            yearFrom = candidate.yearFrom,
            yearTo = candidate.yearTo,
            tags = candidate.tags,
            confidence = candidate.confidence,
            isSelected = candidate.selected,
            humanEdited = candidate.humanEdited
        )
}
