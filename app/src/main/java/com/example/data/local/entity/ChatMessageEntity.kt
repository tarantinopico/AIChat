package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["threadId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: Long,
    val role: String, // "user" or "model"
    val cleanTextContent: String,
    val thinkingContent: String? = null,
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val thinkingTokens: Int = 0,
    val modelId: String = "",
    val finishReason: String? = null,
    val isPinned: Boolean = false,
    val isError: Boolean = false,
    val errorSummary: String? = null,
    val rawErrorJson: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
