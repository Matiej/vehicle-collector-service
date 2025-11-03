package com.emat.vehicle_collector_service.assets

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.Metadata
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.exif.GpsDirectory
import com.drew.metadata.mov.QuickTimeDirectory
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory
import com.drew.metadata.mp4.Mp4Directory
import com.emat.vehicle_collector_service.assets.domain.ExifInfo

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.io.FileInputStream
import java.util.*


@Component
class ExifMetadataExtractor {

    fun extract(file: File, mimeType: String?): Mono<ExifInfo?> =
        Mono.fromCallable {
            mimeType?.let {
                when {
                    mimeType.startsWith("image/") -> extractFromImage(file, mimeType)
                    mimeType.startsWith("video/") -> extractFromMovOrMp4(file, mimeType)
                    else -> null
                }
            }
        }.subscribeOn(Schedulers.boundedElastic())

    private fun extractFromImage(file: File, mimeType: String): ExifInfo? {
        try {
            FileInputStream(file).use { ins ->
                val metadata: Metadata = ImageMetadataReader.readMetadata(ins)
                val subIfd: ExifSubIFDDirectory? = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
                val ifd0: ExifIFD0Directory? = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
                val gps: GpsDirectory? = metadata.getFirstDirectoryOfType(GpsDirectory::class.java)

                val date = subIfd?.dateOriginal
                val takenAt = date?.toInstant()
                val geo = gps?.geoLocation
                val lat = geo?.latitude
                val lng = geo?.longitude
                val make = ifd0?.getString(ExifIFD0Directory.TAG_MAKE)
                val model = ifd0?.getString(ExifIFD0Directory.TAG_MODEL)
                val camera = listOfNotNull(make, model).joinToString(" ").ifBlank { null }

                return if (takenAt != null || lat != null) {
                    ExifInfo(takenAt, lat, lng, camera)
                } else null
            }
        } catch (_: Exception) {
            return null
        }

    }

    private fun extractFromMovOrMp4(file: File, mimeType: String): ExifInfo? {
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

    private fun parseQuickTime(meta: Metadata): ExifInfo? {
        val quickTimeDir = meta.getFirstDirectoryOfType(QuickTimeDirectory::class.java)
        val metaDir = meta.getFirstDirectoryOfType(QuickTimeMetadataDirectory::class.java)

        // Data utworzenia (QuickTime)
        val takenAt = quickTimeDir?.getDate(QuickTimeDirectory.TAG_CREATION_TIME)?.toInstant()

        // GPS (format ISO 6709, np. "+52.2297+21.0122/")
        val iso = metaDir?.getString(QuickTimeMetadataDirectory.TAG_LOCATION_ISO6709)
        val (lat, lng) = parseIso6709(iso)

        val make = metaDir?.getString(QuickTimeMetadataDirectory.TAG_MAKE)
        val model = metaDir?.getString(QuickTimeMetadataDirectory.TAG_MODEL)
        val camera = listOfNotNull(make, model).joinToString(" ").ifBlank { null }

        return if (takenAt != null || lat != null || lng != null || camera != null)
            ExifInfo(takenAt, lat, lng, camera)
        else null
    }

    private fun parseMp4(meta: Metadata): ExifInfo? {
        val mp4 = meta.getFirstDirectoryOfType(Mp4Directory::class.java)
        val takenAt = mp4?.getDate(Mp4Directory.TAG_CREATION_TIME)?.toInstant()
        var latFromMp4: Double? = mp4?.getDouble(Mp4Directory.TAG_LATITUDE)
        var lngFromMp4: Double? = mp4?.getDouble(Mp4Directory.TAG_LONGITUDE)

        if (latFromMp4 == null && lngFromMp4 == null) {
            val iso = meta.getFirstDirectoryOfType(QuickTimeMetadataDirectory::class.java)
                ?.getString(QuickTimeMetadataDirectory.TAG_LOCATION_ISO6709)
            val (isoLat, isoLng) = parseIso6709(iso)
            if (isoLat != null || isoLng != null) {
                latFromMp4 = isoLat
                lngFromMp4 = isoLng
            }
        }

        val qtMeta = meta.getFirstDirectoryOfType(QuickTimeMetadataDirectory::class.java)
        val make = qtMeta?.getString(QuickTimeMetadataDirectory.TAG_MAKE)
        val model = qtMeta?.getString(QuickTimeMetadataDirectory.TAG_MODEL)
        val camera = listOfNotNull(make, model).joinToString(" ").ifBlank { null }

        return if (takenAt != null || latFromMp4 != null || lngFromMp4 != null) {
            ExifInfo(takenAt, latFromMp4, lngFromMp4, camera)
        } else null

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