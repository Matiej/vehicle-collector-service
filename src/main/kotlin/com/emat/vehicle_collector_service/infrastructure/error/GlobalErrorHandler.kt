package com.emat.vehicle_collector_service.infrastructure.error

import com.emat.vehicle_collector_service.assets.AssetUploadException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange

@RestControllerAdvice
class GlobalErrorHandler {
    private val log = LoggerFactory.getLogger(GlobalErrorHandler::class.java)

    @ExceptionHandler(AssetUploadException::class)
    fun handleAssetUpload(ex: AssetUploadException, exchange: ServerWebExchange): ResponseEntity<ApiError> {
        val api =
            ApiError(
                path = exchange.request.path.value(),
                status = ex.status.value(),
                error = ex.status.reasonPhrase,
                code = ex.code,
                message = ex.message
            )
        log.info(
            "Asset upload error: {} {} -> {} {}",
            exchange.request.method,
            exchange.request.path.value(),
            ex.status,
            ex.message
        )
        return ResponseEntity.status(ex.status).body(api)
    }

    @ExceptionHandler(WebExchangeBindException::class, BindException::class)
    fun handleValidation(ex: Exception, exchange: ServerWebExchange): ResponseEntity<ApiError> {
        val status = HttpStatus.BAD_REQUEST
        val msg = when (ex) {
            is WebExchangeBindException -> ex.allErrors.joinToString("; ") { it.defaultMessage ?: it.toString() }
            is BindException -> ex.allErrors.joinToString("; ") { it.defaultMessage ?: it.toString() }
            else -> "Validation failed"
        }
        val api = ApiError(
            path = exchange.request.path.value(),
            status = status.value(),
            error = status.reasonPhrase,
            code = "VALIDATION_ERROR",
            message = msg
        )
        return ResponseEntity.status(status).body(api)
    }

    @ExceptionHandler(ResponseStatusException::class, ErrorResponseException::class)
    fun handleSpringStatus(ex: Exception, exchange: ServerWebExchange): ResponseEntity<ApiError> {
        val status = when (ex) {
            is ResponseStatusException -> ex.statusCode
            is ErrorResponseException -> ex.statusCode
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        val api = ApiError(
            path = exchange.request.path.value(),
            status = status.value(),
            error = "RESPONSE ERROR",
            code = "ERROR",
            message = ex.message
        )
        return ResponseEntity.status(status).body(api)
    }

    @ExceptionHandler(Throwable::class)
    fun handleOther(ex: Throwable, exchange: ServerWebExchange): ResponseEntity<ApiError> {
        log.error("Unhandled error for {} {}:", exchange.request.method, exchange.request.path.value(), ex)
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        val api = ApiError(
            path = exchange.request.path.value(),
            status = status.value(),
            error = status.reasonPhrase,
            code = "INTERNAL_ERROR",
            message = "Unexpected server error"
        )
        return ResponseEntity.status(status).body(api)
    }
}