package com.sonbum.diacalendar2.presentation.shiftcolor

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ShiftColorPickerDialogTest {
    @get:Rule val compose = createComposeRule()

    private var applied: String? = null
    private var dismissed = false

    private fun showPicker() {
        compose.setContent {
            MaterialTheme {
                ShiftColorPickerDialog(
                    title = "주간근무",
                    initialColor = Color(0xFF4285F4),
                    onDismiss = { dismissed = true },
                    onApply = { applied = it }
                )
            }
        }
    }

    @Test fun openingAndApplyingPreservesExistingColorWithoutShowingCode() {
        showPicker()
        compose.onNodeWithText("#4285F4").assertDoesNotExist()
        compose.onNodeWithText("색상 코드").assertDoesNotExist()
        compose.onNodeWithText("적용").performClick()
        compose.runOnIdle { assertEquals("#4285F4", applied) }
    }

    @Test fun draggingOnlyPreviewsAndCancelDoesNotApply() {
        showPicker()
        compose.onNodeWithContentDescription("색의 진하기와 밝기 선택")
            .performTouchInput { click(center) }
        compose.runOnIdle { assertNull(applied) }
        compose.onNodeWithText("취소").performClick()
        compose.runOnIdle {
            assertNull(applied)
            assertEquals(true, dismissed)
        }
    }

    @Test fun saturationBrightnessTopLeftSelectsWhite() {
        showPicker()
        compose.onNodeWithContentDescription("색의 진하기와 밝기 선택")
            .performTouchInput { swipe(center, Offset.Zero) }
        compose.onNodeWithText("적용").performClick()
        compose.runOnIdle { assertEquals("#FFFFFF", applied) }
    }

    @Test fun backDismissesWithoutSaving() {
        showPicker()
        Espresso.pressBack()
        compose.runOnIdle {
            assertNull(applied)
            assertEquals(true, dismissed)
        }
    }
}
