# 근무 전용 실시간 채팅 — PRD & 구현 계획

> Product Requirements Document
> 문서 버전: 1.0
> 작성일: 2026년 5월 30일
> 관련 문서: [PRD.md](PRD.md), [PRD_realtime_share.md](PRD_realtime_share.md), [CLAUDE.md](../CLAUDE.md)

---

## 1. 개요

### 1.1 목적

근무와 관련된 연락만 주고받는 **앱 전용 1:1 실시간 채팅**을 만든다.
카카오톡은 개인적인 대화이므로, **"이 앱으로 온 메시지 = 근무 관련"**이라는 맥락을
사용자가 자연스럽게 인지하게 하는 것이 핵심 가치다. (교번 교대 요청, 충당 협의,
근무 변경 확인 등 근무 맥락의 소통을 사적 메신저와 분리)

### 1.2 핵심 차별점

| | 카카오톡 등 일반 메신저 | 본 기능 |
|---|---|---|
| 맥락 | 사적 + 업무 혼재 | **근무 전용** |
| 상대 | 전체 연락처 | 근무 공유로 연결된 동료 |
| 부가정보 | 없음 | 상대의 **근무 현황**과 한 화면에서 연동 가능 |

### 1.3 확정된 설계 결정

| 항목 | 결정 |
|------|------|
| **채팅 단위** | 1:1 일대일 (그룹은 추후 확장) |
| **사용자 식별** | 기존 **Google 로그인 + Supabase Auth(JWT)** 재사용 ([AuthRepositoryImpl](../app/src/main/java/com/sonbum/diacalendar2/data/repository/AuthRepositoryImpl.kt)) |
| **실시간** | **Supabase Realtime**(foreground) + **FCM**(background) 병행 |
| **서버** | 기존 Board Supabase 프로젝트 재사용 ([BoardSupabaseConfig](../app/src/main/java/com/sonbum/diacalendar2/data/remote/BoardSupabaseConfig.kt)) |

---

## 2. 기존 자산 재사용

채팅은 신규 기능이지만, 인프라 대부분이 이미 존재한다.

| 자산 | 위치 | 채팅에서의 역할 |
|------|------|----------------|
| Google 로그인 + JWT | [AuthRepositoryImpl](../app/src/main/java/com/sonbum/diacalendar2/data/repository/AuthRepositoryImpl.kt) | 채팅 사용자 식별, RLS 토큰 |
| 닉네임/프로필 | `profiles` 테이블, `setNickname` | 채팅 상대 표시 이름 |
| Board Supabase | [BoardSupabaseConfig](../app/src/main/java/com/sonbum/diacalendar2/data/remote/BoardSupabaseConfig.kt) | 채팅 테이블 호스팅 |
| FCM 인프라 | [DiaFirebaseMessagingService](../app/src/main/java/com/sonbum/diacalendar2/core/notification/DiaFirebaseMessagingService.kt) | background 새 메시지 알림 (type별 라우팅 이미 존재) |
| 근무 공유 | [PRD_realtime_share.md](PRD_realtime_share.md) | 채팅 상대 후보 = 공유로 연결된 동료 |

> **주의**: 기존 `ChatNote`([ChatNote.kt](../app/src/main/java/com/sonbum/diacalendar2/domain/model/ChatNote.kt))는
> **채팅이 아니라 개인 메모(혼잣말)**다. 본 기능과 무관하며 이름만 유사하니 혼동 주의.

---

## 3. 서버 설계 (Board Supabase)

### 3.1 테이블

```sql
-- 1:1 대화방 (두 사용자의 쌍당 하나)
create table chat_rooms (
  id          uuid primary key default gen_random_uuid(),
  user_a      uuid not null references auth.users(id),   -- 항상 작은 uuid를 a로 정규화
  user_b      uuid not null references auth.users(id),
  created_at  timestamptz default now(),
  last_message      text,
  last_message_at   timestamptz,
  unique (user_a, user_b)
);

-- 메시지
create table chat_messages (
  id          bigint generated always as identity primary key,
  room_id     uuid not null references chat_rooms(id) on delete cascade,
  sender_id   uuid not null references auth.users(id),
  content     text not null,
  created_at  timestamptz default now(),
  read_at     timestamptz                              -- 상대가 읽은 시각 (null = 안읽음)
);
create index on chat_messages (room_id, created_at desc);

-- FCM 토큰 등록 (푸시 발송 대상)
create table user_push_tokens (
  user_id    uuid not null references auth.users(id),
  fcm_token  text not null,
  updated_at timestamptz default now(),
  primary key (user_id, fcm_token)
);
```

> `user_a / user_b`는 항상 정렬된 순서로 저장(작은 uuid가 a)해 **쌍당 방 1개**를 보장한다.

### 3.2 RLS (프라이버시 — 채팅은 보안이 핵심)

```sql
alter table chat_rooms    enable row level security;
alter table chat_messages enable row level security;

-- 방: 내가 당사자인 방만 조회
create policy room_select on chat_rooms for select
  using (auth.uid() in (user_a, user_b));

-- 메시지: 내가 속한 방의 메시지만 조회
create policy msg_select on chat_messages for select
  using (exists (
    select 1 from chat_rooms r
    where r.id = room_id and auth.uid() in (r.user_a, r.user_b)
  ));

-- 메시지 전송: 내가 속한 방에 + 내 id로만
create policy msg_insert on chat_messages for insert
  with check (
    sender_id = auth.uid()
    and exists (
      select 1 from chat_rooms r
      where r.id = room_id and auth.uid() in (r.user_a, r.user_b)
    )
  );
```

> JWT 기반 RLS이므로 **당사자가 아니면 방·메시지를 읽지도 쓰지도 못한다.**
> (근무 공유의 anon-key 한계와 달리 채팅은 완전한 행 단위 보안 확보)

### 3.3 RPC

```sql
-- 상대와의 방을 가져오거나 없으면 생성 (정규화 포함)
create or replace function get_or_create_room(p_other uuid)
returns uuid language plpgsql security definer as $$
declare a uuid; b uuid; rid uuid;
begin
  if auth.uid() < p_other then a := auth.uid(); b := p_other;
  else a := p_other; b := auth.uid(); end if;
  insert into chat_rooms(user_a, user_b) values (a, b)
    on conflict (user_a, user_b) do nothing;
  select id into rid from chat_rooms where user_a = a and user_b = b;
  return rid;
end; $$;

-- 메시지 전송 + 방 last_message 갱신 + (트리거로) 푸시 큐잉
create or replace function send_message(p_room uuid, p_content text)
returns bigint language plpgsql security definer as $$ ... $$;

-- 읽음 처리
create or replace function mark_read(p_room uuid)
returns void language sql security definer as $$
  update chat_messages set read_at = now()
  where room_id = p_room and sender_id <> auth.uid() and read_at is null;
$$;
```

### 3.4 푸시 발송

`chat_messages` insert 시 DB 트리거 → Edge Function → 상대의 `user_push_tokens`로 FCM 발송.
FCM payload: `{ type: "chat", room_id, sender_name, body }`.

---

## 4. 클라이언트 설계 (Android)

### 4.1 데이터 흐름

```
┌─ 전송 ────────────────────────────────────────────┐
│ 입력 → send_message RPC                            │
│   → 낙관적 UI(로컬 즉시 표시) → 서버 ack로 확정     │
└────────────────────────────────────────────────────┘

┌─ 수신 (foreground) ───────────────────────────────┐
│ Supabase Realtime: chat_messages (room_id 필터) 구독│
│   → 새 메시지 도착 시 즉시 목록 갱신 + mark_read    │
└────────────────────────────────────────────────────┘

┌─ 수신 (background) ───────────────────────────────┐
│ FCM type="chat" → 알림 표시                         │
│   → 탭하면 해당 room으로 deep link                  │
└────────────────────────────────────────────────────┘
```

### 4.2 신규 구성요소

**Domain**
- `ChatRoom(id, otherUserId, otherName, lastMessage, lastMessageAt, unreadCount)`
- `ChatMessage(id, roomId, senderId, content, createdAt, readAt, isMine)`
- `ChatRepository` 인터페이스

**Data**
- `SupabaseChatApi` (Retrofit) — get_or_create_room / send_message / getMessages / mark_read
- `ChatRealtimeClient` — Supabase Realtime WebSocket 구독 (room별)
- `ChatRepositoryImpl`
- **Room 캐시** `chat_messages_cache`, `chat_rooms_cache` — 오프라인 열람 + 즉시 렌더

**Presentation**
- `ChatListScreen` / `ChatListViewModel` — 대화방 목록(최근 메시지·안읽음 배지)
- `ChatRoomScreen` / `ChatRoomViewModel` — 메시지 목록 + 입력창 + 실시간 구독
- 진입점: 동료(공유 상대) 화면의 "채팅" 버튼, 또는 하단 탭

**Core**
- FCM `type="chat"` 라우팅 추가 ([DiaFirebaseMessagingService](../app/src/main/java/com/sonbum/diacalendar2/core/notification/DiaFirebaseMessagingService.kt#L30))
- 로그인 직후 `user_push_tokens` 등록 (`onNewToken` + 로그인 시)

### 4.3 근무 공유와의 연동

- 채팅 상대 후보 = **근무 공유로 연결된 동료** (별도 친구추가 불필요)
- 채팅방 상단에 상대의 **오늘/내일 근무**를 함께 노출(공유 캐시 활용) → "근무 전용"의 가치 극대화
- **식별 통합 완료**: 공유도 로그인(auth uid) 기반으로 전환 결정([PRD_realtime_share.md](PRD_realtime_share.md) §1.3) → 공유·채팅이 **같은 auth uid**를 쓰므로 매핑 불필요. 공유로 연결된 동료(`sharedOwnerId`)에게 그대로 채팅 가능

---

## 5. 사용자 흐름 (UX)

1. (전제) Google 로그인 + 닉네임 설정 — 기존 흐름 재사용
2. 동료/공유 목록에서 상대 선택 → **"채팅"** → `get_or_create_room` → 방 진입
3. 메시지 입력·전송 (낙관적 표시) → 상대는 Realtime(켜짐)/FCM(꺼짐)으로 수신
4. 방 진입 시 자동 `mark_read` → 보낸 쪽에 읽음 표시
5. 대화방 목록: 최근 메시지·시간·안읽음 배지

---

## 6. 리스크 & 대응

| 리스크 | 대응 |
|--------|------|
| **로그인 강제** — 기존 비로그인 사용자 | 채팅·공유 등 소셜 기능만 로그인 게이트. 캘린더 핵심 기능은 비로그인 유지 |
| **Realtime 동시연결 제한**(무료 티어) | foreground 활성 방만 구독, 목록은 폴링/FCM. 백그라운드는 FCM 전담 |
| **스팸·악용·차단** | 차단(block) 테이블 + 신고. 근무 공유로 연결된 상대만 채팅 허용해 1차 차단 |
| ~~식별 이원화~~ (해결됨) | 공유도 로그인 기반 통합 결정 → 공유·채팅 모두 auth uid. 매핑 부채 없음 |
| **메시지 보존·용량** | 텍스트 우선(이미지 추후). 오래된 메시지 보존정책·페이지네이션 |
| **푸시 신뢰성** | Edge Function 실패 대비 재시도. Realtime 미수신분은 재진입 시 서버 재조회로 보정 |

---

## 7. 구현 순서 (검증 쉬운 순)

1. **서버**: 테이블 3개 + RLS + RPC(get_or_create_room, send_message, mark_read)
2. **전송/조회(폴링)**: 방 생성 → 메시지 전송 → 목록 조회 (Realtime 없이 먼저 동작 검증)
3. **Room 캐시 + ChatRoomScreen**: 대화 UI, 낙관적 전송
4. **Realtime 구독**: foreground 즉시 수신
5. **FCM**: `type="chat"` 라우팅 + `user_push_tokens` 등록 + Edge Function 발송
6. **목록/안읽음/읽음표시**: ChatListScreen, mark_read, unread 배지
7. **근무 연동**: 방 상단 상대 근무 표시, 동료에서 채팅 진입
8. **(옵션)** 차단/신고, 이미지, 그룹 채팅 확장

---

## 8. 미해결 / 추후 결정

- [x] ~~식별 통합~~: 공유도 로그인(auth uid) 기반으로 전환 결정 → 공유·채팅 단일 식별. **매핑 불필요**
- [ ] 채팅 진입점 위치 (하단 탭 신설 vs 동료 화면 내부)
- [ ] 이미지/파일 전송 범위 (텍스트 우선 권장)
- [ ] 메시지 보존 기간·삭제 정책
- [ ] 그룹 채팅(조·팀 단위) 확장 시점
- [ ] 차단/신고 정책 (커뮤니티 기능과 통합)
```
