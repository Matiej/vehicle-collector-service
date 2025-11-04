package com.emat.vehicle_collector_service.infrastructure.storage

import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Mono
import java.io.File

interface StorageService {
    fun store(filePart: FilePart, storageKeyPath: String): Mono<String>
    fun store(file: File, storageKeyPath: String): Mono<String>
    fun delete(storageKeyPath: String): Mono<Void>
}