package com.emat.vehicle_collector_service.assets.thumbnail

import com.emat.vehicle_collector_service.assets.domain.ThumbnailSize
import com.emat.vehicle_collector_service.assets.infra.AssetDocument
import com.emat.vehicle_collector_service.configuration.AppData
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Path

@Component
class ThumbnailFileResolver(private val appData: AppData) {

    fun serve(asset: AssetDocument, size: ThumbnailSize): ResponseEntity<Resource> {
        val thumb = asset.file.thumbnails.firstOrNull { it.size == size }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Thumbnail not ready yet")

        return serveFile(thumb.storageKeyPath, MediaType.IMAGE_JPEG, IMMUTABLE_CACHE, "Thumbnail file missing")
    }

    /**
     * HEIC without a thumbnail stays a 404: browsers cannot render it and on-demand HEIC->JPG
     * conversion is a separate task.
     */
    fun servePreview(asset: AssetDocument): ResponseEntity<Resource> {
        val thumb = asset.file.thumbnails.firstOrNull { it.size == ThumbnailSize.THUMB_640 }
        if (thumb != null) {
            return serveFile(thumb.storageKeyPath, MediaType.IMAGE_JPEG, IMMUTABLE_CACHE, "Preview file missing")
        }

        val browserSafeType = BROWSER_SAFE_ORIGINALS[asset.file.mimeType?.lowercase()]
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Preview not ready yet")

        return serveFile(asset.file.storageKeyPath, browserSafeType, REVALIDATE_CACHE, "Original file missing")
    }

    private fun serveFile(
        storageKeyPath: String,
        contentType: MediaType,
        cacheControl: String,
        missingMessage: String
    ): ResponseEntity<Resource> {
        val resource: Resource = FileSystemResource(
            Path.of(appData.getAssetsDir()).resolve(storageKeyPath)
        )
        if (!resource.exists()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, missingMessage)
        }

        return ResponseEntity.ok()
            .header("Cache-Control", cacheControl)
            .contentType(contentType)
            .body(resource)
    }

    companion object {
        private const val IMMUTABLE_CACHE = "private, max-age=31536000, immutable"
        private const val REVALIDATE_CACHE = "private, max-age=0, must-revalidate"
        private val BROWSER_SAFE_ORIGINALS = mapOf(
            "image/jpeg" to MediaType.IMAGE_JPEG,
            "image/jpg" to MediaType.IMAGE_JPEG,
            "image/png" to MediaType.IMAGE_PNG
        )
    }
}
