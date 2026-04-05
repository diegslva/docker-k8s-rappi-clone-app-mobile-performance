package com.rappiclone.gateway.proxy

import com.rappiclone.gateway.config.ServiceEndpoints
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ServiceRouterTest {

    private val endpoints = ServiceEndpoints(
        identity = "http://identity:8002",
        tenant = "http://tenant:8001",
        userProfile = "http://profile:8003",
        catalog = "http://catalog:8004",
        search = "http://search:8005",
        cart = "http://cart:8006",
        order = "http://order:8007",
        payment = "http://payment:8008",
        courier = "http://courier:8009",
        tracking = "http://tracking:8010",
        notification = "http://notification:8011",
        rating = "http://rating:8012",
        promotion = "http://promotion:8013",
        pricing = "http://pricing:8014",
        geolocation = "http://geo:8015",
        media = "http://media:8016"
    )

    private val router = ServiceRouter(endpoints)

    @Test
    fun `deve rotear auth pro identity service`() {
        assertEquals("http://identity:8002", router.resolve("/api/v1/auth/login"))
        assertEquals("http://identity:8002", router.resolve("/api/v1/auth/register"))
        assertEquals("http://identity:8002", router.resolve("/api/v1/auth/refresh"))
        assertEquals("http://identity:8002", router.resolve("/api/v1/auth/me"))
    }

    @Test
    fun `deve rotear tenants pro tenant service`() {
        assertEquals("http://tenant:8001", router.resolve("/api/v1/tenants"))
        assertEquals("http://tenant:8001", router.resolve("/api/v1/tenants/londrina-pr"))
        assertEquals("http://tenant:8001", router.resolve("/api/v1/tenants/londrina-pr/zones"))
    }

    @Test
    fun `deve rotear location pro tenant service`() {
        assertEquals("http://tenant:8001", router.resolve("/api/v1/location/resolve"))
    }

    @Test
    fun `deve rotear stores pro catalog service`() {
        assertEquals("http://catalog:8004", router.resolve("/api/v1/stores"))
        assertEquals("http://catalog:8004", router.resolve("/api/v1/stores/123/menu"))
    }

    @Test
    fun `deve rotear search pro search service`() {
        assertEquals("http://search:8005", router.resolve("/api/v1/search?q=pizza"))
    }

    @Test
    fun `deve rotear cart pro cart service`() {
        assertEquals("http://cart:8006", router.resolve("/api/v1/cart"))
        assertEquals("http://cart:8006", router.resolve("/api/v1/cart/items"))
    }

    @Test
    fun `deve rotear orders pro order service`() {
        assertEquals("http://order:8007", router.resolve("/api/v1/orders"))
        assertEquals("http://order:8007", router.resolve("/api/v1/orders/abc-123"))
    }

    @Test
    fun `deve rotear payments pro payment service`() {
        assertEquals("http://payment:8008", router.resolve("/api/v1/payments"))
    }

    @Test
    fun `deve rotear courier pro courier service`() {
        assertEquals("http://courier:8009", router.resolve("/api/v1/courier/online"))
        assertEquals("http://courier:8009", router.resolve("/api/v1/courier/location"))
    }

    @Test
    fun `deve rotear tracking pro tracking service`() {
        assertEquals("http://tracking:8010", router.resolve("/api/v1/tracking/order-123"))
    }

    @Test
    fun `deve rotear todos os servicos restantes`() {
        assertEquals("http://notification:8011", router.resolve("/api/v1/notifications"))
        assertEquals("http://rating:8012", router.resolve("/api/v1/ratings"))
        assertEquals("http://promotion:8013", router.resolve("/api/v1/promotions"))
        assertEquals("http://pricing:8014", router.resolve("/api/v1/pricing"))
        assertEquals("http://geo:8015", router.resolve("/api/v1/geo/geocode"))
        assertEquals("http://media:8016", router.resolve("/api/v1/media/upload"))
    }

    @Test
    fun `deve retornar null pra rota desconhecida`() {
        assertNull(router.resolve("/api/v1/inexistente"))
        assertNull(router.resolve("/nao-existe"))
        assertNull(router.resolve("/"))
    }

    @Test
    fun `deve resolver rota mais especifica quando ha ambiguidade`() {
        // /api/v1/location deve ir pro tenant (nao catalog)
        assertEquals("http://tenant:8001", router.resolve("/api/v1/location/resolve"))
    }
}
