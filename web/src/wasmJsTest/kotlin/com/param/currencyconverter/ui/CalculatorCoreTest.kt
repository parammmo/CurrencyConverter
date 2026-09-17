package com.param.currencyconverter.ui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Die Fälle aus den Kommentaren in CalculatorCore — vor allem die, bei denen
 * die Handarbeit von BigDecimal abweichen könnte.
 */
class CalculatorCoreTest {

    @Test
    fun moneyRoundsHalfUpOnDecimalText() {
        // Der 1,005-Fall: als Binärwert 1,00499…, als Text "1.005".
        assertEquals("1,01", formatMoney(1.005))
        assertEquals("1,00", formatMoney(0.995))
        assertEquals("0,13", formatMoney(0.125))
    }

    @Test
    fun moneyAlwaysHasTwoDecimals() {
        assertEquals("86,00", formatMoney(86.0))
        assertEquals("1,50", formatMoney(1.5))
        assertEquals("0,00", formatMoney(0.0))
        assertEquals("1234,50", formatMoney(1234.5))
    }

    @Test
    fun moneyNeverShowsNegativeZero() {
        assertEquals("0,00", formatMoney(-0.001))
        assertEquals("0,00", formatMoney(-1e-9))
        assertEquals("-0,50", formatMoney(-0.5))
    }

    @Test
    fun amountUsesScientificAboveTenBillion() {
        assertEquals("1,2346e+10", formatAmount(12345678901.0))
        assertEquals("1,0000e+10", formatAmount(1e10))
        // Mantissen-Überlauf: 9,99996… muss zu 1,0000e+11 werden.
        assertEquals("1,0000e+11", formatAmount(99999600000.0))
    }

    @Test
    fun amountRoundsToSixDecimalsAndEightSignificant() {
        assertEquals("0,333333", formatAmount(1.0 / 3))
        assertEquals("0,3", formatAmount(0.1 + 0.2))
        assertEquals("86", formatAmount(86.0))
        assertEquals("1234567,1", formatAmount(1234567.123456))
    }

    @Test
    fun keypadChainsOperators() {
        val state = "2+3+4=".fold(CalcState()) { s, c -> s.onKey(c.toString()) }
        assertEquals("9,00", state.entry)
    }

    @Test
    fun tripleZeroKeyRespectsDecimalRoom() {
        val state = CalcState(entry = "1,2").onKey("000")
        assertEquals("1,20", state.entry)
    }
}
