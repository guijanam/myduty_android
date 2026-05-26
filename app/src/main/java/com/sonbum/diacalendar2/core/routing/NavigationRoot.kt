package com.sonbum.diacalendar2.core.routing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sonbum.diacalendar2.presentation.calendar.CalendarSelectionScreen
import com.sonbum.diacalendar2.presentation.home.DateDetailScreen
import com.sonbum.diacalendar2.presentation.home.HomeRoot
import com.sonbum.diacalendar2.presentation.main.MainScreen
import com.sonbum.diacalendar2.presentation.memo.MemoEditScreen
import com.sonbum.diacalendar2.presentation.notifications.NotificationsScreen
import com.sonbum.diacalendar2.presentation.profile.ProfileScreen
import com.sonbum.diacalendar2.presentation.recipedetails.RecipeDetailsScreen
import com.sonbum.diacalendar2.presentation.savedrecipes.SavedRecipesRoot
import com.sonbum.diacalendar2.presentation.shift.ShiftSelectionScreen
import com.sonbum.diacalendar2.presentation.signin.SignInScreen
import com.sonbum.diacalendar2.presentation.diatable.DiaTableScreen
import com.sonbum.diacalendar2.presentation.localdia.LocalDiaEditScreen
import com.sonbum.diacalendar2.presentation.localdia.LocalDiaListScreen
import com.sonbum.diacalendar2.presentation.localoffice.LocalOfficeEditScreen
import com.sonbum.diacalendar2.presentation.localoffice.LocalOfficeListScreen
import com.sonbum.diacalendar2.presentation.vacation.VacationSettingScreen
import com.sonbum.diacalendar2.presentation.textsize.TextSizeSettingsScreen
import com.sonbum.diacalendar2.presentation.customshift.CustomShiftListScreen
import com.sonbum.diacalendar2.presentation.customshift.CustomShiftEditScreen
import com.sonbum.diacalendar2.presentation.auth.AuthScreen
import com.sonbum.diacalendar2.presentation.auth.NicknameSetupScreen

import com.sonbum.diacalendar2.presentation.board.BlockedUsersScreen
import com.sonbum.diacalendar2.presentation.community.CommunityScreen
import com.sonbum.diacalendar2.presentation.board.PostDetailScreen
import com.sonbum.diacalendar2.presentation.board.PostEditScreen
import com.sonbum.diacalendar2.presentation.board.PostWriteScreen
import com.sonbum.diacalendar2.presentation.diatable.ServerDiaEditScreen
import com.sonbum.diacalendar2.presentation.diatable.ServerOfficeEditScreen
import com.sonbum.diacalendar2.presentation.menu.MenuScreen
import com.sonbum.diacalendar2.presentation.officewebsite.OfficeWebsiteScreen
import com.sonbum.diacalendar2.presentation.coworker.CoworkerScreen
import com.sonbum.diacalendar2.presentation.coworker.CoworkerGroupScreen
import com.sonbum.diacalendar2.presentation.coworker.CoworkerEditScreen
import com.sonbum.diacalendar2.presentation.subscription.PaywallScreen
import com.sonbum.diacalendar2.domain.repository.SubscriptionRepository
import com.sonbum.diacalendar2.core.util.DeviceIdProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import org.koin.compose.koinInject


@Composable
fun NavigationRoot(
	modifier: Modifier = Modifier,
) {
	val topLevelBackStack = rememberNavBackStack(Route.SignIn)
	var boardRefreshTrigger by remember { mutableIntStateOf(0) }

	//달력 셀 클릭으로 DateDetail 진입 시마다 카운트 → 일정 횟수 후 페이월 시트 노출 (세션 메모리, 미구독자 한정)
	var dateDetailOpenCount by remember { mutableIntStateOf(0) }
	var showDateDetailPaywall by remember { mutableStateOf(false) }
	val dateDetailSubscriptionRepository: SubscriptionRepository = koinInject()
	val navigationScope = rememberCoroutineScope()
	val appContext = LocalContext.current.applicationContext

	// 달력 셀 클릭으로 DateDetail을 새로 열 때 호출 (다른 화면 다녀와 돌아오는 경우는 제외)
	fun openDateDetail(route: Route.DateDetail) {
		topLevelBackStack.add(route)
		navigationScope.launch {
			val ssaid = DeviceIdProvider.getSsaid(appContext)
			if (dateDetailSubscriptionRepository.isVip(ssaid)) return@launch
			dateDetailOpenCount += 1
			if (dateDetailOpenCount >= 3) {
				showDateDetailPaywall = true
			}
		}
	}

	NavDisplay(
		modifier = modifier,
		entryDecorators = listOf(
			rememberSaveableStateHolderNavEntryDecorator(),
			rememberViewModelStoreNavEntryDecorator()
		),
		backStack = topLevelBackStack,
		entryProvider = entryProvider {
			entry<Route.SignIn> {
				SignInScreen(
					onLogin = {
						// 백스택을 [Route.Main]으로 원자적 교체 (snapshot 트랜잭션)
						Snapshot.withMutableSnapshot {
							topLevelBackStack.clear()
							topLevelBackStack.add(Route.Main)
						}
					}
				)
			}

			// 날짜 상세 화면
			// 결제화면 빈도
			entry<Route.DateDetail> { key ->
				DateDetailScreen(
					dateString = key.dateString,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					},
					onAddMemo = {
						topLevelBackStack.add(Route.MemoEdit(key.dateString, null))
					},
					onEditMemo = { memoId ->
						topLevelBackStack.add(Route.MemoEdit(key.dateString, memoId))
					},
					onNavigateToMenu = {
						topLevelBackStack.add(Route.Menu(key.dateString))
					},
					onNavigateToOfficeWebsite = { url, officeName ->
						topLevelBackStack.add(Route.OfficeWebsite(url, officeName))
					},
					openEventDialogOnStart = key.openEventDialog
				)

				if (showDateDetailPaywall) {
					DateDetailPaywallSheet(
						onDismiss = {
							showDateDetailPaywall = false
							dateDetailOpenCount = 0          // 닫으면 카운터 리셋
						},
						onSubscribed = {
							showDateDetailPaywall = false
							dateDetailOpenCount = 0          // 구독 완료 후에도 리셋(이후 isSubscribed로 차단됨)
						}
					)
				}
			}

			// 승무소 사이트 WebView 화면
			entry<Route.OfficeWebsite> { key ->
				OfficeWebsiteScreen(
					url = key.url,
					officeName = key.officeName,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 식단 메뉴 화면
			entry<Route.Menu> { key ->
				MenuScreen(
					dateString = key.dateString,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 메모 편집 화면
			entry<Route.MemoEdit> { key ->
				MemoEditScreen(
					dateString = key.dateString,
					memoId = key.memoId,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 캘린더 선택 화면
			entry<Route.CalendarSelection> {
				CalendarSelectionScreen(
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 승무소 선택 화면
			entry<Route.ShiftSelection> {
				ShiftSelectionScreen(
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					},
					onNavigateToLocalOfficeList = {
						topLevelBackStack.add(Route.LocalOfficeList)
					},
					onNavigateToCustomShiftList = {
						topLevelBackStack.add(Route.CustomShiftList)
					}
				)
			}

			// 근무표 화면
			entry<Route.DiaTable> {
				DiaTableScreen(
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					},
					onNavigateToServerDiaEdit = { diaId ->
						topLevelBackStack.add(Route.ServerDiaEdit(diaId))
					},
					onNavigateToServerOfficeEdit = { officeCode ->
						topLevelBackStack.add(Route.ServerOfficeEdit(officeCode))
					}
				)
			}

			// 서버 근무표 편집
			entry<Route.ServerDiaEdit> { key ->
				ServerDiaEditScreen(
					diaId = key.diaId,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 서버 승무소 교번 패턴 편집
			entry<Route.ServerOfficeEdit> { key ->
				ServerOfficeEditScreen(
					officeCode = key.officeCode,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 휴가 설정 화면
			entry<Route.VacationSetting> {
				VacationSettingScreen(
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 로컬 승무소 목록
			entry<Route.LocalOfficeList> {
				LocalOfficeListScreen(
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					},
					onAddOffice = {
						topLevelBackStack.add(Route.LocalOfficeEdit(null))
					},
					onEditOffice = { officeId ->
						topLevelBackStack.add(Route.LocalOfficeEdit(officeId))
					},
					onManageDias = { officeId ->
						topLevelBackStack.add(Route.LocalDiaList(officeId))
					}
				)
			}

			// 로컬 승무소 편집
			entry<Route.LocalOfficeEdit> { key ->
				LocalOfficeEditScreen(
					officeId = key.officeId,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 로컬 근무표 목록
			entry<Route.LocalDiaList> { key ->
				LocalDiaListScreen(
					officeId = key.officeId,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					},
					onAddDia = {
						topLevelBackStack.add(Route.LocalDiaEdit(key.officeId, null))
					},
					onEditDia = { diaId ->
						topLevelBackStack.add(Route.LocalDiaEdit(key.officeId, diaId))
					}
				)
			}

			// 로컬 근무표 편집
			entry<Route.LocalDiaEdit> { key ->
				LocalDiaEditScreen(
					officeId = key.officeId,
					diaId = key.diaId,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 교대근무 목록
			entry<Route.CustomShiftList> {
				CustomShiftListScreen(
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					},
					onAddShift = {
						topLevelBackStack.add(Route.CustomShiftEdit(null))
					},
					onEditShift = { shiftId ->
						topLevelBackStack.add(Route.CustomShiftEdit(shiftId))
					}
				)
			}

			// 교대근무 편집
			entry<Route.CustomShiftEdit> { key ->
				CustomShiftEditScreen(
					shiftId = key.shiftId,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 텍스트 크기 설정 화면
			entry<Route.TextSizeSettings> {
				TextSizeSettingsScreen(
					onNavigateBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 인증 화면
			entry<Route.Auth> {
				AuthScreen(
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					},
					onNavigateToNicknameSetup = {
						Snapshot.withMutableSnapshot {
							topLevelBackStack.add(Route.NicknameSetup)
							topLevelBackStack.removeAll { it is Route.Auth }
						}
					},
					onNavigateToBoard = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 닉네임 설정 화면
			entry<Route.NicknameSetup> {
				NicknameSetupScreen(
					onComplete = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 게시글 상세
			entry<Route.PostDetail> { key ->
				PostDetailScreen(
					postId = key.postId,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 게시글 작성
			entry<Route.PostWrite> { key ->
				PostWriteScreen(
					initialCategory = key.category,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
						boardRefreshTrigger++
					}
				)
			}

			// 게시글 수정
			entry<Route.PostEdit> { key ->
				PostEditScreen(
					postId = key.postId,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
						boardRefreshTrigger++
					}
				)
			}

			// 차단된 사용자 관리
			entry<Route.BlockedUsers> {
				BlockedUsersScreen(
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 동료 그룹 관리
			entry<Route.CoworkerGroup> {
				CoworkerGroupScreen(
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			// 동료 추가/편집
			entry<Route.CoworkerEdit> { key ->
				CoworkerEditScreen(
					coworkerId = key.coworkerId,
					onBack = {
						if (topLevelBackStack.size > 1) {
							topLevelBackStack.removeAt(topLevelBackStack.lastIndex)
						}
					}
				)
			}

			entry<Route.Main> {
				val backStack = rememberNavBackStack(Route.Home)

				MainScreen(
					backStack = backStack,
					body = {paddedModifier ->
						NavDisplay(
							modifier = Modifier.fillMaxSize(),
							entryDecorators = listOf(
								rememberSaveableStateHolderNavEntryDecorator(),
								rememberViewModelStoreNavEntryDecorator()
							),
							backStack = backStack,
							entryProvider = entryProvider {
								entry<Route.Home> {
									HomeRoot(
									modifier = paddedModifier,
									onNavigateToDetail = { dateString ->
										openDateDetail(Route.DateDetail(dateString))
									},
									onNavigateToCalendarSelection = {
										topLevelBackStack.add(Route.CalendarSelection)
									},
									onNavigateToShiftSelection = {
										topLevelBackStack.add(Route.ShiftSelection)
									},
									onNavigateToAddMemo = { dateString ->
										topLevelBackStack.add(Route.MemoEdit(dateString, null))
									},
									onNavigateToAddEvent = { dateString ->
										topLevelBackStack.add(Route.DateDetail(dateString, openEventDialog = true))
									},
									onNavigateToDiaTable = {
										topLevelBackStack.add(Route.DiaTable)
									},
									onNavigateToVacationSetting = {
										topLevelBackStack.add(Route.VacationSetting)
									},
									onNavigateToTextSizeSettings = {
										topLevelBackStack.add(Route.TextSizeSettings)
									}
								) }
								entry<Route.Coworker> {
									val subscriptionRepository: SubscriptionRepository = koinInject()
									var isSubscribed by remember { mutableStateOf<Boolean?>(null) }
									val coworkerEntryContext = LocalContext.current.applicationContext

									LaunchedEffect(Unit) {
										val ssaid = DeviceIdProvider.getSsaid(coworkerEntryContext)
										isSubscribed = subscriptionRepository.isVip(ssaid)
									}

									when (isSubscribed) {
										null -> Box(
											modifier = paddedModifier,
											contentAlignment = Alignment.Center
										) {
											CircularProgressIndicator()
										}
										true -> CoworkerScreen(
											modifier = paddedModifier,
											onNavigateToCoworkerEdit = { id ->
												topLevelBackStack.add(Route.CoworkerEdit(id))
											},
											onNavigateToGroupManage = {
												topLevelBackStack.add(Route.CoworkerGroup)
											}
										)
										false -> PaywallScreen(
											onSubscribed = { isSubscribed = true },
											onDismiss = {
												Snapshot.withMutableSnapshot {
													backStack.clear()
													backStack.add(Route.Home)
												}
											}
										)
									}
								}
								entry<Route.SavedRecipes> {
									SavedRecipesRoot(
										onNavigateToDetails = { recipeId ->
											// 연타 방지
											topLevelBackStack.removeIf { it is Route.RecipeDetail }
											topLevelBackStack.add(Route.RecipeDetail(recipeId))
										}
									)
								}
								entry<Route.Notifications> {
									NotificationsScreen(
										modifier = paddedModifier,
										boardRefreshTrigger = boardRefreshTrigger,
										onNavigateToPostDetail = { postId ->
											topLevelBackStack.add(Route.PostDetail(postId))
										},
										onNavigateToPostWrite = { category ->
											topLevelBackStack.add(Route.PostWrite(category))
										},
										onNavigateToPostEdit = { postId ->
											topLevelBackStack.add(Route.PostEdit(postId))
										},
										onNavigateToAuth = {
											topLevelBackStack.add(Route.Auth)
										},
										onNavigateToBlockedUsers = {
											topLevelBackStack.add(Route.BlockedUsers)
										}
									)
								}

								entry<Route.Community> {
									CommunityScreen(modifier = paddedModifier)
								}

								entry<Route.Profile> { ProfileScreen() }
							}
						)
					}
				)
			}
			entry<Route.RecipeDetail> { key ->
				RecipeDetailsScreen(key.recipeId)
			}
		}
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDetailPaywallSheet(
	onDismiss: () -> Unit,
	onSubscribed: () -> Unit,
) {
	val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
	ModalBottomSheet(
		onDismissRequest = onDismiss,   // 스와이프/바깥터치 → 닫기 + 리셋
		sheetState = sheetState
	) {
		// PaywallScreen 내부 Scaffold+fillMaxSize가 시트 안에서 무한 높이를 요구하므로
		// 높이를 제한한 컨테이너로 감싼다.
		Box(modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
			PaywallScreen(
				onSubscribed = onSubscribed,
				onDismiss = onDismiss        // PaywallScreen의 "나중에" 버튼도 동일 처리
			)
		}
	}
}