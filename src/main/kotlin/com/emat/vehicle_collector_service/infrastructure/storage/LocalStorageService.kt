package com.emat.vehicle_collector_service.infrastructure.storage

import com.emat.vehicle_collector_service.configuration.AppData
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


@Service
class LocalStorageService(
    private val appData: AppData
) : StorageService {

    init {
        val path = Path.of(appData.getAssetsDir())
        if (Files.notExists(path)) {
            Files.createDirectories(path)
        }
    }

    override fun store(filePart: FilePart, storageKeyPath: String): Mono<String> {
        val path = Path.of(appData.getAssetsDir())
        val target = path.resolve(storageKeyPath)
        val parent = target.parent
        Files.createDirectories(parent)
        return filePart.transferTo(target)
            .thenReturn(storageKeyPath)
    }

    override fun store(file: File, storageKeyPath: String): Mono<String> {
        val path = Path.of(appData.getAssetsDir())
        val target = path.resolve(storageKeyPath)
        val parent = target.parent
        Files.createDirectories(parent)

        return Mono.fromCallable {
            file.copyTo(target.toFile(), overwrite = true)
            storageKeyPath
        }
    }
}