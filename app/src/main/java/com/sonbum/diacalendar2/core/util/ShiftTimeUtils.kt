package com.sonbum.diacalendar2.core.util

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast

/**
 * 근무표 시각 문자열에서 시작 시각(HH, mm)을 추출하고,
 * 시스템 시계 앱 알람으로 넘기는 유틸.
 *
 * 데이터 형식:
 * - workTime: "06:00", "20:08"
 * - firstTime/secondTime: "🔴08:52-10:47", "홍11:10-14:50", "09:26-12:28"
 *   → 색상 이모지/문자·범위(-종료)를 무시하고 맨 앞 시작 시각만 사용
 */
object ShiftTimeUtils {

    private val TIME_REGEX = Regex("""(\d{1,2}):(\d{2})""")

    /** 문자열에서 처음 등장하는 HH:mm을 (hour, minute)로 추출. 없으면 null */
    fun extractStartTime(value: String?): Pair<Int, Int>? {
        if (value.isNullOrBlank()) return null
        val m = TIME_REGEX.find(value) ?: return null
        val h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2].toIntOrNull() ?: return null
        if (h !in 0..23 || min !in 0..59) return null
        return h to min
    }

    /** 시작 시각에서 minutesBefore 만큼 뺀 (hour, minute). 전날로 넘어가면 wrap. */
    fun minusMinutes(start: Pair<Int, Int>, minutesBefore: Int): Pair<Int, Int> {
        val total = (start.first * 60 + start.second - minutesBefore).mod(24 * 60)
        return (total / 60) to (total % 60)
    }

    /**
     * 시스템 시계 앱에 알람 등록 화면을 띄운다 (SKIP_UI 없음 → 사용자가 확인 후 저장).
     * 시계 앱이 없으면 Toast로 안내.
     */
    fun setClockAlarm(context: Context, hour: Int, minute: Int, label: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "시계 앱을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
}
