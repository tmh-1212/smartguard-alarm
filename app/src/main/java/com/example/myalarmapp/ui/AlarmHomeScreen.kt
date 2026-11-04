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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myalarmapp.data.Alarm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmHomeScreen(
    uiState: AlarmUiState,
    onToggleAlarm: (Int, Boolean) -> Unit = { _, _ -> },
    onDeleteAlarm: (Alarm) -> Unit = {},
    onAlarmClick: (Alarm) -> Unit = {},
    onAddAlarmClick: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToTimer: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {},
    onNavigateToAntiTheft: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "My Alarms",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToAntiTheft) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Anti-Theft")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Alarms") },
                    label = { Text("Alarms") },
                    selected = true,
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Security") },
                    label = { Text("Security") },
                    selected = false,
                    onClick = onNavigateToAntiTheft
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlarmClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Alarm"
                )
            }
        }
    ) { innerPadding ->
        if (uiState.alarms.isEmpty()) {
            EmptyAlarmState(
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
                    items = uiState.alarms,
                    key = { alarm -> alarm.id }
                ) { alarm ->
                    AlarmItemCard(
                        alarm = alarm,
                        onToggleAlarm = { isChecked -> onToggleAlarm(alarm.id, isChecked) },
                        onDeleteAlarm = { onDeleteAlarm(alarm) },
                        onAlarmClick = { onAlarmClick(alarm) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmItemCard(
    alarm: Alarm,
    onToggleAlarm: (Boolean) -> Unit,
    onDeleteAlarm: () -> Unit,
    onAlarmClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onAlarmClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                // Time & AM/PM Row (12-hour format)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = alarm.formattedTime,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alarm.amPm,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = alarm.label.ifBlank { "Alarm" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (alarm.repeatDays.isNotEmpty()) {
                        Text(
                            text = "• ${alarm.formattedDays}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggleAlarm
                )
                IconButton(onClick = onDeleteAlarm) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Alarm",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAlarmState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⏰",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Alarms Set",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button below to add your first smart alarm.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
