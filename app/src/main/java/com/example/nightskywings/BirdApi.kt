package com.example.nightskywings

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

object BirdApi {
    private fun headers() = mapOf("X-eBirdApiToken" to Config.EBIRD_KEY)

    @Volatile
    private var ltCache: Map<String, String>? = null

    /**
     * Lithuanian common names keyed by species code. The full eBird taxonomy is large, so it is
     * downloaded at most once every [Config.TAXONOMY_MAX_AGE_DAYS] days and kept in app storage.
     */
    private fun ltNames(ctx: Context): Map<String, String> {
        ltCache?.let { return it }
        val file = File(ctx.filesDir, "taxonomy_lt.json")
        val maxAge = TimeUnit.DAYS.toMillis(Config.TAXONOMY_MAX_AGE_DAYS.toLong())
        val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < maxAge
        if (fresh) {
            val cached = Parsers.namesFromJson(file.readText())
            if (cached.isNotEmpty()) return cached.also { ltCache = it }
        }
        val url = "https://api.ebird.org/v2/ref/taxonomy/ebird?fmt=json&locale=lt"
        val downloaded = Net.get(url, headers())?.let(Parsers::taxonomyNames).orEmpty()
        if (downloaded.isNotEmpty()) {
            file.writeText(Parsers.namesToJson(downloaded))
            return downloaded.also { ltCache = it }
        }
        // Download failed: use a stale copy if there is one, without caching it in memory.
        return if (file.exists()) Parsers.namesFromJson(file.readText()) else emptyMap()
    }

    // Rare (notable) sightings anywhere in Lithuania, last 3 days
    fun rareInLithuania(ctx: Context): List<Sighting> {
        if (Config.EBIRD_KEY.isBlank()) return emptyList()
        val url = "https://api.ebird.org/v2/data/obs/LT/recent/notable?back=3&maxResults=30"
        val json = Net.get(url, headers()) ?: return emptyList()
        return Parsers.sightings(json, ltNames(ctx))
    }

    // Birds recently seen within 25 km of you
    fun nearby(ctx: Context, lat: Double, lon: Double): List<Sighting> {
        if (Config.EBIRD_KEY.isBlank()) return emptyList()
        val url = "https://api.ebird.org/v2/data/obs/geo/recent?lat=$lat&lng=$lon" +
            "&dist=25&back=7&maxResults=60"
        val json = Net.get(url, headers()) ?: return emptyList()
        return Parsers.sightings(json, ltNames(ctx)).distinctBy { it.speciesCode }
    }
}
