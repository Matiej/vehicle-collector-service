package com.emat.vehicle_collector_service.assets

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExifMetadataExtractorTest {

    @TempDir
    lateinit var tmpDir: Path

    private val extractor = ExifMetadataExtractor()

    @Test
    fun `JPEG gives image dimensions`() {
        val result = extract("sample.jpg", "image/jpeg")

        assertNotNull(result)
        assertEquals(2048, result.width)
        assertEquals(1536, result.height)
    }

    @Test
    fun `HEIC gives image dimensions despite no JDK reader`() {
        val result = extract("sample.heic", "image/heic")

        assertNotNull(result)
        assertEquals(2048, result.width)
        assertEquals(1536, result.height)
    }

    @Test
    fun `PNG gives image dimensions`() {
        val result = extract("sample.png", "image/png")

        assertNotNull(result)
        assertEquals(1200, result.width)
        assertEquals(900, result.height)
    }

    @Test
    fun `file without dimension metadata gives null instead of throwing`() {
        val file = tmpDir.resolve("garbage.jpg").toFile()
        file.writeBytes(ByteArray(64) { 0 })

        val result = extractor.extract(file, "image/jpeg").block()

        assertNull(result)
    }

    @Test
    fun `capture block survives alongside dimensions`() {
        val result = extract("sample.jpg", "image/jpeg")

        assertNotNull(result)
        assertNotNull(result.capture)
    }

    @Test
    fun `audio has no dimensions`() {
        val result = extractor.extract(copyToTmp("sample.mp3"), "audio/mpeg").block()

        assertNull(result)
    }

    private fun extract(name: String, mime: String): ExtractedMetadata? =
        extractor.extract(copyToTmp(name), mime).block()

    private fun copyToTmp(name: String): File {
        val bytes = this::class.java.getResource("/assets/$name")?.readBytes()
            ?: error("Missing test file: /assets/$name")
        val file = tmpDir.resolve(name).toFile()
        file.writeBytes(bytes)
        return file
    }
}
