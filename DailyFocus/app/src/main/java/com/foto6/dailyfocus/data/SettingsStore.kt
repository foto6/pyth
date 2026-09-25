package com.foto6.dailyfocus.data

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var goalMinutes: Int
        get() = prefs.getInt(KEY_GOAL_MINUTES, DEFAULT_GOAL_MINUTES).coerceIn(MIN_GOAL, MAX_GOAL)
        set(value) {
            prefs.edit().putInt(KEY_GOAL_MINUTES, value.coerceIn(MIN_GOAL, MAX_GOAL)).commit()
        }

    companion object {
        const val DEFAULT_GOAL_MINUTES = 120
        const val MIN_GOAL = 15
        const val MAX_GOAL = 24 * 60
        private const val PREFS = "daily_focus_settings"
        private const val KEY_GOAL_MINUTES = "goal_minutes"
    }
}
