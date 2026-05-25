package com.sonbum.diacalendar2.presentation.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.BoardCategory
import com.sonbum.diacalendar2.domain.repository.BoardRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostEditState(
    val postId: Long = 0,
    val categories: List<BoardCategory> = emptyList(),
    val category: BoardCategory = BoardCategory.FREE,
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val isLoadingPost: Boolean = true,
    val error: String? = null
)

sealed interface PostEditEvent {
    data object PostUpdated : PostEditEvent
}

class PostEditViewModel(
    private val boardRepository: BoardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostEditState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<PostEditEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            boardRepository.getCategories().onSuccess { categories ->
                _state.update { it.copy(categories = categories) }
            }
        }
    }

    fun loadPost(postId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingPost = true, postId = postId) }
            boardRepository.getPostDetail(postId)
                .onSuccess { post ->
                    _state.update {
                        it.copy(
                            isLoadingPost = false,
                            category = post.category,
                            title = post.title,
                            content = post.content
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isLoadingPost = false, error = error.message)
                    }
                }
        }
    }

    fun updateCategory(category: BoardCategory) {
        _state.update { it.copy(category = category) }
    }

    fun updateTitle(title: String) {
        _state.update { it.copy(title = title, error = null) }
    }

    fun updateContent(content: String) {
        _state.update { it.copy(content = content, error = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.title.isBlank()) {
            _state.update { it.copy(error = "제목을 입력해주세요") }
            return
        }
        if (current.content.isBlank()) {
            _state.update { it.copy(error = "내용을 입력해주세요") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            boardRepository.updatePost(
                postId = current.postId,
                category = current.category,
                title = current.title,
                content = current.content
            ).onSuccess {
                _state.update { it.copy(isLoading = false) }
                _event.emit(PostEditEvent.PostUpdated)
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, error = error.message ?: "게시글 수정 실패")
                }
            }
        }
    }
}
