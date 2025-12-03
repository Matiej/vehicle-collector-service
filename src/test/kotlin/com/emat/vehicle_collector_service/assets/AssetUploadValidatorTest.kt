package com.emat.vehicle_collector_service.assets

import com.emat.vehicle_collector_service.assets.domain.AssetType
import com.emat.vehicle_collector_service.configuration.AppData
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import reactor.core.Exceptions
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.nio.file.Path

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AssetUploadValidatorTest {
    val TEST_PATH = Path.of(System.getProperty("user.dir"), "tmp-test")
    private lateinit var tmpDir: Path
    private lateinit var appData: AppData
    private lateinit var validator: AssetUploadValidator

    @BeforeAll
    fun beforeAll(): Unit {

        if (Files.notExists(TEST_PATH)) {
            Files.createDirectories(TEST_PATH)
        }
    }

    @BeforeEach
    fun setup() {
        tmpDir = Files.createTempDirectory(
            TEST_PATH,
            "validator-test-"
        )

        appData = mock<AppData> {
            on { getTmpDir() } doReturn tmpDir.toString()
            on { getMaxFileSize() } doReturn "20"
        }
        validator = AssetUploadValidator(appData)
    }

    @AfterEach
    fun tearDown() {
        Files.walk(tmpDir)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }
    }

    @AfterAll
    fun afterAll() {
        if (Files.notExists(TEST_PATH)) {
            Files.delete(TEST_PATH)
        }
    }

    @Test
    fun `jpeg OK`() {
        //given
        val fp = filePartMock("img.jpg", "image/jpeg", AssetUploadValidatorFixtures.JPEG_magicBytes)

        //when
        val validated = validator.assetUploadValidate(fp, AssetType.IMAGE).getOrThrow()

        //then
        assertEquals("image/jpeg", validated.mimeType)
        assertEquals("img.jpg", validated.originalFilename)
        assertEquals("jpg", validated.extension)
        assertTrue(validated.tmpFile.exists(), "tmp file should exist on success")

        validated.tmpFile.delete()
    }

    @Test
    fun `mp4 OK by ftyp`() {
        //given
        val fp = filePartMock("clip.mp4", "audio/mp4", AssetUploadValidatorFixtures.MP4_magicBytes)

        //when
        val validated = validator.assetUploadValidate(fp, AssetType.AUDIO).getOrThrow()

        //then
        assertEquals("audio/mp4", validated.mimeType)
        assertEquals("mp4", validated.extension)
        assertTrue(validated.tmpFile.exists())
        validated.tmpFile.delete()
    }

    @Test
    fun `heic OK (ftyp heic)`() {
        //given
        val fp = filePartMock("photo.heic", "image/heic", AssetUploadValidatorFixtures.HEIC_magicBytes)

        //when
        val validated = validator.assetUploadValidate(fp, AssetType.IMAGE).getOrThrow()

        //then
        assertEquals("image/heic", validated.mimeType)
        assertEquals("heic", validated.extension)
        assertTrue(validated.tmpFile.exists())
        validated.tmpFile.delete()
    }

    @Test
    fun `mismatch signature vs content-type - reject`() {
        val fp = filePartMock("img.png", "image/png", AssetUploadValidatorFixtures.JPEG_magicBytes)
        val ex = assertThrows(AssetUploadException::class.java) {
            validator.assetUploadValidate(fp, AssetType.AUDIO).getOrThrow()
        }
        assertEquals(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.status)
    }

    @Test
    fun `too big - reject`() {
        val big = randomBytes((20 * 1024 * 1024) + 1) // 10MB + 1 bite
        val fp = filePartMock("img.jpg", "image/jpeg", big)
        val ex = assertThrows(AssetUploadException::class.java) {
            validator.assetUploadValidate(fp, AssetType.IMAGE).getOrThrow()
        }
        assertEquals(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, ex.status)
    }

    @Test
    fun `bad extension - reject`() {
        val fp = filePartMock("evil.exe", "application/octet-stream", randomBytes(32))
        val ex = assertThrows(AssetUploadException::class.java) {
            validator.assetUploadValidate(fp, AssetType.AUDIO).getOrThrow()
        }
        assertEquals(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.status)
    }

    @Test
    fun `bad content-type - reject`() {
        //given
        val fp = filePartMock("img.jpg", "application/pdf", AssetUploadValidatorFixtures.JPEG_magicBytes)
        val ex = assertThrows(AssetUploadException::class.java) {
            validator.assetUploadValidate(fp, AssetType.IMAGE).getOrThrow()
        }

        //when then
        assertEquals(org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.status)
    }

    private fun randomBytes(n: Int) = ByteArray(n) { 0x7F }

    private fun filePartMock(
        filename: String,
        contentType: String,
        bytes: ByteArray
    ): FilePart {
        val fp = mock<FilePart>()
        whenever(fp.filename()).thenReturn(filename)
        whenever(fp.headers()).thenReturn(headersOf(contentType))
        whenever(fp.transferTo(any<Path>()))
            .thenAnswer { invocation ->
                val target: Path = invocation.arguments[0] as Path
                Files.createDirectories(target.parent ?: tmpDir)
                Files.write(target, bytes)
                Mono.empty<Void>()
            }
        return fp
    }

    private fun <T> Mono<T>.getOrThrow(): T =
        try {
            this.block() ?: error("Mono completed empty")
        } catch (e: Throwable) {
            throw Exceptions.unwrap(e)
        }

    private fun headersOf(contentType: String): HttpHeaders =
        HttpHeaders().apply { this.contentType = MediaType.parseMediaType(contentType) }

}