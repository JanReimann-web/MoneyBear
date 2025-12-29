package com.jan.moneybear.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.jan.moneybear.R
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val appContext = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun signInWithGoogle(activity: Activity): Result<Unit> {
        val serverClientId = runCatching {
            appContext.getString(R.string.default_web_client_id)
        }.getOrNull().orEmpty()

        if (serverClientId.isBlank()) {
            return Result.failure(IllegalStateException("Missing server client id (default_web_client_id)"))
        }

        return runCatching {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                // Keep behavior permissive; allow choosing among all accounts.
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val context = activity
            val response = credentialManager.getCredential(context, request)

            val credential = response.credential
            val idToken =
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                    googleCred.idToken
                } else {
                    throw IllegalStateException(
                        "Unsupported credential type: ${credential::class.java.name}"
                    )
                }

            val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(firebaseCred).await()
            if (auth.currentUser == null) {
                throw IllegalStateException("Firebase user is null after sign-in")
            }
        }.onFailure { e ->
            val msg = e.message ?: e.javaClass.simpleName
            val causeMsg = e.cause?.message
            Log.e("AUTH", "Google sign-in failed: $msg (cause=$causeMsg)", e)
        }.recoverCatching { e ->
            // Normalize Credential Manager exceptions a bit for UI
            if (e is GetCredentialException) {
                val message = e.errorMessage?.toString() ?: "Credential error"
                throw IllegalStateException(message, e)
            } else {
                throw e
            }
        }.map { Unit }
    }

    suspend fun signOut() {
        auth.signOut()
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }.onFailure { e ->
            Log.e(
                "AUTH",
                "Failed to clear credential state on logout: ${e.message} (cause=${e.cause?.message})",
                e
            )
        }
    }
}
