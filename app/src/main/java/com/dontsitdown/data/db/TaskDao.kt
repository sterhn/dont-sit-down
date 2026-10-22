package com.dontsitdown.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dontsitdown.data.model.Task
import com.dontsitdown.data.model.TaskCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY sortOrder, createdAt")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY sortOrder, createdAt")
    suspend fun getAll(): List<Task>

    @Query("SELECT * FROM tasks WHERE isActive = 1 ORDER BY sortOrder, createdAt")
    suspend fun getActive(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: String): Task?

    @Query("SELECT * FROM tasks WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observe(id: String): Flow<Task?>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM tasks")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: Task)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tasks: List<Task>)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("UPDATE tasks SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Int)

    @Transaction
    suspend fun reorder(ids: List<String>) {
        ids.forEachIndexed { i, id -> setSortOrder(id, i) }
    }

    @Insert
    suspend fun insertCompletion(c: TaskCompletion)

    @Query("SELECT * FROM completions WHERE taskId = :taskId ORDER BY date DESC")
    fun observeCompletions(taskId: String): Flow<List<TaskCompletion>>

    @Query("SELECT * FROM completions WHERE date >= :from AND date < :to ORDER BY date")
    fun observeCompletionsBetween(from: Long, to: Long): Flow<List<TaskCompletion>>

    @Query("SELECT * FROM completions WHERE date >= :from AND date < :to ORDER BY date")
    suspend fun completionsBetween(from: Long, to: Long): List<TaskCompletion>

    @Query("SELECT MAX(date) FROM completions")
    suspend fun lastCompletionAt(): Long?
}
