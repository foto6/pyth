package com.foto6.dailyfocus.usage

import android.content.Context
import java.util.Calendar

class UsageCacheStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(snapshot: UsageSnapshot) {
        prefs.edit()
            .putLong(KEY_FIREFOX, snapshot.firefoxMs.coerceAtLeast(0L))
            .putLong(KEY_CHATGPT, snapshot.chatGptMs.coerceAtLeast(0L))
            .putLong(KEY_UPDATED, snapshot.updatedAt.coerceAtLeast(0L))
            .putBoolean(KEY_PERMISSION, snapshot.hasPermission)
            .commit()
    }

    fun read(now: Long = System.currentTimeMillis()): UsageSnapshot {
        val updatedAt = prefs.getLong(KEY_UPDATED, 0L).coerceAtLeast(0L)
        val hasPermission = prefs.getBoolean(KEY_PERMISSION, false)
        if (updatedAt < startOfToday(now) || updatedAt > now) {
            return UsageSnapshot(0L, 0L, 0L, hasPermission)
        }
        return UsageSnapshot(
            firefoxMs = prefs.getLong(KEY_FIREFOX, 0L).coerceAtLeast(0L),
            chatGptMs = prefs.getLong(KEY_CHATGPT, 0L).coerceAtLeast(0L),
            updatedAt = updatedAt,
            hasPermission = hasPermission
        )
    }

    private fun startOfToday(now: Long): Long = Calendar.getInstance().run {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private companion object {
        const val PREFS = "daily_focus_usage_cache"
        const val KEY_FIREFOX = "firefox_ms"
        const val KEY_CHATGPT = "chatgpt_ms"
        const val KEY_UPDATED = "updated_at"
        const val KEY_PERMISSION = "has_permission"
    }
}
