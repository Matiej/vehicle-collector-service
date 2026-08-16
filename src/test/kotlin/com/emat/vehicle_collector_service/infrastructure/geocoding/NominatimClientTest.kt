package com.emat.vehicle_collector_service.infrastructure.geocoding

import com.emat.vehicle_collector_service.assets.domain.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class NominatimClientTest {

    private val krakow = GeoPoint(50.06143, 19.93658)
    private val requests: MutableList<ClientRequest> = Collections.synchronizedList(mutableListOf())
    private val calls = AtomicInteger()

    @Test
    fun `maps the nominatim address onto a place`() {
        val geocoder = geocoder(responding(body(city = "Kraków", countryCode = "pl")))

        val place = geocoder.reverse(krakow).block()!!

        assertEquals("PL", place.countryCode)
        assertEquals("Polska", place.country)
        assertEquals("Kraków", place.city)
        assertEquals("małopolskie", place.region)
    }

    @Test
    fun `city falls back to town, village and municipality`() {
        val town = geocoder(responding(bodyWith(""""town": "Zakliczyn""""))).reverse(krakow).block()!!
        val village = geocoder(responding(bodyWith(""""village": "Wola Batorska""""))).reverse(krakow).block()!!
        val municipality =
            geocoder(responding(bodyWith(""""municipality": "Gmina Igołomia""""))).reverse(krakow).block()!!

        assertEquals("Zakliczyn", town.city)
        assertEquals("Wola Batorska", village.city)
        assertEquals("Gmina Igołomia", municipality.city)
    }

    @Test
    fun `sends the identifying headers and asks nominatim for lon, not lng`() {
        geocoder(responding(body())).reverse(krakow).block()

        val request = requests.single()
        assertEquals("vehicle-collector-service/test", request.headers().getFirst(HttpHeaders.USER_AGENT))
        assertEquals("pl", request.headers().getFirst(HttpHeaders.ACCEPT_LANGUAGE))
        assertTrue(request.url().query!!.contains("lat=50.06143"), request.url().toString())
        assertTrue(request.url().query!!.contains("lon=19.93658"), request.url().toString())
    }

    @Test
    fun `fifty photos from one spot cost a single request`() {
        val geocoder = geocoder(responding(body()))
        val spread = (1..50).map { GeoPoint(krakow.lat + it * 0.000001, krakow.lng + it * 0.000001) }

        val places = Flux.fromIterable(spread)
            .parallel()
            .runOn(Schedulers.parallel())
            .flatMap { geocoder.reverse(it) }
            .sequential()
            .collectList()
            .block()!!

        assertEquals(50, places.size)
        assertEquals(1, calls.get())
    }

    @Test
    fun `the cache is a grid, so neighbours across a cell boundary cost two requests`() {
        val geocoder = geocoder(responding(body()))

        geocoder.reverse(GeoPoint(50.06149, 19.93658)).block()
        geocoder.reverse(GeoPoint(50.06151, 19.93658)).block()

        assertEquals(2, calls.get())
    }

    @Test
    fun `a location further away than the cache precision is asked again`() {
        val geocoder = geocoder(responding(body()))

        geocoder.reverse(krakow).block()
        geocoder.reverse(GeoPoint(52.2297, 21.0122)).block()

        assertEquals(2, calls.get())
    }

    @Test
    fun `the rate limiter spaces requests out`() {
        val geocoder = geocoder(responding(body()), rateLimitPerSecond = 10)

        val startedAt = System.currentTimeMillis()
        (1..3).forEach { geocoder.reverse(GeoPoint(50.0 + it, 19.0 + it)).block() }
        val elapsed = System.currentTimeMillis() - startedAt

        assertEquals(3, calls.get())
        assertTrue(elapsed >= 200, "three calls at 10/s should take at least 200 ms, took $elapsed")
    }

    @Test
    fun `a failing provider yields no place instead of an error`() {
        val geocoder = geocoder(
            ExchangeFunction {
                calls.incrementAndGet()
                Mono.error(RuntimeException("nominatim is down"))
            }
        )

        assertNull(geocoder.reverse(krakow).block())
    }

    @Test
    fun `a response without an address yields no place`() {
        val geocoder = geocoder(responding("""{"licence": "ODbL"}"""))

        assertNull(geocoder.reverse(krakow).block())
    }

    @Test
    fun `a failed lookup is not cached as a miss`() {
        val failing = AtomicInteger()
        val geocoder = geocoder(
            ExchangeFunction { request ->
                calls.incrementAndGet()
                requests.add(request)
                if (failing.getAndIncrement() == 0) Mono.error(RuntimeException("boom"))
                else Mono.just(jsonResponse(body()))
            }
        )

        assertNull(geocoder.reverse(krakow).block())
        assertEquals("Kraków", geocoder.reverse(krakow).block()?.city)
    }

    @Test
    fun `disabled geocoding never touches the network`() {
        val geocoder = geocoder(responding(body()), enabled = false)

        assertNull(geocoder.reverse(krakow).block())
        assertEquals(0, calls.get())
    }

    private fun geocoder(
        exchange: ExchangeFunction,
        enabled: Boolean = true,
        rateLimitPerSecond: Int = 0
    ): NominatimClient =
        NominatimClient(
            GeocodingProperties(
                enabled = enabled,
                providerUrl = "https://nominatim.test/reverse",
                userAgent = "vehicle-collector-service/test",
                language = "pl",
                rateLimitPerSecond = rateLimitPerSecond,
                cachePrecision = 3,
                timeoutMs = 2000,
                maxRetries = 0,
                retryBackoffMs = 10
            ),
            WebClient.builder().exchangeFunction(exchange)
        )

    private fun responding(json: String): ExchangeFunction =
        ExchangeFunction { request ->
            calls.incrementAndGet()
            requests.add(request)
            Mono.just(jsonResponse(json))
        }

    private fun jsonResponse(json: String): ClientResponse =
        ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(json)
            .build()

    private fun body(city: String = "Kraków", countryCode: String = "pl"): String =
        """
        {
          "address": {
            "city": "$city",
            "state": "małopolskie",
            "country": "Polska",
            "country_code": "$countryCode"
          }
        }
        """.trimIndent()

    private fun bodyWith(cityField: String): String =
        """
        {
          "address": {
            $cityField,
            "state": "małopolskie",
            "country": "Polska",
            "country_code": "pl"
          }
        }
        """.trimIndent()
}
