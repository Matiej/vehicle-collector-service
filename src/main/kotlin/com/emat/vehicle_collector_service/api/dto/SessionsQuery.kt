package com.emat.vehicle_collector_service.api.dto

import jakarta.validation.constraints.Min
import org.springframework.data.domain.Sort

data class SessionsQuery(
    @field:Min(value = 0, message = "page must be 0 or greater")
    val page: Int = 0,
    @field:Min(value = 1, message = "size must be 1 or greater")
    val size: Int = 50,
    val sortDir: Sort.Direction = Sort.Direction.DESC
)
