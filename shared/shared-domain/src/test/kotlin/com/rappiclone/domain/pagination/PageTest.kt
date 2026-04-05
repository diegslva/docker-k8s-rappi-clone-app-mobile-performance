package com.rappiclone.domain.pagination

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PageTest {

    @Test
    fun `PageRequest deve ter defaults corretos`() {
        val request = PageRequest()
        assertEquals(1, request.page)
        assertEquals(20, request.size)
        assertEquals(0L, request.offset)
        assertEquals(SortDirection.ASC, request.sortDirection)
    }

    @Test
    fun `PageRequest offset deve calcular corretamente`() {
        assertEquals(0L, PageRequest(page = 1, size = 20).offset)
        assertEquals(20L, PageRequest(page = 2, size = 20).offset)
        assertEquals(40L, PageRequest(page = 3, size = 20).offset)
        assertEquals(50L, PageRequest(page = 2, size = 50).offset)
    }

    @Test
    fun `PageRequest deve rejeitar page menor que 1`() {
        assertThrows(IllegalArgumentException::class.java) { PageRequest(page = 0) }
        assertThrows(IllegalArgumentException::class.java) { PageRequest(page = -1) }
    }

    @Test
    fun `PageRequest deve rejeitar size fora do range`() {
        assertThrows(IllegalArgumentException::class.java) { PageRequest(size = 0) }
        assertThrows(IllegalArgumentException::class.java) { PageRequest(size = 101) }
    }

    @Test
    fun `PageRequest deve aceitar limites de size`() {
        assertDoesNotThrow { PageRequest(size = 1) }
        assertDoesNotThrow { PageRequest(size = 100) }
    }

    @Test
    fun `Page of deve calcular totalPages corretamente`() {
        val request = PageRequest(page = 1, size = 10)

        val page1 = Page.of(listOf(1, 2, 3), request, totalItems = 25)
        assertEquals(3, page1.totalPages)

        val page2 = Page.of(listOf(1, 2), request, totalItems = 20)
        assertEquals(2, page2.totalPages)

        val page3 = Page.of(listOf(1), request, totalItems = 1)
        assertEquals(1, page3.totalPages)

        val page4 = Page.of(emptyList<Int>(), request, totalItems = 0)
        assertEquals(0, page4.totalPages)
    }

    @Test
    fun `Page map deve transformar items mantendo metadata`() {
        val request = PageRequest(page = 1, size = 10)
        val page = Page.of(listOf(1, 2, 3), request, totalItems = 3)

        val mapped = page.map { it * 2 }

        assertEquals(listOf(2, 4, 6), mapped.items)
        assertEquals(1, mapped.page)
        assertEquals(10, mapped.size)
        assertEquals(3, mapped.totalItems)
    }

    @Test
    fun `Page empty deve retornar pagina vazia com metadata`() {
        val request = PageRequest(page = 1, size = 20)
        val empty = Page.empty<String>(request)

        assertTrue(empty.items.isEmpty())
        assertEquals(0, empty.totalItems)
        assertEquals(0, empty.totalPages)
        assertEquals(1, empty.page)
        assertEquals(20, empty.size)
    }
}
