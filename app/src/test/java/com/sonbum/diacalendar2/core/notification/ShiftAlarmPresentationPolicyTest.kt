package com.sonbum.diacalendar2.core.notification

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftAlarmPresentationPolicyTest {

    @Test
    fun `사용 중이고 잠금 해제 상태에서 오버레이 권한이 있으면 전체화면 액티비티를 실행한다`() {
        val result = resolveShiftAlarmPresentation(
            fullScreenEnabled = true,
            isInteractive = true,
            isKeyguardLocked = false,
            canDrawOverlays = true
        )

        assertEquals(ShiftAlarmPresentation.FULL_SCREEN_ACTIVITY, result)
    }

    @Test
    fun `사용 중이어도 오버레이 권한이 없으면 전체화면 알림으로 폴백한다`() {
        val result = resolveShiftAlarmPresentation(
            fullScreenEnabled = true,
            isInteractive = true,
            isKeyguardLocked = false,
            canDrawOverlays = false
        )

        assertEquals(ShiftAlarmPresentation.FULL_SCREEN_NOTIFICATION, result)
    }

    @Test
    fun `잠금 상태에서는 오버레이 권한과 무관하게 전체화면 인텐트를 사용한다`() {
        val result = resolveShiftAlarmPresentation(
            fullScreenEnabled = true,
            isInteractive = true,
            isKeyguardLocked = true,
            canDrawOverlays = true
        )

        assertEquals(ShiftAlarmPresentation.FULL_SCREEN_NOTIFICATION, result)
    }

    @Test
    fun `화면이 꺼져 있으면 오버레이 권한과 무관하게 전체화면 인텐트를 사용한다`() {
        val result = resolveShiftAlarmPresentation(
            fullScreenEnabled = true,
            isInteractive = false,
            isKeyguardLocked = false,
            canDrawOverlays = true
        )

        assertEquals(ShiftAlarmPresentation.FULL_SCREEN_NOTIFICATION, result)
    }

    @Test
    fun `전체화면 옵션이 꺼져 있으면 일반 알림을 사용한다`() {
        val result = resolveShiftAlarmPresentation(
            fullScreenEnabled = false,
            isInteractive = true,
            isKeyguardLocked = false,
            canDrawOverlays = true
        )

        assertEquals(ShiftAlarmPresentation.SIMPLE_NOTIFICATION, result)
    }
}
