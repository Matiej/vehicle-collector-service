package com.emat.vehicle_collector_service.assets

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest

data class FileMetadata(
    val sha256: String,
    val sizeBytes: Long
)

@Component
class FileMetadataReader {

    fun read(file: File): Mono<FileMetadata> =
        Mono.fromCallable { FileMetadata(sha256 = sha256(file), sizeBytes = file.length()) }
            .subscribeOn(Schedulers.boundedElastic())

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance(DIGEST_ALGORITHM)
        DigestInputStream(file.inputStream().buffered(), digest).use { stream ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (stream.read(buffer) != -1) {
                continue
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        private const val DIGEST_ALGORITHM = "SHA-256"
        private const val BUFFER_BYTES = 8 * 1024
    }
}
