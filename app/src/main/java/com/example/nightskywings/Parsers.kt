package com.example.nightskywings

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class Sighting(
    val speciesCode: String,
    val englishName: String,
    val lithuanianName: String,
    val scientificName: String,
    val place: String,
    val date: String
)

/** Pure JSON parsing, kept free of Android classes so it can be unit-tested on the JVM. */
object Parsers {

    /**
     * Average cloud cover (percent) from 21:00 on the first forecast day to 03:00 the next day,
     * from an Open-Meteo response with hourly=cloud_cover.
     */
    fun tonightCloudCover(json: String): Int? = try {
        val hourly = JSONObject(json).getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val clouds = hourly.getJSONArray("cloud_cover")
        val today = times.getString(0).substring(0, 10)
        var sum = 0
        var count = 0
        for (i in 0 until times.length()) {
            if (clouds.isNull(i)) continue
            val t = times.getString(i)
            val hour = t.substring(11, 13).toInt()
            val isToday = t.startsWith(today)
            val night = (isToday && hour >= 21) || (!isToday && hour <= 3)
            if (night) {
                sum += clouds.getInt(i)
                count++
            }
        }
        if (count == 0) null else sum / count
    } catch (e: JSONException) {
        null
    }

    /** eBird observation list. Lithuanian names come from [lt], falling back to English. */
    fun sightings(json: String, lt: Map<String, String>): List<Sighting> = try {
        val arr = JSONArray(json)
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            val code = o.getString("speciesCode")
            Sighting(
                code,
                o.getString("comName"),
                lt[code] ?: o.getString("comName"),
                o.getString("sciName"),
                o.optString("locName"),
                o.getString("obsDt")
            )
        }
    } catch (e: JSONException) {
        emptyList()
    }

    /** eBird taxonomy (fmt=json, locale=lt) to a map of species code to common name. */
    fun taxonomyNames(json: String): Map<String, String> = try {
        val arr = JSONArray(json)
        val map = HashMap<String, String>(arr.length() * 2)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            map[o.getString("speciesCode")] = o.getString("comName")
        }
        map
    } catch (e: JSONException) {
        emptyMap()
    }

    fun namesToJson(map: Map<String, String>): String = JSONObject(map as Map<*, *>).toString()

    fun namesFromJson(json: String): Map<String, String> = try {
        val o = JSONObject(json)
        o.keys().asSequence().associateWith { o.getString(it) }
    } catch (e: JSONException) {
        emptyMap()
    }

    /** Wikipedia REST page summary: thumbnail URL and extract text. */
    fun wikiSummary(json: String): Pair<String?, String?> = try {
        val o = JSONObject(json)
        val image = o.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() }
        val fact = o.optString("extract").takeIf { it.isNotBlank() }
        Pair(image, fact)
    } catch (e: JSONException) {
        Pair(null, null)
    }

    /** First recording's audio URL from a Xeno-canto search response. */
    fun firstRecordingFile(json: String): String? = try {
        val recs = JSONObject(json).optJSONArray("recordings")
        if (recs != null && recs.length() > 0) {
            recs.getJSONObject(0).optString("file").takeIf { it.isNotBlank() }?.let(::absoluteUrl)
        } else null
    } catch (e: JSONException) {
        null
    }

    /** Xeno-canto has returned protocol-relative URLs ("//...") in the past. */
    fun absoluteUrl(url: String): String = if (url.startsWith("//")) "https:$url" else url
}
