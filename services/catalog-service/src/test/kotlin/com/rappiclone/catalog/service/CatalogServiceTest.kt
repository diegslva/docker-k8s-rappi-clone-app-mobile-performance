package com.rappiclone.catalog.service

import com.rappiclone.domain.enums.StoreCategory
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.catalog.model.*
import com.rappiclone.catalog.repository.ProductRepository
import com.rappiclone.catalog.repository.StoreRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class CatalogServiceTest {

    private val storeRepo = mockk<StoreRepository>()
    private val productRepo = mockk<ProductRepository>()
    private val service = CatalogService(storeRepo, productRepo)
    private val tenantId = "londrina-pr"
    private val userId = UUID.randomUUID()

    private val testStore = StoreResponse(
        id = UUID.randomUUID().toString(),
        tenantId = tenantId,
        name = "Padaria Central",
        slug = "padaria-central",
        description = "A melhor padaria de Londrina",
        category = StoreCategory.BAKERY,
        phone = "+5543999999999",
        email = "padaria@email.com",
        logoUrl = null,
        bannerUrl = null,
        neighborhood = "Centro",
        city = "Londrina",
        state = "PR",
        latitude = -23.3045,
        longitude = -51.1696,
        minOrderValue = 15.0,
        avgPrepTime = 20,
        deliveryRadiusKm = 5.0,
        ratingAvg = 0.0,
        ratingCount = 0,
        isOpen = true,
        isActive = true
    )

    private val testProduct = ProductResponse(
        id = UUID.randomUUID().toString(),
        storeId = testStore.id,
        categoryId = null,
        name = "Pao Frances",
        slug = "pao-frances",
        description = "Pao frances quentinho",
        price = 0.75,
        originalPrice = null,
        imageUrl = null,
        prepTimeMin = 5,
        isAvailable = true
    )

    @BeforeEach
    fun setup() = clearAllMocks()

    // --- Store Tests ---

    @Test
    fun `deve criar store com dados validos`() = runTest {
        coEvery { storeRepo.create(any(), userId, tenantId) } returns testStore

        val request = CreateStoreRequest(
            name = "Padaria Central",
            category = StoreCategory.BAKERY,
            street = "Rua Sergipe", number = "100",
            neighborhood = "Centro", city = "Londrina", state = "PR", zipCode = "86010-000"
        )

        val result = service.createStore(request, userId, tenantId)
        assertEquals("Padaria Central", result.name)
        assertEquals(StoreCategory.BAKERY, result.category)
    }

    @Test
    fun `deve rejeitar store com nome vazio`() {
        val request = CreateStoreRequest(
            name = "", category = StoreCategory.BAKERY,
            street = "Rua", number = "1",
            neighborhood = "B", city = "C", state = "PR", zipCode = "00000"
        )
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { service.createStore(request, userId, tenantId) }
        }
    }

    @Test
    fun `deve rejeitar store com UF invalida`() {
        val request = CreateStoreRequest(
            name = "Loja", category = StoreCategory.BAKERY,
            street = "Rua", number = "1",
            neighborhood = "B", city = "C", state = "PARANA", zipCode = "00000"
        )
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { service.createStore(request, userId, tenantId) }
        }
    }

    @Test
    fun `deve rejeitar minOrderValue negativo`() {
        val request = CreateStoreRequest(
            name = "Loja", category = StoreCategory.BAKERY,
            street = "Rua", number = "1",
            neighborhood = "B", city = "C", state = "PR", zipCode = "00000",
            minOrderValue = -5.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { service.createStore(request, userId, tenantId) }
        }
    }

    @Test
    fun `deve rejeitar deliveryRadius zero`() {
        val request = CreateStoreRequest(
            name = "Loja", category = StoreCategory.BAKERY,
            street = "Rua", number = "1",
            neighborhood = "B", city = "C", state = "PR", zipCode = "00000",
            deliveryRadiusKm = 0.0
        )
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { service.createStore(request, userId, tenantId) }
        }
    }

    @Test
    fun `getStore deve retornar store existente`() = runTest {
        coEvery { storeRepo.findById(UUID.fromString(testStore.id), tenantId) } returns testStore
        val result = service.getStore(testStore.id, tenantId)
        assertEquals(testStore.id, result.id)
    }

    @Test
    fun `getStore deve lancar excecao pra store inexistente`() {
        val id = UUID.randomUUID().toString()
        coEvery { storeRepo.findById(UUID.fromString(id), tenantId) } returns null
        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { service.getStore(id, tenantId) }
        }
    }

    @Test
    fun `listStores deve filtrar por categoria`() = runTest {
        coEvery { storeRepo.findByTenant(tenantId, StoreCategory.BAKERY, false) } returns listOf(testStore)
        val result = service.listStores(tenantId, StoreCategory.BAKERY)
        assertEquals(1, result.size)
        assertEquals(StoreCategory.BAKERY, result[0].category)
    }

    // --- Product Tests ---

    @Test
    fun `deve criar produto com dados validos`() = runTest {
        coEvery { storeRepo.findById(UUID.fromString(testStore.id), tenantId) } returns testStore
        coEvery { productRepo.createProduct(UUID.fromString(testStore.id), tenantId, any()) } returns testProduct

        val request = CreateProductRequest(name = "Pao Frances", price = 0.75)
        val result = service.createProduct(testStore.id, tenantId, request)
        assertEquals("Pao Frances", result.name)
        assertEquals(0.75, result.price)
    }

    @Test
    fun `deve rejeitar produto com preco zero`() {
        val request = CreateProductRequest(name = "Produto", price = 0.0)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { service.createProduct(testStore.id, tenantId, request) }
        }
    }

    @Test
    fun `deve rejeitar produto com nome vazio`() {
        val request = CreateProductRequest(name = "", price = 10.0)
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { service.createProduct(testStore.id, tenantId, request) }
        }
    }

    @Test
    fun `deve rejeitar produto em store inexistente`() {
        val storeId = UUID.randomUUID().toString()
        coEvery { storeRepo.findById(UUID.fromString(storeId), tenantId) } returns null

        val request = CreateProductRequest(name = "Produto", price = 10.0)
        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { service.createProduct(storeId, tenantId, request) }
        }
    }

    // --- Menu ---

    @Test
    fun `getStoreMenu deve agregar categorias e produtos`() = runTest {
        val categoryId = UUID.randomUUID().toString()
        val category = CategoryResponse(categoryId, "Paes", "paes", null, 0)
        val productWithCategory = testProduct.copy(categoryId = categoryId)

        coEvery { storeRepo.findById(UUID.fromString(testStore.id), tenantId) } returns testStore
        coEvery { productRepo.findCategoriesByStore(UUID.fromString(testStore.id)) } returns listOf(category)
        coEvery { productRepo.findProductsByStore(UUID.fromString(testStore.id), tenantId) } returns listOf(productWithCategory)

        val menu = service.getStoreMenu(testStore.id, tenantId)

        assertEquals(testStore.name, menu.store.name)
        assertEquals(1, menu.categories.size)
        assertEquals("Paes", menu.categories[0].category.name)
        assertEquals(1, menu.categories[0].products.size)
    }

    @Test
    fun `getStoreMenu deve agrupar produtos sem categoria em Outros`() = runTest {
        val productNoCategory = testProduct.copy(categoryId = null)

        coEvery { storeRepo.findById(UUID.fromString(testStore.id), tenantId) } returns testStore
        coEvery { productRepo.findCategoriesByStore(UUID.fromString(testStore.id)) } returns emptyList()
        coEvery { productRepo.findProductsByStore(UUID.fromString(testStore.id), tenantId) } returns listOf(productNoCategory)

        val menu = service.getStoreMenu(testStore.id, tenantId)

        assertEquals(1, menu.categories.size)
        assertEquals("Outros", menu.categories[0].category.name)
        assertEquals(1, menu.categories[0].products.size)
    }

    // --- Bulk Import ---

    @Test
    fun `bulkImport deve importar CSV valido`() = runTest {
        val csv = "name,price,description\nPao,0.75,Pao frances\nBolo,15.00,Bolo de chocolate"
        coEvery { storeRepo.findById(UUID.fromString(testStore.id), tenantId) } returns testStore
        coEvery { productRepo.bulkCreateProducts(UUID.fromString(testStore.id), tenantId, any()) } returns 2

        val result = service.bulkImportProducts(testStore.id, tenantId, csv.byteInputStream())

        assertEquals(2, result.imported)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun `bulkImport deve reportar erros por linha`() = runTest {
        val csv = "name,price\nPao,0.75\n,invalido\nBolo,abc"
        coEvery { storeRepo.findById(UUID.fromString(testStore.id), tenantId) } returns testStore
        coEvery { productRepo.bulkCreateProducts(UUID.fromString(testStore.id), tenantId, any()) } returns 1

        val result = service.bulkImportProducts(testStore.id, tenantId, csv.byteInputStream())

        assertEquals(1, result.imported)
        assertTrue(result.errors.size >= 1)
    }

    @Test
    fun `bulkImport deve rejeitar store inexistente`() {
        val storeId = UUID.randomUUID().toString()
        coEvery { storeRepo.findById(UUID.fromString(storeId), tenantId) } returns null

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.bulkImportProducts(storeId, tenantId, "name,price\nX,1".byteInputStream())
            }
        }
    }
}
