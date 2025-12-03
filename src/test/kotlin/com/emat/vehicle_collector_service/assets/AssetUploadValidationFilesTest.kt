package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.configuration.AppData
import org.apache.commons.lang3.concurrent.UncheckedFuture.on
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import reactor.core.Exceptions
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AssetUploadValidationFilesTest {

    private var testPath: Path = Path.of(System.getProperty("user.dir")).resolve("tmp-test")
    private lateinit var tmpDir: Path
    private lateinit var maxFileSize: String
    private lateinit var appData: AppData
    private lateinit var validator: AssetUploadValidator

    @BeforeAll
    fun beforeAll() {
        tmpDir = Files.createDirectories(
            Path.of(System.getProperty("user.dir")).resolve("tmp-test").resolve("unit-files")
        )

        maxFileSize = "20"
    }

    @BeforeEach
    fun setup() {
        appData = mock<AppData> {
            on { getTmpDir() } doReturn tmpDir.toString()
            on { getMaxFileSize() } doReturn maxFileSize
        }
        validator = AssetUploadValidator(appData)
    }

    @AfterAll
    fun afterAll() {
        if (tmpDir.exists()) tmpDir.deleteIfExists()
        if (testPath.exists()) testPath.deleteIfExists()
    }

    @Test
    fun `MP3 file validates correctly`() {
        val fp = filePart("sample.mp3", "audio/mpeg")
        val result = validator.assetUploadValidate(fp, AssetType.AUDIO).getOrThrow()
        assertEquals("audio/mpeg", result.mimeType)
        assertEquals("mp3", result.extension)
        result.tmpFile.delete()
    }

    @Test
    fun `MP4 file validates correctly`() {
        val fp = filePart("sample.mp4", "audio/mp4")
        val result = validator.assetUploadValidate(fp, AssetType.AUDIO).getOrThrow()
        assertEquals("mp4", result.extension)
        result.tmpFile.delete()
    }

    @Test
    fun `JPEG file validates correctly`() {
        val fp = filePart("sample.jpg", "image/jpeg")
        val result = validator.assetUploadValidate(fp, AssetType.IMAGE).getOrThrow()
        assertEquals("image/jpeg", result.mimeType)
        assertEquals("jpg", result.extension)
        assertTrue(result.tmpFile.exists())
        result.tmpFile.delete()
    }

    @Test
    fun `PNG file validates correctly`() {
        val fp = filePart("sample.png", "image/png")
        val result = validator.assetUploadValidate(fp, AssetType.IMAGE).getOrThrow()
        assertEquals("image/png", result.mimeType)
        assertEquals("png", result.extension)
        result.tmpFile.delete()
    }

    @Test
    fun `HEIC file validates correctly`() {
        val fp = filePart("sample.heic", "image/heic")
        val result = validator.assetUploadValidate(fp, AssetType.IMAGE).getOrThrow()
        assertTrue(result.mimeType == "image/heic" || result.mimeType == "image/heif")
        assertEquals("heic", result.extension)
        result.tmpFile.delete()
    }

    @Test
    fun `Wrong MIME type should throw`() {
        val fp = filePart("sample.jpg", "image/png")
        val ex = assertThrows<AssetUploadException> { validator.assetUploadValidate(fp, AssetType.IMAGE).getOrThrow() }
        assertEquals(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.status)
    }

    @Test
    fun `Unsupported extension should throw`() {
        val badPart = TestFilePart("file.txt", headersOf("text/plain"), loadFileBytes("sample.jpg"))
        val ex = assertThrows<AssetUploadException> { validator.assetUploadValidate(badPart, AssetType.AUDIO).getOrThrow() }
        assertEquals(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.status)
    }

    @Test
    fun `Too large file should throw`() {
        val tooBigSize = 20 * 1024 * 1024 + 1   // = 10_485_761
        val bytes = ByteArray(tooBigSize) { 0 }
        bytes[0] = 0xFF.toByte()
        bytes[1] = 0xD8.toByte()
        bytes[2] = 0xFF.toByte()
        val part = TestFilePart("big.jpg", headersOf("image/jpeg"), bytes)
        val ex = assertThrows<AssetUploadException> { validator.assetUploadValidate(part, AssetType.IMAGE).getOrThrow() }
        assertEquals(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, ex.status)
    }

    private fun <T> Mono<T>.getOrThrow(): T =
        try {
            this.block() ?: error("Mono completed empty")
        } catch (e: Throwable) {
            throw Exceptions.unwrap(e)
        }

    private fun loadFileBytes(name: String): ByteArray =
        this::class.java.getResource("/assets/$name")?.readBytes()
            ?: error("Missing test file: /assets/$name")

    private fun headersOf(contentType: String): HttpHeaders =
        HttpHeaders().apply { this.contentType = MediaType.parseMediaType(contentType) }

    private fun filePart(name: String, mime: String): TestFilePart =
        TestFilePart(name, headersOf(mime), loadFileBytes(name))
}