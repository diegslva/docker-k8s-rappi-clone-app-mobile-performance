package com.rappiclone.geolocation.service

import com.rappiclone.domain.errors.DomainException
import com.rappiclone.geolocation.model.*
import com.rappiclone.geolocation.provider.GeocodingProvider
import com.rappiclone.geolocation.provider.RoutingProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GeolocationServiceTest {

    private val geocodingProvider = mockk<GeocodingProvider>()
    private val routingProvider = mockk<RoutingProvider>()
    private val service = GeolocationService(geocodingProvider, routingProvider)

    private val londrinalResult = GeocodeResult(
        latitude = -23.3045,
        longitude = -51.1696,
        displayName = "Rua Sergipe, 445, Centro, Londrina, PR, 86010-000, Brasil",
        street = "Rua Sergipe",
        number = "445",
        neighborhood = "Centro",
        city = "Londrina",
        state = "Parana",
        zipCode = "86010-000",
        country = "BR",
        confidence = 0.85
    )

    @BeforeEach
    fun setup() = clearAllMocks()

    // --- Geocoding ---

    @Test
    fun `geocode deve retornar resultados do provider`() = runTest {
        coEvery { geocodingProvider.geocode(any(), "BR") } returns listOf(londrinalResult)

        val results = service.geocode(GeocodeRequest(address = "Rua Sergipe 445", city = "Londrina", state = "PR"))

        assertEquals(1, results.size)
        assertEquals("Rua Sergipe", results[0].street)
        assertEquals(-23.3045, results[0].latitude)
    }

    @Test
    fun `geocode sem resultados deve retornar lista vazia`() = runTest {
        coEvery { geocodingProvider.geocode(any(), "BR") } returns emptyList()

        val results = service.geocode(GeocodeRequest(address = "Endereco inexistente XYZ"))
        assertTrue(results.isEmpty())
    }

    @Test
    fun `geocode deve concatenar cidade e estado na query`() = runTest {
        coEvery { geocodingProvider.geocode(any(), "BR") } returns listOf(londrinalResult)

        service.geocode(GeocodeRequest(address = "Rua A", city = "Londrina", state = "PR"))

        coVerify { geocodingProvider.geocode("Rua A, Londrina, PR", "BR") }
    }

    // --- Reverse Geocoding ---

    @Test
    fun `reverseGeocode deve retornar endereco`() = runTest {
        coEvery { geocodingProvider.reverseGeocode(-23.3045, -51.1696) } returns londrinalResult

        val result = service.reverseGeocode(ReverseGeocodeRequest(-23.3045, -51.1696))

        assertEquals("Londrina", result.city)
        assertEquals("Rua Sergipe", result.street)
    }

    @Test
    fun `reverseGeocode sem resultado deve lancar excecao`() = runTest {
        coEvery { geocodingProvider.reverseGeocode(any(), any()) } returns null

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.reverseGeocode(ReverseGeocodeRequest(0.0, 0.0))
            }
        }
    }

    @Test
    fun `reverseGeocode deve rejeitar latitude invalida`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.reverseGeocode(ReverseGeocodeRequest(91.0, 0.0))
            }
        }
    }

    @Test
    fun `reverseGeocode deve rejeitar longitude invalida`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.reverseGeocode(ReverseGeocodeRequest(0.0, 181.0))
            }
        }
    }

    // --- Routing ---

    @Test
    fun `calculateRoute deve retornar rota do OSRM`() = runTest {
        val routeResult = RouteResult(5000.0, 600.0, 5.0, 10.0, "encoded_polyline")
        coEvery { routingProvider.route(any(), any(), any(), any()) } returns routeResult

        val result = service.calculateRoute(RouteRequest(-23.3045, -51.1696, -23.3367, -51.1308))

        assertEquals(5.0, result.distanceKm)
        assertEquals(10.0, result.durationMinutes)
        assertNotNull(result.geometry)
    }

    @Test
    fun `calculateRoute sem rota deve lancar excecao`() = runTest {
        coEvery { routingProvider.route(any(), any(), any(), any()) } returns null

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.calculateRoute(RouteRequest(-23.3045, -51.1696, -23.3367, -51.1308))
            }
        }
    }

    @Test
    fun `calculateRoute deve validar coordenadas`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest {
                service.calculateRoute(RouteRequest(91.0, 0.0, 0.0, 0.0))
            }
        }
    }

    // --- Distance ---

    @Test
    fun `calculateDistance deve retornar distancia e tempo`() = runTest {
        val routeResult = RouteResult(3500.0, 420.0, 3.5, 7.0, null)
        coEvery { routingProvider.route(any(), any(), any(), any()) } returns routeResult

        val result = service.calculateDistance(DistanceRequest(-23.3045, -51.1696, -23.3367, -51.1308))

        assertEquals(3500.0, result.distanceMeters)
        assertEquals(3.5, result.distanceKm)
        assertEquals(7.0, result.durationMinutes)
    }

    // --- Address Validation ---

    @Test
    fun `validateAddress com resultado de alta confianca deve ser valido`() = runTest {
        coEvery { geocodingProvider.geocode(any(), "BR") } returns listOf(londrinalResult)

        val result = service.validateAddress(ValidateAddressRequest(
            street = "Rua Sergipe", number = "445",
            neighborhood = "Centro", city = "Londrina", state = "PR", zipCode = "86010-000"
        ))

        assertTrue(result.isValid)
        assertNotNull(result.normalizedAddress)
        assertEquals(-23.3045, result.latitude)
        assertTrue(result.confidence >= 0.3)
    }

    @Test
    fun `validateAddress sem resultado deve ser invalido`() = runTest {
        coEvery { geocodingProvider.geocode(any(), "BR") } returns emptyList()

        val result = service.validateAddress(ValidateAddressRequest(
            street = "XYZ", number = "0",
            neighborhood = "N", city = "N", state = "XX", zipCode = "00000"
        ))

        assertFalse(result.isValid)
        assertNull(result.normalizedAddress)
        assertEquals(0.0, result.confidence)
    }

    @Test
    fun `validateAddress com baixa confianca deve ser invalido`() = runTest {
        val lowConfidence = londrinalResult.copy(confidence = 0.1)
        coEvery { geocodingProvider.geocode(any(), "BR") } returns listOf(lowConfidence)

        val result = service.validateAddress(ValidateAddressRequest(
            street = "Rua", number = "1",
            neighborhood = "B", city = "C", state = "PR", zipCode = "00000"
        ))

        assertFalse(result.isValid)
        assertEquals(0.1, result.confidence)
    }
}
