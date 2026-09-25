package com.foto6.dailyfocus.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

class UsageStatsReader(private val context: Context) {
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun readToday(now: Long = System.currentTimeMillis()): UsageSnapshot {
        if (!hasUsageAccess()) {
            return UsageSnapshot(0L, 0L, now, false)
        }

        val start = startOfToday(now)
        val maxPossible = (now - start).coerceAtLeast(0L)
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        return try {
            val stats = manager.queryAndAggregateUsageStats(start, now)
            val firefox = stats[FIREFOX_PACKAGE]?.totalTimeInForeground
                ?.coerceIn(0L, maxPossible) ?: 0L
            val chatGpt = stats[CHATGPT_PACKAGE]?.totalTimeInForeground
                ?.coerceIn(0L, maxPossible) ?: 0L

            UsageSnapshot(
                firefoxMs = firefox,
                chatGptMs = chatGpt,
                updatedAt = now,
                hasPermission = true
            )
        } catch (_: SecurityException) {
            UsageSnapshot(0L, 0L, now, false)
        } catch (_: RuntimeException) {
            val cached = UsageCacheStore(context).read()
            if (cached.updatedAt >= start && cached.updatedAt <= now) {
                cached.copy(hasPermission = true)
            } else {
                UsageSnapshot(0L, 0L, now, true)
            }
        }
    }

    private fun startOfToday(now: Long): Long = Calendar.getInstance().run {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    companion object {
        const val FIREFOX_PACKAGE = "org.mozilla.firefox"
        const val CHATGPT_PACKAGE = "com.openai.chatgpt"
    }
}
