package com.emat.vehicle_collector_service.assets.infra

import com.emat.vehicle_collector_service.assets.domain.TitleSource

data class CurationInfo(
    val title: String? = null,
    val titleSource: TitleSource? = null,
    val favorite: Boolean = false,
    val notes: String? = null,
    val externalInfo: List<ExternalInfo> = emptyList(),
    val albumIds: List<String> = emptyList()
)

data class ExternalInfo(
    val type: String,
    val label: String,
    val url: String? = null,
    val text: String? = null,
    val source: String? = null
)
