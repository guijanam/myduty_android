package com.sonbum.diacalendar2.presentation.menu

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sonbum.diacalendar2.domain.model.CafeteriaMenu
import com.sonbum.diacalendar2.ui.theme.DiaCalendar2Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MenuScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun loadedMenu_showsOnlyNonEmptyMealsWithoutTimes() {
        compose.setContent {
            DiaCalendar2Theme(darkTheme = false, dynamicColor = false) {
                MenuScreenContent(
                    state = loadedState(),
                    formattedDate = "9월 7일 월요일",
                    onBack = {},
                    onUploadClick = {},
                    onRetry = {},
                    onCafeteriaSelected = {}
                )
            }
        }

        compose.onNodeWithText("식단").assertExists()
        compose.onNodeWithText("9월 7일 월요일").assertExists()
        compose.onNodeWithTag("meal_card_breakfast").assertExists()
        compose.onNodeWithTag("meal_card_lunch").assertDoesNotExist()
        compose.onNodeWithTag("meal_card_dinner").assertExists()
        compose.onNodeWithText("소고기육개장").assertExists()
        compose.onNodeWithText("낙지제육비빔밥").assertExists()
        compose.onNodeWithText("07:00 ~ 09:00").assertDoesNotExist()
        compose.onNodeWithText("17:30 ~ 19:30").assertDoesNotExist()
    }

    @Test
    fun cafeteriaDropdown_reportsSelectedCafeteria() {
        var selected = ""
        compose.setContent {
            DiaCalendar2Theme(darkTheme = false, dynamicColor = false) {
                MenuScreenContent(
                    state = loadedState(),
                    formattedDate = "9월 7일 월요일",
                    onBack = {},
                    onUploadClick = {},
                    onRetry = {},
                    onCafeteriaSelected = { selected = it }
                )
            }
        }

        compose.onNodeWithTag("cafeteria_selector").performClick()
        compose.onNodeWithText("서초별관점").performClick()
        compose.runOnIdle {
            assertEquals("서초별관점", selected)
        }
    }

    @Test
    fun toolbarActions_haveAccessibleDescriptions() {
        compose.setContent {
            DiaCalendar2Theme(darkTheme = true, dynamicColor = false) {
                MenuScreenContent(
                    state = loadedState(),
                    formattedDate = "9월 7일 월요일",
                    onBack = {},
                    onUploadClick = {},
                    onRetry = {},
                    onCafeteriaSelected = {}
                )
            }
        }

        compose.onNodeWithContentDescription("뒤로").assertExists()
        compose.onNodeWithContentDescription("식단메뉴사진업로드").assertExists()
    }

    private fun loadedState(): MenuState {
        val menu = CafeteriaMenu(
            cafeteriaName = "동대문별관점",
            date = "2026-09-07",
            dayOfWeek = "월요일",
            breakfast = listOf("소고기육개장", "백미밥"),
            lunch = emptyList(),
            dinner = listOf("낙지제육비빔밥", "포기김치")
        )
        return MenuState(
            isLoading = false,
            allMenus = listOf(menu),
            cafeteriaNames = listOf("동대문별관점", "서초별관점"),
            selectedCafeteria = menu.cafeteriaName,
            dateString = menu.date
        )
    }
}
