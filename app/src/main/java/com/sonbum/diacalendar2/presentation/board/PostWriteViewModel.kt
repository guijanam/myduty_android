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

data class PostWriteState(
    val categories: List<BoardCategory> = emptyList(),
    val category: BoardCategory = BoardCategory.FREE,
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface PostWriteEvent {
    data object PostCreated : PostWriteEvent
}

class PostWriteViewModel(
    private val boardRepository: BoardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PostWriteState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<PostWriteEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            boardRepository.getCategories().onSuccess { categories ->
                _state.update {
                    val current = it.category
                    val resolved = categories.find { c -> c.code == current.code } ?: categories.firstOrNull() ?: current
                    it.copy(categories = categories, category = resolved)
                }
            }
        }
    }

    fun setInitialCategory(categoryCode: String?) {
        categoryCode?.let { code ->
            val categories = _state.value.categories
            val found = BoardCategory.fromCode(code, categories)
            _state.update { it.copy(category = found) }
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

            // 정지 상태 확인
            boardRepository.checkBanStatus().onSuccess { ban ->
                if (ban.isBanned) {
                    _state.update { it.copy(isLoading = false, error = "활동이 정지된 상태입니다: ${ban.reason}") }
                    return@launch
                }
            }

            boardRepository.createPost(
                category = current.category,
                title = current.title,
                content = current.content
            ).onSuccess {
                _state.update { it.copy(isLoading = false) }
                _event.emit(PostWriteEvent.PostCreated)
            }.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, error = error.message ?: "게시글 작성 실패")
                }
            }
        }
    }
}
