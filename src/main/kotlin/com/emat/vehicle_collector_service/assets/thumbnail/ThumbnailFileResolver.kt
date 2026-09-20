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

        val resource: Resource = FileSystemResource(
            Path.of(appData.getAssetsDir()).resolve(thumb.storageKeyPath)
        )
        if (!resource.exists()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Thumbnail file missing")
        }

        return ResponseEntity.ok()
            .header("Cache-Control", "private, max-age=31536000, immutable")
            .contentType(MediaType.IMAGE_JPEG)
            .body(resource)
    }
}
