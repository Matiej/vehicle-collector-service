package com.emat.vehicle_collector_service.session

import com.emat.vehicle_collector_service.api.internal.dto.CreateSessionRequest
import com.emat.vehicle_collector_service.api.internal.dto.SessionResponse
import com.emat.vehicle_collector_service.api.internal.dto.SessionSummaryResponse
import com.emat.vehicle_collector_service.assets.AssetUploadException
import com.emat.vehicle_collector_service.assets.AssetsService
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.session.domain.SessionAsset
import com.emat.vehicle_collector_service.session.domain.SessionStatus
import com.emat.vehicle_collector_service.session.infra.SessionDocument
import com.emat.vehicle_collector_service.session.infra.SessionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.util.*

@Service
class SessionServiceImpl(
    private val sessionRepository: SessionRepository,
    private val assetService: AssetsService
) : SessionService {

    override fun createSession(req: CreateSessionRequest): Mono<SessionResponse> {
        val doc = SessionDocument(
            sessionExternalId = genPublicId("sess"),
            ownerId = req.ownerId,
            sessionMode = req.mode
        )
        return sessionRepository.save(doc)
            .flatMap { saved -> toSessionResponse(saved) }
    }

    override fun getSession(sessionId: String): Mono<SessionResponse> =
        sessionRepository.findById(sessionId)
            .switchIfEmpty(
                Mono.error(
                    AssetUploadException(
                        "Can't find session: $sessionId for upload",
                        HttpStatus.NOT_FOUND
                    )
                )
            )
            .flatMap { toSessionResponse(it) }

    override fun listSessions(
        ownerId: String,
        page: Int,
        size: Int,
        sort: Sort.Direction
    ): Flux<SessionSummaryResponse> =
        findSessionsAssets(
            sessionRepository.findByOwnerId(
                ownerId,
                pageRequest(
                    page,
                    size,
                    Sort.by(sort, "createdAt")
                )
            )
        )


    override fun listSessions(page: Int, size: Int, sort: Sort.Direction): Flux<SessionSummaryResponse> =
        findSessionsAssets(
            sessionRepository.findAllBy(
                pageRequest(
                    page,
                    size,
                    Sort.by(sort, "createdAt")
                )
            )
        )

    override fun changeSessionStatus(sessionId: String, sessionStatus: SessionStatus): Mono<SessionResponse> {
        return sessionRepository.findById(sessionId)
            .map { doc ->
                doc.status = sessionStatus
                doc
            }.switchIfEmpty(
                Mono.error(
                    AssetUploadException(
                        "Can't find session: $sessionId for upload",
                        HttpStatus.NOT_FOUND
                    )
                )
            ).flatMap { sessionRepository.save(it) }
            .flatMap { toSessionResponse(it) }
    }

    private fun findSessionsAssets(sessionDocuments: Flux<SessionDocument>): Flux<SessionSummaryResponse> {
        return sessionDocuments.flatMap { session ->
            assetService.countAllBySessionId(sessionId = session.id!!)
                .defaultIfEmpty(0L)
                .map { cnt -> session to cnt }
        }.flatMap { (session, numberOfAssets) ->
            assetService.findLastAssetThumbnail320BySessionId(session.id!!)
                .map { it.storageKeyPath }
                .defaultIfEmpty("")
                .map { thumb320 ->
                    SessionSummaryResponse(
                        sessionId = session.id,
                        sessionMode = session.sessionMode,
                        ownerId = session.ownerId,
                        assetsCount = numberOfAssets.toInt(),
                        coverThumbnailUrl = if (thumb320.isEmpty()) null else thumb320,
                        sessionStatus = session.status,
                        createdAt = session.createdAt.toString()
                    )
                }
        }
    }

    private fun toSessionResponse(sessionDocument: SessionDocument): Mono<SessionResponse> =
        assetService.getAllAssetsBySessionId(sessionDocument.id!!)
            .map {
                SessionAsset(
                    id = it.id!!,
                    type = it.type.name,
                    status = it.status.name,
                    thumbnailUrl = it.thumbnails.find { it.size == ThumbnailSize.THUMB_320 }?.storageKeyPath
                )
            }
            .collectList()
            .map { list ->
                SessionResponse(
                    sessionId = sessionDocument.id!!,
                    mode = sessionDocument.sessionMode,
                    ownerId = sessionDocument.ownerId,
                    spotId = sessionDocument.spotId,
                    status = sessionDocument.status,
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