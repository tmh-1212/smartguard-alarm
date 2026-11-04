package com.example.myalarmapp.security

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Real Manager for Firebase Authentication (Email/Password & Session management) with detailed error result handling - v2.2.
 */
class FirebaseAuthManager {
    private val auth by lazy { FirebaseAuth.getInstance() }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val currentUserEmail: String?
        get() = auth.currentUser?.email

    suspend fun signInWithEmail(email: String, pass: String): Result<Boolean> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<Boolean> {
        return try {
            auth.createUserWithEmailAndPassword(email, pass).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleCredential(credential: AuthCredential): Result<Boolean> {
        return try {
            auth.signInWithCredential(credential).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
