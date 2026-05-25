package com.sonbum.diacalendar2.presentation.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.Comment
import com.sonbum.diacalendar2.domain.model.Post
import com.sonbum.diacalendar2.domain.repository.AuthRepository
import com.sonbum.diacalendar2.domain.repository.BoardRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostDetailState(
    val post: Post? = null,
    val comments: List<Comment> = emptyList(),
    val commentText: String = "",
    val replyTarget: Comment? = null,
    val isLiked: Boolean = false,
    val isLoading: Boolean = false,
    val isSendingComment: Boolean = false,
    val error: String? = null
)

sealed interface PostDetailEvent {
    data object PostDeleted : PostDetailEvent
    data class ShowSnackbar(val message: String) : PostDetailEvent
    data object UserBlocked : PostDetailEvent
}

class PostDetailViewModel(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostDetailState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<PostDetailEvent>()
    val event = _event.asSharedFlow()

    val currentUserId = authRepository.currentUserId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun loadPost(postId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val postResult = boardRepository.getPostDetail(postId)
            postResult
                .onSuccess { post ->
                    _state.update { it.copy(post = post, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }

            val isAnonymous = postResult.getOrNull()?.category?.isAnonymous == true
            boardRepository.getComments(postId, isAnonymous)
                .onSuccess { comments ->
                    _state.update { it.copy(comments = comments) }
                }

            // 조회수 증가
            boardRepository.incrementViewCount(postId)

            // 좋아요 상태 확인
            boardRepository.isPostLiked(postId)
                .onSuccess { liked ->
                    _state.update { it.copy(isLiked = liked) }
                }
        }
    }

    fun updateCommentText(text: String) {
        _state.update { it.copy(commentText = text) }
    }

    fun setReplyTarget(comment: Comment?) {
        _state.update { it.copy(replyTarget = comment) }
    }

    fun sendComment() {
        val current = _state.value
        val post = current.post ?: return
        val text = current.commentText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isSendingComment = true) }

            // 정지 상태 확인
            boardRepository.checkBanStatus().onSuccess { ban ->
                if (ban.isBanned) {
                    _state.update { it.copy(isSendingComment = false) }
                    _event.emit(PostDetailEvent.ShowSnackbar("활동이 정지된 상태입니다"))
                    return@launch
                }
            }

            boardRepository.createComment(
                postId = post.id,
                parentId = current.replyTarget?.id,
                content = text
            ).onSuccess {
                _state.update {
                    it.copy(commentText = "", replyTarget = null, isSendingComment = false)
                }
                // 댓글 목록 다시 로드
                val isAnonymous = post.category.isAnonymous
                boardRepository.getComments(post.id, isAnonymous)
                    .onSuccess { comments ->
                        _state.update { it.copy(comments = comments) }
                    }
            }.onFailure {
                _state.update { it.copy(isSendingComment = false) }
            }
        }
    }

    fun toggleLike() {
        val post = _state.value.post ?: return
        val wasLiked = _state.value.isLiked
        // 낙관적 UI 업데이트
        _state.update {
            it.copy(
                isLiked = !wasLiked,
                post = it.post?.copy(likeCount = it.post.likeCount + if (wasLiked) -1 else 1)
            )
        }
        viewModelScope.launch {
            boardRepository.toggleLike(post.id)
                .onFailure {
                    // 실패 시 롤백
                    _state.update {
                        it.copy(
                            isLiked = wasLiked,
                            post = it.post?.copy(likeCount = it.post.likeCount + if (wasLiked) 1 else -1)
                        )
                    }
                    _event.emit(PostDetailEvent.ShowSnackbar("좋아요 처리에 실패했습니다"))
                }
        }
    }

    fun deletePost() {
        val post = _state.value.post ?: return
        viewModelScope.launch {
            boardRepository.deletePost(post.id)
                .onSuccess {
                    _event.emit(PostDetailEvent.PostDeleted)
                }
        }
    }

    fun deleteComment(commentId: Long) {
        val post = _state.value.post ?: return
        viewModelScope.launch {
            boardRepository.deleteComment(commentId)
                .onSuccess {
                    boardRepository.getComments(post.id, post.category.isAnonymous)
                        .onSuccess { comments ->
                            _state.update { it.copy(comments = comments) }
                        }
                }
        }
    }

    fun reportPost(reason: String) {
        val post = _state.value.post ?: return
        viewModelScope.launch {
            boardRepository.reportPost(post.id, post.authorId, reason)
                .onSuccess {
                    _event.emit(PostDetailEvent.ShowSnackbar("신고가 접수되었습니다"))
                }
                .onFailure { error ->
                    _event.emit(PostDetailEvent.ShowSnackbar(error.message ?: "신고에 실패했습니다"))
                }
        }
    }

    fun reportComment(commentId: Long, authorId: String, reason: String) {
        viewModelScope.launch {
            boardRepository.reportComment(commentId, authorId, reason)
                .onSuccess {
                    _event.emit(PostDetailEvent.ShowSnackbar("신고가 접수되었습니다"))
                }
                .onFailure { error ->
                    _event.emit(PostDetailEvent.ShowSnackbar(error.message ?: "신고에 실패했습니다"))
                }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            boardRepository.blockUser(userId)
                .onSuccess {
                    _event.emit(PostDetailEvent.ShowSnackbar("사용자를 차단했습니다"))
                    _event.emit(PostDetailEvent.UserBlocked)
                }
                .onFailure { error ->
                    _event.emit(PostDetailEvent.ShowSnackbar(error.message ?: "차단에 실패했습니다"))
                }
        }
    }
}
