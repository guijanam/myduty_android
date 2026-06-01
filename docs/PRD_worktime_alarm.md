# 근무시간 기반 풀스크린 알람 — PRD & 구현 계획

> Product Requirements Document
> 문서 버전: 1.0
> 작성일: 2026년 5월 30일
> 관련 문서: [PRD.md](PRD.md), [CLAUDE.md](../CLAUDE.md)

---

## 1. 개요

### 1.1 목적

근무표가 이미 알고 있는 **각 근무의 시각 정보**(출근/전반사업/후반사업)를 기준으로,
사용자 설정에 따라 **운영체제 잠금화면 위로 전체화면을 깨우고 알람음·진동을 울리는
알람시계급 알람**을 제공한다. 단순 노티가 아니라 **"해제해야 꺼지는 진짜 알람"**이 목표다.

### 1.2 AI 미사용 (중요)

본 기능은 **LLM/AI를 사용하지 않는다.** 모든 동작은 기기가 이미 가진 데이터
(`workTime`=출근, `firstTime`=전반사업, `secondTime`=후반사업)와 사용자 설정에 따른 **규칙 기반**이다.
(자연어 브리핑 등 LLM 기반 기능은 별도 검토 대상이며 본 문서 범위 밖)

### 1.3 현재 구현 대비 격차

| 항목 | 현재 | 목표 |
|------|------|------|
| 트리거 시각 | `firstTime` **하나만**, N분 전 ([ShiftReminderWorker](../app/src/main/java/com/sonbum/diacalendar2/core/notification/ShiftReminderWorker.kt)) | **출근/전반사업/후반사업 3종** 각각 |
| 알림 형태 | 단순 노티 (`showShiftNotification`) | **풀스크린 + 알람음 + 진동** (해제 전까지 지속) |
| 설정 | `enabled` + `minutesBefore` 1쌍 ([NotificationPreferences](../app/src/main/java/com/sonbum/diacalendar2/data/local/datastore/NotificationPreferences.kt)) | **시각별 on/off·분·소리·진동** |
| 화면 깸 | 없음 | 잠금화면 위 **풀스크린 액티비티** |

---

## 2. 기존 자산 재사용

대부분의 뼈대가 이미 존재한다. 신규는 "풀스크린 격상"과 "시각 3종 확장"이 핵심.

| 자산 | 위치 | 역할 |
|------|------|------|
| 정확 알람 스케줄러 | [AlarmScheduler](../app/src/main/java/com/sonbum/diacalendar2/core/notification/AlarmScheduler.kt) | `setExactAndAllowWhileIdle`로 정시 발화 (재사용) |
| 알람 수신부 | [AlarmReceiver](../app/src/main/java/com/sonbum/diacalendar2/core/notification/AlarmReceiver.kt) | 발화 시점 진입점 (풀스크린 분기 추가) |
| 주기 등록 워커 | [ShiftReminderWorker](../app/src/main/java/com/sonbum/diacalendar2/core/notification/ShiftReminderWorker.kt) | 7일치 알람 미리 등록 (3종으로 확장) |
| 부팅 복원 | [BootReceiver](../app/src/main/java/com/sonbum/diacalendar2/core/notification/BootReceiver.kt) | 재부팅 후 알람 재등록 (재사용) |
| 근무시간 데이터 | `DiaDto.firstTime/secondTime/thirdTime/workTime` | 알람 기준 시각 |
| 설정 저장 | [NotificationPreferences](../app/src/main/java/com/sonbum/diacalendar2/data/local/datastore/NotificationPreferences.kt) | DataStore (스키마 확장) |

---

## 3. 알람 시점 정의

`DiaDto`의 시각 필드를 3종 알람으로 매핑한다. **각각 독립 on/off + 분 설정.**

### 3.1 필드 의미 (실제)

| 필드 | 실제 의미 | 알람 사용 |
|------|-----------|-----------|
| `workTime` | **출근 시각** | ✅ 출근 알람 |
| `firstTime` | 전반 사업(작업) 시각 | ✅ 전반사업 알람 |
| `secondTime` | 후반 사업(작업) 시각 | ✅ 후반사업 알람 |
| `thirdTime` | **불확정** — 시각일 수도, 특정 단어일 수도 | ❌ **제외** (파싱 불안정) |

> ⚠️ **기존 버그**: 현재 [ShiftReminderWorker](../app/src/main/java/com/sonbum/diacalendar2/core/notification/ShiftReminderWorker.kt#L48)는
> `firstTime`을 출근시각으로 간주해 알람을 건다. 실제 출근은 **`workTime`**이므로,
> 이번 작업에서 출근 알람 기준을 `firstTime` → `workTime`으로 **수정**해야 한다.

### 3.2 알람 3종

| 알람 종류 | 기준 필드 | 기본 동작 | 용도 |
|-----------|-----------|-----------|------|
| **출근 알람** | `workTime` | N분 **전** | 출근 준비 (핵심) |
| **전반사업 알람** | `firstTime` | 정시 또는 N분 전 | 전반 작업 시작 |
| **후반사업 알람** | `secondTime` | 정시 또는 N분 전 | 후반 작업 시작 |

> 해당 필드가 공란이거나 시각 형식(`HH:mm`)이 아니면 그 알람은 건너뛴다.
> (특히 `thirdTime`은 단어가 올 수 있어 아예 알람 대상에서 제외)
> 시각 파싱은 기존 `LocalTime.parse(it, "HH:mm")` 패턴 재사용.

---

## 4. 풀스크린 알람 설계 (핵심)

### 4.1 노티 vs 풀스크린의 차이

"알람시계급"의 핵심은 **Full-Screen Intent + 전용 알람 액티비티**다.

```
일반 노티       : 상태바에 조용히 표시, 화면 안 깸
풀스크린 알람   : 잠금화면 위로 액티비티 전체 표시 + 알람음 루프 + 진동
                 → 사용자가 "해제/스누즈" 눌러야 멈춤
```

### 4.2 필요한 권한·플래그

```xml
<!-- AndroidManifest -->
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT"/>
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>   <!-- 이미 사용 중 -->
<uses-permission android:name="android.permission.VIBRATE"/>

<activity android:name=".presentation.alarm.AlarmRingActivity"
    android:showWhenLocked="true"
    android:turnScreenOn="true"
    android:excludeFromRecents="true"
    android:launchMode="singleInstance"/>
```

> Android 14+ 는 `USE_FULL_SCREEN_INTENT`가 알람/통화 앱 외에는 제한될 수 있어,
> 설치 후 권한 상태 확인 + 안내 필요(폴백: 고우선순위 헤드업 노티).

### 4.3 발화 흐름

```
AlarmScheduler (정시 발화)
  → AlarmReceiver.onReceive
      ├─ 풀스크린 모드: AlarmRingActivity를 full-screen intent로 기동
      │     · showWhenLocked + turnScreenOn → 잠금화면 위 화면 켜짐
      │     · Ringtone/MediaPlayer 알람음 루프 + Vibrator 패턴
      │     · "해제" / "스누즈(N분)" 버튼
      └─ 폴백(권한 없음): 고우선순위 헤드업 노티 + 소리
```

### 4.4 신규 구성요소

| 구성요소 | 역할 |
|----------|------|
| `AlarmRingActivity` (presentation/alarm) | 풀스크린 알람 화면: 근무명·시각·해제·스누즈 |
| `AlarmSoundController` | Ringtone/MediaPlayer 재생·정지, AudioFocus, 진동 |
| `AlarmReceiver` 분기 추가 | 풀스크린 vs 노티 결정 |
| `AlarmScheduler` 확장 | 출근/전반/후반 type별 request code 분리 |

---

## 5. 설정 (NotificationPreferences 확장)

```kotlin
data class WorkAlarmPrefs(
    // 출근 (workTime)
    val commuteEnabled: Boolean = false,
    val commuteMinutesBefore: Int = 60,
    // 전반사업 (firstTime)
    val firstEnabled: Boolean = false,
    val firstMinutesBefore: Int = 0,
    // 후반사업 (secondTime)
    val secondEnabled: Boolean = false,
    val secondMinutesBefore: Int = 0,
    // 공통 강도
    val fullScreen: Boolean = true,    // 풀스크린 알람시계급
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val soundUri: String? = null,      // 사용자 지정 알람음 (옵션)
    val snoozeMinutes: Int = 5
)
```

> 기존 `ShiftReminderPrefs(enabled, minutesBefore)`는 **출근 알람(commute)으로 마이그레이션**.
> (단, 기준 필드는 `firstTime` → `workTime`으로 교정)

---

## 6. 스케줄 등록 로직 (ShiftReminderWorker 확장)

```kotlin
// 7일치 스케줄 순회 (기존 골격 유지)
for (schedule in schedules) {
    val dia = diaDao.getDiaByDiaIdAndOffice(schedule.shiftName, config.officeName) ?: continue

    if (prefs.commuteEnabled) scheduleAt(dia.workTime,   -prefs.commuteMinutesBefore, ALARM_COMMUTE)
    if (prefs.firstEnabled)   scheduleAt(dia.firstTime,  -prefs.firstMinutesBefore,   ALARM_FIRST)
    if (prefs.secondEnabled)  scheduleAt(dia.secondTime, -prefs.secondMinutesBefore,  ALARM_SECOND)
    // thirdTime: 단어가 올 수 있어 알람 대상에서 제외
}
// scheduleAt: 시각 파싱(HH:mm) 실패/공란이면 skip, 과거면 skip
```

- request code: `type(출근/전반/후반) × date` 조합으로 분리 (3종이 서로 안 덮어쓰게)
- 재등록 트리거: 기존과 동일 — 설정 변경 / 근무 재설정 / 자정 워커 / 부팅

---

## 7. 사용자 흐름 (UX)

1. 설정 → "근무 알람" → 출근/전반사업/후반사업 각 토글 + 분 + (소리/진동/풀스크린)
2. (Android 12+) 정확 알람 권한, (14+) 풀스크린 권한 안내·요청
3. 해당 시각 도래 → 화면 켜지며 풀스크린 알람 → **해제/스누즈**
4. 폴백: 권한 없으면 헤드업 노티

---

## 8. 리스크 & 대응

| 리스크 | 대응 |
|--------|------|
| **풀스크린 권한 제한** (Android 14+) | 권한 상태 확인 + 설정 유도, 미허용 시 고우선순위 노티 폴백 |
| **Doze/제조사 절전** | `setExactAndAllowWhileIdle`(이미 사용), 배터리 최적화 예외 안내 |
| **정확 알람 권한**(Android 12+) | `canScheduleExactAlarms` 체크(이미 있음) + 설정 유도 |
| **시각 데이터 공란/형식 오류** | 파싱 실패·공란이면 해당 알람 skip (조용히) |
| **알람음 무한 루프** | 타임아웃(예: 5분 후 자동 정지), AudioFocus 반환 |
| **중복 발화** | type×date request code로 유일성 보장 |
| **재부팅 후 소실** | BootReceiver에서 워커 재실행(이미 존재) |

---

## 9. 구현 순서 (검증 쉬운 순)

1. **설정 확장**: `WorkAlarmPrefs` + 설정 UI(3종 토글·분·강도)
2. **스케줄러 확장**: `AlarmScheduler`에 type별 request code, `ShiftReminderWorker` 3종 등록
3. **풀스크린 액티비티**: `AlarmRingActivity` + manifest 플래그/권한
4. **소리/진동**: `AlarmSoundController` (루프·타임아웃·AudioFocus·Vibrator)
5. **수신 분기**: `AlarmReceiver`에서 풀스크린 intent 기동 + 폴백 노티
6. **권한 안내**: 정확알람·풀스크린 권한 체크/유도
7. **스누즈/해제**: 버튼 액션, 스누즈 재스케줄
8. **마이그레이션**: 기존 `ShiftReminderPrefs` → 출근 알람으로 이전

---

## 10. 미해결 / 추후 결정

- [ ] 전반/후반사업 알람 기본값: "정시" vs "N분 전" 중 무엇을 기본으로
- [ ] 사용자 지정 알람음 선택 UI 범위 (시스템 링톤 피커 연동?)
- [ ] 스누즈 횟수 제한 / 자동 해제 시간
- [x] ~~필드 의미 확정~~: `workTime`=출근, `firstTime`=전반, `secondTime`=후반, `thirdTime`=제외 (확정됨)
- [ ] 출근 알람 기준 교정: 기존 `ShiftReminderWorker`의 `firstTime` → `workTime`으로 변경 (구현 시 반영)
- [ ] (범위 밖) 자연어 브리핑 등 LLM 기능은 별도 문서에서 검토
```
