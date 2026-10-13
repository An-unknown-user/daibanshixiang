package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reminder.ReminderManager
import com.example.ui.TodoViewModel
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.TodoDetailSheet
import com.example.ui.screens.TodoEditorSheet
import com.example.ui.screens.TodoListScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TodoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle notification deep link if launched via notification click
        handleNotificationIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val todoId = intent?.getLongExtra(ReminderManager.EXTRA_TODO_ID, -1L) ?: -1L
        if (todoId != -1L) {
            viewModel.openDetail(todoId)
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: TodoViewModel
) {
    // Request runtime permissions on first launch
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled gracefully
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val importantTodos by viewModel.importantTodos.collectAsStateWithLifecycle()
    val inProgressTodos by viewModel.inProgressTodos.collectAsStateWithLifecycle()
    val completedTodos by viewModel.completedTodos.collectAsStateWithLifecycle()

    val detailTodo by viewModel.detailTodo.collectAsStateWithLifecycle()
    val isEditorOpen by viewModel.isEditorOpen.collectAsStateWithLifecycle()
    val editingTodo by viewModel.editingTodo.collectAsStateWithLifecycle()
    val defaultFlagForCreate by viewModel.defaultFlagForCreate.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()

    // Handle back button when sub-screens are open
    if (detailTodo != null) {
        BackHandler {
            viewModel.closeDetail()
        }
    } else if (isEditorOpen) {
        BackHandler {
            viewModel.closeEditor()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main List Screen
            TodoListScreen(
                currentTab = selectedTab,
                importantList = importantTodos,
                inProgressList = inProgressTodos,
                completedList = completedTodos,
                onSelectTab = { viewModel.selectTab(it) },
                onCardClick = { viewModel.openDetail(it) },
                onToggleComplete = { id, completed -> viewModel.toggleCompleted(id, completed) },
                onFabClick = { viewModel.openCreate() },
                onOpenSettings = { viewModel.openSettings() }
            )

            // Detail Screen Overlay
            AnimatedContent(
                targetState = detailTodo,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "detail_transition"
            ) { activeItem ->
                if (activeItem != null) {
                    TodoDetailSheet(
                        item = activeItem,
                        onBack = { viewModel.closeDetail() },
                        onEdit = { viewModel.openEdit(activeItem) },
                        onDelete = { viewModel.deleteTodo(activeItem.todo.id) },
                        onToggleComplete = {
                            viewModel.toggleCompleted(activeItem.todo.id, !activeItem.todo.isCompleted)
                        },
                        onToggleChecklistItem = { itemId, isChecked ->
                            viewModel.toggleChecklistItem(itemId, isChecked)
                        },
                        onCalculateTravel = { todo ->
                            viewModel.getEstimatedTravel(todo)
                        },
                        onSendTestNotification = { todo ->
                            viewModel.sendTestNotification(todo)
                        }
                    )
                }
            }

            // Editor Screen (Create / Edit)
            if (isEditorOpen) {
                TodoEditorSheet(
                    initialItem = editingTodo,
                    defaultFlagged = defaultFlagForCreate,
                    onSave = { todo, checklist ->
                        viewModel.saveTodo(todo, checklist)
                    },
                    onDismiss = { viewModel.closeEditor() }
                )
            }

            // Settings Dialog
            if (isSettingsOpen) {
                SettingsDialog(
                    onDismiss = { viewModel.closeSettings() }
                )
            }
        }
    }
}
