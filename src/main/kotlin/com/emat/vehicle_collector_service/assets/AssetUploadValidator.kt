package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.configuration.AppData
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path

@Component
class AssetUploadValidator(
    private val appData: AppData
) {
    init {
        val tmpDir = Path.of(appData.getTmpDir())
        if (Files.notExists(tmpDir)) {
            Files.createDirectories(tmpDir)
        }
    }

    fun assetUploadValidate(filePart: FilePart): Mono<ValidatedFile> {
        val originalFileName = filePart.filename()
        val fileExtension = originalFileName.substringAfterLast('.', "").lowercase()

        if (fileExtension !in allowedExtensions) {
            return Mono.error(
                AssetUploadException(
                    "Not supported extension: .$fileExtension",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE
                )
            )
        }

        val mime = filePart.headers().contentType?.toString()?.lowercase()
            ?: return Mono.error(AssetUploadException("No content type", HttpStatus.UNSUPPORTED_MEDIA_TYPE))

        if (mime !in allowedMime) {
            return Mono.error(
                AssetUploadException(
                    "Not supported Content-Type $mime",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE
                )
            )
        }

        val tmpFileMono = Mono.fromCallable {
            Files.createTempFile(Path.of(appData.getTmpDir()), "upload-", "-$originalFileName").toFile()
        }.subscribeOn(Schedulers.boundedElastic())

        return tmpFileMono.flatMap { tmp ->
            filePart.transferTo(tmp.toPath())
                .then(
                    Mono.fromCallable {
                        if (tmp.length() > MAX_BYTES) {
                            throw AssetUploadException("Max file size 10 MB", HttpStatus.PAYLOAD_TOO_LARGE)
                        }
                        val header = FileInputStream(tmp).use { it.readNBytes(HEADER_MAX_SCAN_BYTES) }
                        val looksValid = when {
                            mime == "image/jpeg" -> isJpeg(header)
                            mime == "image/png" -> isPng(header)
                            mime == "image/heic" || mime == "image/heif" -> isHeicFamily(header)
                            mime == "audio/mpeg" -> isMp3(header)
                            mime == "audio/mp4" || mime == "audio/x-m4a" || mime == "video/mp4" -> isMp4Family(header)
                            mime == "audio/wav" -> isWav(header)
                            else -> false
                        }
                        if (!looksValid) {
                            throw AssetUploadException(
                                "File signature is not equal Content-Type: $mime",
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE
                            )
                        }
                        ValidatedFile(tmp, mime, originalFileName, fileExtension)
                    }.subscribeOn(Schedulers.boundedElastic())
                ).onErrorResume { e ->
                    val mapped = if (e is AssetUploadException) e
                    else AssetUploadException("Cant validate file", HttpStatus.BAD_REQUEST)
                    Mono.fromCallable { tmp.delete() }
                        .subscribeOn(Schedulers.boundedElastic())
                        .then(Mono.error(mapped))
                }
        }
    }


    private fun isJpeg(h: ByteArray): Boolean =
        h.size >= 3 &&
                h[0] == 0xFF.toByte() &&
                h[1] == 0xD8.toByte() &&
                h[2] == 0xFF.toByte()

    private fun isPng(h: ByteArray): Boolean =
        h.size >= 8 &&
                h[0] == 0x89.toByte() &&
                h[1] == 0x50.toByte() && // 'P'
                h[2] == 0x4E.toByte() && // 'N'
                h[3] == 0x47.toByte() && // 'G'
                h[4] == 0x0D.toByte() &&
                h[5] == 0x0A.toByte() &&
                h[6] == 0x1A.toByte() &&
                h[7] == 0x0A.toByte()

    private fun isWav(h: ByteArray): Boolean =
        h.size >= 12 &&
                h[0] == 0x52.toByte() && // 'R'
                h[1] == 0x49.toByte() && // 'I'
                h[2] == 0x46.toByte() && // 'F'
                h[3] == 0x46.toByte() && // 'F'
                h[8] == 0x57.toByte() && // 'W'
                h[9] == 0x41.toByte() && // 'A'
                h[10] == 0x56.toByte() && // 'V'
                h[11] == 0x45.toByte()    // 'E'

    private fun isMp3(h: ByteArray): Boolean {
        if (h.size >= 3 &&
            h[0] == 0x49.toByte() && // 'I'
            h[1] == 0x44.toByte() && // 'D'
            h[2] == 0x33.toByte()    // '3'
        ) return true
        if (h.size >= 2 && h[0] == 0xFF.toByte()) {
            val b1 = h[1].toInt() and 0xE0
            if (b1 == 0xE0) return true
        }
        return false
    }

    private fun isMp4Family(h: ByteArray): Boolean {
        if (h.size < 12) return false
        for (i in 0..h.size - 8) {
            if (h[i] == 0x66.toByte() && // 'f'
                h[i + 1] == 0x74.toByte() && // 't'
                h[i + 2] == 0x79.toByte() && // 'y'
                h[i + 3] == 0x70.toByte()    // 'p'
            ) return true // np. mp41, mp42, isom, iso2, M4A
        }
        return false
    }

    private fun isHeicFamily(h: ByteArray): Boolean {
        if (h.size < 16) return false
        for (i in 0..h.size - 8) {
            if (h[i] == 0x66.toByte() && // 'f'
                h[i + 1] == 0x74.toByte() && // 't'
                h[i + 2] == 0x79.toByte() && // 'y'
                h[i + 3] == 0x70.toByte()    // 'p'
            ) {
                val brand = String(h.copyOfRange(i + 4, i + 8)).lowercase()
                return brand in setOf("heic", "heif", "mif1", "msf1")
            }
        }
        return false
    }

    data class ValidatedFile(
        val tmpFile: File,
        val mimeType: String,
        val originalFilename: String,
        val extension: String
    )

    companion object {
        private const val HEADER_MAX_SCAN_BYTES = 64
        private const val MAX_BYTES: Long = 10L * 1024 * 1024 // 10 MB

        private val allowedExtensions = setOf("jpg", "jpeg", "png", "heic", "mp3", "m4a", "wav", "mp4")
        private val allowedMime = setOf(
            "image/jpeg", "image/png", "image/heic", "image/heif",
            "audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/wav"
        )
    }
}