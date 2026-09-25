package com.foto6.dailyfocus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.foto6.dailyfocus.core.FocusMath
import com.foto6.dailyfocus.data.SettingsStore
import com.foto6.dailyfocus.data.TaskItem
import com.foto6.dailyfocus.data.TaskStore
import com.foto6.dailyfocus.ui.theme.DailyFocusTheme
import com.foto6.dailyfocus.usage.UsageCacheStore
import com.foto6.dailyfocus.usage.UsageSnapshot
import com.foto6.dailyfocus.usage.UsageStatsReader
import com.foto6.dailyfocus.widget.DailyFocusWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val taskStore by lazy { TaskStore(this) }
    private val settingsStore by lazy { SettingsStore(this) }
    private val usageCache by lazy { UsageCacheStore(this) }
    private val usageReader by lazy { UsageStatsReader(this) }

    private var uiState by mutableStateOf(UiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyFocusTheme {
                DailyFocusScreen(
                    state = uiState,
                    onGrantUsage = ::openUsageAccess,
                    onRefresh = ::refreshEverything,
                    onAddTask = ::addTask,
                    onToggleTask = ::toggleTask,
                    onDeleteTask = ::deleteTask,
                    onClearCompleted = ::clearCompleted,
                    onSetGoal = ::setGoal
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshEverything()
    }

    private fun refreshEverything() {
        val snapshot = usageReader.readToday()
        usageCache.save(snapshot)
        uiState = UiState(
            usage = snapshot,
            tasks = taskStore.getTasks(),
            goalMinutes = settingsStore.goalMinutes
        )
        DailyFocusWidgetProvider.renderAll(this, snapshot)
    }

    private fun refreshTasksOnly() {
        uiState = uiState.copy(
            tasks = taskStore.getTasks(),
            goalMinutes = settingsStore.goalMinutes
        )
        DailyFocusWidgetProvider.renderAll(this, usageCache.read())
    }

    private fun addTask(title: String) {
        taskStore.add(title)
        refreshTasksOnly()
    }

    private fun toggleTask(id: String) {
        taskStore.toggle(id)
        refreshTasksOnly()
    }

    private fun deleteTask(id: String) {
        taskStore.delete(id)
        refreshTasksOnly()
    }

    private fun clearCompleted() {
        taskStore.clearCompleted()
        refreshTasksOnly()
    }

    private fun setGoal(minutes: Int) {
        settingsStore.goalMinutes = minutes
        refreshTasksOnly()
    }

    private fun openUsageAccess() {
        val direct = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(direct)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }
}

data class UiState(
    val usage: UsageSnapshot = UsageSnapshot.empty(),
    val tasks: List<TaskItem> = emptyList(),
    val goalMinutes: Int = SettingsStore.DEFAULT_GOAL_MINUTES
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DailyFocusScreen(
    state: UiState,
    onGrantUsage: () -> Unit,
    onRefresh: () -> Unit,
    onAddTask: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearCompleted: () -> Unit,
    onSetGoal: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Medium)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Header(
                    goalMinutes = state.goalMinutes,
                    onGoalClick = { showGoalDialog = true },
                    onRefresh = onRefresh
                )
            }
            item {
                UsageCard(
                    usage = state.usage,
                    goalMinutes = state.goalMinutes,
                    onGrantUsage = onGrantUsage
                )
            }
            item {
                TasksHeader(
                    tasks = state.tasks,
                    onClearCompleted = onClearCompleted
                )
            }
            if (state.tasks.isEmpty()) {
                item { EmptyTasksCard() }
            } else {
                items(state.tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onToggle = { onToggleTask(task.id) },
                        onDelete = { onDeleteTask(task.id) }
                    )
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onAdd = {
                onAddTask(it)
                showAddDialog = false
            }
        )
    }

    if (showGoalDialog) {
        GoalDialog(
            currentMinutes = state.goalMinutes,
            onDismiss = { showGoalDialog = false },
            onSave = {
                onSetGoal(it)
                showGoalDialog = false
            }
        )
    }
}

@Composable
private fun Header(goalMinutes: Int, onGoalClick: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "СЕГОДНЯ",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
            Text(
                text = "Daily Focus",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
        TextButton(onClick = onRefresh) { Text("↻") }
        Surface(
            onClick = onGoalClick,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "Цель ${FocusMath.formatGoal(goalMinutes)}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun UsageCard(usage: UsageSnapshot, goalMinutes: Int, onGrantUsage: () -> Unit) {
    val total = usage.totalMs
    val over = FocusMath.overGoalMs(total, goalMinutes)
    val fraction = FocusMath.progressPermille(total, goalMinutes) / 1000f

    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = FocusMath.formatDuration(total),
                    fontSize = 38.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "  / ${FocusMath.formatGoal(goalMinutes)}",
                    modifier = Modifier.padding(bottom = 5.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            MaterialTheme.colorScheme.primary
                        )
                )
            }

            Text(
                text = when {
                    !usage.hasPermission -> "Чтобы считать время автоматически, дай доступ к статистике использования."
                    over > 0 -> "Цель выполнена · +${FocusMath.formatDuration(over)}"
                    else -> "Осталось ${FocusMath.formatDuration(FocusMath.remainingMs(total, goalMinutes))}"
                },
                color = if (over > 0 && usage.hasPermission) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppStatPill(
                    name = "Firefox",
                    value = FocusMath.formatDuration(usage.firefoxMs),
                    modifier = Modifier.weight(1f)
                )
                AppStatPill(
                    name = "ChatGPT",
                    value = FocusMath.formatDuration(usage.chatGptMs),
                    modifier = Modifier.weight(1f)
                )
            }

            if (!usage.hasPermission) {
                Button(onClick = onGrantUsage, modifier = Modifier.fillMaxWidth()) {
                    Text("Разрешить статистику")
                }
            } else if (usage.updatedAt > 0L) {
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(usage.updatedAt))
                Text(
                    text = "Обновлено $time",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun AppStatPill(name: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text(name, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun TasksHeader(tasks: List<TaskItem>, onClearCompleted: () -> Unit) {
    val done = tasks.count { it.done }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Задачи", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(
            "$done/${tasks.size}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        if (done > 0) {
            Spacer(Modifier.width(6.dp))
            TextButton(onClick = onClearCompleted) { Text("Очистить") }
        }
    }
}

@Composable
private fun EmptyTasksCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Пока пусто", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.height(5.dp))
            Text(
                "Нажми + и добавь первую задачу на сегодня.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun TaskRow(task: TaskItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.done, onCheckedChange = { onToggle() })
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                fontWeight = FontWeight.Medium
            )
            TextButton(onClick = onDelete, modifier = Modifier.size(width = 46.dp, height = 42.dp)) {
                Text("×", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая задача") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(120) },
                label = { Text("Что сделать?") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (text.isNotBlank()) onAdd(text) }, enabled = text.isNotBlank()) {
                Text("Добавить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun GoalDialog(currentMinutes: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var minutesText by remember(currentMinutes) { mutableStateOf(currentMinutes.toString()) }
    val parsed = minutesText.toIntOrNull()?.takeIf { it in SettingsStore.MIN_GOAL..SettingsStore.MAX_GOAL }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Дневная цель") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Общая цель для Firefox + ChatGPT. Можно поставить от 15 минут до 24 часов.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 120, 180, 240).forEach { minutes ->
                        Surface(
                            onClick = { minutesText = minutes.toString() },
                            shape = RoundedCornerShape(14.dp),
                            color = if (minutesText == minutes.toString()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = FocusMath.formatGoal(minutes),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                color = if (minutesText == minutes.toString()) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { value -> minutesText = value.filter(Char::isDigit).take(4) },
                    label = { Text("Минут") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        if (parsed != null) Text("Это ${FocusMath.formatGoal(parsed)}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { parsed?.let(onSave) }, enabled = parsed != null) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
