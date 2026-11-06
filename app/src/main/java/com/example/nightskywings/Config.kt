package com.example.nightskywings

object Config {
    // Keys come from local.properties via BuildConfig. See HANDOFF.md.
    val EBIRD_KEY: String = BuildConfig.EBIRD_KEY
    val XENO_KEY: String = BuildConfig.XENO_KEY

    // Fallback location: Klaipeda
    const val DEFAULT_LAT = 55.7033
    const val DEFAULT_LON = 21.1443

    // A clear night means average cloud cover at or below this percent
    const val CLEAR_THRESHOLD = 30

    // Re-download the Lithuanian bird-name list after this many days
    const val TAXONOMY_MAX_AGE_DAYS = 30
}
