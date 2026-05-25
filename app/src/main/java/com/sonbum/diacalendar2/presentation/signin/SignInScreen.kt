package com.sonbum.diacalendar2.presentation.signin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonbum.diacalendar2.data.local.datastore.OnboardingPreferences
import com.sonbum.diacalendar2.presentation.onboarding.OnboardingScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SignInScreen(
	modifier: Modifier = Modifier,
	onLogin: () -> Unit = {},
	viewModel: SignInViewModel = viewModel(),
) {
	val onboardingPreferences: OnboardingPreferences = koinInject()
	val isOnboardingCompleted by onboardingPreferences.isOnboardingCompleted.collectAsState(initial = null)
	val coroutineScope = rememberCoroutineScope()

	// 온보딩 완료 상태가 로드될 때까지 대기
	when (isOnboardingCompleted) {
		null -> {
			// 로딩 중 - 빈 화면 또는 스플래시
		}
		false -> {
			// 온보딩 미완료 - 온보딩 화면 표시
			OnboardingScreen(
				modifier = modifier,
				onComplete = {
					coroutineScope.launch {
						onboardingPreferences.setOnboardingCompleted(true)
						onLogin()
					}
				}
			)
		}
		true -> {
			// 온보딩 완료 - 바로 메인 화면으로 이동
			LaunchedEffect(Unit) {
				onLogin()
			}
		}
	}
}