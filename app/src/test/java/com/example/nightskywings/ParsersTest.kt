package com.example.nightskywings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsersTest {

    private fun openMeteo(clouds: List<Int?>): String {
        val times = (0 until 48).map { h ->
            val day = if (h < 24) "2026-11-06" else "2026-11-07"
            "\"${day}T%02d:00\"".format(h % 24)
        }
        val values = clouds.map { it?.toString() ?: "null" }
        return """{"hourly":{"time":[${times.joinToString(",")}],"cloud_cover":[${values.joinToString(",")}]}}"""
    }

    @Test
    fun cloudCoverAveragesOnlyTonight() {
        // 100% all day, 10% from 21:00 to 03:00 (7 hours: 21,22,23,0,1,2,3)
        val clouds = (0 until 48).map { h -> if (h in 21..27) 10 else 100 }
        assertEquals(10, Parsers.tonightCloudCover(openMeteo(clouds)))
    }

    @Test
    fun cloudCoverSkipsNullHours() {
        val clouds = (0 until 48).map { h -> if (h == 21) null else if (h in 22..27) 20 else 100 }
        assertEquals(20, Parsers.tonightCloudCover(openMeteo(clouds)))
    }

    @Test
    fun cloudCoverBadJsonIsNull() {
        assertNull(Parsers.tonightCloudCover("{\"error\":true}"))
        assertNull(Parsers.tonightCloudCover("not json"))
    }

    @Test
    fun sightingsUseLithuanianNameWhenKnown() {
        val json = """[
          {"speciesCode":"eurrob1","comName":"European Robin","sciName":"Erithacus rubecula",
           "locName":"Klaipeda","obsDt":"2026-11-05 08:10"},
          {"speciesCode":"unknown1","comName":"Mystery Bird","sciName":"Avis mysteria",
           "obsDt":"2026-11-05 09:00"}
        ]"""
        val s = Parsers.sightings(json, mapOf("eurrob1" to "Liepsnelė"))
        assertEquals(2, s.size)
        assertEquals("Liepsnelė", s[0].lithuanianName)
        assertEquals("European Robin", s[0].englishName)
        assertEquals("Klaipeda", s[0].place)
        assertEquals("Mystery Bird", s[1].lithuanianName)
        assertEquals("", s[1].place)
    }

    @Test
    fun sightingsBadJsonIsEmpty() {
        assertTrue(Parsers.sightings("{\"errors\":[]}", emptyMap()).isEmpty())
    }

    @Test
    fun taxonomyRoundTripsThroughCacheFormat() {
        val names = Parsers.taxonomyNames(
            """[{"speciesCode":"eurrob1","comName":"Liepsnelė"},{"speciesCode":"grstit1","comName":"Didžioji zylė"}]"""
        )
        assertEquals(2, names.size)
        assertEquals(names, Parsers.namesFromJson(Parsers.namesToJson(names)))
    }

    @Test
    fun wikiSummaryReadsImageAndExtract() {
        val (img, fact) = Parsers.wikiSummary(
            """{"thumbnail":{"source":"https://upload.example/robin.jpg"},"extract":"A small bird."}"""
        )
        assertEquals("https://upload.example/robin.jpg", img)
        assertEquals("A small bird.", fact)
        assertEquals(Pair<String?, String?>(null, null), Parsers.wikiSummary("{}"))
    }

    @Test
    fun recordingFileIsMadeAbsolute() {
        assertEquals(
            "https://xeno-canto.org/123/download",
            Parsers.firstRecordingFile("""{"recordings":[{"file":"//xeno-canto.org/123/download"}]}""")
        )
        assertEquals(
            "https://xeno-canto.org/9/download",
            Parsers.firstRecordingFile("""{"recordings":[{"file":"https://xeno-canto.org/9/download"}]}""")
        )
        assertNull(Parsers.firstRecordingFile("""{"recordings":[]}"""))
    }
}
