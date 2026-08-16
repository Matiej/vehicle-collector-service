package com.emat.vehicle_collector_service.api.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PageResponseTest {

    @Test
    fun `total pages rounds up when the last page is partial`() {
        val page = PageResponse.of(content = listOf("a", "b"), page = 0, size = 2, totalElements = 5)

        assertEquals(3, page.totalPages)
        assertEquals(5, page.totalElements)
    }

    @Test
    fun `total pages is exact when the set divides evenly`() {
        val page = PageResponse.of(content = listOf("a", "b"), page = 1, size = 2, totalElements = 4)

        assertEquals(2, page.totalPages)
    }

    @Test
    fun `empty set has no pages`() {
        val page = PageResponse.of(content = emptyList<String>(), page = 0, size = 50, totalElements = 0)

        assertEquals(0, page.totalPages)
        assertEquals(0, page.totalElements)
    }

    @Test
    fun `total pages does not depend on how full the current page is`() {
        val lastPage = PageResponse.of(content = listOf("e"), page = 2, size = 2, totalElements = 5)

        assertEquals(3, lastPage.totalPages)
        assertEquals(5, lastPage.totalElements)
    }
}
