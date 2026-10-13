package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class TodoWithChecklist(
    @Embedded
    val todo: TodoItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "todoId"
    )
    val checklist: List<ChecklistItem> = emptyList()
) {
    val totalChecklistCount: Int
        get() = checklist.size

    val checkedCount: Int
        get() = checklist.count { it.isChecked }

    val hasUncheckedItems: Boolean
        get() = checklist.any { !it.isChecked }

    val uncheckedItemContents: List<String>
        get() = checklist.filter { !it.isChecked }.map { it.content }
}
