package com.sonbum.diacalendar2.presentation.anniversary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.Anniversary
import com.sonbum.diacalendar2.domain.repository.AnniversaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnniversaryState(
    val list: List<Anniversary> = emptyList(),
    val isLoading: Boolean = true
)

class AnniversaryViewModel(
    private val repository: AnniversaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnniversaryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAll().collect { list ->
                _state.update { it.copy(list = list, isLoading = false) }
            }
        }
    }

    fun add(anniversary: Anniversary) {
        viewModelScope.launch { repository.add(anniversary) }
    }

    fun update(anniversary: Anniversary) {
        viewModelScope.launch { repository.update(anniversary) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}
