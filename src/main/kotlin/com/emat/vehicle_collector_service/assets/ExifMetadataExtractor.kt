package com.emat.vehicle_collector_service.assets

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Directory
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifDirectoryBase
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.heif.HeifDirectory
import com.drew.metadata.jpeg.JpegDirectory
import com.drew.metadata.mov.QuickTimeDirectory
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory
import com.drew.metadata.mp4.Mp4Directory
import com.drew.metadata.png.PngDirectory
import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import com.emat.vehicle_collector_service.assets.infra.CameraInfo
import com.emat.vehicle_collector_service.assets.infra.CaptureInfo

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.io.FileInputStream
import java.time.Instant
import java.util.*


data class ExtractedMetadata(
    val capture: CaptureInfo = CaptureInfo(),
    val width: Int? = null,
    val height: Int? = null
)

@Component
class ExifMetadataExtractor {

    fun extract(file: File, mimeType: String?): Mono<ExtractedMetadata> =
        Mono.defer { Mono.justOrEmpty(extractBlocking(file, mimeType)) }
            .subscribeOn(Schedulers.boundedElastic())

    private fun extractBlocking(file: File, mimeType: String?): ExtractedMetadata? =
        mimeType?.let {
            when {
                mimeType.startsWith("image/") -> extractFromImage(file)
                mimeType.startsWith("video/") -> extractFromMovOrMp4(file, mimeType)?.let(::extractedMetadata)
                else -> null
            }
        }

    private fun extractFromImage(file: File): ExtractedMetadata? {
        try {
            FileInputStream(file).use { ins ->
                val metadata: Metadata = ImageMetadataReader.readMetadata(ins)
                val subIfd: ExifSubIFDDirectory? = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
                val ifd0: ExifIFD0Directory? = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
                val gps: GpsDirectory? = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)

                val takenAt = subIfd?.dateOriginal?.toInstant()
                val geo = gps?.geoLocation
                val camera = cameraInfo(
                    make = ifd0?.getString(ExifDirectoryBase.TAG_MAKE),
                    model = ifd0?.getString(ExifDirectoryBase.TAG_MODEL),
                    lens = subIfd?.getString(ExifDirectoryBase.TAG_LENS_MODEL),
                    iso = subIfd?.getInteger(ExifDirectoryBase.TAG_ISO_EQUIVALENT),
                    exposure = subIfd?.getDescription(ExifDirectoryBase.TAG_EXPOSURE_TIME),
                    fNumber = subIfd?.getDoubleObject(ExifDirectoryBase.TAG_FNUMBER),
                    focalLength = subIfd?.getDoubleObject(ExifDirectoryBase.TAG_FOCAL_LENGTH)
                )

                return extractedMetadata(
                    captureInfo(takenAt, geo?.latitude, geo?.longitude, camera),
                    imageDimensions(metadata)
                )
            }
        } catch (_: Exception) {
            return null
        }

    }

    private fun imageDimensions(metadata: Metadata): Dimensions? =
        dimensions(metadata, HeifDirectory::class.java, HeifDirectory.TAG_IMAGE_WIDTH, HeifDirectory.TAG_IMAGE_HEIGHT)
            ?: dimensions(
                metadata,
                JpegDirectory::class.java,
                JpegDirectory.TAG_IMAGE_WIDTH,
                JpegDirectory.TAG_IMAGE_HEIGHT
            )
            ?: dimensions(metadata, PngDirectory::class.java, PngDirectory.TAG_IMAGE_WIDTH, PngDirectory.TAG_IMAGE_HEIGHT)
            ?: dimensions(
                metadata,
                ExifSubIFDDirectory::class.java,
                ExifDirectoryBase.TAG_EXIF_IMAGE_WIDTH,
                ExifDirectoryBase.TAG_EXIF_IMAGE_HEIGHT
            )

    private fun <T : Directory> dimensions(
        metadata: Metadata,
        directoryType: Class<T>,
        widthTag: Int,
        heightTag: Int
    ): Dimensions? =
        metadata.getDirectoriesOfType(directoryType)
            .asSequence()
            .mapNotNull { directory ->
                val width = directory.getInteger(widthTag)
                val height = directory.getInteger(heightTag)
                if (width != null && height != null && width > 0 && height > 0) Dimensions(width, height) else null
            }
            .firstOrNull()

    private fun extractedMetadata(capture: CaptureInfo?, dimensions: Dimensions? = null): ExtractedMetadata? =
        if (capture == null && dimensions == null) null
        else ExtractedMetadata(
            capture = capture ?: CaptureInfo(),
            width = dimensions?.width,
            height = dimensions?.height
        )

    private data class Dimensions(val width: Int, val height: Int)

    private fun extractFromMovOrMp4(file: File, mimeType: String): CaptureInfo? {
        val filename = file.name.lowercase(Locale.ROOT)
        val isMov = mimeType.contains("quicktime") || filename.endsWith(".mov")
        val isMp4 = mimeType.contains("mp4") || filename.endsWith(".mp4")
        if (!isMov && !isMp4) return null

        try {
            val metadata: Metadata =
                FileInputStream(file).use { ins -> ImageMetadataReader.readMetadata(ins) }
            return if (isMov) parseQuickTime(metadata) else parseMp4(metadata)
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseQuickTime(meta: Metadata): CaptureInfo? {
        val quickTimeDir = meta.getFirstDirectoryOfType(QuickTimeDirectory::class.java)
        val metaDir = meta.getFirstDirectoryOfType(QuickTimeMetadataDirectory::class.java)

        // Data utworzenia (QuickTime)
        val takenAt = quickTimeDir?.getDate(QuickTimeDirectory.TAG_CREATION_TIME)?.toInstant()

        // GPS (format ISO 6709, np. "+52.2297+21.0122/")
        val iso = metaDir?.getString(QuickTimeMetadataDirectory.TAG_LOCATION_ISO6709)
        val (lat, lng) = parseIso6709(iso)

        return captureInfo(takenAt, lat, lng, quickTimeCamera(metaDir))
    }

    private fun parseMp4(meta: Metadata): CaptureInfo? {
        val mp4 = meta.getFirstDirectoryOfType(Mp4Directory::class.java)
        val takenAt = mp4?.getDate(Mp4Directory.TAG_CREATION_TIME)?.toInstant()
        var latFromMp4: Double? = mp4?.getDouble(Mp4Directory.TAG_LATITUDE)
        var lngFromMp4: Double? = mp4?.getDouble(Mp4Directory.TAG_LONGITUDE)

        val qtMeta = meta.getFirstDirectoryOfType(QuickTimeMetadataDirectory::class.java)

        if (latFromMp4 == null && lngFromMp4 == null) {
            val iso = qtMeta?.getString(QuickTimeMetadataDirectory.TAG_LOCATION_ISO6709)
            val (isoLat, isoLng) = parseIso6709(iso)
            if (isoLat != null || isoLng != null) {
                latFromMp4 = isoLat
                lngFromMp4 = isoLng
            }
        }

        return captureInfo(takenAt, latFromMp4, lngFromMp4, quickTimeCamera(qtMeta))
    }

    private fun quickTimeCamera(metaDir: QuickTimeMetadataDirectory?): CameraInfo? =
        cameraInfo(
            make = metaDir?.getString(QuickTimeMetadataDirectory.TAG_MAKE),
            model = metaDir?.getString(QuickTimeMetadataDirectory.TAG_MODEL),
            lens = null,
            iso = null,
            exposure = null,
            fNumber = null,
            focalLength = null
        )

    private fun cameraInfo(
        make: String?,
        model: String?,
        lens: String?,
        iso: Int?,
        exposure: String?,
        fNumber: Double?,
        focalLength: Double?
    ): CameraInfo? {
        val camera = CameraInfo(
            make = make?.trim()?.ifBlank { null },
            model = model?.trim()?.ifBlank { null },
            lens = lens?.trim()?.ifBlank { null },
            iso = iso,
            exposure = exposure?.trim()?.ifBlank { null },
            fNumber = fNumber,
            focalLength = focalLength
        )
        return if (camera == CameraInfo()) null else camera
    }

    private fun captureInfo(
        takenAt: Instant?,
        lat: Double?,
        lng: Double?,
        camera: CameraInfo?
    ): CaptureInfo? {
        val exifGps = if (lat != null && lng != null) GeoPoint(lat, lng) else null
        return if (takenAt == null && exifGps == null && camera == null) null
        else CaptureInfo(takenAt = takenAt, exifGps = exifGps, camera = camera)
    }

    private fun parseIso6709(value: String?): Pair<Double?, Double?> {
        if (value.isNullOrBlank()) return null to null
        // Przykład: "+52.2297+021.0122/"
        val regex = Regex("([+-]\\d+\\.\\d+)([+-]\\d+\\.\\d+)")
        val match = regex.find(value) ?: return null to null
        val lat = match.groupValues[1].toDoubleOrNull()
        val lng = match.groupValues[2].toDoubleOrNull()
        return lat to lng
    }
}
