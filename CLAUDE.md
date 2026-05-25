# DiaCalendar v2.0

> 한국 철도와 지하철 근무자를 위한 교번(교대) 근무 캘린더 앱 리빌드 프로젝트

## 프로젝트 개요

DiaCalendar는 약 1,000명의 활성 사용자를 보유한 철도 근무자용 교번 관리 앱입니다.
기존 앱을 최신 기술 스택으로 완전히 재구축하는 프로젝트입니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 |
| **Architecture** | MVVM (ViewModel + StateFlow) |
| **DI** | Koin |
| **Local DB** | Room |
| **Remote API** | Supabase REST API (Retrofit) |
| **Calendar** | Kizitonwose Calendar + Android CalendarProvider |
| **Async** | Coroutines + Flow |
| **Serialization** | Kotlinx Serialization |
| **Image** | Coil |
| **Navigation** | Navigation3 Compose |

## 현재 구현 상태

### ✅ 완료된 기능

#### 1. 프로젝트 기본 구조 (Phase 1)
- [x] Compose 프로젝트 생성
- [x] build.gradle 의존성 설정
- [x] Koin DI 모듈 구성
- [x] Room Database 설정
- [x] Navigation3 구조

#### 2. 메모 기능
- [x] **Room Database**
  - `MemoEntity` - 메모 데이터 저장
  - `MemoDao` - CRUD 쿼리
  - `AppDatabase` - Room Database 클래스

- [x] **Domain Layer**
  - `Memo` - Domain 모델
  - `MemoRepository` - Repository 인터페이스

- [x] **Data Layer**
  - `MemoRepositoryImpl` - Repository 구현체
  - `MemoMapper` - Entity ↔ Domain 변환

- [x] **Presentation Layer**
  - `HomeScreen` - 월간 캘린더 (Kizitonwose Calendar)
  - `HomeViewModel` - 캘린더 상태 관리, 메모 로드
  - `DateDetailScreen` - 날짜별 메모 목록
  - `DateDetailViewModel` - 메모 목록 관리, 순서 변경
  - `MemoEditScreen` - 메모 생성/편집 UI
  - `MemoEditViewModel` - 메모 저장/삭제 로직

#### 3. 메모 상세 기능
- [x] 메모 생성 (제목, 내용, 색상, 시간)
- [x] 메모 편집
- [x] 메모 삭제
- [x] 메모 완료 체크 (취소선 표시)
- [x] **연속 일정 생성** - 시작일~종료일까지 한번에 메모 생성
- [x] **드래그 앤 드롭** - 메모 순서 변경 (길게 눌러서)
- [x] 달력에 메모 인디케이터 표시 (색상 + 제목)
- [x] 완료된 메모 달력에서 취소선 표시

#### 4. 기기 캘린더 연동 (Phase 4 완료)
- [x] **권한 처리**
  - `READ_CALENDAR`, `WRITE_CALENDAR` 런타임 권한 요청
  - `rememberMultiplePermissionsState` 사용

- [x] **캘린더 선택**
  - `CalendarSelectionScreen` - 연동할 캘린더 선택 UI
  - `CalendarPreferences` - DataStore로 선택된 캘린더 ID 저장
  - 여러 캘린더 동시 선택 가능

- [x] **이벤트 조회 (Read)**
  - `CalendarContract.Instances` 테이블 사용 (반복 일정 포함)
  - 종일 이벤트 UTC 시간대 처리
  - 연속 일정(multi-day events) 올바르게 표시
  - 반복 일정(RRULE) 인스턴스 조회

- [x] **이벤트 생성/수정/삭제 (CRUD)**
  - `DeviceCalendarRepositoryImpl` - CalendarProvider API 래핑
  - 종일 이벤트 UTC 기준 저장
  - 반복 일정 RRULE + DURATION 처리
  - SharedFlow로 변경사항 알림

- [x] **반복 일정 지원**
  - 반복 주기: 매일, 매주, 매월, 매년 (FREQ)
  - 반복 종료 조건: 계속 반복, 횟수(COUNT), 종료일(UNTIL)
  - RRULE 파싱 및 빌드 함수

- [x] **연속 일정 지원**
  - 시작일/종료일 DatePicker로 선택
  - 여러 날에 걸친 이벤트 생성

- [x] **Domain Layer**
  - `CalendarEvent` - 캘린더 이벤트 모델 (rrule 필드 포함)
  - `DeviceCalendar` - 기기 캘린더 정보 모델
  - `DeviceCalendarRepository` - Repository 인터페이스

- [x] **UI**
  - `CalendarEventEditDialog` - 일정 생성/수정 다이얼로그
  - `CalendarEventCard` - 일정 카드 컴포넌트
  - `TimePickerDialog` - 시간 선택 다이얼로그
  - 다이얼로그 스크롤 지원

#### 5. 교번(교대 근무) 관리 시스템 (Phase 2 완료)
- [x] **Supabase 연동**
  - `OfficeDto` / `DiaDto` - Supabase REST API DTO
  - `SupabaseApi` - Retrofit 인터페이스
  - 승무소 목록 / 근무표 데이터 서버에서 fetch

- [x] **Room Database (교번 관련)**
  - `OfficeEntity` - 승무소 정보 (officeCode, officeName, diaTurns1/2, subTurns, diaSelects 등)
  - `DiaEntity` - 근무표 상세 데이터
  - `UserShiftConfigEntity` - 사용자 교번 설정 (승무소, 포지션, 교번패턴, 시작날짜, 기준날짜, 기준근무)
  - `ShiftScheduleEntity` - 날짜별 근무 스케줄 (date, shiftName)
  - `OfficeDao`, `DiaDao`, `UserShiftConfigDao`, `ShiftScheduleDao`
  - DB 마이그레이션: v3→4 (offices/dias 추가), v4→5 (user_shift_config/shift_schedules 추가), v5→6 (referenceDate 컬럼 추가), v12→13 (local_offices에 diaTurns3 추가), v15→16 (custom_shifts 추가)

- [x] **Domain Layer**
  - `Office` - 승무소 도메인 모델
  - `UserShiftConfig` - 사용자 교번 설정 모델 (officeCode, officeName, position, shiftPattern, startDate, todayShift, referenceDate)
  - `ShiftSchedule` - 날짜별 근무 스케줄 모델
  - `OfficeRepository`, `DiaRepository`, `ShiftRepository` - Repository 인터페이스

- [x] **Data Layer**
  - `OfficeRepositoryImpl` - 승무소 Repository (로컬 + 서버 동기화)
  - `DiaRepositoryImpl` - 근무표 Repository
  - `ShiftRepositoryImpl` - 교번 스케줄 Repository
  - `ShiftMapper` - Entity ↔ Domain 변환 (UserShiftConfig, ShiftSchedule)

- [x] **Presentation Layer**
  - `ShiftSelectionScreen` - 교번 설정 화면 (5단계 설정 UI)
  - `ShiftSelectionViewModel` - 교번 설정 로직

- [x] **교번 설정 5단계 UI**
  1. **승무소 선택** - 검색 드롭다운으로 승무소 선택
  2. **포지션 선택** - 기관사/차장/4조2교대 FilterChip
  3. **시작 날짜** - 달력에 교번이 표시되기 시작하는 날짜 (DatePicker)
  4. **기준교번 선택** - 기준 날짜 + 기준 근무 선택 (DatePicker + Dropdown)
  5. **근무 생성** - 3년치 스케줄 생성 버튼

- [x] **교번 생성 알고리즘**
  - `referenceDate`(기준 날짜)에 `todayShift`(기준 근무)가 오도록 패턴 정렬
  - `startDate`(시작 날짜)부터 3년간 스케줄 생성
  - 공식: `shiftIndex = ((refShiftIndex + daysFromRefToStart + dayOffset) % patternSize + patternSize) % patternSize`
  - 배치 저장 (1000개씩 chunked insert)

- [x] **재설정 시 기존 스케줄 보존**
  - 시작 날짜 이전의 기존 근무 순서는 보존
  - 시작 날짜 이후만 삭제 후 새로 생성 (`deleteFromDate` 쿼리)

- [x] **캘린더에 교번 표시**
  - `HomeScreen`에서 `shiftScheduleMap` Flow 구독
  - `ShiftBadge` Composable로 날짜 오른쪽 상단에 교번 표시
  - 교번별 색상 구분 (비/휴 = tertiary, 주 = secondary, 기타 = primary)

- [x] **기존 설정 복원**
  - 앱 재진입 시 기존 설정을 UI에 자동 반영
  - 기존 설정 경고 배너 표시

- [x] **Supabase 데이터 파싱**
  - `parseShiftList()` - JSON 배열(`["1","2","비"]`), 중괄호(`{주,...,임시}`), 일반 CSV 모두 지원
  - `[]`, `{}`, `"` 제거 후 콤마 분리

#### 6. 충당(ShiftInput) 기능
- [x] **Room Database (충당 관련)**
  - `ShiftInputTypeEntity` - 충당 유형 (name, shortName, colorHex, requiresLateWork)
  - `ShiftInputRecordEntity` - 충당 기록 (date, shiftInputTypeId, targetShiftName, originalShiftName, groupId)
  - `ShiftInputTypeDao`, `ShiftInputRecordDao` - CRUD 쿼리
  - DB 마이그레이션: v11→12 (shift_input_types/shift_input_records 추가)

- [x] **Domain Layer**
  - `ShiftInputType` - 충당 유형 모델 (기본값: 대기충당, 휴무충당, 지근충당)
  - `ShiftInputRecord` - 충당 기록 모델
  - `ShiftInputTypeRepository`, `ShiftInputRecordRepository` - Repository 인터페이스

- [x] **Data Layer**
  - `ShiftInputTypeRepositoryImpl` - 충당 유형 Repository (기본값 자동 삽입)
  - `ShiftInputRecordRepositoryImpl` - 충당 기록 Repository (교번교체와 동일한 패턴 알고리즘)

- [x] **충당 유형 3가지**
  | 유형 | 색상 | colorHex | 조건 |
  |------|------|----------|------|
  | 대기충당 | 초록색 | #4CAF50 | 없음 |
  | 휴무충당 | 보라색 | #9C27B0 | 없음 |
  | 지근충당 | 하늘색 | #03A9F4 | 지근 설정 필요 |

- [x] **UI**
  - `ShiftInputDialog` - 충당 설정 다이얼로그
    - 충당 유형 드롭다운 (색상 인디케이터 포함)
    - 교체할 교번 드롭다운
    - 충당일수 선택 (1~2일, 기본값 1)
  - `WorkTimeCard`에 충당 버튼 추가
  - 날짜 상세 화면에서 충당 정보 즉시 반영

- [x] **캘린더에 충당 표시**
  - `HomeViewModel`에서 `shiftInputMap` Flow 구독
  - `ShiftBadge`에 충당 색상 적용 (colorHex 파싱)
  - 충당 유형별 색상으로 배지 표시

- [x] **유효 교번 우선순위**
  ```kotlin
  // Priority: 지휴 > 지근 > 충당 > 교번교체 > 원래 교번
  val effectiveName = when {
      lateHoliday != null -> lateHoliday.lateHolidayName
      lateWork != null -> lateWork.lateWorkName
      shiftInput != null -> shiftInput.targetShiftName
      swap != null -> swap.swappedShiftName
      else -> originalShift
  }
  ```

#### 7. 교대근무자(CustomShift) 기능
- [x] **Room Database**
  - `CustomShiftEntity` - 교대근무 정보 (id, shiftName, shiftPattern CSV, createdAt)
  - `CustomShiftDao` - CRUD 쿼리 (Flow 기반 목록 조회)
  - DB 마이그레이션: v15→16 (custom_shifts 테이블 추가)

- [x] **Domain Layer**
  - `CustomShift` - 도메인 모델 (`shiftPattern: List<String>`)
  - `CustomShiftRepository` - Repository 인터페이스

- [x] **Data Layer**
  - `CustomShiftRepositoryImpl` - Repository 구현체 (CSV ↔ List 변환)

- [x] **Presentation Layer**
  - `CustomShiftListScreen` + `CustomShiftListViewModel` - 교대근무 목록/삭제
  - `CustomShiftEditScreen` + `CustomShiftEditViewModel` - 교대근무 생성/편집 (14일 미리보기)

- [x] **ShiftSelectionScreen 통합**
  - `OfficeSource` enum에 `CUSTOM_SHIFT` 추가 (서버용/내부용/교대근무자 3개 FilterChip)
  - `FlowRow`로 FilterChip 레이아웃 변경
  - 교대근무자 선택 시 포지션 단계 건너뜀 (4단계 설정)
  - "교대근무편집" TextButton으로 CustomShiftListScreen 네비게이션
  - `CustomShiftSelector` 드롭다운으로 교대근무 선택

- [x] **교대근무자 officeCode 식별 규칙**
  - `officeCode = -(10000 + customShiftId)` → 서버 승무소(양수), 내부 승무소(-1~-9999)와 구분
  - `isCustomShift` 판별: `officeCode <= -10000`
  - `position`은 `ENGINEER` 기본값 (실제 미사용)

- [x] **교대근무자 UI 분기 처리**
  - `HomeScreen` MonthFooter: 교대근무자일 때 공간 유지, 내용만 숨김
  - `DateDetailScreen`: 교대근무자일 때 교번교체/충당 버튼 숨김
  - `ShiftSelectionScreen`: 교대근무자일 때 "내부 승무소가 없습니다" 메시지 숨김

- [x] **기존 스케줄 생성 로직 완전 재사용**
  - `shiftRepository.generateAndSaveSchedules()` 그대로 사용
  - 서버 Supabase fetch는 건너뜀 (`officeCode >= 0`일 때만)

### 🚧 진행 예정

#### Phase 3 - 근무표 시스템
- [ ] 근무표 편집 (개별 날짜 근무 변경)
- [ ] .diacal 파일 내보내기/가져오기

## 프로젝트 구조

```
app/src/main/java/com/sonbum/diacalendar2/
├── di/
│   └── KoinModule.kt              # Koin DI 모듈 (DB, Repository, ViewModel)
│
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── MemoDao.kt         # 메모 DAO
│   │   │   ├── HolidayDao.kt      # 공휴일 DAO
│   │   │   ├── OfficeDao.kt       # 승무소 DAO
│   │   │   ├── DiaDao.kt          # 근무표 DAO
│   │   │   ├── UserShiftConfigDao.kt  # 사용자 교번 설정 DAO
│   │   │   ├── ShiftScheduleDao.kt    # 근무 스케줄 DAO
│   │   │   ├── ShiftInputTypeDao.kt   # 충당 유형 DAO
│   │   │   ├── ShiftInputRecordDao.kt # 충당 기록 DAO
│   │   │   └── CustomShiftDao.kt      # 교대근무 DAO
│   │   ├── entity/
│   │   │   ├── MemoEntity.kt      # 메모 Entity
│   │   │   ├── HolidayEntity.kt   # 공휴일 Entity
│   │   │   ├── OfficeEntity.kt    # 승무소 Entity
│   │   │   ├── DiaEntity.kt       # 근무표 Entity
│   │   │   ├── UserShiftConfigEntity.kt  # 사용자 교번 설정 Entity
│   │   │   ├── ShiftScheduleEntity.kt   # 근무 스케줄 Entity
│   │   │   ├── ShiftInputTypeEntity.kt  # 충당 유형 Entity
│   │   │   ├── ShiftInputRecordEntity.kt # 충당 기록 Entity
│   │   │   └── CustomShiftEntity.kt     # 교대근무 Entity
│   │   ├── database/
│   │   │   └── AppDatabase.kt     # Room Database (v16, 마이그레이션 포함)
│   │   ├── datastore/
│   │   │   └── CalendarPreferences.kt  # 캘린더 설정 DataStore
│   │   └── mapper/
│   │       ├── MemoMapper.kt      # 메모 Entity ↔ Domain 변환
│   │       └── ShiftMapper.kt     # 교번 Entity ↔ Domain 변환
│   ├── remote/
│   │   ├── api/
│   │   │   └── SupabaseApi.kt     # Supabase REST API (Retrofit)
│   │   └── dto/
│   │       ├── OfficeDto.kt       # 승무소 DTO
│   │       └── DiaDto.kt          # 근무표 DTO
│   └── repository/
│       ├── MemoRepositoryImpl.kt  # 메모 Repository 구현체
│       ├── DeviceCalendarRepositoryImpl.kt  # 캘린더 Repository 구현체
│       ├── OfficeRepositoryImpl.kt    # 승무소 Repository 구현체
│       ├── DiaRepositoryImpl.kt       # 근무표 Repository 구현체
│       ├── ShiftRepositoryImpl.kt     # 교번 스케줄 Repository 구현체
│       ├── ShiftInputTypeRepositoryImpl.kt   # 충당 유형 Repository 구현체
│       ├── ShiftInputRecordRepositoryImpl.kt # 충당 기록 Repository 구현체
│       └── CustomShiftRepositoryImpl.kt      # 교대근무 Repository 구현체
│
├── domain/
│   ├── model/
│   │   ├── Memo.kt                # 메모 Domain 모델
│   │   ├── CalendarEvent.kt       # 캘린더 이벤트 모델 (rrule 포함)
│   │   ├── DeviceCalendar.kt      # 기기 캘린더 정보 모델
│   │   ├── Office.kt              # 승무소 Domain 모델
│   │   ├── UserShiftConfig.kt     # 사용자 교번 설정 모델
│   │   ├── ShiftSchedule.kt       # 근무 스케줄 모델 (UserShiftConfig.kt 내 정의)
│   │   ├── ShiftInputType.kt      # 충당 유형 모델
│   │   ├── ShiftInputRecord.kt    # 충당 기록 모델
│   │   └── CustomShift.kt         # 교대근무 Domain 모델
│   └── repository/
│       ├── MemoRepository.kt      # 메모 Repository 인터페이스
│       ├── DeviceCalendarRepository.kt  # 캘린더 Repository 인터페이스
│       ├── OfficeRepository.kt    # 승무소 Repository 인터페이스
│       ├── DiaRepository.kt       # 근무표 Repository 인터페이스
│       ├── ShiftRepository.kt     # 교번 스케줄 Repository 인터페이스
│       ├── ShiftInputTypeRepository.kt   # 충당 유형 Repository 인터페이스
│       ├── ShiftInputRecordRepository.kt # 충당 기록 Repository 인터페이스
│       └── CustomShiftRepository.kt      # 교대근무 Repository 인터페이스
│
├── presentation/
│   ├── home/
│   │   ├── HomeScreen.kt          # 월간 캘린더 화면 (교번 배지 포함)
│   │   ├── HomeRoot.kt            # Home 진입점
│   │   ├── HomeViewModel.kt       # 캘린더 ViewModel (교번 스케줄 구독)
│   │   ├── HomeState.kt           # State/Action/Event
│   │   ├── DateDetailScreen.kt    # 날짜 상세 (메모 + 캘린더 이벤트)
│   │   └── DateDetailViewModel.kt # 메모/이벤트 목록 관리
│   ├── memo/
│   │   ├── MemoEditScreen.kt      # 메모 생성/편집 화면
│   │   ├── MemoEditViewModel.kt   # 메모 편집 로직
│   │   └── MemoEditState.kt       # State/Action/Event
│   ├── shift/
│   │   ├── ShiftSelectionScreen.kt    # 교번 설정 화면 (서버/내부/교대근무자 3소스)
│   │   └── ShiftSelectionViewModel.kt # 교번 설정 ViewModel
│   ├── customshift/
│   │   ├── CustomShiftListScreen.kt       # 교대근무 목록 화면
│   │   ├── CustomShiftListViewModel.kt    # 교대근무 목록 ViewModel
│   │   ├── CustomShiftEditScreen.kt       # 교대근무 편집 화면
│   │   └── CustomShiftEditViewModel.kt    # 교대근무 편집 ViewModel
│   ├── calendar/
│   │   └── CalendarSelectionScreen.kt  # 연동 캘린더 선택 화면
│   ├── main/
│   │   └── MainScreen.kt          # 메인 화면 (Bottom Nav)
│   └── ... (기타 화면들)
│
├── core/
│   └── routing/
│       ├── Route.kt               # Navigation Route 정의
│       └── NavigationRoot.kt      # Navigation 설정
│
└── ui/theme/                      # Material 3 테마
```

## Koin 모듈 구성

```kotlin
// databaseModule
- AppDatabase (Room, v16)
- MemoDao, HolidayDao, OfficeDao, DiaDao
- UserShiftConfigDao, ShiftScheduleDao
- ShiftInputTypeDao, ShiftInputRecordDao
- CustomShiftDao
- CalendarPreferences (DataStore)
- SupabaseApi (Retrofit)

// repositoryModule
- MemoRepositoryImpl → MemoRepository
- DeviceCalendarRepositoryImpl → DeviceCalendarRepository
- OfficeRepositoryImpl → OfficeRepository
- DiaRepositoryImpl → DiaRepository
- ShiftRepositoryImpl → ShiftRepository
- ShiftInputTypeRepositoryImpl → ShiftInputTypeRepository
- ShiftInputRecordRepositoryImpl → ShiftInputRecordRepository
- CustomShiftRepositoryImpl → CustomShiftRepository

// viewModelModule
- HomeViewModel
- DateDetailViewModel
- MemoEditViewModel
- CalendarSelectionViewModel
- ShiftSelectionViewModel
- CustomShiftListViewModel
- CustomShiftEditViewModel
```

## 데이터 흐름

```
[UI] ← StateFlow ← [ViewModel] ← Flow ← [Repository] ← [Room DAO]
         │                          │
         └── Action ────────────────┘
```

## 주요 화면 흐름

```
SignIn → Main
           ├── Home (캘린더 + 교번 배지)
           │     ├── DateDetail (날짜 상세)
           │     │     └── MemoEdit (메모 편집)
           │     └── ShiftSelection (교번 설정)
           │           └── CustomShiftList (교대근무 목록)
           │                 └── CustomShiftEdit (교대근무 편집)
           ├── SavedRecipes
           ├── Notifications
           └── Profile
```

## 코딩 컨벤션

### Kotlin
- Kotlin 공식 코딩 스타일 가이드 준수
- 함수/변수: camelCase
- 클래스/인터페이스: PascalCase
- 상수: SCREAMING_SNAKE_CASE

### Compose
- Composable 함수: PascalCase (예: `MemoEditScreen`)
- Modifier는 첫 번째 파라미터로

### Architecture
- Repository 패턴 사용
- ViewModel에서 UI 상태 관리 (StateFlow)
- 이벤트는 SharedFlow로 처리
- State/Action/Event 패턴

## Room Entity 네이밍

```kotlin
// Entity 클래스
@Entity(tableName = "memos")
data class MemoEntity(...)

// DAO 인터페이스
@Dao
interface MemoDao { ... }

// Domain 모델
data class Memo(...)
```

## 자주 사용하는 명령어

```bash
# 빌드
./gradlew assembleDebug

# 테스트
./gradlew test

# Lint 체크
./gradlew ktlintCheck
```

## 참고 링크

- [Jetpack Compose 공식 문서](https://developer.android.com/jetpack/compose)
- [Room 공식 문서](https://developer.android.com/training/data-storage/room)
- [Koin 공식 문서](https://insert-koin.io/)
- [Kizitonwose Calendar](https://github.com/kizitonwose/Calendar)
- [CalendarProvider 가이드](https://developer.android.com/guide/topics/providers/calendar-provider)
- [Supabase Kotlin 클라이언트](https://supabase.com/docs/reference/kotlin)

## 캘린더 이벤트 RRULE 형식

```kotlin
// 반복 주기 (FREQ)
- FREQ=DAILY    // 매일
- FREQ=WEEKLY   // 매주
- FREQ=MONTHLY  // 매월
- FREQ=YEARLY   // 매년

// 반복 종료 조건
- (없음)                    // 계속 반복
- ;COUNT=10                // 10회 반복
- ;UNTIL=20260201          // 2026년 2월 1일까지

// 예시
"FREQ=WEEKLY;COUNT=10"     // 매주 10회 반복
"FREQ=MONTHLY;UNTIL=20261231"  // 매월 2026년 12월 31일까지
```

## CalendarProvider API 주요 사항

```kotlin
// 반복 일정 조회 시 Instances 테이블 사용
CalendarContract.Instances.CONTENT_URI

// 반복 일정 생성 시 DTEND 대신 DURATION 사용
put(CalendarContract.Events.RRULE, "FREQ=DAILY")
put(CalendarContract.Events.DURATION, "P1D")  // 종일: P1D, 시간: PT3600S

// 종일 이벤트는 UTC 시간대로 저장
put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
```

## 교번 생성 알고리즘 상세

```kotlin
// 핵심 개념:
// - startDate: 달력에 교번이 표시되기 시작하는 날짜 (3년간 생성)
// - referenceDate: 기준 날짜 (이 날짜에 기준 근무가 오도록 정렬)
// - todayShift: 기준 근무 (referenceDate에 해당하는 교번)
// - shiftPattern: 교번 순환 패턴 (예: ["1", "2", "비", "주"])

// 오프셋 계산
val daysFromRefToStart = ChronoUnit.DAYS.between(referenceDate, startDate)
val refShiftIndex = shiftPattern.indexOf(todayShift)

// 각 날짜의 교번 인덱스
val totalOffset = daysFromRefToStart + dayOffset
val shiftIndex = ((refShiftIndex + totalOffset) % patternSize + patternSize) % patternSize

// 재설정 시: startDate 이전 스케줄은 보존, 이후만 삭제 후 재생성
shiftScheduleDao.deleteFromDate(startDateStr)  // WHERE date >= :fromDate
```

## 충당 기능 알고리즘

```kotlin
// 충당 생성 알고리즘 (ShiftInputRecordRepositoryImpl)
// 교번교체와 동일한 패턴 기반 순환 알고리즘 사용

// 1. 그룹 ID 생성 (동일 충당 건 묶음)
val groupId = UUID.randomUUID().toString()

// 2. 시작일부터 days 일수만큼 순환 패턴 적용
val targetIndex = shiftPattern.indexOf(targetShiftName)
for (offset in 0 until days) {
    val currentDate = startDate.plusDays(offset.toLong())

    // 교체할 교번 계산 (순환 패턴)
    val swappedIndex = ((targetIndex + offset) % patternSize + patternSize) % patternSize
    val swappedShiftName = shiftPattern[swappedIndex]

    // 레코드 생성 (shiftInputType 색상 정보 포함)
    ShiftInputRecordEntity(
        date = dateStr,
        shiftInputTypeId = shiftInputType.id,
        shortName = shiftInputType.shortName,
        colorHex = shiftInputType.colorHex,
        targetShiftName = swappedShiftName,
        originalShiftName = originalShiftName,
        groupId = groupId
    )
}
```

## 교대근무자(CustomShift) officeCode 규칙

```kotlin
// officeCode 범위별 식별
// 양수 (1~)       → 서버 승무소 (Supabase)
// 음수 (-1~-9999) → 내부 승무소 (LocalOffice)
// 음수 (-10001~)  → 교대근무자 (CustomShift)
//   공식: officeCode = -(10000 + customShiftId)
//   역산: customShiftId = -(officeCode) - 10000

// isCustomShift 판별
val isCustomShift = officeCode <= -10000

// 교대근무자일 때 숨기는 UI 요소:
// - MonthFooter 내용 (공간 유지)
// - DateDetailScreen의 교번교체/충당 버튼
// - ShiftSelectionScreen의 포지션 선택 단계
```

## 현재 이슈/TODO

- [ ] 기존 Realm → Room 마이그레이션 전략 수립
- [x] ~~CalendarProvider RRULE 파싱 라이브러리 선정~~ → 자체 파싱 함수 구현
- [x] ~~교번 패턴 Entity 설계~~ → UserShiftConfigEntity + ShiftScheduleEntity
- [x] ~~충당(ShiftInput) 기능 구현~~ → ShiftInputTypeEntity + ShiftInputRecordEntity
- [x] ~~교대근무자(CustomShift) 기능 구현~~ → CustomShiftEntity + 교번설정 통합
- [ ] .diacal 파일 스키마 확정
- [ ] 근무표 개별 날짜 편집 기능

---

> 💡 **팁**: docs/PRD.md를 읽고 전체 요구사항을 파악하세요.
