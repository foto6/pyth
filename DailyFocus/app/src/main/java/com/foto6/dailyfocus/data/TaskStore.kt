package com.foto6.dailyfocus.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TaskStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun getTasks(): List<TaskItem> = synchronized(lock) {
        val current = rollOverIfNeeded(decode(prefs.getString(KEY_TASKS, null)))
        current.sortedWith(compareBy<TaskItem> { it.done }.thenBy { it.createdAt })
    }

    fun add(title: String): TaskItem? = synchronized(lock) {
        val clean = title.trim().replace(Regex("\\s+"), " ").take(120)
        if (clean.isBlank()) return null

        val current = rollOverIfNeeded(decode(prefs.getString(KEY_TASKS, null)))
        val item = TaskItem(
            id = UUID.randomUUID().toString(),
            title = clean,
            done = false,
            createdAt = System.currentTimeMillis()
        )
        persist(current + item)
        item
    }

    fun toggle(id: String): Boolean = synchronized(lock) {
        val current = rollOverIfNeeded(decode(prefs.getString(KEY_TASKS, null)))
        var changed = false
        val updated = current.map {
            if (it.id == id) {
                changed = true
                it.copy(done = !it.done)
            } else it
        }
        if (changed) persist(updated)
        changed
    }

    fun delete(id: String): Boolean = synchronized(lock) {
        val current = rollOverIfNeeded(decode(prefs.getString(KEY_TASKS, null)))
        val updated = current.filterNot { it.id == id }
        val changed = updated.size != current.size
        if (changed) persist(updated)
        changed
    }

    fun clearCompleted(): Int = synchronized(lock) {
        val current = rollOverIfNeeded(decode(prefs.getString(KEY_TASKS, null)))
        val updated = current.filterNot { it.done }
        val removed = current.size - updated.size
        if (removed > 0) persist(updated)
        removed
    }

    private fun rollOverIfNeeded(tasks: List<TaskItem>): List<TaskItem> {
        val today = todayKey()
        val storedDay = prefs.getString(KEY_DAY, null)
        if (storedDay == today) return tasks

        // New day: completed items disappear, unfinished items roll over.
        val rolled = tasks.filterNot { it.done }
        persist(rolled, today)
        return rolled
    }

    private fun persist(tasks: List<TaskItem>, day: String = todayKey()) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("done", task.done)
                    put("createdAt", task.createdAt)
                }
            )
        }
        prefs.edit()
            .putString(KEY_TASKS, array.toString())
            .putString(KEY_DAY, day)
            .commit()
    }

    private fun decode(raw: String?): List<TaskItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    val title = obj.optString("title").trim()
                    if (id.isBlank() || title.isBlank()) continue
                    add(
                        TaskItem(
                            id = id,
                            title = title.take(120),
                            done = obj.optBoolean("done", false),
                            createdAt = obj.optLong("createdAt", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())

    private companion object {
        val lock = Any()
        const val PREFS = "daily_focus_tasks"
        const val KEY_TASKS = "tasks_json"
        const val KEY_DAY = "day_key"
    }
}
