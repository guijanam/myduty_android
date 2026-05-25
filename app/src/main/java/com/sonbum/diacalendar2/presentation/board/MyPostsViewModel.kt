package com.sonbum.diacalendar2.presentation.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.Post
import com.sonbum.diacalendar2.domain.repository.AuthRepository
import com.sonbum.diacalendar2.domain.repository.BoardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPostsState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MyPostsViewModel(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MyPostsState())
    val state = _state.asStateFlow()

    val isLoggedIn = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadMyPosts()
    }

    fun loadMyPosts() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            boardRepository.getMyPosts()
                .onSuccess { posts ->
                    _state.update { it.copy(posts = posts, isLoading = false) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun deletePost(postId: Long) {
        viewModelScope.launch {
            boardRepository.deletePost(postId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(posts = state.posts.filter { it.id != postId })
                    }
                }
        }
    }
}
