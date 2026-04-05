package com.rappiclone.domain.values

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Value object imutavel representando um valor monetario.
 * Sempre usa BigDecimal internamente pra evitar erros de ponto flutuante.
 * Serializado como string pra preservar precisao no JSON.
 */
@Serializable
data class Money(
    val amount: String,
    val currency: String = "BRL"
) : Comparable<Money> {

    val value: BigDecimal
        get() = BigDecimal(amount).setScale(2, RoundingMode.HALF_UP)

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Nao pode somar moedas diferentes: $currency vs ${other.currency}" }
        return Money((value + other.value).toPlainString(), currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Nao pode subtrair moedas diferentes: $currency vs ${other.currency}" }
        return Money((value - other.value).toPlainString(), currency)
    }

    operator fun times(multiplier: BigDecimal): Money =
        Money((value * multiplier).setScale(2, RoundingMode.HALF_UP).toPlainString(), currency)

    operator fun times(multiplier: Int): Money = times(BigDecimal(multiplier))

    fun isPositive(): Boolean = value > BigDecimal.ZERO
    fun isZero(): Boolean = value.compareTo(BigDecimal.ZERO) == 0
    fun isNegative(): Boolean = value < BigDecimal.ZERO

    override fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Nao pode comparar moedas diferentes: $currency vs ${other.currency}" }
        return value.compareTo(other.value)
    }

    companion object {
        val ZERO_BRL = Money("0.00", "BRL")

        fun brl(amount: String): Money = Money(amount, "BRL")
        fun brl(amount: BigDecimal): Money = Money(amount.setScale(2, RoundingMode.HALF_UP).toPlainString(), "BRL")
        fun brl(amount: Double): Money = brl(BigDecimal.valueOf(amount))
    }
}
