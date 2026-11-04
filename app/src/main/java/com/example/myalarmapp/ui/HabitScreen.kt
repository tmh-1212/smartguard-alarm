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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.myalarmapp.data.Habit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    uiState: HabitUiState,
    errorMessage: String?,
    onToggleHabitToday: (Int, Boolean) -> Unit,
    onSaveHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onClearError: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToTimer: () -> Unit
) {
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedHabitForEdit by remember { mutableStateOf<Habit?>(null) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }
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
                        text = "My Habits",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToAlarms) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Alarms")
                    }
                    IconButton(onClick = onNavigateToTasks) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Tasks")
                    }
                    IconButton(onClick = onNavigateToTimer) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = "Timer")
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
                    selectedHabitForEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (uiState.habitsWithStreak.isEmpty()) {
            EmptyHabitState(
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
                    items = uiState.habitsWithStreak,
                    key = { item -> item.habit.id }
                ) { item ->
                    HabitItemCard(
                        habitWithStreak = item,
                        onToggleToday = { onToggleHabitToday(item.habit.id, item.isCompletedToday) },
                        onEditClick = {
                            selectedHabitForEdit = item.habit
                            showAddEditDialog = true
                        },
                        onDeleteClick = {
                            habitToDelete = item.habit
                        }
                    )
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditHabitDialog(
            habitToEdit = selectedHabitForEdit,
            onDismiss = {
                showAddEditDialog = false
                selectedHabitForEdit = null
            },
            onSave = { habitToSave ->
                onSaveHabit(habitToSave)
                showAddEditDialog = false
                selectedHabitForEdit = null
            }
        )
    }

    // Explicit Delete Confirmation Dialog
    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Delete Habit") },
            text = { Text("Are you sure you want to delete '${habit.title}'? This will also remove its completion history.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteHabit(habit)
                        habitToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { habitToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun HabitItemCard(
    habitWithStreak: HabitWithStreak,
    onToggleToday: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = habitWithStreak.habit
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEditClick() },
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (habitWithStreak.isCompletedToday)
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
                    checked = habitWithStreak.isCompletedToday,
                    onCheckedChange = { onToggleToday() }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (habitWithStreak.isCompletedToday) TextDecoration.LineThrough else null
                    )

                    if (!habit.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Streak Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Streak",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${habitWithStreak.currentStreak} day streak",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Habit",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Habit",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyHabitState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Habits Yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to create a daily habit",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
