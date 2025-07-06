package com.apptolast.models

import kotlinx.serialization.Serializable

@Serializable
data class Chapter(
    val id: String,
    val bookId: String,
    val userId: String,
    val title: String,
    val content: String,
    val editedDate: String,
    val orderIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false
)
