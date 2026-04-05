package com.rappiclone.search.service

import com.rappiclone.domain.enums.StoreCategory
import com.rappiclone.search.index.SearchIndex
import com.rappiclone.search.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SearchServiceTest {

    private val searchIndex = mockk<SearchIndex>()
    private val service = SearchService(searchIndex)
    private val tenantId = "londrina-pr"

    private val storeResult = StoreSearchResult(
        id = "store-1", name = "Padaria Central", slug = "padaria-central",
        category = StoreCategory.BAKERY, logoUrl = null,
        neighborhood = "Centro", city = "Londrina",
        ratingAvg = 4.5, ratingCount = 120, avgPrepTime = 20,
        minOrderValue = 15.0, isOpen = true, distanceKm = 1.2, score = 0.95
    )

    private val productResult = ProductSearchResult(
        id = "prod-1", name = "Pao Frances", storeId = "store-1",
        storeName = "Padaria Central", price = 0.75,
        imageUrl = null, isAvailable = true, score = 0.88
    )

    @BeforeEach
    fun setup() = clearAllMocks()

    @Test
    fun `search deve retornar stores e products`() = runTest {
        coEvery { searchIndex.searchStores(any(), tenantId, any(), any(), any(), any(), 0, 20) } returns
            Pair(1L, listOf(storeResult))
        coEvery { searchIndex.searchProducts(any(), tenantId, 0, 20) } returns
            Pair(1L, listOf(productResult))

        val result = service.search(SearchRequest(query = "padaria", tenantId = tenantId))

        assertEquals("padaria", result.query)
        assertEquals(2, result.totalResults)
        assertEquals(1, result.stores.size)
        assertEquals("Padaria Central", result.stores[0].name)
        assertEquals(1, result.products.size)
        assertEquals("Pao Frances", result.products[0].name)
    }

    @Test
    fun `search sem resultados deve retornar listas vazias`() = runTest {
        coEvery { searchIndex.searchStores(any(), any(), any(), any(), any(), any(), any(), any()) } returns Pair(0L, emptyList())
        coEvery { searchIndex.searchProducts(any(), any(), any(), any()) } returns Pair(0L, emptyList())

        val result = service.search(SearchRequest(query = "inexistente", tenantId = tenantId))

        assertEquals(0, result.totalResults)
        assertTrue(result.stores.isEmpty())
        assertTrue(result.products.isEmpty())
    }

    @Test
    fun `search deve calcular offset corretamente`() = runTest {
        coEvery { searchIndex.searchStores(any(), any(), any(), any(), any(), any(), any(), any()) } returns Pair(0L, emptyList())
        coEvery { searchIndex.searchProducts(any(), any(), any(), any()) } returns Pair(0L, emptyList())

        service.search(SearchRequest(query = "pizza", tenantId = tenantId, page = 3, size = 10))

        coVerify { searchIndex.searchStores("pizza", tenantId, null, null, 10.0, null, 20, 10) }
    }

    @Test
    fun `search deve rejeitar query vazio`() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.search(SearchRequest(query = "", tenantId = tenantId))
            }
        }
    }

    @Test
    fun `search deve rejeitar page menor que 1`() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.search(SearchRequest(query = "pizza", tenantId = tenantId, page = 0))
            }
        }
    }

    @Test
    fun `search deve rejeitar size fora do range`() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.search(SearchRequest(query = "pizza", tenantId = tenantId, size = 101))
            }
        }
    }

    @Test
    fun `search deve passar filtro de categoria`() = runTest {
        coEvery { searchIndex.searchStores(any(), any(), any(), any(), any(), "BAKERY", any(), any()) } returns Pair(1L, listOf(storeResult))
        coEvery { searchIndex.searchProducts(any(), any(), any(), any()) } returns Pair(0L, emptyList())

        service.search(SearchRequest(query = "padaria", tenantId = tenantId, category = StoreCategory.BAKERY))

        coVerify { searchIndex.searchStores("padaria", tenantId, null, null, 10.0, "BAKERY", 0, 20) }
    }

    @Test
    fun `search deve passar coordenadas geo`() = runTest {
        coEvery { searchIndex.searchStores(any(), any(), -23.3045, -51.1696, 5.0, any(), any(), any()) } returns Pair(0L, emptyList())
        coEvery { searchIndex.searchProducts(any(), any(), any(), any()) } returns Pair(0L, emptyList())

        service.search(SearchRequest(
            query = "pizza", tenantId = tenantId,
            latitude = -23.3045, longitude = -51.1696, radiusKm = 5.0
        ))

        coVerify { searchIndex.searchStores("pizza", tenantId, -23.3045, -51.1696, 5.0, null, 0, 20) }
    }

    // --- Autocomplete ---

    @Test
    fun `autocomplete deve retornar sugestoes`() = runTest {
        coEvery { searchIndex.autocomplete("pad", tenantId, 5) } returns listOf("Padaria Central", "Padaria do Joao")

        val result = service.autocomplete(AutocompleteRequest(query = "pad", tenantId = tenantId))

        assertEquals(2, result.suggestions.size)
        assertEquals("Padaria Central", result.suggestions[0])
    }

    @Test
    fun `autocomplete deve rejeitar query com menos de 2 caracteres`() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.autocomplete(AutocompleteRequest(query = "p", tenantId = tenantId))
            }
        }
    }

    // --- Indexacao ---

    @Test
    fun `indexStore deve chamar searchIndex`() = runTest {
        val doc = StoreDocument("s1", tenantId, "Loja", "loja", null, "BAKERY",
            "Centro", "Londrina", "PR", -23.3, -51.1, 4.5, 100, 20, 15.0, true, true, null)
        coEvery { searchIndex.indexStore(doc) } just Runs

        service.indexStore(doc)

        coVerify { searchIndex.indexStore(doc) }
    }

    @Test
    fun `indexProduct deve chamar searchIndex`() = runTest {
        val doc = ProductDocument("p1", tenantId, "s1", "Loja", "Produto", "produto",
            "Desc", 10.0, "Cat", null, true, true)
        coEvery { searchIndex.indexProduct(doc) } just Runs

        service.indexProduct(doc)

        coVerify { searchIndex.indexProduct(doc) }
    }

    @Test
    fun `removeStore deve chamar searchIndex`() = runTest {
        coEvery { searchIndex.deleteStore("s1") } just Runs
        service.removeStore("s1")
        coVerify { searchIndex.deleteStore("s1") }
    }

    @Test
    fun `removeProduct deve chamar searchIndex`() = runTest {
        coEvery { searchIndex.deleteProduct("p1") } just Runs
        service.removeProduct("p1")
        coVerify { searchIndex.deleteProduct("p1") }
    }
}
