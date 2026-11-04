package com.example.myalarmapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.myalarmapp.data.Task
import com.example.myalarmapp.data.TaskPriority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    uiState: TaskUiState,
    errorMessage: String?,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onSaveTask: (Task) -> Unit,
    onClearError: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToTimer: () -> Unit,
    onNavigateToHabits: () -> Unit
) {
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedTaskForEdit by remember { mutableStateOf<Task?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "My Tasks",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToAlarms) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Alarms")
                    }
                    IconButton(onClick = onNavigateToTimer) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = "Timer")
                    }
                    IconButton(onClick = onNavigateToHabits) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "Habits")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedTaskForEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.tasks.isEmpty()) {
            EmptyTaskState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = uiState.tasks,
                    key = { task -> task.id }
                ) { task ->
                    TaskItemCard(
                        task = task,
                        onToggleCompletion = { onToggleTask(task) },
                        onEditClick = {
                            selectedTaskForEdit = task
                            showAddEditDialog = true
                        },
                        onDeleteClick = { onDeleteTask(task) }
                    )
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditTaskDialog(
            taskToEdit = selectedTaskForEdit,
            onDismiss = {
                showAddEditDialog = false
                selectedTaskForEdit = null
            },
            onSave = { taskToSave: Task ->
                onSaveTask(taskToSave)
                showAddEditDialog = false
                selectedTaskForEdit = null
            }
        )
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    onToggleCompletion: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (task.isCompleted) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleCompletion() }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    // Title
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.onSurface
                    )

                    // Description
                    if (!task.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Priority & Due Date Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Priority Badge
                        val priorityColor = when (task.priority) {
                            TaskPriority.HIGH -> MaterialTheme.colorScheme.error
                            TaskPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                            TaskPriority.LOW -> MaterialTheme.colorScheme.secondary
                        }
                        Text(
                            text = task.priority.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor
                        )

                        // Due Date if present
                        task.dueDateTime?.let { dueTime ->
                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dueTime))
                            Text(
                                text = "• Due: $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Task",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTaskState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Tasks Yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to create a task",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
