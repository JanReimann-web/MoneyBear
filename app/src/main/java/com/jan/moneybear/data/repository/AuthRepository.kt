package com.jan.moneybear.data.repository

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.jan.moneybear.R
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val appContext = context.applicationContext
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(appContext.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(appContext, gso)
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun startGoogleSignIn(): Intent = googleSignInClient.signInIntent

    suspend fun finishGoogleSignIn(data: Intent?): Result<FirebaseUser> {
        return runCatching {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                ?: throw IllegalStateException("Google account is null")
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).await()
            auth.currentUser ?: throw IllegalStateException("Firebase user is null after sign-in")
        }
    }

    suspend fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().await()
    }
}
