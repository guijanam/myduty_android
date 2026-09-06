package com.sonbum.diacalendar2.presentation.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sonbum.diacalendar2.LocalScaffoldPaddingValues
import com.sonbum.diacalendar2.core.routing.Route
import org.koin.androidx.compose.koinViewModel

/**
 * 하단 네비게이션 바 높이 (시스템 인셋 제외).
 * Material3 기본값은 80.dp이나, 홈 화면 달력의 세로 공간 확보를 위해 축소했다.
 * 아이콘 + 한글 라벨을 유지하는 하한은 56.dp.
 */
private val BOTTOM_BAR_HEIGHT = 60.dp

@Composable
fun MainScreen(
	body: @Composable (modifier: Modifier) -> Unit,
	backStack: NavBackStack<NavKey>,
	modifier: Modifier = Modifier,
	viewModel: MainViewModel = koinViewModel()
) {
	val currentRoute = backStack.lastOrNull()
	val hasNewBoardPosts by viewModel.hasNewBoardPosts.collectAsStateWithLifecycle()
	val officeWebsiteTabState by viewModel.officeWebsiteTabState.collectAsStateWithLifecycle()

	fun switchTab(route: NavKey) {
		if (currentRoute == route) return
		// clear() 후 add()는 중간에 빈 백스택 상태가 관찰되어
		// NavDisplay("backstack cannot be empty")가 crash하므로,
		// 먼저 새 탭을 넣은 뒤 나머지를 제거한다.
		Snapshot.withMutableSnapshot {
			backStack.add(route)
			backStack.retainAll { it == route }
		}
	}

	LifecycleResumeEffect(Unit) {
		viewModel.checkNewPosts()
		onPauseOrDispose { }
	}

	Scaffold(
		bottomBar = {
			// NavigationBar 내부는 windowInsetsPadding을 먼저 적용한 뒤 최소 높이(80.dp)를
			// 잡으므로, 기본 총 높이는 80.dp + 인셋이다. 여기서는 인셋을 NavigationBar에서
			// 떼어내(WindowInsets(0)) 바깥에서 직접 패딩으로 주고, 콘텐츠 높이만 고정한다.
			NavigationBar(
				modifier = Modifier
					.windowInsetsPadding(WindowInsets.navigationBars)
					.height(BOTTOM_BAR_HEIGHT),
				windowInsets = WindowInsets(0),
			) {
				//home
				NavigationBarItem(
					selected = currentRoute is Route.Home,
					onClick = { switchTab(Route.Home) },
					icon = {
						Icon(Icons.Default.Home, contentDescription = "Home")
					},
					label = { Text("Home") }
				)

				// 동료근무
				NavigationBarItem(
					selected = currentRoute is Route.Coworker,
					onClick = { switchTab(Route.Coworker) },
					icon = {
						Icon(Icons.Default.Groups, contentDescription = "동료")
					},
					label = { Text("동료") }
				)

			
				//profile
				NavigationBarItem(
					selected = currentRoute is Route.Profile,
					onClick = { switchTab(Route.Profile) },
					icon = {
						Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
					},
					label = { Text("MyInfo") }
				)

				// 승무소 사이트 탭 (URL이 등록된 승무소 사용자에게만 표시)
				if (officeWebsiteTabState is MainViewModel.OfficeWebsiteTabState.Available) {
					NavigationBarItem(
						selected = currentRoute is Route.OfficeWebsiteTab,
						onClick = { switchTab(Route.OfficeWebsiteTab) },
						icon = {
							Icon(Icons.Default.Language, contentDescription = "승무소 사이트")
						},
						label = { Text("승무소") }
					)
				}
			}
		}
	) { innerPadding -> // Scaffold가 계산한 하단 바 높이
		// 1. 위에서 만든 LocalScaffoldPaddingValues에 값을 주입합니다.
		CompositionLocalProvider(LocalScaffoldPaddingValues provides innerPadding) {
			// 2. body에 패딩이 적용된 modifier를 전달합니다.
			// 이 modifier는 NavDisplay를 거쳐 각 Screen으로 전달되어야 합니다.
			body(Modifier.padding(innerPadding))
		}
	}
}