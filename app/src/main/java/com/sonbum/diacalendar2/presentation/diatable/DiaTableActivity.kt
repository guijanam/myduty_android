package com.sonbum.diacalendar2.presentation.diatable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sonbum.diacalendar2.core.routing.Route
import com.sonbum.diacalendar2.data.local.datastore.ThemeMode
import com.sonbum.diacalendar2.data.local.datastore.ThemePreferences
import com.sonbum.diacalendar2.ui.theme.DiaCalendar2Theme
import org.koin.android.ext.android.inject

/**
 * 근무표를 앱 본체와 분리된 별도 창(태스크)으로 띄우는 Activity.
 * OfficeWebsiteActivity와 동일하게 자체 taskAffinity를 가져 최근앱/멀티윈도우에서 분리되어 보인다.
 *
 * 편집 모드의 하위 화면(ServerDiaEdit/ServerOfficeEdit)은 Activity 내부 NavDisplay로 처리한다.
 */
class DiaTableActivity : ComponentActivity() {
    private val themePreferences: ThemePreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            // 앱 내부 다크모드 설정(ThemePreferences)을 따르도록 함 (MainActivity와 동일)
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DiaCalendar2Theme(darkTheme = isDarkTheme) {
                val backStack = rememberNavBackStack(Route.DiaTable)

                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    backStack = backStack,
                    entryProvider = entryProvider {
                        // 근무표 (최상위) - 뒤로가기 시 Activity 종료
                        entry<Route.DiaTable> {
                            DiaTableScreen(
                                onBack = { finish() },
                                onNavigateToServerDiaEdit = { diaId ->
                                    backStack.add(Route.ServerDiaEdit(diaId))
                                },
                                onNavigateToServerOfficeEdit = { officeCode ->
                                    backStack.add(Route.ServerOfficeEdit(officeCode))
                                }
                            )
                        }

                        // 서버 근무표 편집
                        entry<Route.ServerDiaEdit> { key ->
                            ServerDiaEditScreen(
                                diaId = key.diaId,
                                onBack = {
                                    if (backStack.size > 1) {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                }
                            )
                        }

                        // 서버 승무소 교번 패턴 편집
                        entry<Route.ServerOfficeEdit> { key ->
                            ServerOfficeEditScreen(
                                officeCode = key.officeCode,
                                onBack = {
                                    if (backStack.size > 1) {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}
