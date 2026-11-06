package com.example.nightskywings

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class TimeMathTest {
    private fun cal(h: Int, m: Int) = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(2026, Calendar.NOVEMBER, 6, h, m, 0)
    }

    @Test
    fun laterTodayIsSameDay() {
        assertEquals(30 * 60_000L, TimeMath.millisUntil(7, 30, cal(7, 0)))
    }

    @Test
    fun pastTimeRollsToTomorrow() {
        assertEquals((24 * 60 - 30) * 60_000L, TimeMath.millisUntil(7, 30, cal(8, 0)))
    }

    @Test
    fun exactlyNowRollsToTomorrow() {
        assertEquals(24 * 60 * 60_000L, TimeMath.millisUntil(16, 0, cal(16, 0)))
    }
}
