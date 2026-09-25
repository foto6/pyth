package com.foto6.dailyfocus.usage

data class UsageSnapshot(
    val firefoxMs: Long,
    val chatGptMs: Long,
    val updatedAt: Long,
    val hasPermission: Boolean
) {
    val totalMs: Long get() = firefoxMs + chatGptMs

    companion object {
        fun empty() = UsageSnapshot(0L, 0L, 0L, false)
    }
}
