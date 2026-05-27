package com.sonbum.diacalendar2.presentation.main

import androidx.compose.foundation.layout.padding
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sonbum.diacalendar2.LocalScaffoldPaddingValues
import com.sonbum.diacalendar2.core.routing.Route
import org.koin.androidx.compose.koinViewModel

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
		Snapshot.withMutableSnapshot {
			backStack.clear()
			backStack.add(route)
		}
	}

	LifecycleResumeEffect(Unit) {
		viewModel.checkNewPosts()
		onPauseOrDispose { }
	}

	Scaffold(
		bottomBar = {
			NavigationBar {
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

				//saved
//				NavigationBarItem(
//					selected = currentRoute is Route.SavedRecipes,
//					onClick = {
//						backStack.clear()
//						backStack.add(Route.SavedRecipes)
//					},
//					icon = {
//						Icon(Icons.Default.Favorite, contentDescription = "Saved Recipes")
//					},
//					label = { Text("Notice") }
//				)

				//게시판
//				NavigationBarItem(
//					selected = currentRoute is Route.Notifications,
//					onClick = {
//						viewModel.markBoardChecked()
//						switchTab(Route.Notifications)
//					},
//					icon = {
//						BadgedBox(badge = {
//							if (hasNewBoardPosts) Badge()
//						}) {
//							Icon(Icons.Default.Forum, contentDescription = "게시판")
//						}
//					},
//					label = { Text("게시판") }
//				)

				//커뮤니티(seoulmetrospace)
//				NavigationBarItem(
//					selected = currentRoute is Route.Community,
//					onClick = {
//						backStack.clear()
//						backStack.add(Route.Community)
//					},
//					icon = {
//						Icon(Icons.Default.Language, contentDescription = "커뮤니티")
//					},
//					label = { Text("커뮤니티") }
//				)



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