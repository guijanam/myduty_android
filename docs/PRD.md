# DiaCalendar v2.0 PRD

> Product Requirements Document  
> 문서 버전: 1.4  
> 작성일: 2025년 1월 14일

---

## 1. 프로젝트 개요

### 1.1 앱 소개

DiaCalendar는 한국 철도와 지하철근무자를 위한 교번(교대) 근무 캘린더 앱입니다. 복잡한 교번 패턴을 쉽게 관리하고, 동료들과 일정을 공유하며, 기기 캘린더와 연동하여 개인 일정을 통합 관리할 수 있습니다.

### 1.2 리빌드 목적

- Realm에서 Room으로 전환하여 테이블별 개별 백업/복원 기능 구현
- 최신 Android 기술 스택 적용으로 유지보수성 향상
- Jetpack Compose 기반 선언형 UI로 코드 간소화
- 구독 전용 수익 모델에 최적화된 구조 설계
- P2P 근무표 공유로 서버 의존성 감소

### 1.3 타겟 사용자

한국 철도공사(코레일), 서울교통공사 등 철도 관련 기관의 교번 근무자

---

## 2. 기술 스택

| 분류 | 기술 | 비고 |
|------|------|------|
| UI Framework | Jetpack Compose | Material 3 Design |
| Architecture | MVVM | ViewModel + StateFlow |
| DI | Koin | 간결한 설정 |
| Local DB | Room | 개별 테이블 백업/복원 |
| Remote API | Supabase REST API | 근무표 데이터 소스 |
| Calendar | CalendarProvider | 기기 캘린더 CRUD |
| 비동기 처리 | Coroutines + Flow | Reactive streams |
| 네트워크 | Retrofit + OkHttp | REST API 통신 |
| 직렬화 | Kotlinx Serialization | JSON 파일 처리 |
| 이미지 로딩 | Coil | Compose 최적화 |
| Navigation | Navigation Compose | Type-safe navigation |

---

## 3. 핵심 기능

### 3.1 교번 관리 (MVP)

- 교번 패턴 생성 및 편집
- 교번 적용 시작일 설정
- 캘린더 뷰에서 교번 일정 표시
- 교번 색상 커스터마이징

### 3.2 캘린더 뷰 (MVP)

- 월간 캘린더 뷰
- 주간 타임라인 뷰 (Google Calendar 스타일)
- 일별 상세 뷰
- 이벤트 드래그 앤 드롭 생성/수정

### 3.3 기기 캘린더 연동 (MVP)

> Android CalendarProvider를 통해 기기에 등록된 모든 캘린더와 연동합니다.

#### 지원 캘린더

- Google Calendar
- Samsung Calendar
- Outlook Calendar
- 기타 기기에 등록된 모든 캘린더 계정

#### 캘린더 관리

- 기기에 등록된 캘린더 목록 조회
- 연동할 캘린더 선택 (다중 선택)
- 캘린더별 표시/숨김 설정
- 캘린더 색상 표시 (원본 색상 유지)

#### 이벤트 조회 (Read)

- 선택된 캘린더의 이벤트 조회
- 교번 일정과 함께 통합 표시
- 이벤트 상세 정보 조회 (제목, 시간, 장소, 설명)
- 반복 이벤트 인스턴스 조회

#### 이벤트 생성 (Create)

- 앱 내에서 새 이벤트 생성
- 저장할 캘린더 선택
- 제목, 시간, 장소, 설명 입력
- 알림 설정 (Reminders)
- 반복 규칙 설정 (RRULE)
- 종일 이벤트 지원

#### 이벤트 수정 (Update)

- 기존 이벤트 정보 수정
- 드래그 앤 드롭으로 시간 변경
- 반복 이벤트 수정 옵션: 이 일정만 / 이후 모든 일정 / 모든 일정
- 기기 캘린더에 즉시 반영

#### 이벤트 삭제 (Delete)

- 이벤트 삭제 (확인 다이얼로그)
- 반복 이벤트 삭제 옵션: 이 일정만 / 이후 모든 일정 / 모든 일정
- 기기 캘린더에 즉시 반영

### 3.4 근무표 관리 (MVP)

> Supabase 서버와 P2P 파일 공유를 통한 이중화된 근무표 업데이트 시스템을 제공합니다.

#### 3.4.1 서버 다운로드 (Supabase)

- 소속 기관/노선/사업소/직종 설정
- "근무표 업데이트" 버튼으로 서버에서 다운로드
- 다운로드한 데이터 Room DB에 저장
- 마지막 업데이트 시간 표시

#### 3.4.2 근무표 편집

- 다운로드한 근무표 데이터 앱 내에서 수정 가능
- 교번 패턴 추가/수정/삭제
- 근무 시간 조정
- 신규 교번 생성
- 수정 이력 관리 (수정일, 수정자 정보)

#### 3.4.3 근무표 내보내기 (Export)

- 현재 근무표를 `.diacal` 파일로 내보내기
- 파일 포맷: JSON 기반 (버전 정보 포함)
- 내보내기 범위 선택 (전체/특정 노선/사업소)
- 파일에 포함되는 정보: 기관, 노선, 사업소, 교번 패턴, 수정일, 버전
- 공유 기능 (카카오톡, 이메일, 드라이브 등)

#### 3.4.4 근무표 가져오기 (Import)

- `.diacal` 파일에서 근무표 데이터 가져오기
- 파일 선택 (파일 관리자, 공유된 파일)
- 가져오기 전 미리보기 (변경사항 비교)
- 버전 비교: 최신 파일인지 확인
- 가져오기 옵션: 덮어쓰기 / 병합
- 호환성 체크 (파일 버전, 데이터 무결성)

#### 3.4.5 P2P 공유 시나리오

Supabase 서버 장애 시에도 사용자 커뮤니티를 통해 근무표를 유지할 수 있습니다.

1. 관리자 사용자가 근무표 변경사항을 앱에서 수정
2. 수정된 근무표를 `.diacal` 파일로 내보내기
3. 카카오톡/커뮤니티 등을 통해 파일 공유
4. 다른 사용자들이 파일을 받아 가져오기
5. 모든 사용자가 최신 근무표 사용 가능

### 3.5 동료 일정 (Premium)

- 동료 추가 및 관리
- 동료 교번 일정 조회
- 무료 사용자 동료 수 제한

### 3.6 백업 및 복원 (Premium)

- 테이블별 개별 백업/복원
- Google Drive 클라우드 백업
- 로컬 파일 백업

### 3.7 Wear OS 연동 (Premium)

- 워치 페이스 컴플리케이션
- 오늘/내일 교번 정보 표시
- 타일 서비스

---

## 4. 데이터 모델

### 4.1 Room Entities (로컬)

| Entity | 설명 |
|--------|------|
| Organization | 기관 정보 (코레일, 서울교통공사 등) |
| Line | 노선 정보 |
| Station | 사업소 정보 |
| ShiftPattern | 교번 패턴 정의 (이름, 색상, 근무시간 등) |
| ShiftCycle | 교번 사이클 (패턴 순서, 반복 주기) |
| ShiftAssignment | 특정 날짜에 적용된 교번 |
| ShiftScheduleMeta | 근무표 메타데이터 (버전, 수정일, 수정자) |
| CustomEvent | 사용자 정의 이벤트 (메모, 일정) |
| Coworker | 동료 정보 (이름, 교번 패턴) |
| UserSettings | 앱 설정 (테마, 알림 등) |
| ShiftDownloadConfig | 근무표 다운로드 설정 |
| CalendarSyncConfig | 캘린더 연동 설정 (선택된 캘린더 ID 등) |

### 4.2 Supabase Tables (원격)

| Table | 설명 |
|-------|------|
| organizations | 기관 목록 |
| lines | 노선 목록 |
| stations | 사업소 목록 |
| shift_schedules | 근무표 마스터 데이터 |
| shift_patterns | 교번 패턴 정의 |

### 4.3 CalendarProvider URI

| URI | 설명 |
|-----|------|
| Calendars.CONTENT_URI | 캘린더 목록 조회 |
| Events.CONTENT_URI | 이벤트 CRUD |
| Instances.CONTENT_URI | 반복 이벤트 인스턴스 조회 |
| Reminders.CONTENT_URI | 알림 설정 |
| Attendees.CONTENT_URI | 참석자 정보 |

### 4.4 근무표 파일 포맷 (.diacal)

JSON 기반의 근무표 교환 포맷입니다.

```json
{
  "version": "1.0",
  "exportedAt": "2025-01-14T10:30:00Z",
  "exportedBy": "사용자명 (선택적)",
  "organization": { "id": 1, "name": "서울교통공사" },
  "line": { "id": 2, "name": "2호선" },
  "station": { "id": 3, "name": "동대문승무소" },
  "patterns": [
    {
      "id": "pattern_001",
      "name": "주간",
      "color": "#FF5733",
      "startTime": "06:00",
      "endTime": "15:00",
      "isNightShift": false
    }
  ],
  "cycles": [],
  "metadata": {
    "lastModified": "2025-01-14T10:00:00Z",
    "modifiedBy": "관리자"
  }
}
```

---

## 5. CalendarProvider 연동 상세

### 5.1 필요 권한

| 권한 | 용도 |
|------|------|
| READ_CALENDAR | 캘린더 및 이벤트 조회 |
| WRITE_CALENDAR | 이벤트 생성/수정/삭제 |

- 런타임 권한 요청 필요 (Android 6.0+)
- 권한 거부 시 대체 UI 제공
- 권한 설정 화면으로 안내

### 5.2 CalendarProvider vs Google Calendar API

| 항목 | CalendarProvider | Google Calendar API |
|------|------------------|---------------------|
| 인증 | 권한만 필요 | OAuth 2.0 필요 |
| 지원 캘린더 | 모든 기기 캘린더 | Google만 |
| 오프라인 | 완전 지원 | 제한적 |
| 동기화 | 기기가 처리 | 앱에서 구현 |
| 구현 복잡도 | 낮음 | 높음 |

### 5.3 주요 구현 사항

- ContentResolver를 통한 CRUD 작업
- Cursor 기반 데이터 조회 → Flow로 래핑
- ContentObserver로 변경 감지
- 반복 이벤트 처리 (RRULE 파싱)
- 예외 인스턴스 (EXDATE) 처리
- 타임존 처리

---

## 6. 수익 모델

### 6.1 구독 플랜

| 플랜 | 무료 | 프리미엄 |
|------|------|----------|
| 교번 관리 | 기본 기능 | 전체 기능 |
| 기기 캘린더 | 조회만 | CRUD 전체 |
| 근무표 다운로드 | 지원 | 지원 |
| 근무표 편집 | 지원 | 지원 |
| 근무표 내보내기 | 지원 | 지원 |
| 근무표 가져오기 | 지원 | 지원 |
| 동료 수 | 3명 제한 | 무제한 |
| 개인 백업/복원 | 제한적 | 전체 기능 |
| Wear OS | 미지원 | 지원 |
| 광고 | 있음 | 없음 |

> ※ 근무표 관련 기능 (다운로드/편집/내보내기/가져오기)은 무료로 제공하여 커뮤니티 기반 근무표 공유를 활성화합니다.

---

## 7. 개발 마일스톤

| Phase | 목표 | 주요 작업 |
|-------|------|----------|
| Phase 1 | 프로젝트 셋업 | Compose 프로젝트 구조, Koin 설정, Room 스키마 |
| Phase 2 | 핵심 기능 | 교번 관리, 캘린더 뷰, 기본 UI |
| Phase 3 | 근무표 시스템 | Supabase 연동, 편집, 내보내기/가져오기 |
| Phase 4 | 캘린더 연동 | CalendarProvider CRUD 구현 |
| Phase 5 | Premium 기능 | 백업/복원, Wear OS, 구독 시스템 |
| Phase 6 | 출시 준비 | 테스트, 마이그레이션 가이드, 스토어 등록 |

---

## 8. 기술적 고려사항

### 8.1 Room 마이그레이션 전략

기존 Realm 데이터를 Room으로 마이그레이션하는 일회성 마이그레이션 로직 구현이 필요합니다. 사용자가 앱 업데이트 시 데이터 손실 없이 전환할 수 있도록 해야 합니다.

### 8.2 백업/복원 설계

- Room 테이블별 JSON/CSV export
- 선택적 테이블 복원 기능
- 버전 호환성 체크

### 8.3 근무표 파일 시스템

- 파일 확장자: `.diacal` (application/json MIME type)
- Kotlinx Serialization을 이용한 JSON 직렬화
- 파일 버전 관리로 하위 호환성 보장
- 데이터 무결성 체크 (checksum)
- Intent Filter로 `.diacal` 파일 앱 연결
- FileProvider를 통한 안전한 파일 공유

### 8.4 CalendarProvider 연동

- 런타임 권한 요청 (READ_CALENDAR, WRITE_CALENDAR)
- ContentResolver로 CRUD 작업
- Repository 패턴으로 추상화
- Cursor → Flow 변환
- ContentObserver로 실시간 변경 감지
- RRULE 파싱 라이브러리 활용 (lib-recur 등)
- 타임존 처리 주의 (UTC ↔ Local)

### 8.5 Supabase 연동

- Retrofit을 통한 REST API 호출
- API Key 보안 관리 (BuildConfig 또는 local.properties)
- 서버 장애 시 P2P 공유로 Fallback
- 에러 핸들링 및 사용자 피드백

### 8.6 최소 SDK

Android 8.0 (API 26) 이상 - Compose 및 최신 라이브러리 호환성

---

## 9. 참고 사항

- 기존 앱 사용자: 약 1,000명 활성 사용자
- 연간 구독자: 약 100명
- 기존 Supabase 인프라 활용
- 기존 코드베이스 참조 가능
- P2P 공유로 서버 의존성 감소 및 커뮤니티 활성화
- CalendarProvider로 OAuth 없이 다양한 캘린더 지원

## 10. supabase 데이터

private const val SUPABASE_BASE_URL = "https://srsyxsddjbojnwvjoera.supabase.co/rest/v1/"

private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNyc3l4c2RkamJvam53dmpvZXJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTg3NjY4NTEsImV4cCI6MjA3NDM0Mjg1MX0.wmTrMzDqBs10qL_wBeJJQTjQMuCuvfAmY6_eW2brqT8"

private const val SUPABASE_HOST = "srsyxsddjbojnwvjoera.supabase.co" // ⭐️ 헤더 구분을 위한 호스트



## 11. supabase 공지등록
Supabase 설정 (수동 작업 필요)
Supabase 대시보드에서 아래 두 가지를 설정해야 합니다:

1. Edge Function 생성 → 대시보드 → Edge Functions → send-document-notice

환경변수 FCM_SERVER_KEY 설정 (Firebase 콘솔 → 프로젝트 설정 → 클라우드 메시징 → 서버 키)
2. DB 트리거 생성 → 대시보드 → SQL Editor에서 실행:


CREATE EXTENSION IF NOT EXISTS pg_net;

CREATE OR REPLACE FUNCTION notify_new_document()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM net.http_post(
    url := 'https://<메인_프로젝트_ID>.supabase.co/functions/v1/send-document-notice',
    body := json_build_object('title', NEW.title, 'description', NEW.description)::text,
    headers := '{"Authorization": "Bearer <service_role_key>", "Content-Type": "application/json"}'::jsonb
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER on_document_insert
AFTER INSERT ON public.documents
FOR EACH ROW EXECUTE FUNCTION notify_new_document();
또한 documents 테이블의 RLS 정책에서 anon 역할의 SELECT를 허용해야 앱에서 목록 조회가 됩니다.




## 11. supabase 공지등록

Supabase Edge Function 생성
1. 대시보드 접속
Supabase 대시보드 → 좌측 메뉴 Edge Functions → New Function → 이름: send-document-notice

2. 함수 코드

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

serve(async (req) => {
  try {
    const { title, description } = await req.json()
    const FCM_SERVER_KEY = Deno.env.get("FCM_SERVER_KEY")!

    const response = await fetch("https://fcm.googleapis.com/fcm/send", {
      method: "POST",
      headers: {
        "Authorization": `key=${FCM_SERVER_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        to: "/topics/documents",
        notification: {
          title: title,
          body: description ?? ""
        },
        data: {
          type: "document"
        }
      })
    })

    const result = await response.json()
    return new Response(JSON.stringify({ ok: true, result }), {
      headers: { "Content-Type": "application/json" }
    })
  } catch (e) {
    return new Response(JSON.stringify({ ok: false, error: String(e) }), {
      status: 500,
      headers: { "Content-Type": "application/json" }
    })
  }
})
3. 환경변수 설정 (중요)
Edge Functions → send-document-notice → Secrets 탭에서 추가:

Key	Value
FCM_SERVER_KEY	Firebase 서버 키
Firebase 서버 키 찾는 방법:

Firebase 콘솔 → 프로젝트 설정 → 클라우드 메시징 탭 → 서버 키 (Legacy 항목)

4. DB 트리거 (SQL Editor에서 실행)
Supabase 대시보드 → SQL Editor → New Query:


-- pg_net 확장 활성화 (이미 있으면 생략)
CREATE EXTENSION IF NOT EXISTS pg_net;

-- Edge Function 호출 함수
CREATE OR REPLACE FUNCTION notify_new_document()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM net.http_post(
    url     := 'https://<프로젝트_ID>.supabase.co/functions/v1/send-document-notice',
    body    := json_build_object('title', NEW.title, 'description', NEW.description)::text,
    headers := json_build_object(
                 'Authorization', 'Bearer <service_role_key>',
                 'Content-Type', 'application/json'
               )::jsonb
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 트리거 등록
CREATE TRIGGER on_document_insert
AFTER INSERT ON public.documents
FOR EACH ROW EXECUTE FUNCTION notify_new_document();
<프로젝트_ID> 와 <service_role_key> 는:

Supabase 대시보드 → Project Settings → API 에서 확인

Project ID → URL에서 확인
service_role secret key → API Keys 섹션