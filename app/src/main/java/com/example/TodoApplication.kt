package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.repository.TodoRepository
import com.example.reminder.ReminderManager

class TodoApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: TodoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        ReminderManager.initNotificationChannel(this)

        database = AppDatabase.getInstance(this)
        repository = TodoRepository(this, database.todoDao())
    }

    companion object {
        lateinit var instance: TodoApplication
            private set
    }
}
