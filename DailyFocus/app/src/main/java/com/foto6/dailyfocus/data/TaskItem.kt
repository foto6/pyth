package com.foto6.dailyfocus.data

data class TaskItem(
    val id: String,
    val title: String,
    val done: Boolean,
    val createdAt: Long
)
