package com.example.myalarmapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myalarmapp.alarm.AlarmSchedulerImpl
import com.example.myalarmapp.security.SecurityPreferences
import com.example.myalarmapp.data.Alarm
import com.example.myalarmapp.data.FocusRepository
import com.example.myalarmapp.data.HabitRepository
import com.example.myalarmapp.data.SecurityRepository
import com.example.myalarmapp.data.TaskRepository
import com.example.myalarmapp.ui.AddEditAlarmDialog
import com.example.myalarmapp.ui.AlarmHomeScreen
import com.example.myalarmapp.ui.AlarmViewModel
import com.example.myalarmapp.ui.AlarmViewModelFactory
import com.example.myalarmapp.ui.AntiTheftScreen
import com.example.myalarmapp.ui.DashboardScreen
import com.example.myalarmapp.ui.DashboardViewModel
import com.example.myalarmapp.ui.DashboardViewModelFactory
import com.example.myalarmapp.ui.HabitScreen
import com.example.myalarmapp.ui.HabitViewModel
import com.example.myalarmapp.ui.HabitViewModelFactory
import com.example.myalarmapp.ui.PermissionOnboardingScreen
import com.example.myalarmapp.ui.SecurityViewModel
import com.example.myalarmapp.ui.SecurityViewModelFactory
import com.example.myalarmapp.ui.TaskScreen
import com.example.myalarmapp.ui.TaskViewModel
import com.example.myalarmapp.ui.TaskViewModelFactory
import com.example.myalarmapp.ui.TimerScreen
import com.example.myalarmapp.ui.TimerViewModel
import com.example.myalarmapp.ui.TimerViewModelFactory
import com.example.myalarmapp.ui.theme.MyAlarmAppTheme

enum class AppScreen {
    DASHBOARD,
    ALARMS,
    TASKS,
    TIMER,
    HABITS,
    ANTI_THEFT,
    PERMISSIONS
}

class MainActivity : ComponentActivity() {

    private val viewModel: AlarmViewModel by viewModels {
        val app = application as AlarmApplication
        val scheduler = AlarmSchedulerImpl(applicationContext)
        AlarmViewModelFactory(app.repository, scheduler)
    }

    private val taskViewModel: TaskViewModel by viewModels {
        val app = application as AlarmApplication
        val taskRepo = TaskRepository(app.database.taskDao())
        TaskViewModelFactory(taskRepo)
    }

    private val timerViewModel: TimerViewModel by viewModels {
        val app = application as AlarmApplication
        val focusRepo = FocusRepository(app.database.focusDao())
        TimerViewModelFactory(focusRepo)
    }

    private val habitViewModel: HabitViewModel by viewModels {
        val app = application as AlarmApplication
        val habitRepo = HabitRepository(app.database.habitDao())
        HabitViewModelFactory(habitRepo)
    }

    private val securityViewModel: SecurityViewModel by viewModels {
        val app = application as AlarmApplication
        val securityRepo = SecurityRepository(app.database.securityDao())
        SecurityViewModelFactory(application, securityRepo)
    }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        val app = application as AlarmApplication
        DashboardViewModelFactory(app.repository, TaskRepository(app.database.taskDao()), HabitRepository(app.database.habitDao()))
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = SecurityPreferences(this)
        if (prefs.isCustomLockscreenEnabled) {
            val lockIntent = Intent(this, CustomLockscreenActivity::class.java)
            startActivity(lockIntent)
        }

        requestNotificationPermission()

        setContent {
            val userPrefs = remember { SecurityPreferences(this) }
            var isDarkMode by remember { mutableStateOf(userPrefs.isDarkMode) }

            MyAlarmAppTheme(darkTheme = isDarkMode) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val taskUiState by taskViewModel.uiState.collectAsStateWithLifecycle()
                val taskError by taskViewModel.errorMessage.collectAsStateWithLifecycle()
                val timerUiState by timerViewModel.uiState.collectAsStateWithLifecycle()
                val habitUiState by habitViewModel.uiState.collectAsStateWithLifecycle()
                val habitError by habitViewModel.errorMessage.collectAsStateWithLifecycle()
                val securityUiState by securityViewModel.uiState.collectAsStateWithLifecycle()
                val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
                var showAddEditDialog by remember { mutableStateOf(false) }
                var selectedAlarmForEdit by remember { mutableStateOf<Alarm?>(null) }

                // Handle back press to return to Dashboard from any sub-screen
                if (currentScreen != AppScreen.DASHBOARD) {
                    BackHandler {
                        currentScreen = AppScreen.DASHBOARD
                    }
                }

                when (currentScreen) {
                    AppScreen.DASHBOARD -> {
                        DashboardScreen(
                            uiState = dashboardUiState,
                            isDarkMode = isDarkMode,
                            onToggleTheme = {
                                isDarkMode = !isDarkMode
                                userPrefs.isDarkMode = isDarkMode
                            },
                            onFetchAiInsight = dashboardViewModel::fetchAiCoachInsight,
                            onResetAiInsight = dashboardViewModel::resetAiCoachState,
                            onNavigateToAlarms = { currentScreen = AppScreen.ALARMS },
                            onNavigateToTasks = { currentScreen = AppScreen.TASKS },
                            onNavigateToTimer = { currentScreen = AppScreen.TIMER },
                            onNavigateToHabits = { currentScreen = AppScreen.HABITS },
                            onNavigateToAntiTheft = { currentScreen = AppScreen.ANTI_THEFT }
                        )
                    }
                    AppScreen.ALARMS -> {
                        AlarmHomeScreen(
                            uiState = uiState,
                            onToggleAlarm = viewModel::toggleAlarm,
                            onDeleteAlarm = viewModel::deleteAlarm,
                            onAlarmClick = { alarm ->
                                selectedAlarmForEdit = alarm
                                showAddEditDialog = true
                            },
                            onAddAlarmClick = {
                                selectedAlarmForEdit = null
                                showAddEditDialog = true
                            },
                            onNavigateToTasks = { currentScreen = AppScreen.TASKS },
                            onNavigateToTimer = { currentScreen = AppScreen.TIMER },
                            onNavigateToHabits = { currentScreen = AppScreen.HABITS },
                            onNavigateToAntiTheft = { currentScreen = AppScreen.ANTI_THEFT }
                        )
                    }
                    AppScreen.TASKS -> {
                        TaskScreen(
                            uiState = taskUiState,
                            errorMessage = taskError,
                            onToggleTask = taskViewModel::toggleTaskCompletion,
                            onDeleteTask = taskViewModel::deleteTask,
                            onSaveTask = taskViewModel::saveTask,
                            onClearError = taskViewModel::clearError,
                            onNavigateToAlarms = { currentScreen = AppScreen.ALARMS },
                            onNavigateToTimer = { currentScreen = AppScreen.TIMER },
                            onNavigateToHabits = { currentScreen = AppScreen.HABITS }
                        )
                    }
                    AppScreen.TIMER -> {
                        TimerScreen(
                            uiState = timerUiState,
                            onSessionTypeSelected = timerViewModel::setSessionType,
                            onStart = timerViewModel::startTimer,
                            onPause = timerViewModel::pauseTimer,
                            onResume = timerViewModel::resumeTimer,
                            onReset = timerViewModel::resetTimer,
                            onNavigateToAlarms = { currentScreen = AppScreen.ALARMS },
                            onNavigateToTasks = { currentScreen = AppScreen.TASKS },
                            onNavigateToHabits = { currentScreen = AppScreen.HABITS }
                        )
                    }
                    AppScreen.HABITS -> {
                        HabitScreen(
                            uiState = habitUiState,
                            errorMessage = habitError,
                            onToggleHabitToday = habitViewModel::toggleHabitToday,
                            onSaveHabit = habitViewModel::saveHabit,
                            onDeleteHabit = habitViewModel::deleteHabit,
                            onClearError = habitViewModel::clearError,
                            onNavigateToAlarms = { currentScreen = AppScreen.ALARMS },
                            onNavigateToTasks = { currentScreen = AppScreen.TASKS },
                            onNavigateToTimer = { currentScreen = AppScreen.TIMER }
                        )
                    }
                    AppScreen.ANTI_THEFT -> {
                        AntiTheftScreen(
                            uiState = securityUiState,
                            onVerifyPin = securityViewModel::verifyPin,
                            onUpdatePin = securityViewModel::updateSecurityPin,
                            onToggleAntiTheft = securityViewModel::toggleAntiTheft,
                            onSaveCustomCommands = securityViewModel::updateCustomCommands,
                            onDeleteLog = securityViewModel::deleteLog,
                            onNavigateToDashboard = { currentScreen = AppScreen.DASHBOARD },
                            onNavigateToPermissions = { currentScreen = AppScreen.PERMISSIONS },
                            onToggleAuthDialog = securityViewModel::setShowAuthDialog,
                            onSignIn = securityViewModel::signIn,
                            onSignUp = securityViewModel::signUp,
                            onSignOut = securityViewModel::signOut,
                            onSendPasswordReset = securityViewModel::sendPasswordResetEmail,
                            onTestPhotoCapture = securityViewModel::testPhotoCaptureAndSync,
                            onTestLockscreen = { startActivity(Intent(this, CustomLockscreenActivity::class.java)) },
                            onToggleCustomLockscreen = securityViewModel::toggleCustomLockscreen,
                            onToggleMotionAntiTheft = securityViewModel::toggleMotionAntiTheft,
                            onUpdateLockscreenPin = securityViewModel::updateLockscreenPin,
                            onUpdateLockscreenType = securityViewModel::updateLockscreenType,
                            onUpdateLockscreenPassword = securityViewModel::updateLockscreenPassword,
                            onUpdateEmergencyContact = securityViewModel::updateEmergencyContact,
                            onSaveTelegram = securityViewModel::updateTelegramSettings,
                            onTriggerSos = securityViewModel::triggerSos
                        )
                    }
                    AppScreen.PERMISSIONS -> {
                        PermissionOnboardingScreen(
                            onFinishOnboarding = { currentScreen = AppScreen.DASHBOARD }
                        )
                    }
                }

                if (showAddEditDialog) {
                    AddEditAlarmDialog(
                        alarmToEdit = selectedAlarmForEdit,
                        onDismiss = {
                            showAddEditDialog = false
                            selectedAlarmForEdit = null
                        },
                        onSave = { alarmToSave ->
                            viewModel.saveAlarm(alarmToSave)
                            showAddEditDialog = false
                            selectedAlarmForEdit = null
                        }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
