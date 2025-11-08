package com.emat.vehicle_collector_service.session.domain

enum class SessionStatus {
    CREATED,
    OPEN,           // trwa upload (można dodawać assety)
    CLOSED,         // zakończona, dane kompletne
    ERROR
}
