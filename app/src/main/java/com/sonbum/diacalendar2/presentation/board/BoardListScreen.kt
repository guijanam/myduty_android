package com.sonbum.diacalendar2.presentation.board

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.domain.model.Announcement
import com.sonbum.diacalendar2.domain.model.Post
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardListScreen(
    onNavigateToPostDetail: (Long) -> Unit,
    onNavigateToPostWrite: (String?) -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit = {},
    boardRefreshTrigger: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: BoardListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val nickname by viewModel.currentNickname.collectAsStateWithLifecycle()
    val guidelinesAgreed by viewModel.guidelinesAgreed.collectAsStateWithLifecycle()
    var selectedAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // 글 작성/수정 후 돌아올 때 새로고침 + 스크롤 최상단
    LaunchedEffect(boardRefreshTrigger) {
        if (boardRefreshTrigger > 0) {
            viewModel.loadPostsAndScrollToTop()
        }
    }

    // ViewModel에서 데이터 로드 완료 후 스크롤 이벤트 수신
    LaunchedEffect(Unit) {
        viewModel.scrollToTopEvent.collect {
            listState.animateScrollToItem(0)
        }
    }

    // 포그라운드 복귀 시 데이터 재로드
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onScreenResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 에러 발생 시 Snackbar 표시 (게시글이 이미 있는 상태에서 새로고침 실패 등)
    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        Log.e("BoardError", "UI 화면 에러 발생: $error")
        if (state.posts.isNotEmpty()) {
            val isSessionError = error.contains("세션") || error.contains("로그인")
            val result = snackbarHostState.showSnackbar(
                message = error,
                actionLabel = if (isSessionError) "로그인" else "다시 시도"
            )
            if (result == SnackbarResult.ActionPerformed) {
                if (isSessionError) onNavigateToAuth() else viewModel.loadPosts()
            }
        }
    }

    // 공지사항 상세 다이얼로그
    selectedAnnouncement?.let { announcement ->
        AnnouncementDetailDialog(
            announcement = announcement,
            onDismiss = { selectedAnnouncement = null }
        )
    }

    // 커뮤니티 가이드라인 동의 다이얼로그
    if (isLoggedIn && !guidelinesAgreed) {
        CommunityGuidelinesDialog(
            onAgree = { viewModel.agreeToGuidelines() },
            onDismiss = { /* 동의하지 않으면 닫기만 */ }
        )
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 로그인 상태 바
            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = nickname ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(onClick = onNavigateToBlockedUsers) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = "차단 관리",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(
	                        Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "로그아웃",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 비로그인 배너
            if (!isLoggedIn) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "글을 쓰려면 로그인이 필요합니다",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onNavigateToAuth) {
                            Text("로그인")
                        }
                    }
                }
            }

            // 정지 배너
            val banStatus = state.banStatus
            if (banStatus?.isBanned == true) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "활동이 정지되었습니다",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (banStatus.reason.isNotBlank()) {
                            Text(
                                text = "사유: ${banStatus.reason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        val until = banStatus.bannedUntil
                        Text(
                            text = if (until != null) "해제일: ${until.take(10)}" else "영구 정지",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // 검색바
            if (state.isSearchMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("제목 또는 내용 검색") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    viewModel.updateSearchQuery("")
                                    viewModel.search()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "지우기", modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { viewModel.toggleSearchMode() }) {
                        Text("취소")
                    }
                }
            }

            // 카테고리 필터 + 검색 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("전체") }
                        )
                    }
                    items(state.categories, key = { it.code }) { category ->
                        FilterChip(
                            selected = state.selectedCategory?.code == category.code,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category.displayName) }
                        )
                    }
                }
                if (!state.isSearchMode) {
                    IconButton(onClick = { viewModel.toggleSearchMode() }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "검색",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 게시글 목록
            when {
                state.isLoading && state.posts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.posts.isEmpty() -> {
                    val error = state.error ?: "오류가 발생했습니다"
                    val isSessionError = error.contains("세션") || error.contains("로그인")
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (isSessionError) {
                                TextButton(onClick = onNavigateToAuth) {
                                    Text("다시 로그인")
                                }
                            } else {
                                TextButton(onClick = { viewModel.loadPosts() }) {
                                    Text("다시 시도")
                                }
                            }
                        }
                    }
                }
                state.posts.isEmpty() && state.announcements.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "게시글이 없거나 로그인시 보입니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            if (state.announcements.isNotEmpty()) {
                                items(state.announcements, key = { "ann_${it.id}" }) { announcement ->
                                    AnnouncementCard(
                                        announcement = announcement,
                                        onClick = { selectedAnnouncement = announcement }
                                    )
                                }
                            }
                            items(state.posts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    onClick = { onNavigateToPostDetail(post.id) }
                                )
                            }
                            if (state.hasMore && !state.isLoadingMore) {
                                item {
                                    LaunchedEffect(state.posts.size) {
                                        viewModel.loadMore()
                                    }
                                }
                            }
                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB - 바텀 네비 위에 위치 (정지 중이면 숨김)
        if (isLoggedIn && state.banStatus?.isBanned != true) {
            FloatingActionButton(
                onClick = { onNavigateToPostWrite(state.selectedCategory?.code) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "글 작성")
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}



@Composable
private fun PostCard(
    post: Post,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
	    onClick = onClick, // Card 자체의 onClick 사용 (Ripple 효과가 카드 전체에 예쁘게 퍼짐)
	    modifier = modifier
		    .fillMaxWidth()
		    .padding(horizontal = 16.dp, vertical = 6.dp),
	    shape = RoundedCornerShape(16.dp), // 조금 더 둥근 모서리로 부드러운 인상
	    colors = CardDefaults.cardColors(
		    containerColor = MaterialTheme.colorScheme.surface,
	    ),
	    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) // 미세한 테두리
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "[${post.category.displayName}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.authorNickname,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = formatRelativeTime(post.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (post.viewCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${post.viewCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    if (post.likeCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ThumbUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${post.likeCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    if (post.commentCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${post.commentCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(
    announcement: Announcement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (announcement.isPinned) "[공지]" else "[알림]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = announcement.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatRelativeTime(announcement.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun AnnouncementDetailDialog(
    announcement: Announcement,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (announcement.isPinned) "[공지]" else "[알림]",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = announcement.content,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formatRelativeTime(announcement.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

internal fun formatRelativeTime(isoDateTime: String): String {
    return try {
        val instant = java.time.Instant.parse(isoDateTime)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)

        when {
            duration.toMinutes() < 1 -> "방금 전"
            duration.toHours() < 1 -> "${duration.toMinutes()}분 전"
            duration.toDays() < 1 -> "${duration.toHours()}시간 전"
            duration.toDays() < 30 -> "${duration.toDays()}일 전"
            else -> {
                val dateTime = java.time.LocalDateTime.ofInstant(
                    instant, java.time.ZoneId.systemDefault()
                )
                "${dateTime.monthValue}/${dateTime.dayOfMonth}"
            }
        }
    } catch (_: Exception) {
        isoDateTime.take(10)
    }
}
