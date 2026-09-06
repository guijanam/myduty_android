package com.sonbum.diacalendar2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.google.firebase.messaging.FirebaseMessaging
import com.sonbum.diacalendar2.core.routing.NavigationRoot
import com.sonbum.diacalendar2.core.update.InAppUpdateManager
import com.sonbum.diacalendar2.core.update.UpdateState
import com.sonbum.diacalendar2.data.local.datastore.ThemeMode
import com.sonbum.diacalendar2.data.local.datastore.ThemePreferences
import com.sonbum.diacalendar2.ui.theme.DiaCalendar2Theme
import com.sonbum.diacalendar2.widget.WidgetUpdater
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

	private val themePreferences: ThemePreferences by inject()
	private lateinit var inAppUpdateManager: InAppUpdateManager
	private var updateCheckRequested = false

	private val updateLauncher = registerForActivityResult(
		ActivityResultContracts.StartIntentSenderForResult()
	) { result ->
		inAppUpdateManager.handleUpdateResult(result.resultCode)
		// IMMEDIATE 업데이트가 취소되면 앱 종료 (강제 업데이트)
		if (result.resultCode == Activity.RESULT_CANCELED) {
			// 강제 업데이트 정책: 업데이트 취소 시 앱 종료
			// 선택적: 아래 줄 주석 해제하면 업데이트 취소 시 앱 종료
			finish()
		}
	}

	// 설정 화면에서 돌아올 때 onResume에서 체크하기 위한 플래그
	private var waitingForNotificationPermission = false

	// 앱 최초 실행 시 알림 권한 요청 런처 (Android 13+)
	private val notificationPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { granted ->
		if (!granted) {
			// 거부 → 앱 알림 설정 화면으로 강제 이동
			waitingForNotificationPermission = true
			navigateToNotificationSettings()
		}
	}

	// [수정] Android 15 이상에서만 실행되도록 제한하는 @RequiresApi는 제거하는 것이 좋습니다.
	// 대신 내부 로직에서 버전 체크를 하거나, 하위 호환 함수를 사용해야 앱이 구버전에서도 실행됩니다.
	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		// In-App Update 초기화
		inAppUpdateManager = InAppUpdateManager(this)
		inAppUpdateManager.registerUpdateLauncher(updateLauncher)
		// 업데이트 확인은 onResume에서 수행 (onCreate 시점엔 아직 RESUMED 상태가 아니라
		// checkForUpdate의 isActivityResumed() 가드에 걸려 동작하지 않음)

		// 앱 최초 실행 시 알림 권한 요청 (Android 13+)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			val hasPermission = ContextCompat.checkSelfPermission(
				this, Manifest.permission.POST_NOTIFICATIONS
			) == PackageManager.PERMISSION_GRANTED
			if (!hasPermission) {
				notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
			}
		}

		// FCM 토픽 구독 (공지사항)
		FirebaseMessaging.getInstance().subscribeToTopic("documents")
			.addOnCompleteListener { task ->
				android.util.Log.d("FCM", "documents 토픽 구독: ${task.isSuccessful}")
			}

		// 딥링크 처리 (이메일 확인 콜백)
		handleDeepLink(intent)

		setContent {
			val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
			val updateState by inAppUpdateManager.updateState.collectAsState()
			val snackbarHostState = remember { SnackbarHostState() }

			val isDarkTheme = when (themeMode) {
				ThemeMode.SYSTEM -> isSystemInDarkTheme()
				ThemeMode.LIGHT -> false
				ThemeMode.DARK -> true
			}

			// 테마 변경 시 상태바/네비게이션바 아이콘 색상 동적 업데이트
			SideEffect {
				enableEdgeToEdge(
					statusBarStyle = if (isDarkTheme) {
						SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
					} else {
						SystemBarStyle.light(
							android.graphics.Color.TRANSPARENT,
							android.graphics.Color.TRANSPARENT
						)
					},
					navigationBarStyle = if (isDarkTheme) {
						SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
					} else {
						SystemBarStyle.light(
							android.graphics.Color.TRANSPARENT,
							android.graphics.Color.TRANSPARENT
						)
					}
				)
			}

			DiaCalendar2Theme(darkTheme = isDarkTheme) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.background(MaterialTheme.colorScheme.background)
						.safeDrawingPadding() // 시스템 바, 키보드 등을 모두 고려한 패딩
				) {
					NavigationRoot()

					// 업데이트 상태에 따른 UI
					when (val state = updateState) {
						is UpdateState.Downloading -> {
							// 다운로드 진행률 표시 (FLEXIBLE 업데이트 시)
							LinearProgressIndicator(
								progress = { state.progress / 100f },
								modifier = Modifier
									.fillMaxWidth()
									.align(Alignment.TopCenter)
									.statusBarsPadding(),
								color = MaterialTheme.colorScheme.primary
							)
						}
						is UpdateState.Downloaded -> {
							// 다운로드 완료 - 설치 스낵바 표시
							SnackbarHost(
								hostState = snackbarHostState,
								modifier = Modifier
									.align(Alignment.BottomCenter)
									.navigationBarsPadding()
							) {
								Snackbar(
									action = {
										TextButton(
											onClick = { inAppUpdateManager.completeUpdate() }
										) {
											Text("설치")
										}
									}
								) {
									Text("업데이트가 다운로드되었습니다")
								}
							}

							LaunchedEffect(Unit) {
								snackbarHostState.showSnackbar("업데이트가 다운로드되었습니다")
							}
						}
						is UpdateState.Failed -> {
							// 실패 시 재시도 버튼 (선택적)
							LaunchedEffect(state.message) {
								snackbarHostState.showSnackbar("업데이트 실패: ${state.message}")
							}
						}
						else -> {}
					}
				}
			}
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		handleDeepLink(intent)
	}

	private fun handleDeepLink(intent: Intent) {
		// Google OAuth는 Credential Manager를 통해 처리되므로 딥링크 불필요
	}

	private fun navigateToNotificationSettings() {
		val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
			data = Uri.fromParts("package", packageName, null)
		}
		startActivity(intent)
	}

	override fun onResume() {
		super.onResume()
		if (::inAppUpdateManager.isInitialized) {
			if (!updateCheckRequested) {
				// 콜드 스타트 1회: 업데이트 확인 및 강제(IMMEDIATE) 업데이트 시작
				// onCreate가 아닌 onResume에서 호출해야 RESUMED 상태라 정상 동작함
				updateCheckRequested = true
				inAppUpdateManager.checkForUpdate(forceImmediate = true)
			} else {
				// IMMEDIATE 업데이트가 중단된 경우 다시 시작
				inAppUpdateManager.checkUpdateOnResume()
			}
		}

		// 설정 화면에서 돌아왔을 때 권한 재확인
		if (waitingForNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			val hasPermission = ContextCompat.checkSelfPermission(
				this, Manifest.permission.POST_NOTIFICATIONS
			) == PackageManager.PERMISSION_GRANTED
			if (!hasPermission) {
				// 아직 허용 안 함 → 다시 설정 화면으로
				navigateToNotificationSettings()
			} else {
				waitingForNotificationPermission = false
			}
		}
	}

	override fun onPause() {
		super.onPause()
		// 앱이 백그라운드로 갈 때 위젯 갱신
		WidgetUpdater.updateAll(this)
	}

	override fun onDestroy() {
		super.onDestroy()
		if (::inAppUpdateManager.isInitialized) {
			inAppUpdateManager.unregisterListener()
		}
	}
}
