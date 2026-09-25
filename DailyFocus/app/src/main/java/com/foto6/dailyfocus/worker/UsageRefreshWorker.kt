package com.foto6.dailyfocus.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.foto6.dailyfocus.usage.UsageCacheStore
import com.foto6.dailyfocus.usage.UsageStatsReader
import com.foto6.dailyfocus.widget.DailyFocusWidgetProvider

class UsageRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return try {
            val snapshot = UsageStatsReader(applicationContext).readToday()
            UsageCacheStore(applicationContext).save(snapshot)
            DailyFocusWidgetProvider.renderAll(applicationContext, snapshot)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
