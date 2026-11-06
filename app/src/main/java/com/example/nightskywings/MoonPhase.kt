package com.example.nightskywings

import kotlin.math.cos

/** Simple mean-cycle moon model. Accurate to within about a day, which is enough for stargazing tips. */
object MoonPhase {
    private const val SYNODIC = 29.530588853

    // Known new moon: 6 Jan 2000, 18:14 UTC
    private const val REF_NEW_MOON_MILLIS = 947182440000L

    /** Days since the last new moon, 0 until 29.53. */
    fun age(nowMillis: Long = System.currentTimeMillis()): Double {
        val days = (nowMillis - REF_NEW_MOON_MILLIS) / 86400000.0
        return ((days % SYNODIC) + SYNODIC) % SYNODIC
    }

    /** 0 to 100 percent of the disc lit. */
    fun illuminationPercent(nowMillis: Long = System.currentTimeMillis()): Int {
        val fraction = (1 - cos(2 * Math.PI * age(nowMillis) / SYNODIC)) / 2
        return (fraction * 100).toInt()
    }

    fun name(nowMillis: Long = System.currentTimeMillis()): String {
        val a = age(nowMillis)
        return when {
            // Eight equal bands of about 3.69 days, each centred on its phase
            // (new 0, first quarter 7.38, full 14.77, last quarter 22.15).
            a < 1.85 -> "New moon"
            a < 5.54 -> "Waxing crescent"
            a < 9.23 -> "First quarter"
            a < 12.92 -> "Waxing gibbous"
            a < 16.61 -> "Full moon"
            a < 20.30 -> "Waning gibbous"
            a < 23.99 -> "Last quarter"
            a < 27.68 -> "Waning crescent"
            else -> "New moon"
        }
    }

    fun tip(percent: Int): String = when {
        percent < 25 -> "dark skies, great for stars and the Milky Way"
        percent > 80 -> "bright moon, great for moon watching"
        else -> "decent for stars and the moon"
    }
}
