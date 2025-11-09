package com.emat.vehicle_collector_service.session.infra

import com.emat.vehicle_collector_service.session.domain.SessionStatus
import com.emat.vehicle_collector_service.session.domain.SessionMode
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class SessionDocument(
    @Id
    val id: String? = null,
    val sessionExternalId: String,
    val ownerId: String,
    val sessionMode: SessionMode,
    val spotId: String? = null,
    var status: SessionStatus = SessionStatus.CREATED,
    @CreatedDate
    var createdAt: Instant? = null,
    @LastModifiedDate
    var updatedAt: Instant? = null,
    @Version
    var version: Long? = null


)