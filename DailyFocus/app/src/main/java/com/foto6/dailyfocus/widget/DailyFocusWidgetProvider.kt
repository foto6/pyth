package com.foto6.dailyfocus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.foto6.dailyfocus.MainActivity
import com.foto6.dailyfocus.R
import com.foto6.dailyfocus.core.FocusMath
import com.foto6.dailyfocus.core.WidgetLayoutPolicy
import com.foto6.dailyfocus.core.WidgetMode
import com.foto6.dailyfocus.data.SettingsStore
import com.foto6.dailyfocus.data.TaskItem
import com.foto6.dailyfocus.data.TaskStore
import com.foto6.dailyfocus.usage.UsageCacheStore
import com.foto6.dailyfocus.usage.UsageSnapshot
import com.foto6.dailyfocus.usage.UsageStatsReader
import com.foto6.dailyfocus.worker.WorkScheduler

class DailyFocusWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WorkScheduler.schedule(context)
        refreshAndRenderAll(context)
    }

    override fun onDisabled(context: Context) {
        WorkScheduler.cancel(context)
        super.onDisabled(context)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, manager, appWidgetIds)
        val snapshot = UsageStatsReader(context).readToday()
        UsageCacheStore(context).save(snapshot)
        appWidgetIds.forEach { renderOne(context, manager, it, snapshot) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        renderOne(context, appWidgetManager, appWidgetId, UsageCacheStore(context).read())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> refreshAndRenderAll(context)
            ACTION_TOGGLE_TASK -> {
                val id = intent.getStringExtra(EXTRA_TASK_ID)
                if (!id.isNullOrBlank()) TaskStore(context).toggle(id)
                renderAll(context, UsageCacheStore(context).read())
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.foto6.dailyfocus.widget.REFRESH"
        private const val ACTION_TOGGLE_TASK = "com.foto6.dailyfocus.widget.TOGGLE_TASK"
        private const val EXTRA_TASK_ID = "task_id"

        private val taskRows = intArrayOf(
            R.id.widget_task_row_1,
            R.id.widget_task_row_2,
            R.id.widget_task_row_3,
            R.id.widget_task_row_4
        )
        private val taskMarks = intArrayOf(
            R.id.widget_task_mark_1,
            R.id.widget_task_mark_2,
            R.id.widget_task_mark_3,
            R.id.widget_task_mark_4
        )
        private val taskTitles = intArrayOf(
            R.id.widget_task_title_1,
            R.id.widget_task_title_2,
            R.id.widget_task_title_3,
            R.id.widget_task_title_4
        )

        fun refreshAndRenderAll(context: Context) {
            val snapshot = UsageStatsReader(context).readToday()
            UsageCacheStore(context).save(snapshot)
            renderAll(context, snapshot)
        }

        fun renderAll(context: Context, snapshot: UsageSnapshot = UsageCacheStore(context).read()) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DailyFocusWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { renderOne(context, manager, it, snapshot) }
        }

        private fun renderOne(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
            snapshot: UsageSnapshot
        ) {
            val settings = SettingsStore(context)
            val tasks = TaskStore(context).getTasks()

            val options = manager.getAppWidgetOptions(appWidgetId)
            val widgetWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250)
            val widgetHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 185)
            val layout = WidgetLayoutPolicy.forSize(widgetWidth, widgetHeight)
            val layoutRes = when (layout.mode) {
                WidgetMode.NARROW -> R.layout.widget_daily_focus_narrow
                WidgetMode.COMPACT -> R.layout.widget_daily_focus_compact
                WidgetMode.FULL -> R.layout.widget_daily_focus
            }
            val views = RemoteViews(context.packageName, layoutRes)

            val goal = settings.goalMinutes
            val total = snapshot.totalMs
            val over = FocusMath.overGoalMs(total, goal)

            views.setTextViewText(R.id.widget_usage, FocusMath.formatDuration(total))
            views.setTextViewText(R.id.widget_goal_pill, "Цель ${FocusMath.formatGoal(goal)}")
            views.setProgressBar(
                R.id.widget_progress,
                1000,
                FocusMath.progressPermille(total, goal),
                false
            )

            val (status, statusColor) = when {
                !snapshot.hasPermission ->
                    "Нужен доступ к статистике" to R.color.widget_warning
                over > 0L ->
                    "✓ Цель выполнена · +${FocusMath.formatDuration(over)}" to R.color.widget_leaf_dark
                else ->
                    "До цели ${FocusMath.formatDuration(FocusMath.remainingMs(total, goal))}" to R.color.widget_sky
            }
            views.setTextViewText(R.id.widget_status, status)
            views.setTextColor(R.id.widget_status, context.getColor(statusColor))
            views.setTextViewText(R.id.widget_firefox_time, FocusMath.formatDuration(snapshot.firefoxMs))
            views.setTextViewText(R.id.widget_chatgpt_time, FocusMath.formatDuration(snapshot.chatGptMs))

            views.setTextViewText(
                R.id.widget_title,
                if (layout.mode == WidgetMode.NARROW) "Focus" else "Daily Focus"
            )
            views.setTextViewText(
                R.id.widget_usage_caption,
                if (layout.mode == WidgetMode.NARROW) "из ${FocusMath.formatGoal(goal)}" else "сегодня"
            )
            views.setTextViewText(
                R.id.widget_goal_pill,
                if (layout.mode == WidgetMode.COMPACT) FocusMath.formatGoal(goal) else "Цель ${FocusMath.formatGoal(goal)}"
            )

            views.setViewVisibility(
                R.id.widget_subtitle,
                if (layout.showSubtitle) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widget_status,
                if (layout.showStatus) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widget_apps,
                if (layout.showApps) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widget_tasks_header,
                if (layout.showTasksHeader) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widget_goal_pill,
                if (layout.showGoalPill) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widget_refresh,
                if (layout.showRefresh) View.VISIBLE else View.GONE
            )

            val completed = tasks.count { it.done }
            views.setTextViewText(
                R.id.widget_tasks_count,
                if (tasks.isEmpty()) "0 задач" else "$completed / ${tasks.size}"
            )
            views.setViewVisibility(
                R.id.widget_empty_tasks,
                if (layout.showTasksHeader && tasks.isEmpty()) View.VISIBLE else View.GONE
            )
            bindTasks(context, views, if (layout.showTasksHeader) tasks.take(layout.maxTasks) else emptyList())

            val open = openAppIntent(context)
            // Explicit zones: background taps do nothing, so task toggles cannot
            // be shadowed by a root-level open-app action.
            views.setOnClickPendingIntent(R.id.widget_title_area, open)
            views.setOnClickPendingIntent(R.id.widget_goal_pill, open)
            views.setOnClickPendingIntent(R.id.widget_tasks_header, open)
            views.setOnClickPendingIntent(R.id.widget_empty_tasks, open)
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshIntent(context))
            manager.updateAppWidget(appWidgetId, views)
        }

        private fun bindTasks(context: Context, views: RemoteViews, tasks: List<TaskItem>) {
            taskRows.indices.forEach { index ->
                val task = tasks.getOrNull(index)
                if (task == null) {
                    views.setViewVisibility(taskRows[index], View.GONE)
                } else {
                    views.setViewVisibility(taskRows[index], View.VISIBLE)
                    views.setTextViewText(taskMarks[index], if (task.done) "✓" else "○")
                    views.setTextColor(
                        taskMarks[index],
                        context.getColor(if (task.done) R.color.widget_leaf else R.color.widget_faint)
                    )
                    views.setTextViewText(taskTitles[index], task.title)
                    views.setTextColor(
                        taskTitles[index],
                        context.getColor(if (task.done) R.color.widget_task_done else R.color.widget_text)
                    )
                    val toggle = toggleTaskIntent(context, task.id)
                    // Entire row is the primary 48dp target. Bind the mark and
                    // title too for consistent launcher hit-testing.
                    views.setOnClickPendingIntent(taskRows[index], toggle)
                    views.setOnClickPendingIntent(taskMarks[index], toggle)
                    views.setOnClickPendingIntent(taskTitles[index], toggle)
                }
            }
        }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                100,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun refreshIntent(context: Context): PendingIntent {
            val intent = Intent(context, DailyFocusWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                data = Uri.parse("dailyfocus://widget/refresh")
            }
            return PendingIntent.getBroadcast(
                context,
                101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun toggleTaskIntent(context: Context, id: String): PendingIntent {
            val intent = Intent(context, DailyFocusWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TASK
                data = Uri.parse("dailyfocus://widget/task/$id")
                putExtra(EXTRA_TASK_ID, id)
            }
            return PendingIntent.getBroadcast(
                context,
                id.hashCode() and 0x7fffffff,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
