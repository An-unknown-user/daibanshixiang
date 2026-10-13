package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ChecklistItem
import com.example.data.model.TodoItem
import com.example.data.model.TodoWithChecklist
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {

    @Transaction
    @Query("SELECT * FROM todos ORDER BY startTime ASC")
    fun getAllTodosWithChecklist(): Flow<List<TodoWithChecklist>>

    @Transaction
    @Query("SELECT * FROM todos WHERE id = :id")
    fun getTodoWithChecklistById(id: Long): Flow<TodoWithChecklist?>

    @Transaction
    @Query("SELECT * FROM todos WHERE id = :id")
    suspend fun getTodoWithChecklistByIdSync(id: Long): TodoWithChecklist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem): Long

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)

    @Query("DELETE FROM todos WHERE id = :id")
    suspend fun deleteTodoById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItem>)

    @Query("DELETE FROM checklist_items WHERE todoId = :todoId")
    suspend fun deleteChecklistItemsByTodoId(todoId: Long)

    @Update
    suspend fun updateChecklistItem(item: ChecklistItem)

    @Query("UPDATE checklist_items SET isChecked = :isChecked WHERE id = :id")
    suspend fun setChecklistItemStatus(id: Long, isChecked: Boolean)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItemById(id: Long)

    @Transaction
    @Query("SELECT * FROM todos WHERE isCompleted = 0 AND remindType = 'SMART'")
    suspend fun getUncompletedSmartTodosSync(): List<TodoWithChecklist>

    @Transaction
    @Query("SELECT * FROM todos WHERE isCompleted = 0")
    suspend fun getUncompletedTodosSync(): List<TodoWithChecklist>

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0 AND remindType = 'SMART'")
    fun getUncompletedSmartTodosCountFlow(): Flow<Int>

    @Query("UPDATE todos SET preNoticeSent = :sent WHERE id = :id")
    suspend fun updatePreNoticeSent(id: Long, sent: Boolean)

    @Query("UPDATE todos SET lastSmartNotifyAt = :notifyAt, lastSmartTravelMin = :travelMin WHERE id = :id")
    suspend fun updateSmartNotifyRecord(id: Long, notifyAt: Long, travelMin: Int)

    @Query("UPDATE todos SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTodoCompleted(id: Long, isCompleted: Boolean)
}
