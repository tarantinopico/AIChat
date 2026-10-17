package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatThreadEntity
import com.example.data.local.entity.FileArtifactEntity
import kotlinx.coroutines.flow.Flow

data class ModelTokenStats(
    val modelId: String,
    val totalPromptTokens: Long,
    val totalCandidateTokens: Long,
    val totalThinkingTokens: Long,
    val messageCount: Int
)

data class OverallTokenStats(
    val totalPromptTokens: Long,
    val totalCandidateTokens: Long,
    val totalThinkingTokens: Long,
    val totalMessages: Int
)

@Dao
interface ChatDao {

    // --- Threads ---
    @Query("SELECT * FROM chat_threads ORDER BY updatedAt DESC")
    fun getAllThreads(): Flow<List<ChatThreadEntity>>

    @Query("SELECT * FROM chat_threads WHERE id = :id")
    suspend fun getThreadById(id: Long): ChatThreadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ChatThreadEntity): Long

    @Update
    suspend fun updateThread(thread: ChatThreadEntity)

    @Query("UPDATE chat_threads SET updatedAt = :timestamp WHERE id = :id")
    suspend fun touchThread(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM chat_threads WHERE id = :id")
    suspend fun deleteThreadById(id: Long)

    @Query("DELETE FROM chat_threads")
    suspend fun deleteAllThreads()

    // --- Messages ---
    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY createdAt ASC, id ASC")
    fun getMessagesForThread(threadId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY createdAt ASC, id ASC")
    suspend fun getMessagesForThreadSync(threadId: Long): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :id")
    suspend fun getMessageById(id: Long): ChatMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)

    // Edit message branching: delete this message and all subsequent messages in thread
    @Query("DELETE FROM chat_messages WHERE threadId = :threadId AND id >= :fromMessageId")
    suspend fun deleteMessagesFrom(threadId: Long, fromMessageId: Long)

    @Query("UPDATE chat_messages SET isPinned = :isPinned WHERE id = :id")
    suspend fun updateMessagePinStatus(id: Long, isPinned: Boolean)

    // --- Files ---
    @Query("SELECT * FROM file_artifacts WHERE messageId = :messageId ORDER BY id ASC")
    fun getFilesForMessage(messageId: Long): Flow<List<FileArtifactEntity>>

    @Query("SELECT * FROM file_artifacts WHERE messageId = :messageId ORDER BY id ASC")
    suspend fun getFilesForMessageSync(messageId: Long): List<FileArtifactEntity>

    @Query("SELECT * FROM file_artifacts WHERE threadId = :threadId ORDER BY createdAt DESC")
    fun getFilesForThread(threadId: Long): Flow<List<FileArtifactEntity>>

    @Query("SELECT * FROM file_artifacts WHERE id = :id")
    suspend fun getFileById(id: Long): FileArtifactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileArtifactEntity): Long

    @Query("DELETE FROM file_artifacts WHERE messageId = :messageId")
    suspend fun deleteFilesForMessage(messageId: Long)

    // --- Token Statistics for $300 Free Trial Dashboard ---
    @Query("""
        SELECT 
            modelId, 
            SUM(promptTokens) as totalPromptTokens, 
            SUM(candidateTokens) as totalCandidateTokens, 
            SUM(thinkingTokens) as totalThinkingTokens,
            COUNT(id) as messageCount
        FROM chat_messages 
        WHERE role = 'model' AND modelId != ''
        GROUP BY modelId
    """)
    fun getTokenStatsByModel(): Flow<List<ModelTokenStats>>

    @Query("""
        SELECT 
            COALESCE(SUM(promptTokens), 0) as totalPromptTokens, 
            COALESCE(SUM(candidateTokens), 0) as totalCandidateTokens, 
            COALESCE(SUM(thinkingTokens), 0) as totalThinkingTokens,
            COUNT(id) as totalMessages
        FROM chat_messages 
        WHERE role = 'model'
    """)
    fun getOverallTokenStats(): Flow<OverallTokenStats>
}
