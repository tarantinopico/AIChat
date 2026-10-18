package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "file_artifacts",
    foreignKeys = [
        ForeignKey(
            entity = ChatMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChatThreadEntity::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["messageId"]), Index(value = ["threadId"])]
)
data class FileArtifactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long,
    val threadId: Long,
    val fileName: String,
    val language: String,
    val mimeType: String,
    val content: String,
    val lineCount: Int,
    val sizeBytes: Long,
    val isComplete: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
