package com.sonbum.diacalendar2.presentation.diatable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.domain.model.Dia
import com.sonbum.diacalendar2.domain.model.LocalDia
import com.sonbum.diacalendar2.domain.repository.DiaRepository
import com.sonbum.diacalendar2.domain.repository.LocalDiaRepository
import com.sonbum.diacalendar2.domain.repository.OfficeRepository
import com.sonbum.diacalendar2.domain.repository.ShiftRepository
import com.sonbum.diacalendar2.data.local.datastore.TextSizePreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiaTableState(
    val categories: List<DiaCategory> = emptyList(),
    val selectedCategoryIndex: Int = 0,
    val officeName: String = "",
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val isServerOffice: Boolean = false,
    val currentOfficeCode: Long? = null,
    val backupCount: Int = 0
)

sealed interface DiaTableEvent {
    data class RestoreResult(val count: Int) : DiaTableEvent
    data class Error(val message: String) : DiaTableEvent
}

data class DiaCategory(
    val name: String,
    val dias: List<Dia>
)

class DiaTableViewModel(
    private val diaRepository: DiaRepository,
    private val localDiaRepository: LocalDiaRepository,
    private val shiftRepository: ShiftRepository,
    private val officeRepository: OfficeRepository,
    private val textSizePreferences: TextSizePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(DiaTableState())
    val state: StateFlow<DiaTableState> = _state.asStateFlow()

    private val _event = MutableSharedFlow<DiaTableEvent>()
    val event = _event.asSharedFlow()

    /** 근무표 글자 크기 단계 인덱스 (저장된 값, 앱 재실행 후 유지) */
    val fontScaleIndex: StateFlow<Int> = textSizePreferences.diaTableFontScaleIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setFontScaleIndex(index: Int) {
        viewModelScope.launch { textSizePreferences.saveDiaTableFontScaleIndex(index) }
    }

    companion object {
        val CATEGORY_ORDER = listOf(
            "평일", "평평", "휴일", "평휴", "휴평", "휴휴", "평토", "토", "토휴", "휴토"
        )
    }

    init {
        observeConfig()
        loadBackupCount()
    }

    private fun observeConfig() {
        viewModelScope.launch {
            // Flow로 구독하여 config 변경 시 자동 갱신
            shiftRepository.getUserConfig().collect { config ->
                if (config == null) {
                    _state.value = _state.value.copy(
                        categories = emptyList(),
                        officeName = "",
                        isLoading = false
                    )
                    return@collect
                }

                val isServer = config.officeCode >= 0
                _state.value = _state.value.copy(
                    isLoading = true,
                    officeName = config.officeName,
                    isServerOffice = isServer,
                    currentOfficeCode = config.officeCode
                )
                loadDiasForConfig(config)
            }
        }
    }

    private suspend fun loadDiasForConfig(config: com.sonbum.diacalendar2.domain.model.UserShiftConfig) {
        val isLocalOffice = config.officeCode < 0

        if (isLocalOffice) {
            // 내부 승무소: local_dias 테이블에서 조회
            localDiaRepository.getLocalDiasByOfficeName(config.officeName).collect { localDias ->
                val dias = localDias.map { it.toDia() }
                val grouped = groupAndSortDias(dias)
                _state.value = _state.value.copy(
                    categories = grouped,
                    isLoading = false
                )
            }
        } else {
            // 서버 승무소: dias 테이블에서 조회
            diaRepository.getDiasByOfficeName(config.officeName).collect { dias ->
                val grouped = groupAndSortDias(dias)
                _state.value = _state.value.copy(
                    categories = grouped,
                    isLoading = false
                )
            }
        }
    }

    private fun LocalDia.toDia(): Dia = Dia(
        id = id,
        diaId = diaId,
        officeName = officeName,
        officeId = null,
        typeName = typeName,
        firstTime = firstTime,
        numTr1 = numTr1,
        numTr2 = numTr2,
        secondTime = secondTime,
        thirdTime = thirdTime,
        totalTime = totalTime,
        workTime = workTime
    )

    fun selectCategory(index: Int) {
        _state.value = _state.value.copy(selectedCategoryIndex = index)
    }

    fun toggleEditMode() {
        _state.value = _state.value.copy(isEditMode = !_state.value.isEditMode)
    }

    fun restoreBackups() {
        viewModelScope.launch {
            try {
                val officeCount = officeRepository.restoreEditedOffices()
                val diaCount = diaRepository.restoreEditedDias()
                _event.emit(DiaTableEvent.RestoreResult(officeCount + diaCount))
                loadBackupCount()
            } catch (e: Exception) {
                _event.emit(DiaTableEvent.Error("복원 실패: ${e.message}"))
            }
        }
    }

    fun clearBackups() {
        viewModelScope.launch {
            officeRepository.clearEditBackups()
            diaRepository.clearDiaEditBackups()
            loadBackupCount()
        }
    }

    private fun loadBackupCount() {
        viewModelScope.launch {
            val officeCount = officeRepository.getEditBackupCount()
            val diaCount = diaRepository.getDiaEditBackupCount()
            _state.value = _state.value.copy(backupCount = officeCount + diaCount)
        }
    }

    private fun groupAndSortDias(dias: List<Dia>): List<DiaCategory> {
        val grouped = dias.groupBy { it.typeName ?: "기타" }
        val naturalComparator = Comparator<Dia> { a, b -> naturalCompare(a.diaId, b.diaId) }
        return CATEGORY_ORDER.mapNotNull { categoryName ->
            grouped[categoryName]?.let { categoryDias ->
                DiaCategory(
                    name = categoryName,
                    dias = categoryDias.sortedWith(naturalComparator)
                )
            }
        } + grouped.filterKeys { it !in CATEGORY_ORDER }.map { (name, categoryDias) ->
            DiaCategory(
                name = name,
                dias = categoryDias.sortedWith(naturalComparator)
            )
        }
    }

    private fun naturalCompare(a: String, b: String): Int {
        val regex = Regex("(\\d+)|(\\D+)")
        val partsA = regex.findAll(a).toList()
        val partsB = regex.findAll(b).toList()
        for (i in 0 until minOf(partsA.size, partsB.size)) {
            val pa = partsA[i].value
            val pb = partsB[i].value
            val result = if (pa[0].isDigit() && pb[0].isDigit()) {
                pa.toLong().compareTo(pb.toLong())
            } else {
                pa.compareTo(pb)
            }
            if (result != 0) return result
        }
        return partsA.size.compareTo(partsB.size)
    }
}
