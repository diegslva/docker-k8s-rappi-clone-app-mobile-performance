package com.rappiclone.domain.values

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class LocationTest {

    // Coordenadas reais de Londrina-PR
    private val londrinaCentro = Location(-23.3045, -51.1696)
    private val londrinaGleba = Location(-23.3367, -51.1308)

    // Coordenadas reais de SP
    private val spPaulista = Location(-23.5613, -46.6560)

    @Test
    fun `deve criar Location com coordenadas validas`() {
        val loc = Location(-23.3045, -51.1696)
        assertEquals(-23.3045, loc.latitude)
        assertEquals(-51.1696, loc.longitude)
    }

    @Test
    fun `deve aceitar limites extremos`() {
        assertDoesNotThrow { Location(90.0, 180.0) }
        assertDoesNotThrow { Location(-90.0, -180.0) }
        assertDoesNotThrow { Location(0.0, 0.0) }
    }

    @Test
    fun `deve rejeitar latitude fora do range`() {
        assertThrows(IllegalArgumentException::class.java) { Location(91.0, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { Location(-91.0, 0.0) }
    }

    @Test
    fun `deve rejeitar longitude fora do range`() {
        assertThrows(IllegalArgumentException::class.java) { Location(0.0, 181.0) }
        assertThrows(IllegalArgumentException::class.java) { Location(0.0, -181.0) }
    }

    @Test
    fun `distanceTo de mesmo ponto deve ser zero`() {
        val distance = londrinaCentro.distanceTo(londrinaCentro)
        assertEquals(0.0, distance, 0.01)
    }

    @Test
    fun `distanceTo entre pontos em Londrina deve ser coerente`() {
        // Centro de Londrina ate Gleba Palhano: ~5km
        val distance = londrinaCentro.distanceTo(londrinaGleba)
        assertTrue(distance > 4000, "Deveria ser > 4km: ${distance}m")
        assertTrue(distance < 7000, "Deveria ser < 7km: ${distance}m")
    }

    @Test
    fun `distanceTo Londrina-SP deve ser ~460km em linha reta`() {
        val distance = londrinaCentro.distanceTo(spPaulista)
        val km = distance / 1000.0
        assertTrue(km > 440, "Deveria ser > 440km: ${km}km")
        assertTrue(km < 480, "Deveria ser < 480km: ${km}km")
    }

    @Test
    fun `distanceKmTo deve retornar valor em km`() {
        val km = londrinaCentro.distanceKmTo(londrinaGleba)
        assertTrue(km > 4.0, "Deveria ser > 4km: ${km}km")
        assertTrue(km < 7.0, "Deveria ser < 7km: ${km}km")
    }

    @Test
    fun `distancia deve ser simetrica`() {
        val d1 = londrinaCentro.distanceTo(spPaulista)
        val d2 = spPaulista.distanceTo(londrinaCentro)
        assertEquals(d1, d2, 0.01)
    }
}
