package com.example.myalarmapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myalarmapp.data.FocusSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    uiState: TimerUiState,
    onSessionTypeSelected: (SessionType) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onNavigateToAlarms: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToHabits: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        label = "TimerProgress"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Focus Timer",
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
                    IconButton(onClick = onNavigateToHabits) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "Habits")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Session Type Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SessionType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.sessionType == type,
                        onClick = { onSessionTypeSelected(type) },
                        label = { Text(type.title) }
                    )
                }
            }

            // Timer Display with Circular Progress Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(260.dp)
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = uiState.formattedTime,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val statusText = when {
                        uiState.isRunning -> "Running..."
                        uiState.isPaused -> "Paused"
                        else -> "Ready"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Control Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when {
                        uiState.isRunning -> {
                            Button(
                                onClick = onPause,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Pause", fontSize = 16.sp)
                            }
                        }
                        uiState.isPaused -> {
                            Button(
                                onClick = onResume,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Resume", fontSize = 16.sp)
                            }
                        }
                        else -> {
                            Button(
                                onClick = onStart,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Start", fontSize = 16.sp)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Reset", fontSize = 16.sp)
                    }
                }
            }

            // Focus History & Stats Section
            FocusHistorySection(
                totalFocusMinutes = uiState.totalFocusMinutes,
                sessions = uiState.sessions
            )
        }
    }
}

@Composable
fun FocusHistorySection(
    totalFocusMinutes: Int,
    sessions: List<FocusSession>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Focus History & Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Total Focus Time: $totalFocusMinutes minutes",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            if (sessions.isEmpty()) {
                Text(
                    text = "No completed focus sessions yet. Start a timer to record your progress!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recent Sessions:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
                    sessions.take(10).forEach { session ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = session.sessionType,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = dateFormat.format(Date(session.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "+${session.durationMinutes}m",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
