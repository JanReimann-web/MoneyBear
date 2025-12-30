package com.jan.moneybear.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jan.moneybear.MoneyBearApp
import com.jan.moneybear.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyBearApp
    val authRepository = app.authRepository
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var legalDocument by remember { mutableStateOf<LegalDocument?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                isLoading = true
                errorMessage = null
                val signInResult = authRepository.finishGoogleSignIn(result.data)
                isLoading = false

                if (signInResult.isSuccess) {
                    onLoginSuccess()
                } else {
                    errorMessage = signInResult.exceptionOrNull()?.message
                        ?: context.getString(R.string.login_failed)
                }
            }
        } else {
            isLoading = false
        }
    }

    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    legalDocument?.let { document ->
        ModalBottomSheet(
            onDismissRequest = { legalDocument = null },
            sheetState = modalState
        ) {
            LegalSheetContent(
                document = document,
                onDismiss = { legalDocument = null }
            )
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .padding(top = 72.dp, bottom = 24.dp)
            ) {
                LoginHeroSection(
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                Spacer(modifier = Modifier.weight(1f))

                GoogleLoginButton(
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onGoogleLogin = {
                        isLoading = true
                        errorMessage = null
                        val signInIntent = authRepository.startGoogleSignIn()
                        launcher.launch(signInIntent)
                    }
                )

                LegalLinks(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    onTermsClick = { legalDocument = LegalDocument.TERMS },
                    onPrivacyClick = { legalDocument = LegalDocument.PRIVACY }
                )
            }
        }
    }
}

@Composable
private fun LoginHeroSection(
    modifier: Modifier = Modifier
) {
    val heroGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.primaryContainer
        )
    )

    Surface(
        modifier = modifier.heightIn(min = 220.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(36.dp))
                .background(heroGradient)
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        lineHeight = 36.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_moneybear),
                    contentDescription = null,
                    modifier = Modifier.size(140.dp)
                )
            }

            Text(
                text = stringResource(R.string.login_tagline),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f)
                ),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GoogleLoginButton(
    isLoading: Boolean,
    errorMessage: String?,
    onGoogleLogin: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onGoogleLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            enabled = !isLoading,
            shape = RoundedCornerShape(18.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = stringResource(R.string.login_with_google),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LegalLinks(
    modifier: Modifier = Modifier,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onTermsClick) {
                Text(text = stringResource(R.string.login_terms_label))
            }
            Text(
                text = "•",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onPrivacyClick) {
                Text(text = stringResource(R.string.login_privacy_label))
            }
        }
        Text(
            text = stringResource(R.string.login_terms_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private enum class LegalDocument { TERMS, PRIVACY }

@Composable
private fun LegalSheetContent(
    document: LegalDocument,
    onDismiss: () -> Unit
) {
    val title = when (document) {
        LegalDocument.TERMS -> stringResource(R.string.login_terms_sheet_title)
        LegalDocument.PRIVACY -> stringResource(R.string.login_privacy_sheet_title)
    }
    val body = when (document) {
        LegalDocument.TERMS -> stringResource(R.string.login_terms_sheet_body)
        LegalDocument.PRIVACY -> stringResource(R.string.login_privacy_sheet_body)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = stringResource(R.string.login_legal_sheet_close))
        }
    }
}

























