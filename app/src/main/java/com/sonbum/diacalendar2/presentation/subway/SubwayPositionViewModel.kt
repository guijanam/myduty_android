package com.sonbum.diacalendar2.presentation.subway

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto
import com.sonbum.diacalendar2.domain.repository.DiaRepository
import com.sonbum.diacalendar2.domain.repository.SubwayRepository
import com.sonbum.diacalendar2.domain.util.SubwayTrainParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubwayTrainUi(
    val dto: SubwayPositionDto,
    val isMine: Boolean,
    val isPrevious: Boolean,
    val seq: Int
)

data class SubwayPositionState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val myTrainNo: String = "",
    val line: Int = 0,
    val trains: List<SubwayTrainUi> = emptyList(),
    val notRunning: Boolean = false
)

class SubwayPositionViewModel(
    private val subwayRepository: SubwayRepository,
    private val diaRepository: DiaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SubwayPositionState())
    val state = _state.asStateFlow()

    fun initialize(myTrainNo: String, line: Int, officeName: String) {
        _state.update { it.copy(myTrainNo = myTrainNo, line = line) }
        load(myTrainNo, line, officeName)
    }

    fun refresh(myTrainNo: String, line: Int, officeName: String) =
        load(myTrainNo, line, officeName)

    private fun load(myTrainNo: String, line: Int, officeName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            // 2호선 홀수 교대 보조: 내 열번이 홀수면 전 열번도 조회.
            val prevTrainNo: String? =
                if (SubwayTrainParser.isOdd(myTrainNo)) previousTrainNo(myTrainNo, officeName) else null

            subwayRepository.getLinePositions(line).fold(
                onSuccess = { list ->
                    val (trains, notRunning) = buildUi(list, myTrainNo, prevTrainNo)
                    _state.update {
                        it.copy(isLoading = false, trains = trains, notRunning = notRunning)
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isLoading = false, errorMessage = e.message ?: "조회 실패")
                    }
                }
            )
        }
    }

    /**
     * 사무소 전체 dia의 numTr1/numTr2 중 target이 마지막 토큰으로 오는 문자열을 찾아
     * 바로 앞 토큰(전 열번)을 반환.
     */
    private suspend fun previousTrainNo(target: String, officeName: String): String? {
        val dias = diaRepository.getDiasByOfficeName(officeName).first()
        for (d in dias) {
            SubwayTrainParser.previousTokenIfLast(d.numTr1, target)?.let { return it }
            SubwayTrainParser.previousTokenIfLast(d.numTr2, target)?.let { return it }
        }
        return null
    }

    /**
     * 내 열번 인접군 + (있으면) 전 열번 인접군을 합쳐 dedup·정렬.
     * @return Pair(표시 리스트, notRunning) — 둘 다 못 찾으면 notRunning=true.
     */
    private fun buildUi(
        all: List<SubwayPositionDto>,
        myTrainNo: String,
        prevTrainNo: String?
    ): Pair<List<SubwayTrainUi>, Boolean> {
        val merged = (adjacentTrains(all, myTrainNo) +
                (prevTrainNo?.let { adjacentTrains(all, it) } ?: emptyList()))
            .distinctBy { it.trainNo }
            .sortedBy { seq(it) }

        val ui = merged.map { dto ->
            SubwayTrainUi(
                dto = dto,
                isMine = dto.trainNo?.let { SubwayTrainParser.sameTrain(it, myTrainNo) } ?: false,
                isPrevious = prevTrainNo != null &&
                        (dto.trainNo?.let { SubwayTrainParser.sameTrain(it, prevTrainNo) } ?: false),
                seq = seq(dto)
            )
        }
        return ui to ui.isEmpty()
    }

    /** 같은 방향(updnLine) 열차를 statnId 순번 정렬 후 내 열차 앞/뒤 1대씩(클램프). */
    private fun adjacentTrains(all: List<SubwayPositionDto>, trainNo: String): List<SubwayPositionDto> {
        val mine = all.firstOrNull { it.trainNo?.let { t -> SubwayTrainParser.sameTrain(t, trainNo) } == true }
            ?: return emptyList()
        val sameDir = all
            .filter { it.updnLine == mine.updnLine }
            .sortedBy { seq(it) }
        val idx = sameDir.indexOfFirst { it.trainNo?.let { t -> SubwayTrainParser.sameTrain(t, trainNo) } == true }
        if (idx < 0) return listOf(mine)
        val lower = maxOf(0, idx - 1)
        val upper = minOf(sameDir.lastIndex, idx + 1)
        return sameDir.subList(lower, upper + 1)
    }

    private fun seq(dto: SubwayPositionDto): Int =
        dto.statnId?.takeLast(4)?.toIntOrNull() ?: 0
}
