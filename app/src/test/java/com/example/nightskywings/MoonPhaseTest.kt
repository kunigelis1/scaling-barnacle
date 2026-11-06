package com.example.nightskywings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MoonPhaseTest {
    private fun at(iso: String) = Instant.parse(iso).toEpochMilli()

    @Test
    fun referenceNewMoonIsAgeZero() {
        assertEquals(0.0, MoonPhase.age(at("2000-01-06T18:14:00Z")), 1e-6)
    }

    @Test
    fun fullMoonJanuary2024IsAlmostFullyLit() {
        // Full moon: 25 Jan 2024, 17:54 UTC
        val t = at("2024-01-25T17:54:00Z")
        assertTrue(MoonPhase.illuminationPercent(t) >= 95)
        assertEquals("Full moon", MoonPhase.name(t))
    }

    @Test
    fun newMoonJanuary2024IsAlmostDark() {
        // New moon: 11 Jan 2024, 11:57 UTC
        val t = at("2024-01-11T11:57:00Z")
        assertTrue(MoonPhase.illuminationPercent(t) <= 5)
        assertEquals("New moon", MoonPhase.name(t))
    }

    @Test
    fun quarterMoonsAreNamedCorrectly() {
        // First quarter: 18 Jan 2024, 03:53 UTC. Last quarter: 4 Jan 2024, 03:30 UTC.
        assertEquals("First quarter", MoonPhase.name(at("2024-01-18T03:53:00Z")))
        assertEquals("Last quarter", MoonPhase.name(at("2024-01-04T03:30:00Z")))
    }

    @Test
    fun tipBands() {
        assertTrue(MoonPhase.tip(10).startsWith("dark skies"))
        assertTrue(MoonPhase.tip(90).startsWith("bright moon"))
        assertTrue(MoonPhase.tip(50).startsWith("decent"))
    }
}
