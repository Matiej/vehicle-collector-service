package com.emat.vehicle_collector_service.api.web.dto

import jakarta.validation.constraints.Min

data class PhotoListQuery(
    @field:Min(value = 0, message = "page must be 0 or greater")
    val page: Int = 0,
    @field:Min(value = 1, message = "size must be 1 or greater")
    val size: Int = 50,
    val badge: String? = null,
    val sessionPublicId: String? = null,
    val unassigned: Boolean = false,
    val q: String? = null,
    val favorite: Boolean = false,
    val sort: String = "createdAt,desc",
    val ownerId: String? = null
)
