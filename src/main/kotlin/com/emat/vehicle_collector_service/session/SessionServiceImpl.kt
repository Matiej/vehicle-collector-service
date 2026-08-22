package com.emat.vehicle_collector_service.session

import com.emat.vehicle_collector_service.api.dto.CreateSessionRequest
import com.emat.vehicle_collector_service.api.dto.PageResponse
import com.emat.vehicle_collector_service.api.dto.SessionResponse
import com.emat.vehicle_collector_service.api.dto.SessionSummaryResponse
import com.emat.vehicle_collector_service.assets.AssetsService
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.infrastructure.error.ResourceNotFoundException
import com.emat.vehicle_collector_service.session.domain.SessionAsset
import com.emat.vehicle_collector_service.session.domain.SessionStatus
import com.emat.vehicle_collector_service.session.infra.SessionDocument
import com.emat.vehicle_collector_service.session.infra.SessionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.*
import kotlin.math.min

@Service
class SessionServiceImpl(
    private val sessionRepository: SessionRepository,
    private val assetService: AssetsService
) : SessionService {

    private val SESSIONS_CONCURRENCY: Int =
        min(Runtime.getRuntime().availableProcessors() * 2, 16)

    override fun createSession(sessionRequest: CreateSessionRequest, ownerId: String): Mono<SessionResponse> {
        val doc = SessionDocument(
            sessionPublicId = genPublicId("sess"),
            sessionName = sessionRequest.sessionName,
            ownerId = ownerId,
            sessionMode = sessionRequest.mode,
            device = sessionRequest.device
        )
        return sessionRepository.save(doc)
            .flatMap { saved -> toSessionResponse(saved) }
    }

    override fun getSessionBySessionPublicId(sessionPublicId: String, ownerId: String): Mono<SessionResponse> =
        findOwnedSession(sessionPublicId, ownerId)
            .flatMap { toSessionResponse(it) }

    override fun listSessions(
        ownerId: String,
        page: Int,
        size: Int,
        sort: Sort.Direction
    ): Mono<PageResponse<SessionSummaryResponse>> =
        toPage(
            sessionRepository.countByOwnerId(ownerId),
            sessionRepository.findByOwnerId(ownerId, pageRequest(page, size, Sort.by(sort, "createdAt"))),
            page,
            size
        )

    override fun listSessions(page: Int, size: Int, sort: Sort.Direction): Mono<PageResponse<SessionSummaryResponse>> =
        toPage(
            sessionRepository.count(),
            sessionRepository.findAllBy(pageRequest(page, size, Sort.by(sort, "createdAt"))),
            page,
            size
        )

    private fun toPage(
        total: Mono<Long>,
        sessions: Flux<SessionDocument>,
        page: Int,
        size: Int
    ): Mono<PageResponse<SessionSummaryResponse>> =
        Mono.zip(total, findSessionsAssets(sessions).collectList())
            .map { tuple ->
                PageResponse.of(
                    content = tuple.t2,
                    page = page,
                    size = size,
                    totalElements = tuple.t1
                )
            }

    override fun changeSessionStatus(
        sessionPublicId: String,
        ownerId: String,
        sessionStatus: SessionStatus
    ): Mono<SessionResponse> {
        return findOwnedSession(sessionPublicId, ownerId)
            .map { doc ->
                doc.status = sessionStatus
                doc
            }
            .flatMap { sessionRepository.save(it) }
            .flatMap { toSessionResponse(it) }
    }

    private fun findOwnedSession(sessionPublicId: String, ownerId: String): Mono<SessionDocument> =
        sessionRepository.findBySessionPublicIdAndOwnerId(sessionPublicId, ownerId)
            .switchIfEmpty(
                Mono.error(
                    ResourceNotFoundException("Session $sessionPublicId not found for owner $ownerId")
                )
            )

    private fun findSessionsAssets(sessionDocuments: Flux<SessionDocument>): Flux<SessionSummaryResponse> {
        return sessionDocuments.flatMapSequential({ session ->
            assetService.countAllBySessionPublicId(session.sessionPublicId)
                .defaultIfEmpty(0L)
                .zipWith(
                    assetService.findLastAssetThumbnail320BySessionPublicId(session.sessionPublicId)
                        .defaultIfEmpty("")
                )
                { numberOfAssets, coverThumbnailUrl ->
                    SessionSummaryResponse(
                        sessionPublicId = session.sessionPublicId,
                        sessionName = session.sessionName,
                        sessionMode = session.sessionMode,
                        ownerId = session.ownerId,
                        assetsCount = numberOfAssets.toInt(),
                        coverThumbnailUrl = coverThumbnailUrl.ifEmpty { null },
                        sessionStatus = session.status,
                        createdAt = session.createdAt.toString()
                    )
                }
        }, SESSIONS_CONCURRENCY)
    }

    private fun toSessionResponse(sessionDocument: SessionDocument): Mono<SessionResponse> =
        assetService.getAllAssetsBySessionPublicIdDescByCreatedAt(sessionDocument.sessionPublicId)
            .map { asset ->
                SessionAsset(
                    assetPublicId = asset.assetPublicId,
                    type = asset.type.name,
                    status = asset.status.name,
                    thumbnailSmallUrl = asset.thumbnails.firstOrNull { it.size == ThumbnailSize.THUMB_320 }
                        ?.let { "/api/public/assets/${asset.assetPublicId}/thumbnail?size=THUMB_320" },
                    thumbnailMediumUrl = asset.thumbnails.firstOrNull { it.size == ThumbnailSize.THUMB_640 }
                        ?.let { "/api/public/assets/${asset.assetPublicId}/thumbnail?size=THUMB_640" }
                )
            }
            .collectList()
            .map { list ->
                SessionResponse(
                    sessionPublicId = sessionDocument.sessionPublicId,
                    sessionName = sessionDocument.sessionName,
                    mode = sessionDocument.sessionMode,
                    ownerId = sessionDocument.ownerId,
                    sessionStatus = sessionDocument.status,
                    createdAt = sessionDocument.createdAt.toString(),
                    assets = list
                )
            }

    private fun pageRequest(page: Int, size: Int, sort: Sort) = PageRequest.of(page, size, sort)

    private fun genPublicId(prefix: String): String {
        val now = LocalDate.now()
        return "${prefix}_${now.year}_${now.month.value}_${UUID.randomUUID().toString().take(8)}"
    }
}