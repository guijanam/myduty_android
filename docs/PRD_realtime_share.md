# 실시간 근무 현황 공유 — PRD & 구현 계획

> Product Requirements Document
> 문서 버전: 1.0
> 작성일: 2026년 5월 30일
> 관련 문서: [PRD.md](PRD.md), [CLAUDE.md](../CLAUDE.md)

---

## 1. 개요

### 1.1 목적

사용자가 서로를 등록해 **각자의 실제 근무 현황을 거의 실시간으로 확인**할 수 있게 한다.
같은 승무소일 필요는 없으며, 임의의 두 사용자가 서로를 등록하면 상대의 근무를 본다.

### 1.2 기존 기능과의 차이

현재 동료 기능([Coworker.kt](../app/src/main/java/com/sonbum/diacalendar2/domain/model/Coworker.kt))은
동료의 **패턴 + 기준일을 로컬에 수동 입력**하고 [calculateScheduleForMonth](../app/src/main/java/com/sonbum/diacalendar2/data/repository/CoworkerRepositoryImpl.kt#L87)로 계산해서 재현한다.
이 방식은 **상대의 교번교체·충당·근태(개인 오버레이)가 반영되지 않는다.**

본 기능은 이 한계를 서버 공유로 해결한다. 상대 기기가 직접 계산한
**유효 근무(최종 근무명)**를 서버에 올리고, 내가 그것을 구독해 그대로 표시한다.

### 1.3 핵심 설계 원칙

| 원칙 | 내용 |
|------|------|
| **계산 결과만 공유** | 원본 레코드(swap/input/근태)가 아니라, 이미 우선순위가 반영된 `(date, shiftName, color)`만 공유한다. 받는 쪽은 알고리즘 없이 그리기만 한다 → 알고리즘 버전 불일치 위험 0 |
| **2개월 슬라이딩 윈도우** | 항상 **오늘 ~ 오늘+2개월**만 유지한다. 과거(지나간 근무)는 공유하지 않는다 |
| **입력 즉시 반영** | 근무설정 시 윈도우 전체를 발행하고, 이후 근태·교체·충당을 입력/삭제하면 해당 날짜를 **즉시 발행**한다. 상대는 거의 실시간으로 본다 |
| **자정 = 윈도우 슬라이딩 전용** | 자정 워커는 "반영"이 아니라 윈도우를 하루 앞으로 굴리는 역할이다. 지나간 날을 삭제하고 새로 들어온 +2개월 끝날짜를 채운다 |
| **로그인 기반 식별 (auth uid)** | 공유는 **Google 로그인(Supabase Auth, JWT)** 으로 식별한다. `share_code`는 내 코드로 유지하되 뒤에 `auth uid`가 매핑된다. 채팅과 식별 체계를 통일해 매핑 부채를 없앤다 ([PRD_work_chat.md](PRD_work_chat.md) §8) |
| **소셜만 로그인 게이트** | 캘린더 핵심(교번·메모·위젯)은 **비로그인 유지**. 공유·채팅 등 소셜 기능에 진입할 때만 로그인을 요구한다 (기존 1,000명 사용자 보호) |
| **RLS 완전 보안** | auth uid 기반이므로 anon-key RPC 절충 없이 **행 단위 RLS**로 보호한다 |
| **폴링 우선** | 1,000명 규모 무료 티어를 고려해 Realtime보다 폴링(foreground)을 기본으로 한다 |

---

## 2. 공유 데이터 정의

### 2.1 유효 근무 (Effective Shift)

발행 데이터는 HomeViewModel이 이미 계산하는 유효 근무명을 그대로 쓴다.

```
우선순위: 지휴 > 지근 > 충당 > 교번교체 > 원래 교번
```

### 2.2 발행 윈도우

```
시작일 = 오늘 (LocalDate.now())
종료일 = 오늘 + 2개월 (today.plusMonths(2))
→ 약 60~62행 / 사용자
```

- **과거 미포함**: 오늘 이전 날짜는 발행하지 않는다.
- 매일 갱신 시 어제까지의 행은 서버에서 삭제하고, 새로 늘어난 끝날짜를 채운다.

### 2.3 공유 페이로드

```
(owner_device_id, date, shift_name, color)
```

`color`는 캘린더 배지에 쓰는 색상(충당/교체 색 포함)으로, 받는 쪽이 동일하게 표시하기 위함이다.

---

## 3. 서버 설계 (Supabase)

### 3.1 테이블

> 식별자는 **`auth.users(id)` (auth uid)**. 채팅과 동일 체계라 매핑 테이블이 불필요하다.

```sql
-- 공개 프로필: 서로 찾고 등록하기 위한 핸들 (uid ↔ share_code)
create table shared_profiles (
  user_id      uuid primary key references auth.users(id) on delete cascade,
  display_name text not null,
  share_code   text unique not null,        -- 6자리, 서로 등록용
  updated_at   timestamptz default now()
);

-- 평탄화된 유효 근무 (오늘 ~ +2개월)
create table shared_schedules (
  owner_id   uuid not null references auth.users(id) on delete cascade,
  date       date not null,
  shift_name text not null,
  color      text,
  updated_at timestamptz default now(),
  primary key (owner_id, date)
);

create index on shared_schedules (owner_id, date);
```

### 3.2 RLS (프라이버시 — 완전 행 단위 보안)

로그인(auth uid) 기반이므로 anon-key RPC 절충이 필요 없다.
**발행은 본인만, 조회는 share_code로 연결된 상대만** 허용한다.

```sql
alter table shared_schedules enable row level security;
alter table shared_profiles  enable row level security;

-- 프로필: 본인 행만 쓰기, 조회는 (등록을 위해) 인증 사용자에게 허용하되
--         share_code 단건 조회만 RPC로 노출 (전체 스캔 차단은 아래 RPC로)
create policy profile_write on shared_profiles for all
  using (user_id = auth.uid()) with check (user_id = auth.uid());

-- 근무: 발행은 본인만
create policy sched_write on shared_schedules for all
  using (owner_id = auth.uid()) with check (owner_id = auth.uid());

-- 근무 조회: 내가 "구독 등록"한 상대의 행만 (subscriptions 테이블 기준)
create table shared_subscriptions (         -- 내가 등록한 상대 목록
  subscriber_id uuid not null references auth.users(id) on delete cascade,
  owner_id      uuid not null references auth.users(id) on delete cascade,
  primary key (subscriber_id, owner_id)
);
create policy sched_read on shared_schedules for select
  using (exists (
    select 1 from shared_subscriptions s
    where s.owner_id = shared_schedules.owner_id
      and s.subscriber_id = auth.uid()
  ));
```

> 당사자(본인 발행분 + 내가 등록한 상대분)만 행을 볼 수 있다.
> JWT 없이는 아무것도 못 읽으므로 anon key 탈취 위험이 사라진다.

### 3.3 등록/발행 RPC

```sql
-- 공유코드로 상대 uid 해석 + 내 구독목록에 추가 (등록 시 1회)
create or replace function subscribe_by_code(p_code text)
returns table(owner_id uuid, display_name text)
language plpgsql security definer as $$
declare oid uuid; nm text;
begin
  select user_id, display_name into oid, nm
    from shared_profiles where share_code = p_code;
  if oid is null then raise exception 'invalid_code'; end if;
  insert into shared_subscriptions(subscriber_id, owner_id)
    values (auth.uid(), oid) on conflict do nothing;
  return query select oid, nm;
end; $$;

-- 프로필 등록/갱신 (본인 uid 사용)
create or replace function upsert_profile(p_name text)
returns text  -- 발급/유지된 share_code 반환
language plpgsql security definer as $$ ... auth.uid() ... $$;

-- 2개월치 일괄 발행: 과거 삭제 + 윈도우 upsert를 한 트랜잭션에서 (본인 uid)
create or replace function publish_schedule(p_rows jsonb)
returns void
language plpgsql security definer as $$
begin
  delete from shared_schedules
   where owner_id = auth.uid() and date < current_date;
  insert into shared_schedules(owner_id, date, shift_name, color)
  select auth.uid(), (r->>'date')::date, r->>'shift_name', r->>'color'
  from jsonb_array_elements(p_rows) r
  on conflict (owner_id, date)
  do update set shift_name = excluded.shift_name,
                color = excluded.color,
                updated_at = now();
end;
$$;
```

---

## 4. 클라이언트 설계 (Android)

### 4.1 데이터 흐름

```
┌─ 발행(Publish) ───────────────────────────────────────┐
│ HomeViewModel 유효근무 계산 (이미 존재)                  │
│   → 오늘 ~ 오늘+2개월 평탄화 (date, shiftName, color)    │
│   → publish_schedule RPC (과거삭제 + upsert)            │
│   트리거: ① 근무설정/재설정(전체) ② 근태·교체·충당 입력  │
│           시 즉시 ③ 자정 워커(윈도우 슬라이딩 전용)       │
└───────────────────────────────────────────────────────┘

┌─ 구독(Subscribe) ─────────────────────────────────────┐
│ 내가 등록한 상대(shared_subscriptions, RLS로 자동 필터)  │
│   → shared_schedules SELECT (date>=today) 폴링(foreground)│
│   → Room 캐시(shared_schedule_cache) 저장               │
│   → Coworker 캘린더에 표시 (계산 없이 그대로)            │
└───────────────────────────────────────────────────────┘
```

> 조회는 RLS가 "내가 등록한 상대"로 자동 필터하므로, 별도 조회 RPC 없이
> `shared_schedules` 직접 SELECT가 가능하다(JWT 필수).

### 4.2 모델/스키마 변경

- **Coworker 확장**: `sharedOwnerId: String?`(상대 auth uid), `shareCode: String?` 추가
  → 기존 "수동입력형 동료"와 "공유형 동료(서버 구독)"를 한 모델로 통합.
  공유형이면 `calculateScheduleForMonth` 대신 서버 캐시를 읽는다.
- **신규 Room 테이블** `shared_schedule_cache(ownerId, date, shiftName, color)` — 구독 결과 캐시.
- **신규 DataStore/엔티티** — 내 `share_code`, `display_name` 저장.
- **로그인 게이트**: 공유 진입 시 미로그인이면 기존 Google 로그인 흐름으로 유도.

### 4.3 발행 평탄화 로직

```kotlin
// 의사코드
val start = LocalDate.now()
val end = start.plusMonths(2)
val rows = (start..end).map { date ->
    PublishRow(
        date = date.toString(),
        shiftName = effectiveShiftFor(date),   // 기존 우선순위 계산 재사용
        color = colorFor(date)
    )
}
api.publishSchedule(rows)                      // owner_id=auth.uid()는 서버가 결정, 과거삭제+upsert도 서버 처리
```

### 4.4 발행 트리거 위치 (입력 즉시 반영)

기존 `WidgetUpdater.updateAll()` 호출 지점에 발행을 함께 끼운다:

| 시점 | 동작 |
|------|------|
| **근무설정 / 재설정** | 오늘~+2개월 **전체 발행** (초기 동기화) |
| **근태·교체·충당 입력/삭제** | 변경된 날짜가 윈도우(오늘~+2개월) 안이면 **즉시 발행** (해당 날짜만 또는 윈도우 전체 upsert) |
| **자정 워커(`MidnightWidgetWorker`)** | 발행 "반영"이 아니라 **윈도우 슬라이딩 전용** — 과거 날 삭제 + 새로 들어온 +2개월 끝날짜 채움 |

> `publish_schedule` RPC가 "과거 삭제 + 윈도우 upsert"를 한 트랜잭션에서 처리하므로,
> 입력 즉시 호출이든 자정 호출이든 **같은 RPC 하나로 일관되게** 동작한다.
> 네트워크 실패 시 다음 발행/자정 워커에서 자연 복구된다.

### 4.5 SupabaseApi 추가 메서드

```kotlin
// 모든 호출에 Authorization: Bearer <accessToken> 헤더 (기존 AuthRepository 토큰)
@POST("rpc/upsert_profile")     suspend fun upsertProfile(...): String          // share_code 반환
@POST("rpc/subscribe_by_code")  suspend fun subscribeByCode(...): List<ProfileDto> // 코드→상대 등록
@GET("shared_schedules")        suspend fun getSubscribedSchedules(...): List<SharedScheduleDto> // RLS 자동필터, date=gte.today
@POST("rpc/publish_schedule")   suspend fun publishSchedule(...)
```

---

## 5. 사용자 흐름 (UX)

### 5.1 공유 시작 (발행자)
1. 동료 화면 → "내 근무 공유하기" → (미로그인이면 Google 로그인) → 표시 이름 입력
2. 서버가 6자리 **공유코드** 발급 → 화면에 노출(복사/공유 버튼)
3. 이후 자동 발행 (오늘~+2개월, 입력 즉시 + 자정 슬라이딩)

### 5.2 동료 등록 (구독자)
1. 동료 화면 → "공유코드로 추가" → (미로그인이면 로그인) → 코드 입력
2. `subscribe_by_code`로 상대 확인 + 구독 등록 → 이름 표시
3. `Coworker(sharedOwnerId, shareCode)`로 저장 → 캘린더에 상대 근무 표시

### 5.3 표시
- 기존 Coworker 캘린더 UI 그대로. 공유형 동료는 서버 캐시값으로 채워진다.

---

## 6. 리스크 & 대응

| 리스크 | 대응 |
|--------|------|
| **로그인 강제** (기존 비로그인 사용자) | 캘린더 핵심은 비로그인 유지. **공유 진입 시에만** 로그인 게이트 |
| **프라이버시** | auth uid + RLS로 행 단위 보안. JWT 없이는 조회 불가 (anon 우회 위험 제거) |
| **Realtime 동시연결 제한** (무료 티어) | 폴링 우선. foreground 진입/주기 폴링 |
| **윈도우 밖 빈 날짜** | 매일 자정 워커가 과거 삭제 + 끝날짜 채움 (`publish_schedule`가 보장) |
| **상대가 며칠 앱 미실행** | 발행이 멈춰 끝쪽 날짜가 빌 수 있음 → 받는 쪽에 "최근 갱신 시각" 표시 검토 |
| **공유코드 충돌/추측** | 6자리 영숫자(대문자+숫자) 충돌검사 후 발급. 필요 시 자리수 확대 |

---

## 7. 구현 순서 (검증 쉬운 순)

1. **로그인 게이트**: 공유 진입 시 기존 Google 로그인/닉네임 흐름 연결 (채팅과 공통 기반)
2. **서버**: 테이블 3개(shared_profiles, shared_schedules, shared_subscriptions) + RLS + RPC(upsert_profile, subscribe_by_code, publish_schedule)
3. **발행**: 유효근무 2개월 평탄화 → `publish_schedule` (내 데이터가 서버에 올라가는지 먼저 검증)
4. **프로필/공유코드 UI**: 발급·노출·복사
5. **등록**: 공유코드 입력 → `subscribe_by_code` → Coworker에 `sharedOwnerId/shareCode` 연결
6. **구독**: `shared_schedules` SELECT(RLS 자동필터) 폴링 → Room 캐시 → 캘린더 표시
7. **트리거 정리**: 입력 즉시 발행 + 자정 워커 윈도우 슬라이딩
8. **(옵션)** Realtime 전환

---

## 8. 미해결 / 추후 결정

- [ ] 공유코드 자리수·문자셋 확정 (6자리 영숫자 제안)
- [ ] 폴링 주기 (foreground 진입 시 + N분 간격?)
- [ ] 발행 중지/공유 해제 UX (프로필 삭제 → cascade 삭제)
- [ ] 로그인 도입 시점 (프라이버시 강화 + 기기교체 대응)
```
