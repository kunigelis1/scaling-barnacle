package com.example.nightskywings

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SkyWorker(private val ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val (lat, lon) = currentLatLon(ctx)
        val cloud = WeatherApi.tonightCloudCover(lat, lon) ?: return Result.retry()
        if (cloud <= Config.CLEAR_THRESHOLD) {
            val moon = MoonPhase.illuminationPercent()
            Notifier.send(
                ctx, 1, "Clear skies tonight",
                "Cloud cover about $cloud%. ${MoonPhase.name()}, $moon% lit: ${MoonPhase.tip(moon)}."
            )
        }
        return Result.success()
    }
}

class RareBirdWorker(private val ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val prefs = ctx.getSharedPreferences("nsw", Context.MODE_PRIVATE)
        val seen = prefs.getStringSet("seen_rare", emptySet())!!.toMutableSet()
        val fresh = BirdApi.rareInLithuania(ctx).filter { (it.speciesCode + it.date) !in seen }
        fresh.take(3).forEach {
            Notifier.send(
                ctx, 100 + (it.speciesCode.hashCode() and 0xff),
                "Rare bird: ${it.englishName} / ${it.lithuanianName}",
                "Spotted at ${it.place} on ${it.date}."
            )
        }
        fresh.forEach { seen.add(it.speciesCode + it.date) }
        prefs.edit().putStringSet("seen_rare", seen).apply()
        return Result.success()
    }
}

class MorningBirdWorker(private val ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val (lat, lon) = currentLatLon(ctx)
        val birds = BirdApi.nearby(ctx, lat, lon).take(5)
        if (birds.isNotEmpty()) {
            Notifier.send(
                ctx, 2, "Likely birds near you today",
                birds.joinToString(", ") { "${it.englishName} (${it.lithuanianName})" }
            )
        }
        val pick = birds.randomOrNull() ?: BirdApi.rareInLithuania(ctx).randomOrNull()
        if (pick != null) {
            Notifier.send(
                ctx, 3, "Bird of the day: ${pick.englishName} / ${pick.lithuanianName}",
                "Open the app for the picture, fact and song."
            )
        }
        return Result.success()
    }
}
