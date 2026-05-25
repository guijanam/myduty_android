package com.sonbum.diacalendar2.presentation.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.sonbum.diacalendar2.BuildConfig
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@SuppressLint("SuspiciousIndentation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onBack: () -> Unit,
    onNavigateToNicknameSetup: () -> Unit,
    onNavigateToBoard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                AuthEvent.NavigateToNicknameSetup -> onNavigateToNicknameSetup()
                AuthEvent.NavigateToBoard -> onNavigateToBoard()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("로그인") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "내근무 게시판",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "게시판 이용을 위해 로그인해주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
	        Text(
		        text = "게시판전용 새로운 구글계정 사용을 권장합니다.",
		        style = MaterialTheme.typography.bodyMedium,
		        color = MaterialTheme.colorScheme.outline
	        )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            Log.d("AuthScreen", "=== Google Sign-In 시작 ===")
                            Log.d("AuthScreen", "GOOGLE_WEB_CLIENT_ID: ${BuildConfig.GOOGLE_WEB_CLIENT_ID}")
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetSignInWithGoogleOption.Builder(
                                BuildConfig.GOOGLE_WEB_CLIENT_ID
                            ).build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            Log.d("AuthScreen", "CredentialManager.getCredential() 호출")
                            val result = credentialManager.getCredential(
                                context as Activity,
                                request
                            )
                            Log.d("AuthScreen", "Credential 타입: ${result.credential.type}")
                            Log.d("AuthScreen", "Credential 데이터 키: ${result.credential.data.keySet()}")
                            val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(result.credential.data)
                            Log.d("AuthScreen", "idToken 길이: ${googleIdTokenCredential.idToken.length}")
                            Log.d("AuthScreen", "displayName: ${googleIdTokenCredential.displayName}")
                            Log.d("AuthScreen", "id(email): ${googleIdTokenCredential.id}")
                            viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                        } catch (e: GetCredentialCancellationException) {
                            Log.d("AuthScreen", "사용자가 로그인 취소함")
                        } catch (e: Exception) {
                            Log.e("AuthScreen", "Google Sign-In 실패: ${e.javaClass.simpleName}", e)
                            viewModel.setError("Google 로그인 실패: ${e.message}")
                        }
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Google로 로그인")
                }
            }

            state.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
