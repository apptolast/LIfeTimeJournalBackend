package com.apptolast.models

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val coverImage: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true
)