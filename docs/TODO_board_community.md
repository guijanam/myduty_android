# 게시판/커뮤니티 기능 복원 TODO

> 작성일: 2025-05-27  
> 현재 상태: 공지 기능 구현을 우선 진행하면서 게시판 탭이 임시로 비활성화됨

---

## 현재 상태

`NotificationsScreen.kt`의 탭이 **공지 탭만 활성화**된 상태.  
게시판(BoardListScreen)과 내 글 관리(MyPostsScreen)는 주석 처리되어 있음.

```
현재: [공지]
목표: [게시판] [공지] [내 글 관리]
```

---

## 복원 방법

### `presentation/notifications/NotificationsScreen.kt` 수정

주석 처리된 탭과 화면을 아래와 같이 복원한다.

```kotlin
@Composable
fun NotificationsScreen(
    onNavigateToPostDetail: (Long) -> Unit = {},
    onNavigateToPostWrite: (String?) -> Unit = {},
    onNavigateToPostEdit: (Long) -> Unit = {},
    onNavigateToDocumentDetail: (String) -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onNavigateToBlockedUsers: () -> Unit = {},
    boardRefreshTrigger: Int = 0,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("게시판") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("공지") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("내 글 관리") }
            )
        }

        when (selectedTab) {
            0 -> BoardListScreen(
                onNavigateToPostDetail = onNavigateToPostDetail,
                onNavigateToPostWrite = onNavigateToPostWrite,
                onNavigateToAuth = onNavigateToAuth,
                onNavigateToBlockedUsers = onNavigateToBlockedUsers,
                boardRefreshTrigger = boardRefreshTrigger,
                modifier = Modifier.weight(1f)
            )
            1 -> DocumentListScreen(
                onNavigateToDetail = onNavigateToDocumentDetail,
                modifier = Modifier.weight(1f)
            )
            2 -> MyPostsScreen(
                onNavigateToPostDetail = onNavigateToPostDetail,
                onNavigateToPostEdit = onNavigateToPostEdit,
                onNavigateToAuth = onNavigateToAuth,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

---

## MainScreen BottomNav 확인

게시판 탭을 복원할 때 `MainScreen.kt`의 BottomNav에서  
`Route.Notifications` 탭 항목도 함께 활성화 여부 확인 필요.

현재 `NavigationRoot.kt`에서 `Route.Notifications` entry는 그대로 존재하므로  
NotificationsScreen 내부 탭만 복원하면 됨.

---

## 관련 파일

| 파일 | 설명 |
|------|------|
| `presentation/notifications/NotificationsScreen.kt` | 탭 복원 대상 |
| `presentation/board/BoardListScreen.kt` | 게시판 목록 화면 (이미 완성) |
| `presentation/board/MyPostsScreen.kt` | 내 글 관리 화면 (이미 완성) |
| `presentation/notifications/DocumentListScreen.kt` | 공지 목록 화면 (신규 구현됨) |
| `core/routing/NavigationRoot.kt` | Notifications entry (수정 불필요) |

---

## 선행 조건

게시판 기능은 **Board Supabase 프로젝트** (`BoardSupabaseConfig`) 및  
**로그인(Auth)** 기능과 연동되어 있으므로, 해당 기능이 준비된 시점에 복원.
