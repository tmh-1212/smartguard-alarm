package com.example.myalarmapp.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myalarmapp.data.SecurityLog
import com.example.myalarmapp.security.MyDeviceAdminReceiver
import com.example.myalarmapp.security.SecurityActionExecutor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiTheftScreen(
    uiState: SecurityUiState,
    onVerifyPin: (Context, String) -> Boolean,
    onUpdatePin: (String) -> Unit,
    onToggleAntiTheft: (Boolean) -> Unit,
    onSaveCustomCommands: (String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteLog: (SecurityLog) -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToPermissions: () -> Unit = {},
    onToggleAuthDialog: (Boolean) -> Unit = {},
    onSignIn: (String, String) -> Unit = { _, _ -> },
    onSignUp: (String, String) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    onSendPasswordReset: (String) -> Unit = {},
    onTestPhotoCapture: (Context) -> Unit = {},
    onTestLockscreen: () -> Unit = {},
    onToggleCustomLockscreen: (Boolean) -> Unit = {},
    onToggleMotionAntiTheft: (Boolean) -> Unit = {},
    onUpdateLockscreenPin: (String) -> Unit = {},
    onUpdateLockscreenType: (String) -> Unit = {},
    onUpdateLockscreenPassword: (String) -> Unit = {},
    onUpdateEmergencyContact: (String) -> Unit = {},
    onSaveTelegram: (String, String) -> Unit = { _, _ -> },
    onTestTelegram: (Context) -> Unit = {},
    onTriggerSos: (Context) -> Unit = {}
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var newLockscreenPinInput by remember { mutableStateOf("") }

    if (!uiState.isUnlocked) {
        AlertDialog(
            onDismissRequest = onNavigateToDashboard,
            title = {
                Text(
                    text = "🔒 Security PIN Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your Owner Security PIN to access Anti-Theft settings. (Default PIN: 1234)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 8) pinInput = it },
                        label = { Text("Security PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!uiState.pinErrorMessage.isNullOrBlank()) {
                        Text(
                            text = uiState.pinErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onVerifyPin(context, pinInput)
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = onNavigateToDashboard) {
                    Text("Cancel")
                }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Anti-Theft Security",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Dashboard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cloud Sync Message banner
                if (!uiState.cloudSyncMessage.isNullOrBlank()) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = uiState.cloudSyncMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // 1. Cloud Account Setup Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    onClick = { onToggleAuthDialog(true) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "☁️ Cloud Account Setup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.currentUserEmail != null)
                                    "Connected: ${uiState.currentUserEmail}"
                                else
                                    "Tap to sign in or register with Google/Email",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 2. Anti-Theft Main Switch Card
                AntiTheftMainCard(
                    isEnabled = uiState.isAntiTheftEnabled,
                    onToggle = onToggleAntiTheft
                )

                // 3. Device Admin Protection Card
                DeviceAdminProtectionCard(context = context)

                // 3.5 Overlay Permission Card ("Display over Other Apps")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚙️ 'Display over Other Apps' Permission",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Required for Custom Lockscreen Shield to pop up instantly when screen turns on.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Grant Overlay Permission", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 3.6 Accessibility App Shield Settings Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🛡️ App Shield (Accessibility Service)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Required for Custom Lockscreen Shield to appear when opening any app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Open Accessibility Settings", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 4. Change PIN Card
                ChangePinCard(
                    newPin = newPinInput,
                    onPinChange = { newPinInput = it },
                    onSavePin = {
                        onUpdatePin(newPinInput)
                        newPinInput = ""
                    }
                )

                // 4.5 Custom Lockscreen Shield Switch Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom Lockscreen Shield",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Secure shield screen on wake up (triggers photo on 3 wrong PINs)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isCustomLockscreenEnabled,
                            onCheckedChange = onToggleCustomLockscreen
                        )
                    }
                }

                // 4.5.1 Motion / Pocket Anti-Theft Switch Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🏃‍♂️ Pocket / Motion Anti-Theft",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Detects pickup/movement (gives 15s grace period before siren)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isMotionAntiTheftEnabled,
                            onCheckedChange = onToggleMotionAntiTheft
                        )
                    }
                }

                // 4.6 Lockscreen Shield Configuration Card (PIN, Password, Biometric)
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Lockscreen Shield Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose how you want to unlock your Custom Lockscreen Shield",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val lockTypes = listOf(
                            "pin" to "🔢 Numeric PIN",
                            "password" to "🔤 Alphanumeric Password",
                            "system" to "🛡️ Biometric / Fingerprint / Face"
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            lockTypes.forEach { (typeKey, typeName) ->
                                FilterChip(
                                    selected = uiState.lockscreenType == typeKey,
                                    onClick = { onUpdateLockscreenType(typeKey) },
                                    label = { Text(typeName) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (uiState.lockscreenType == "pin") {
                            OutlinedTextField(
                                value = newLockscreenPinInput,
                                onValueChange = { if (it.length <= 8) newLockscreenPinInput = it },
                                label = { Text("New Shield PIN (4-8 digits)") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    onUpdateLockscreenPin(newLockscreenPinInput)
                                    newLockscreenPinInput = ""
                                },
                                enabled = newLockscreenPinInput.length >= 4,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Update Shield PIN", fontWeight = FontWeight.Bold)
                            }
                        } else if (uiState.lockscreenType == "password") {
                            var newPassInput by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = newPassInput,
                                onValueChange = { newPassInput = it },
                                label = { Text("New Shield Password (Letters, Numbers, Symbols)") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    onUpdateLockscreenPassword(newPassInput)
                                    newPassInput = ""
                                },
                                enabled = newPassInput.length >= 4,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Update Shield Password", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "✨ Biometric / Fingerprint / Face Unlock is active. Your device's built-in secure biometric lock will be used to verify ownership.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 4.7 SOS Emergency Contact Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🚨 SOS Emergency Alert",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Set emergency phone number to instantly text GPS location in emergencies.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        var emergencyInput by remember(uiState.emergencyContact) { mutableStateOf(uiState.emergencyContact) }

                        OutlinedTextField(
                            value = emergencyInput,
                            onValueChange = { emergencyInput = it },
                            label = { Text("Emergency Phone Number") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { onUpdateEmergencyContact(emergencyInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Save Emergency Contact", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onTriggerSos(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "🚨 Trigger Test SOS Alert", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 4.8 Telegram Bot Notifications Card
                TelegramSettingsCard(
                    chatIdValue = uiState.telegramChatId,
                    onSaveTelegram = onSaveTelegram,
                    onTestTelegram = onTestTelegram
                )

                // 5. Custom Commands Card
                CustomCommandsCard(
                    locateCmd = uiState.locateCommand,
                    photoCmd = uiState.photoCommand,
                    sirenCmd = uiState.sirenCommand,
                    stopCmd = uiState.stopCommand,
                    recordCmd = uiState.recordCommand,
                    callCmd = uiState.callCommand,
                    onSaveCommands = onSaveCustomCommands
                )

                // 5.5 Permissions Setup Card
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    onClick = onNavigateToPermissions
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Review Permissions & Setup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Check Camera, Location, SMS & Notifications permissions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Test Cloud Photo Sync Button
                Button(
                    onClick = { onTestPhotoCapture(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "📸 Test Cloud Photo Sync Now", fontWeight = FontWeight.Bold)
                }

                // Test Custom Lockscreen Shield Button
                Button(
                    onClick = onTestLockscreen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "🔒 Test Custom Lockscreen Shield", fontWeight = FontWeight.Bold)
                }

                // 6. Security Logs Section Header
                Text(
                    text = "Security Activity Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // 7. Logs List
                if (uiState.logs.isEmpty()) {
                    EmptySecurityLogState()
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.logs.forEach { log ->
                            SecurityLogItemCard(
                                log = log,
                                onDelete = { onDeleteLog(log) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAuthDialog) {
        AuthDialog(
            currentEmail = uiState.currentUserEmail,
            cloudSyncMessage = uiState.cloudSyncMessage,
            onSignIn = onSignIn,
            onSignUp = onSignUp,
            onSignOut = onSignOut,
            onSendPasswordReset = onSendPasswordReset,
            onDismiss = { onToggleAuthDialog(false) }
        )
    }
}

@Composable
fun DeviceAdminProtectionCard(context: Context, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🛡️ Anti-Uninstall Protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Activate Device Administrator to prevent unauthorized uninstallation of MyAlarmApp by intruders.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val componentName = ComponentName(context, MyDeviceAdminReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Required to protect MyAlarmApp from unauthorized uninstallation."
                        )
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Activate Device Admin", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AntiTheftMainCard(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Anti-Theft Protection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Detects wrong PIN and triggers secret photo & GPS tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun ChangePinCard(
    newPin: String,
    onPinChange: (String) -> Unit,
    onSavePin: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Change Security PIN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = newPin,
                onValueChange = { if (it.length <= 8) onPinChange(it) },
                label = { Text("New PIN (4-8 digits)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onSavePin,
                enabled = newPin.length >= 4,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Update Security PIN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CustomCommandsCard(
    locateCmd: String,
    photoCmd: String,
    sirenCmd: String,
    stopCmd: String,
    recordCmd: String,
    callCmd: String,
    onSaveCommands: (String, String, String, String, String, String) -> Unit
) {
    var locate by remember { mutableStateOf(locateCmd) }
    var photo by remember { mutableStateOf(photoCmd) }
    var siren by remember { mutableStateOf(sirenCmd) }
    var stop by remember { mutableStateOf(stopCmd) }
    var record by remember { mutableStateOf(recordCmd) }
    var call by remember { mutableStateOf(callCmd) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Secret SMS Commands",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = locate,
                onValueChange = { locate = it },
                label = { Text("Locate Command") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = photo,
                onValueChange = { photo = it },
                label = { Text("Photo Command") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = siren,
                onValueChange = { siren = it },
                label = { Text("Siren Command") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = stop,
                onValueChange = { stop = it },
                label = { Text("Stop Command") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = record,
                onValueChange = { record = it },
                label = { Text("Record Audio Command") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = call,
                onValueChange = { call = it },
                label = { Text("Spy Call Command") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onSaveCommands(locate, photo, siren, stop, record, call) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Save SMS Commands", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SecurityLogItemCard(log: SecurityLog, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateStr = dateFormat.format(Date(log.timestamp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.eventType,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Log",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(
                text = "Time: $dateStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!log.details.isNullOrBlank()) {
                Text(
                    text = "Details: ${log.details}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (log.latitude != null && log.longitude != null) {
                Text(
                    text = "GPS: ${log.latitude}, ${log.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!log.imagePath.isNullOrBlank()) {
                val file = File(log.imagePath)
                if (file.exists()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Intruder Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySecurityLogState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "No Security Breaches Detected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your device is safe and secure.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TelegramSettingsCard(
    chatIdValue: String,
    onSaveTelegram: (String, String) -> Unit,
    onTestTelegram: (Context) -> Unit
) {
    var chatId by remember(chatIdValue) { mutableStateOf(chatIdValue) }
    val context = LocalContext.current
    val isConnected = chatIdValue.isNotBlank()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🤖 Telegram Push Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isConnected) "🟢 Connected" else "🔴 Not Connected",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "1. Open @myidbot in Telegram & send /getid to get your Chat ID.\n2. Paste your numeric Chat ID below and tap Save.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/myidbot"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(text = "🔗 Open @myidbot on Telegram", fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = chatId,
                onValueChange = { chatId = it },
                label = { Text("Telegram Chat ID (e.g. 6001128930)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onSaveTelegram("", chatId)
                        Toast.makeText(context, "✅ Telegram connected successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Save & Connect", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }

                if (isConnected) {
                    OutlinedButton(
                        onClick = { onTestTelegram(context) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = "Test Alert", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
