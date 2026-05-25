package com.sonbum.diacalendar2.presentation.board

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.datastore.AuthPreferences
import com.sonbum.diacalendar2.domain.model.Announcement
import com.sonbum.diacalendar2.domain.model.BanStatus
import com.sonbum.diacalendar2.domain.model.BoardCategory
import com.sonbum.diacalendar2.domain.model.Post
import com.sonbum.diacalendar2.domain.repository.AuthRepository
import com.sonbum.diacalendar2.domain.repository.BoardRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardListState(
    val posts: List<Post> = emptyList(),
    val announcements: List<Announcement> = emptyList(),
    val categories: List<BoardCategory> = emptyList(),
    val banStatus: BanStatus? = null,
    val selectedCategory: BoardCategory? = null,
    val searchQuery: String = "",
    val isSearchMode: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

class BoardListViewModel(
    private val boardRepository: BoardRepository,
    private val authRepository: AuthRepository,
    private val authPreferences: AuthPreferences
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
        private const val TAG = "BoardVM"
    }

    private val _state = MutableStateFlow(BoardListState())
    val state = _state.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val scrollToTopEvent = _scrollToTopEvent.asSharedFlow()

    private var blockedUserIds: Set<String> = emptySet()
    private var loadPostsJob: kotlinx.coroutines.Job? = null
    private var resumeJob: kotlinx.coroutines.Job? = null
    private var initLoadJob: kotlinx.coroutines.Job? = null
    private var shouldScrollToTop = false
    private var isInitialResume = true

    val isLoggedIn = authRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentNickname = authRepository.currentNickname
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val guidelinesAgreed = authPreferences.guidelinesAgreed
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun agreeToGuidelines() {
        viewModelScope.launch {
            authPreferences.saveGuidelinesAgreed()
        }
    }

    init {
        // 로그인 상태 변화 감시
        viewModelScope.launch {
            authRepository.isLoggedIn.distinctUntilChanged().collect { loggedIn ->
                Log.d(TAG, "init: isLoggedIn changed → $loggedIn")
                initLoadJob?.cancel()
                if (loggedIn) {
                    initLoadJob = launch {
                        // 토큰 갱신 먼저 시도 (탭 전환으로 ViewModel 재생성 시 만료 가능)
                        authRepository.refreshAccessToken()
                        launch { loadCategories() }
                        launch { loadBlockedUsers() }
                        launch { checkBanStatus() }
                        launch { loadAnnouncements() }
                        loadPosts()
                    }
                } else {
                    Log.d(TAG, "init: 로그아웃 상태 → 데이터 초기화")
                    blockedUserIds = emptySet()
                    _state.update { it.copy(posts = emptyList(), announcements = emptyList(), categories = emptyList(), banStatus = null, error = null, hasMore = true) }
                }
            }
        }
    }

    private suspend fun loadCategories() {
        Log.d(TAG, "loadCategories 시작")
        boardRepository.getCategories()
            .onSuccess { categories ->
                Log.d(TAG, "loadCategories 성공: ${categories.size}개")
                val savedCode = authPreferences.selectedCategory.first()
                val selected = savedCode?.let { code ->
                    categories.find { it.code == code }
                }
                _state.update { it.copy(categories = categories, selectedCategory = selected) }
            }
            .onFailure { error ->
                Log.e(TAG, "loadCategories 실패", error)
            }
    }

    private suspend fun loadBlockedUsers() {
        Log.d(TAG, "loadBlockedUsers 시작")
        boardRepository.getBlockedUserIds()
            .onSuccess { ids ->
                Log.d(TAG, "loadBlockedUsers 성공: ${ids.size}명")
                blockedUserIds = ids
            }
            .onFailure { error ->
                Log.e(TAG, "loadBlockedUsers 실패", error)
            }
    }

    private suspend fun loadAnnouncements() {
        Log.d(TAG, "loadAnnouncements 시작")
        boardRepository.getAnnouncements()
            .onSuccess { announcements ->
                Log.d(TAG, "loadAnnouncements 성공: ${announcements.size}개")
                _state.update { it.copy(announcements = announcements) }
            }
            .onFailure { error ->
                Log.e(TAG, "loadAnnouncements 실패", error)
            }
    }

    private suspend fun checkBanStatus() {
        Log.d(TAG, "checkBanStatus 시작")
        boardRepository.checkBanStatus()
            .onSuccess { banStatus ->
                Log.d(TAG, "checkBanStatus 성공: isBanned=${banStatus?.isBanned}")
                _state.update { it.copy(banStatus = banStatus) }
            }
            .onFailure { error ->
                Log.e(TAG, "checkBanStatus 실패", error)
            }
    }

    fun selectCategory(category: BoardCategory?) {
        _state.update { it.copy(selectedCategory = category) }
        viewModelScope.launch {
            authPreferences.saveSelectedCategory(category?.code)
        }
        loadPosts()
    }

    fun loadPostsAndScrollToTop() {
        shouldScrollToTop = true
        loadPosts()
    }

    fun loadPosts() {
        Log.d(TAG, "loadPosts 시작 (기존 posts=${_state.value.posts.size}개)")
        loadPostsJob?.cancel()
        loadPostsJob = viewModelScope.launch {
            val currentState = _state.value
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val query = currentState.searchQuery.takeIf { it.isNotBlank() }
                boardRepository.getPosts(
                    category = currentState.selectedCategory,
                    searchQuery = query,
                    limit = PAGE_SIZE,
                    offset = 0
                )
                    .onSuccess { posts ->
                        val filtered = posts.filter { it.authorId !in blockedUserIds }
                        Log.d(TAG, "loadPosts 성공: 전체=${posts.size}, 필터후=${filtered.size}")
                        _state.update {
                            it.copy(
                                posts = filtered,
                                isLoading = false,
                                hasMore = posts.size >= PAGE_SIZE
                            )
                        }
                        if (shouldScrollToTop) {
                            shouldScrollToTop = false
                            _scrollToTopEvent.tryEmit(Unit)
                        }
                    }
                    .onFailure { error ->
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        Log.e(TAG, "loadPosts 실패: ${error.message}", error)
                        _state.update {
                            it.copy(isLoading = false, error = error.message)
                        }
                    }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "loadPosts 취소됨")
                _state.update { it.copy(isLoading = false) }
                throw e
            }
        }
    }

    fun loadMore() {
        if (!isLoggedIn.value) return
        val currentState = _state.value
        if (currentState.isLoadingMore || !currentState.hasMore) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            val query = currentState.searchQuery.takeIf { it.isNotBlank() }
            boardRepository.getPosts(
                category = currentState.selectedCategory,
                searchQuery = query,
                limit = PAGE_SIZE,
                offset = currentState.posts.size
            )
                .onSuccess { newPosts ->
                    val filtered = newPosts.filter { it.authorId !in blockedUserIds }
                    _state.update {
                        val existingIds = it.posts.map { p -> p.id }.toSet()
                        val deduplicated = filtered.filter { p -> p.id !in existingIds }
                        it.copy(
                            posts = it.posts + deduplicated,
                            isLoadingMore = false,
                            hasMore = newPosts.size >= PAGE_SIZE
                        )
                    }
                }
                .onFailure {

                    _state.update { it.copy(isLoadingMore = false) }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun search() {
        loadPosts()
    }

    fun toggleSearchMode() {
        val newMode = !_state.value.isSearchMode
        _state.update { it.copy(isSearchMode = newMode, searchQuery = "") }
        if (!newMode) {
            loadPosts()
        }
    }

    fun logout() {
        Log.d(TAG, "logout 호출 → 모든 Job 취소")
        // 진행 중인 모든 API 호출 취소 (resume/init 중 로그아웃 시 경쟁 방지)
        resumeJob?.cancel()
        resumeJob = null
        initLoadJob?.cancel()
        initLoadJob = null
        loadPostsJob?.cancel()
        loadPostsJob = null
        // 즉시 로딩 상태 해제 (UI가 반응할 수 있도록)
        _state.update { it.copy(isLoading = false, isRefreshing = false, isLoadingMore = false) }
        viewModelScope.launch {
            authRepository.signOut()
            Log.d(TAG, "logout 완료 → signOut 성공")
            // isLoggedIn collect에서 posts 비움
        }
    }

    fun onScreenResumed() {
        if (isInitialResume) {
            Log.d(TAG, "onScreenResumed: 최초 resume → 스킵 (init에서 로드)")
            isInitialResume = false
            return // init 블록에서 이미 데이터 로드
        }
        if (!isLoggedIn.value) {
            Log.d(TAG, "onScreenResumed: 비로그인 상태 → 스킵")
            return
        }
        Log.d(TAG, "onScreenResumed: 포그라운드 복귀 → 토큰 갱신 + 데이터 재로드")
        // 포그라운드 복귀 시 토큰 갱신 후 데이터 재로드
        resumeJob?.cancel()
        resumeJob = viewModelScope.launch {
            // 토큰이 만료되었을 수 있으므로 먼저 refresh 시도
            val token = authRepository.refreshAccessToken()
            Log.d(TAG, "onScreenResumed: 토큰 갱신 결과=${if (token != null) "성공" else "실패"}, isLoggedIn=${isLoggedIn.value}")
            if (token == null && !isLoggedIn.value) {
                // refresh 실패 + 이미 로그아웃 처리됨 → 중단
                Log.d(TAG, "onScreenResumed: 토큰 갱신 실패 + 로그아웃 → 중단")
                return@launch
            }
            // 자식 코루틴으로 병렬 실행 (resumeJob 취소 시 함께 취소됨)
            launch { loadCategories() }
            launch { loadBlockedUsers() }
            launch { loadAnnouncements() }
            launch { checkBanStatus() }
            loadPosts()
        }
    }

    fun refresh() {
        Log.d(TAG, "refresh 시작")
        if (!isLoggedIn.value) return
        viewModelScope.launch {
            launch { loadCategories() }
            launch { loadBlockedUsers() }
            launch { loadAnnouncements() }
            launch { checkBanStatus() }
            val currentState = _state.value
            _state.update { it.copy(isRefreshing = true) }
            val query = currentState.searchQuery.takeIf { it.isNotBlank() }
            boardRepository.getPosts(
                category = currentState.selectedCategory,
                searchQuery = query,
                limit = PAGE_SIZE,
                offset = 0
            )
                .onSuccess { posts ->
                    val filtered = posts.filter { it.authorId !in blockedUserIds }
                    _state.update {
                        it.copy(
                            posts = filtered,
                            isRefreshing = false,
                            hasMore = posts.size >= PAGE_SIZE
                        )
                    }
                }
//                .onFailure {
//
//                    _state.update { it.copy(isRefreshing = false) }
//                }
	            .onFailure { error ->
		            Log.e("BoardError", "새로고침(refresh) 실패", error) // 로그 추가
		            // 기존 코드에는 에러 발생 시 UI에 알리는 처리가 없어서 추가했습니다.
		            _state.update { it.copy(isRefreshing = false, error = error.message) }
	            }
        }
    }
}
