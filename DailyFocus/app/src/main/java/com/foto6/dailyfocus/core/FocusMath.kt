package com.foto6.dailyfocus.core

import kotlin.math.roundToInt

object FocusMath {
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 60L * MINUTE_MS

    fun formatDuration(milliseconds: Long): String {
        val safe = milliseconds.coerceAtLeast(0L)
        if (safe in 1 until MINUTE_MS) return "<1м"
        val totalMinutes = safe / MINUTE_MS
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}ч ${minutes}м"
            hours > 0 -> "${hours}ч"
            else -> "${minutes}м"
        }
    }

    fun formatGoal(minutes: Int): String = formatDuration(minutes.coerceAtLeast(0) * MINUTE_MS)

    fun progressPermille(usedMs: Long, goalMinutes: Int): Int {
        if (goalMinutes <= 0) return 0
        val goalMs = goalMinutes * MINUTE_MS
        return ((usedMs.coerceAtLeast(0).toDouble() / goalMs) * 1000.0)
            .roundToInt()
            .coerceIn(0, 1000)
    }

    fun overGoalMs(usedMs: Long, goalMinutes: Int): Long {
        val goalMs = goalMinutes.coerceAtLeast(0) * MINUTE_MS
        return (usedMs.coerceAtLeast(0L) - goalMs).coerceAtLeast(0L)
    }

    fun remainingMs(usedMs: Long, goalMinutes: Int): Long {
        val goalMs = goalMinutes.coerceAtLeast(0) * MINUTE_MS
        return (goalMs - usedMs.coerceAtLeast(0L)).coerceAtLeast(0L)
    }
}
