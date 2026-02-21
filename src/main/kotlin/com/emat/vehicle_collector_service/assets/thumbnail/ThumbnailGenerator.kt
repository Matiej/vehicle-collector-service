package com.emat.vehicle_collector_service.assets.thumbnail

import com.emat.vehicle_collector_service.configuration.AppData
import net.coobird.thumbnailator.Thumbnails
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class ThumbnailGenerator(
    private val appData: AppData
) {
    fun generate(inputPath: Path, outputPath: Path, maxDimension: Int) {
        Files.createDirectories(outputPath.parent)
        Thumbnails.of(inputPath.toFile())
            .size(maxDimension, maxDimension)
            .outputQuality(appData.getThumbnailsQuality())
            .toFile(outputPath.toFile())
    }
}
