package com.sonbum.diacalendar2.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.Document
import com.sonbum.diacalendar2.domain.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentState(
    val documents: List<Document> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

class DocumentViewModel(
    private val repository: DocumentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentState())
    val state = _state.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getDocuments()
                .onSuccess { docs ->
                    _state.update { it.copy(documents = docs, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true, error = null) }
        viewModelScope.launch {
            repository.getDocuments()
                .onSuccess { docs ->
                    _state.update { it.copy(documents = docs, isRefreshing = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message, isRefreshing = false) }
                }
        }
    }
}
