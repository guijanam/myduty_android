package com.sonbum.diacalendar2.presentation.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonbum.diacalendar2.domain.model.Comment
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PostDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var showReportPostDialog by remember { mutableStateOf(false) }
    var showReportCommentDialog by remember { mutableStateOf<Pair<Long, String>?>(null) } // commentId, authorId
    var showBlockConfirmDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // userId, nickname
    var showDeleteCommentDialog by remember { mutableStateOf<Long?>(null) } // commentId
    var showDeletePostDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                PostDetailEvent.PostDeleted -> onBack()
                is PostDetailEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                PostDetailEvent.UserBlocked -> onBack()
            }
        }
    }

    // 신고 다이얼로그
    if (showReportPostDialog) {
        ReportDialog(
            onDismiss = { showReportPostDialog = false },
            onConfirm = { reason ->
                viewModel.reportPost(reason)
                showReportPostDialog = false
            }
        )
    }

    showReportCommentDialog?.let { (commentId, authorId) ->
        ReportDialog(
            onDismiss = { showReportCommentDialog = null },
            onConfirm = { reason ->
                viewModel.reportComment(commentId, authorId, reason)
                showReportCommentDialog = null
            }
        )
    }

    // 차단 확인 다이얼로그
    showBlockConfirmDialog?.let { (userId, nickname) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = null },
            title = { Text("사용자 차단") },
            text = { Text("${nickname}님을 차단하시겠습니까?\n차단된 사용자의 게시글과 댓글은 더 이상 표시되지 않습니다.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.blockUser(userId)
                    showBlockConfirmDialog = null
                }) {
                    Text("차단")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = null }) {
                    Text("취소")
                }
            }
        )
    }

    // 게시글 삭제 확인 다이얼로그
    if (showDeletePostDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeletePostDialog = false },
            title = { Text("게시글 삭제") },
            text = { Text("게시글을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePost()
                    showDeletePostDialog = false
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePostDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // 댓글 삭제 확인 다이얼로그
    showDeleteCommentDialog?.let { commentId ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteCommentDialog = null },
            title = { Text("댓글 삭제") },
            text = { Text("댓글을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteComment(commentId)
                    showDeleteCommentDialog = null
                }) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCommentDialog = null }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.post?.category?.displayName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (isLoggedIn && state.post != null) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "메뉴")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (state.post?.authorId == currentUserId) {
                                    DropdownMenuItem(
                                        text = { Text("삭제") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            showDeletePostDialog = true
                                        }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = { Text("신고하기") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Flag, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            showReportPostDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("이 사용자 차단") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Block, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            val post = state.post!!
                                            showBlockConfirmDialog = post.authorId to post.authorNickname
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isLoggedIn) {
                CommentInputBar(
                    commentText = state.commentText,
                    replyTarget = state.replyTarget,
                    isSending = state.isSendingComment,
                    onTextChange = viewModel::updateCommentText,
                    onSend = viewModel::sendComment,
                    onCancelReply = { viewModel.setReplyTarget(null) }
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.post == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("게시글을 찾을 수 없습니다")
                }
            }
            else -> {
                val post = state.post!!
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // 게시글 본문
                    item {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = post.authorNickname,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = formatRelativeTime(post.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            LinkableText(
                                text = post.content,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // 조회수 & 좋아요
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${post.viewCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (isLoggedIn) viewModel.toggleLike() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (state.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                            contentDescription = "좋아요",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (state.isLiked) MaterialTheme.colorScheme.primary
                                                   else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${post.likeCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (state.isLiked) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }

                    // 댓글 헤더
                    item {
                        Text(
                            text = "댓글 ${state.comments.sumOf { 1 + it.replies.size }}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // 댓글 목록
                    items(state.comments, key = { it.id }) { comment ->
                        CommentItem(
                            comment = comment,
                            currentUserId = currentUserId,
                            isLoggedIn = isLoggedIn,
                            onReply = { viewModel.setReplyTarget(comment) },
                            onDelete = { showDeleteCommentDialog = comment.id },
                            onReport = { showReportCommentDialog = comment.id to comment.authorId },
                            onBlock = { showBlockConfirmDialog = comment.authorId to comment.authorNickname }
                        )
                        // 대댓글
                        comment.replies.forEach { reply ->
                            CommentItem(
                                comment = reply,
                                currentUserId = currentUserId,
                                isLoggedIn = isLoggedIn,
                                isReply = true,
                                onReply = {},
                                onDelete = { showDeleteCommentDialog = reply.id },
                                onReport = { showReportCommentDialog = reply.id to reply.authorId },
                                onBlock = { showBlockConfirmDialog = reply.authorId to reply.authorNickname }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    isLoggedIn: Boolean,
    isReply: Boolean = false,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit = {},
    onBlock: () -> Unit = {}
) {
    var showCommentMenu by remember { mutableStateOf(false) }
    val startPadding = if (isReply) 40.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 16.dp, top = 8.dp, bottom = 8.dp)
    ) {
        if (comment.isDeleted) {
            Text(
                text = "삭제된 메시지입니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textDecoration = TextDecoration.LineThrough
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.authorNickname,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatRelativeTime(comment.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row {
                    if (isLoggedIn && !isReply) {
                        TextButton(onClick = onReply) {
                            Icon(
                                Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "답글",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("답글", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (comment.authorId == currentUserId) {
                        TextButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "삭제",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (isLoggedIn) {
                        Box {
                            IconButton(
                                onClick = { showCommentMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "더보기",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showCommentMenu,
                                onDismissRequest = { showCommentMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("신고하기", style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        showCommentMenu = false
                                        onReport()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("사용자 차단", style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    onClick = {
                                        showCommentMenu = false
                                        onBlock()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinkableText(
                text = comment.content,
                style = MaterialTheme.typography.bodySmall
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CommentInputBar(
    commentText: String,
    replyTarget: Comment?,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancelReply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        if (replyTarget != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${replyTarget.authorNickname}에게 답글",
                        style = MaterialTheme.typography.labelSmall
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "답글 취소",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = onTextChange,
                placeholder = { Text("댓글을 입력하세요") },
                modifier = Modifier.weight(1f),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = commentText.isNotBlank() && !isSending
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "보내기")
                }
            }
        }
    }
}

private val urlPattern = Regex(
    """https?://[^\s<>\[\](){}"']+""",
    RegexOption.IGNORE_CASE
)

@Composable
private fun LinkableText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val textColor = style.color.takeIf { it != androidx.compose.ui.graphics.Color.Unspecified }
        ?: MaterialTheme.colorScheme.onSurface

    val annotatedString = remember(text, linkColor, textColor) {
        buildAnnotatedString {
            var lastIndex = 0
            urlPattern.findAll(text).forEach { matchResult ->
                val start = matchResult.range.first
                val end = matchResult.range.last + 1
                if (start > lastIndex) {
                    withStyle(SpanStyle(color = textColor)) {
                        append(text.substring(lastIndex, start))
                    }
                }
                pushStringAnnotation(tag = "URL", annotation = matchResult.value)
                withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                    append(matchResult.value)
                }
                pop()
                lastIndex = end
            }
            if (lastIndex < text.length) {
                withStyle(SpanStyle(color = textColor)) {
                    append(text.substring(lastIndex))
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    ClickableText(
        text = annotatedString,
        style = style,
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    try {
                        uriHandler.openUri(annotation.item)
                    } catch (_: Exception) { }
                }
        }
    )
}
