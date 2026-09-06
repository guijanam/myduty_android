package com.sonbum.diacalendar2.presentation.trainformation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.TrainFormation
import com.sonbum.diacalendar2.domain.repository.TrainFormationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrainFormationListState(
    val query: String = "",
    val results: List<TrainFormation> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

class TrainFormationListViewModel(
    private val repository: TrainFormationRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val all = repository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 검색은 메모리 필터링.
     * SQLite LIKE 는 한글을 정규화하지 않아 교번명/메모 검색이 부정확하고,
     * 데이터가 근무일 하루 2행 수준이라 전체 로드 비용이 무의미하다.
     */
    val state: StateFlow<TrainFormationListState> =
        combine(all, query) { list, q ->
            val trimmed = q.trim()
            val filtered = if (trimmed.isBlank()) {
                list
            } else {
                list.filter { f ->
                    f.formationNo.toString().contains(trimmed) ||
                        f.note.contains(trimmed, ignoreCase = true) ||
                        f.shiftName.contains(trimmed, ignoreCase = true) ||
                        f.numTr.contains(trimmed, ignoreCase = true)
                }
            }
            TrainFormationListState(
                query = q,
                results = filtered,
                totalCount = list.size,
                isLoading = false
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TrainFormationListState()
        )

    fun onQueryChange(q: String) {
        query.value = q
    }

    /** 편성번호/메모만 수정. 스냅샷(shiftName, numTr)은 그대로 둔다. */
    fun update(formation: TrainFormation) {
        viewModelScope.launch { repository.update(formation) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
