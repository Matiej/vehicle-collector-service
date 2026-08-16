package com.emat.vehicle_collector_service.api.dto

import com.emat.vehicle_collector_service.assets.domain.AssetType
import org.springframework.data.domain.Sort

data class AssetsOwnerQuery(
    val type: AssetType? = null,
    val page: Int = 0,
    val size: Int = 50,
    val sortDir: Sort.Direction = Sort.Direction.DESC
) {
}