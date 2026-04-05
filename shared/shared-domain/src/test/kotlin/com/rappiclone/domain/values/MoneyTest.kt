package com.rappiclone.domain.values

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun `brl factory deve criar Money com 2 casas decimais`() {
        val money = Money.brl("10.50")
        assertEquals(BigDecimal("10.50"), money.value)
        assertEquals("BRL", money.currency)
    }

    @Test
    fun `brl de Double deve arredondar pra 2 casas`() {
        val money = Money.brl(10.555)
        assertEquals(BigDecimal("10.56"), money.value)
    }

    @Test
    fun `soma de mesma moeda deve funcionar`() {
        val a = Money.brl("10.00")
        val b = Money.brl("5.50")
        val result = a + b
        assertEquals(BigDecimal("15.50"), result.value)
        assertEquals("BRL", result.currency)
    }

    @Test
    fun `subtracao de mesma moeda deve funcionar`() {
        val a = Money.brl("20.00")
        val b = Money.brl("7.30")
        val result = a - b
        assertEquals(BigDecimal("12.70"), result.value)
    }

    @Test
    fun `soma de moedas diferentes deve lancar excecao`() {
        val brl = Money.brl("10.00")
        val usd = Money("10.00", "USD")
        assertThrows(IllegalArgumentException::class.java) { brl + usd }
    }

    @Test
    fun `subtracao de moedas diferentes deve lancar excecao`() {
        val brl = Money.brl("10.00")
        val usd = Money("10.00", "USD")
        assertThrows(IllegalArgumentException::class.java) { brl - usd }
    }

    @Test
    fun `multiplicacao por BigDecimal deve arredondar`() {
        val money = Money.brl("10.00")
        val result = money * BigDecimal("1.5")
        assertEquals(BigDecimal("15.00"), result.value)
    }

    @Test
    fun `multiplicacao por Int deve funcionar`() {
        val money = Money.brl("7.50")
        val result = money * 3
        assertEquals(BigDecimal("22.50"), result.value)
    }

    @Test
    fun `isPositive isZero isNegative devem funcionar`() {
        assertTrue(Money.brl("10.00").isPositive())
        assertFalse(Money.brl("10.00").isZero())
        assertFalse(Money.brl("10.00").isNegative())

        assertTrue(Money.ZERO_BRL.isZero())
        assertFalse(Money.ZERO_BRL.isPositive())
        assertFalse(Money.ZERO_BRL.isNegative())

        val negative = Money.brl("5.00") - Money.brl("10.00")
        assertTrue(negative.isNegative())
    }

    @Test
    fun `compareTo deve ordenar corretamente`() {
        val small = Money.brl("5.00")
        val big = Money.brl("10.00")
        val equal = Money.brl("5.00")

        assertTrue(small < big)
        assertTrue(big > small)
        assertEquals(0, small.compareTo(equal))
    }

    @Test
    fun `compareTo de moedas diferentes deve lancar excecao`() {
        val brl = Money.brl("10.00")
        val usd = Money("10.00", "USD")
        assertThrows(IllegalArgumentException::class.java) { brl.compareTo(usd) }
    }

    @Test
    fun `ZERO_BRL deve ser zero`() {
        assertTrue(Money.ZERO_BRL.isZero())
        assertEquals("BRL", Money.ZERO_BRL.currency)
        assertEquals(BigDecimal("0.00"), Money.ZERO_BRL.value)
    }

    @Test
    fun `valor com muitas casas decimais deve arredondar HALF_UP`() {
        val money = Money.brl("10.125")
        assertEquals(BigDecimal("10.13"), money.value)

        val money2 = Money.brl("10.124")
        assertEquals(BigDecimal("10.12"), money2.value)
    }
}
