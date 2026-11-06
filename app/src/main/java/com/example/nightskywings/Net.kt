package com.example.nightskywings

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object Net {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Returns the response body, or null on any network error or non-2xx status. Never throws. */
    fun get(url: String, headers: Map<String, String> = emptyMap()): String? {
        val builder = Request.Builder().url(url)
            .header("User-Agent", "NightSkyWings/1.0")
        headers.forEach { (k, v) -> builder.header(k, v) }
        return try {
            client.newCall(builder.build()).execute().use { r ->
                if (r.isSuccessful) r.body?.string() else null
            }
        } catch (e: IOException) {
            null
        }
    }
}
