package com.emat.vehicle_collector_service.assets

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FileMetadataReaderTest {

    @TempDir
    lateinit var tmpDir: Path

    private val reader = FileMetadataReader()

    @Test
    fun `image hash matches sha256sum from the console`() {
        val result = read("sample.jpg")

        assertNotNull(result)
        assertEquals("0153cfff78fc38bb7ec85879549f9c2bd4a6b3ff363c93927ad8cb85f1faabe6", result.sha256)
        assertEquals(509768, result.sizeBytes)
    }

    @Test
    fun `audio is hashed the same way as an image`() {
        val result = read("sample.mp3")

        assertNotNull(result)
        assertEquals("4123fb6d82c048bd52b2fdb1fdfa5612ecef864aeee12c076100ff8e9373b287", result.sha256)
        assertEquals(38880, result.sizeBytes)
    }

    @Test
    fun `hash is lowercase hex of 64 characters`() {
        val result = read("sample.png")

        assertNotNull(result)
        assertTrue(result.sha256.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun `same content gives the same hash`() {
        val first = read("sample.jpg")
        val second = read("sample.jpg")

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(first.sha256, second.sha256)
    }

    @Test
    fun `20 MB file is hashed without loading it into memory`() {
        val file = tmpDir.resolve("big.jpg").toFile()
        val chunk = ByteArray(1024 * 1024) { it.toByte() }
        file.outputStream().buffered().use { out -> repeat(20) { out.write(chunk) } }

        val result = reader.read(file).block()

        assertNotNull(result)
        assertEquals(20L * 1024 * 1024, result.sizeBytes)
        assertTrue(result.sha256.matches(Regex("[0-9a-f]{64}")))
    }

    private fun read(name: String): FileMetadata? =
        reader.read(copyToTmp(name)).block()

    private fun copyToTmp(name: String): File {
        val bytes = this::class.java.getResource("/assets/$name")?.readBytes()
            ?: error("Missing test file: /assets/$name")
        val file = tmpDir.resolve(name).toFile()
        file.writeBytes(bytes)
        return file
    }
}
