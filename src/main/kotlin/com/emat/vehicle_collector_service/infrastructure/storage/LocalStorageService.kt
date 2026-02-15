package com.emat.vehicle_collector_service.infrastructure.storage

import com.emat.vehicle_collector_service.assets.AssetUploadException
import com.emat.vehicle_collector_service.configuration.AppData
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


@Service
class LocalStorageService(
    private val appData: AppData
) : StorageService {

    private val log = LoggerFactory.getLogger(StorageService::class.java)

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

    override fun delete(storageKeyPath: String): Mono<Void> {
        val target = Path.of(appData.getAssetsDir()).resolve(storageKeyPath)
        val fileName = target.fileName
        return Mono.defer {
            try {
                log.debug("Trying to remove file: {}", fileName)
                if (!Files.deleteIfExists(target)) {
                    return@defer Mono.error<Void>(
                        AssetUploadException("File not found: $storageKeyPath", HttpStatus.NOT_FOUND, "FILE_NOT_FOUND")
                    )
                }
                log.debug("File deleted successfully: {}", fileName)
                Mono.empty()
            } catch (e: Exception) {
                Mono.error(
                    AssetUploadException(
                        "Can't delete file: $storageKeyPath",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "DELETE_FAILED"
                    )
                )
            }
        }.subscribeOn(Schedulers.boundedElastic())
    }
}
