package com.emat.vehicle_collector_service.api.web

import com.emat.vehicle_collector_service.api.web.dto.CandidateDto
import com.emat.vehicle_collector_service.api.web.dto.ExifDto
import com.emat.vehicle_collector_service.api.web.dto.ExternalInfoDto
import com.emat.vehicle_collector_service.api.web.dto.FileMetaDto
import com.emat.vehicle_collector_service.api.web.dto.GpsDto
import com.emat.vehicle_collector_service.api.web.dto.PhotoDetailDto
import com.emat.vehicle_collector_service.api.web.dto.PhotoListItemDto
import com.emat.vehicle_collector_service.api.web.dto.RecognitionDto
import com.emat.vehicle_collector_service.api.web.dto.SessionRefDto
import com.emat.vehicle_collector_service.assets.domain.AnalysisStatus
import com.emat.vehicle_collector_service.assets.domain.VectorStatus
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.assets.infra.VehicleCandidate
import com.emat.vehicle_collector_service.assets.infra.VehicleRecognition
import java.time.Instant

object PhotoWebMapper {

    private val candidateTieBreak = compareByDescending<VehicleCandidate> { it.confidence ?: Double.NEGATIVE_INFINITY }
        .thenByDescending { it.createdAt ?: Instant.EPOCH }
        .thenByDescending { it.ragCandidateId }

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
        val exifGps = asset.capture.exifGps

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
                vectorStatus = ragStatus.vectorStatus,
                analysisStatus = ragStatus.analysisStatus,
                suggestionStatus = ragStatus.suggestionStatus,
                verificationStatus = ragStatus.verificationStatus,
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
                gps = exifGps?.let { GpsDto(lat = it.lat, lon = it.lng, altitude = null) }
            ),
            session = session,
            notes = asset.annotations.notes,
            externalInfo = asset.annotations.externalInfo.map {
                ExternalInfoDto(source = it.source ?: it.type, title = it.label, url = it.url ?: "")
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
     * `ragCandidateId`. When nobody is flagged `selected`, the pick is deterministic:
     * confidence DESC -> createdAt DESC -> ragCandidateId DESC.
     */
    private fun splitBestAndCandidates(recognition: VehicleRecognition): Pair<VehicleCandidate?, List<VehicleCandidate>> {
        val pool = LinkedHashMap<Long, VehicleCandidate>()
        recognition.candidates.forEach { pool.putIfAbsent(it.ragCandidateId, it) }
        recognition.best?.let { pool.putIfAbsent(it.ragCandidateId, it) }
        val all = pool.values.toList()
        if (all.isEmpty()) return null to emptyList()

        val best = all.firstOrNull { it.selected } ?: all.sortedWith(candidateTieBreak).first()
        val rest = all.filterNot { it.ragCandidateId == best.ragCandidateId }
        return best to rest
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
