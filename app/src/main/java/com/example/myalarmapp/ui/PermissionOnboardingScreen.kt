package com.example.myalarmapp.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.myalarmapp.security.MyDeviceAdminReceiver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }

    // Check permission states
    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    val hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val adminComponent = ComponentName(context, MyDeviceAdminReceiver::class.java)
    val hasDeviceAdmin = dpm.isAdminActive(adminComponent)

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTrigger++ }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTrigger++ }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTrigger++ }

    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTrigger++ }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "🛡️ Security & Permissions Setup",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onFinishOnboarding) {
                        Icon(imageVector = Icons.Default.Home, contentDescription = "Dashboard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "To ensure Anti-Theft protection and reliable alarms work smoothly, please grant the following permissions:",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 1. Notification Permission
            PermissionCard(
                title = "Notifications",
                description = "Required for alarm ringing alerts and security notifications.",
                icon = Icons.Default.Notifications,
                isGranted = hasNotificationPermission,
                onRequest = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            // 2. Camera Permission
            PermissionCard(
                title = "Camera (Secret Capture)",
                description = "Required to secretly capture a photo of the intruder when an incorrect PIN is entered.",
                icon = Icons.Default.Star,
                isGranted = hasCameraPermission,
                onRequest = {
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                }
            )

            // 3. Location Permission
            PermissionCard(
                title = "GPS Location",
                description = "Required to track device location when a theft or breach occurs.",
                icon = Icons.Default.Place,
                isGranted = hasLocationPermission,
                onRequest = {
                    locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            )

            // 4. SMS Permission
            PermissionCard(
                title = "SMS Commands & Spy Calls",
                description = "Required for remote tracking via SMS, emergency alerts, and silent spy call-backs.",
                icon = Icons.Default.Info,
                isGranted = hasSmsPermission && ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED,
                onRequest = {
                    smsLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECEIVE_SMS,
                            Manifest.permission.SEND_SMS,
                            Manifest.permission.CALL_PHONE
                        )
                    )
                }
            )

            // 5. Device Administrator Permission
            PermissionCard(
                title = "Device Administrator (Anti-Uninstall)",
                description = "Prevents unauthorized uninstallation and locks screen on security breaches.",
                icon = Icons.Default.Lock,
                isGranted = hasDeviceAdmin,
                onRequest = {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Required to protect MyAlarmApp from unauthorized uninstallation and lock device on security breaches."
                        )
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onFinishOnboarding,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Continue to Dashboard 🚀", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(onClick = onRequest) {
                    Text("Grant")
                }
            }
        }
    }
}
