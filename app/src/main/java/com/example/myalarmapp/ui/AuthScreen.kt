package com.example.myalarmapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AuthDialog(
    currentEmail: String?,
    cloudSyncMessage: String?,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onSendPasswordReset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun validateStrongPassword(pass: String): String? {
        if (pass.length < 8) return "Password must be at least 8 characters long."
        if (!pass.any { it.isUpperCase() }) return "Password must contain at least one uppercase letter (A-Z)."
        if (!pass.any { it.isLowerCase() }) return "Password must contain at least one lowercase letter (a-z)."
        if (!pass.any { it.isDigit() }) return "Password must contain at least one number (0-9)."
        if (!pass.any { !it.isLetterOrDigit() }) return "Password must contain at least one special symbol (!@#\$%^&*)."
        return null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentEmail != null) "☁️ Cloud Account Connected" else if (isSignUp) "📝 Register New Account" else "🔐 Sign In to Cloud Sync",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (currentEmail != null) {
                    Text(
                        text = "Logged in as: $currentEmail",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Your anti-theft logs, intruder photos, and GPS locations are securely synced to your private cloud account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; validationError = null },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; validationError = null },
                        label = { Text("Password (8+ chars, A-Z, a-z, 0-9, symbol)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Forgot password button
                    if (!isSignUp) {
                        TextButton(
                            onClick = { onSendPasswordReset(email.trim()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Forgot Password? Reset via Email", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Expressive Error Display Card
                    val displayError = validationError ?: cloudSyncMessage
                    if (!displayError.isNullOrBlank()) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = displayError,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentEmail != null) {
                TextButton(onClick = onSignOut) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Button(
                    onClick = {
                        val trimmedEmail = email.trim()
                        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
                            validationError = "Please enter a valid email address."
                        } else if (isSignUp) {
                            val passError = validateStrongPassword(password)
                            if (passError != null) {
                                validationError = passError
                            } else {
                                validationError = null
                                onSignUp(trimmedEmail, password)
                            }
                        } else {
                            if (password.isBlank()) {
                                validationError = "Please enter your password."
                            } else {
                                validationError = null
                                onSignIn(trimmedEmail, password)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSignUp) "Register Account" else "Sign In", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (currentEmail == null) {
                TextButton(
                    onClick = {
                        isSignUp = !isSignUp
                        validationError = null
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSignUp) "Have an account? Sign In" else "Need an account? Register")
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    )
}
