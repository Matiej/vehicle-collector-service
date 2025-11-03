package com.emat.vehicle_collector_service.assets

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Files

import java.nio.file.Path

class TestFilePart(
    private val fileName: String,
    private val headers: HttpHeaders,
    private val fileBytes: ByteArray
)  : FilePart {
    override fun name(): String = fileName
    override fun headers(): org.springframework.http.HttpHeaders = headers
    override fun content(): Flux<DataBuffer> = Flux.empty()
    override fun filename(): String = fileName
    override fun transferTo(dest: Path): Mono<Void> {
        Files.createDirectories(dest.parent)
        Files.write(dest, fileBytes)
        return Mono.empty()
    }
}