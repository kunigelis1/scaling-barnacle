package com.example.nightskywings

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {
    private val online = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun scheduleAll(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        // Stargazing check every day at 16:00
        wm.enqueueUniquePeriodicWork(
            "sky", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<SkyWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(TimeMath.millisUntil(16, 0), TimeUnit.MILLISECONDS)
                .setConstraints(online).build()
        )
        // Birds near you and bird of the day at 07:30
        wm.enqueueUniquePeriodicWork(
            "morning", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<MorningBirdWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(TimeMath.millisUntil(7, 30), TimeUnit.MILLISECONDS)
                .setConstraints(online).build()
        )
        // Rare bird check every 3 hours
        wm.enqueueUniquePeriodicWork(
            "rare", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<RareBirdWorker>(3, TimeUnit.HOURS)
                .setConstraints(online).build()
        )
    }
}
