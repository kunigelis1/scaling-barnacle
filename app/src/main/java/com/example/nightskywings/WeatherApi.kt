package com.example.nightskywings

object WeatherApi {
    // Average cloud cover (percent) from 21:00 tonight to 03:00 tomorrow
    fun tonightCloudCover(lat: Double, lon: Double): Int? {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&hourly=cloud_cover&forecast_days=2&timezone=auto"
        return Parsers.tonightCloudCover(Net.get(url) ?: return null)
    }
}
