package com.emat.vehicle_collector_service.session

import com.emat.vehicle_collector_service.api.internal.dto.CreateSessionRequest
import com.emat.vehicle_collector_service.api.internal.dto.SessionResponse
import com.emat.vehicle_collector_service.api.internal.dto.SessionSummaryResponse
import com.emat.vehicle_collector_service.assets.AssetUploadException
import com.emat.vehicle_collector_service.assets.AssetsService
import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.session.domain.SessionAsset
import com.emat.vehicle_collector_service.session.infra.SessionDocument
import com.emat.vehicle_collector_service.session.infra.SessionRepository
import org.springframework.data.domain.PageRequest
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


    override fun listSessions(ownerId: String, page: Int, size: Int): Flux<SessionSummaryResponse> =
        sessionRepository.findByOwnerId(ownerId, pageRequest(page, size))
            .flatMap { session ->
                assetService.countAllBySessionId(sessionId = session.id!!).map { cnt -> session to cnt }
            }.flatMap { (sessiion, numberOfAssets) ->
                assetService.findFirstAssetThumbnail320BySessionId(sessiion.id!!)
                    .map { thumb320->
                        SessionSummaryResponse(
                            sessionId = sessiion.id,
                            sessionMode = sessiion.sessionMode,
                            ownerId = sessiion.ownerId,
                            assetsCount = numberOfAssets,
                            coverThumbnailUrl = thumb320.storageKeyPath,
                            createdAt = sessiion.createdAt.toString()
                        )
                    }
            }

    private fun toSessionResponse(sessionDocument: SessionDocument): Mono<SessionResponse> =
        assetService.findBySessionId(sessionDocument.id!!)
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

    private fun pageRequest(page: Int, size: Int) = PageRequest.of(page, size)

    private fun genPublicId(prefix: String): String {
      val now = LocalDate.now()
      return "${prefix}_${now.year}_${now.month.value}_${UUID.randomUUID().toString().take(8)}"
    }
}