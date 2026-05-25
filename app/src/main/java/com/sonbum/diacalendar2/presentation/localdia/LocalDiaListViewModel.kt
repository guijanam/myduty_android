package com.sonbum.diacalendar2.presentation.localdia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.LocalDia
import com.sonbum.diacalendar2.domain.repository.LocalDiaRepository
import com.sonbum.diacalendar2.domain.repository.LocalOfficeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocalDiaCategory(
    val name: String,
    val dias: List<LocalDia>
)

data class LocalDiaListState(
    val officeId: Long = 0,
    val officeName: String = "",
    val categories: List<LocalDiaCategory> = emptyList(),
    val selectedCategoryIndex: Int = 0,
    val isLoading: Boolean = true
)

class LocalDiaListViewModel(
    private val localDiaRepository: LocalDiaRepository,
    private val localOfficeRepository: LocalOfficeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LocalDiaListState())
    val state = _state.asStateFlow()

    companion object {
        val CATEGORY_ORDER = listOf(
            "평일", "평평", "휴일", "평휴", "휴평", "휴휴", "평토", "토", "토휴", "휴토"
        )
    }

    fun initialize(officeId: Long) {
        _state.update { it.copy(officeId = officeId) }
        viewModelScope.launch {
            val office = localOfficeRepository.getLocalOfficeById(officeId)
            if (office != null) {
                _state.update { it.copy(officeName = office.officeName) }
            }
        }
        viewModelScope.launch {
            localDiaRepository.getLocalDiasByOfficeId(officeId).collect { dias ->
                val categories = groupAndSortDias(dias)
                _state.update { it.copy(categories = categories, isLoading = false) }
            }
        }
    }

    fun onCategorySelected(index: Int) {
        _state.update { it.copy(selectedCategoryIndex = index) }
    }

    fun deleteDia(id: Long) {
        viewModelScope.launch {
            localDiaRepository.deleteLocalDia(id)
        }
    }

    private fun groupAndSortDias(dias: List<LocalDia>): List<LocalDiaCategory> {
        val grouped = dias.groupBy { it.typeName ?: "기타" }
        val ordered = CATEGORY_ORDER.mapNotNull { categoryName ->
            grouped[categoryName]?.let { LocalDiaCategory(categoryName, sortDiasNaturally(it)) }
        }
        val remaining = grouped.filter { it.key !in CATEGORY_ORDER }
            .map { (name, diaList) -> LocalDiaCategory(name, sortDiasNaturally(diaList)) }
        return ordered + remaining
    }

    private fun sortDiasNaturally(dias: List<LocalDia>): List<LocalDia> {
        return dias.sortedWith(compareBy { extractNumber(it.diaId) })
    }

    private fun extractNumber(diaId: String): Int {
        return diaId.filter { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
    }
}
