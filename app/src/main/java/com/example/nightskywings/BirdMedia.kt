package com.example.nightskywings

import java.net.URLEncoder

data class BirdCard(
    val english: String,
    val lithuanian: String,
    val scientific: String,
    val imageUrl: String?,
    val fact: String?,
    val songUrl: String?
)

object BirdMedia {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    fun card(s: Sighting): BirdCard {
        val wiki = Net.get(
            "https://en.wikipedia.org/api/rest_v1/page/summary/" + enc(s.scientificName.replace(' ', '_'))
        )
        val (image, fact) = wiki?.let(Parsers::wikiSummary) ?: Pair(null, null)
        return BirdCard(s.englishName, s.lithuanianName, s.scientificName, image, fact, song(s.scientificName))
    }

    /**
     * Xeno-canto API v3 song lookup. The exact v3 query syntax was not verified when this was
     * written, so several forms are tried in order and the first one with a recording wins.
     */
    private fun song(scientificName: String): String? {
        if (Config.XENO_KEY.isBlank()) return null
        val parts = scientificName.trim().split(" ")
        val queries = buildList {
            if (parts.size >= 2) {
                add("gen:${parts[0]} sp:${parts[1]} q:A type:song")
                add("gen:${parts[0]} sp:${parts[1]}")
            }
            add("sp:\"$scientificName\" q:A type:song")
        }
        for (q in queries) {
            val json = Net.get(
                "https://xeno-canto.org/api/3/recordings?query=" + enc(q) + "&key=" + enc(Config.XENO_KEY)
            ) ?: continue
            Parsers.firstRecordingFile(json)?.let { return it }
        }
        return null
    }
}
