package com.foto6.dailyfocus.core

enum class WidgetMode { NARROW, COMPACT, FULL }

data class WidgetLayout(
    val mode: WidgetMode,
    val showSubtitle: Boolean,
    val showStatus: Boolean,
    val showApps: Boolean,
    val showTasksHeader: Boolean,
    val showGoalPill: Boolean,
    val showRefresh: Boolean,
    val maxTasks: Int
)

object WidgetLayoutPolicy {
    private const val TASK_ROW_DP = 55
    private const val TASK_HEADER_DP = 29

    fun forSize(widthDp: Int, heightDp: Int): WidgetLayout {
        val w = widthDp.coerceAtLeast(0)
        val h = heightDp.coerceAtLeast(0)
        val mode = when {
            w < 150 -> WidgetMode.NARROW
            h < 160 -> WidgetMode.COMPACT
            w < 280 -> WidgetMode.COMPACT
            else -> WidgetMode.FULL
        }

        val coreHeight = when (mode) {
            WidgetMode.NARROW -> 102
            WidgetMode.COMPACT -> 103
            WidgetMode.FULL -> 117
        }
        var remaining = (h - coreHeight).coerceAtLeast(0)

        val reserveForOneTask = TASK_HEADER_DP + TASK_ROW_DP
        val showStatus = mode != WidgetMode.NARROW && remaining >= reserveForOneTask + 29
        if (showStatus) remaining -= 29

        val showApps = mode == WidgetMode.FULL && remaining >= reserveForOneTask + 50
        if (showApps) remaining -= 50

        val showTasksHeader = remaining >= reserveForOneTask
        val maxTasks = if (showTasksHeader) {
            ((remaining - TASK_HEADER_DP) / TASK_ROW_DP).coerceIn(1, 4)
        } else 0

        return WidgetLayout(
            mode = mode,
            showSubtitle = mode == WidgetMode.FULL && w >= 320,
            showStatus = showStatus,
            showApps = showApps,
            showTasksHeader = showTasksHeader,
            showGoalPill = mode != WidgetMode.NARROW && w >= 190,
            showRefresh = mode != WidgetMode.NARROW && w >= 180,
            maxTasks = maxTasks
        )
    }

    fun forHeight(heightDp: Int): WidgetLayout = forSize(320, heightDp)
}
