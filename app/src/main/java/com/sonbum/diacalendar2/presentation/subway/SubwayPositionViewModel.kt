package com.sonbum.diacalendar2.presentation.subway

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.SubwayStationRegistry
import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto
import com.sonbum.diacalendar2.domain.repository.DiaRepository
import com.sonbum.diacalendar2.domain.repository.SubwayRepository
import com.sonbum.diacalendar2.domain.util.SubwayTrainParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

/** 하단 탭 하나. 본선은 방향별로, 지선은 구간별로 하나씩. */
data class SubwayTabUi(
	val key: String,
	val label: String,
	val segmentId: String,
	/** null이면 방향 구분 없이 해당 구간 전체를 표시(지선). */
	val updnLine: String?,
	val trainCount: Int
)

/** 노선도 한 행. 열차가 없어도 역은 항상 존재한다. */
data class SubwayStationUi(
	val statnId: String,
	val name: String,
	val trains: List<SubwayTrainUi>
)

data class SubwayPositionState(
	val isLoading: Boolean = true,
	val errorMessage: String? = null,
	val myTrainNo: String = "",
	/** 이 근무의 전체 열번. 모두 "내 열차"로 강조한다. */
	val myTrainNos: List<String> = emptyList(),
	/** 그중 지금 실제 운행 중인 열번들. */
	val runningTrainNos: List<String> = emptyList(),
	val line: Int = 0,
	val tabs: List<SubwayTabUi> = emptyList(),
	val selectedTabKey: String? = null,
	/** 선택된 탭의 전체 역(열차 없는 역 포함). */
	val stations: List<SubwayStationUi> = emptyList(),
	/** 내 열차가 있는 역의 인덱스. 자동 스크롤 타깃. */
	val myTrainIndex: Int? = null,
	/** 헤더 "N대" — 분기역 중복을 피해 원본 응답에서 계산. */
	val totalTrainCount: Int = 0,
	/** 헤더 "HH:mm 기준" — 열차 수신시각(recptnDt)의 최댓값. */
	val baseTimeText: String = "",
	val isLoop: Boolean = false,
	/** asset에 해당 호선 역 데이터가 없음 → 폴백 렌더. */
	val stationDataMissing: Boolean = false,
	val notRunning: Boolean = false,
	val secondsUntilRefresh: Int = AUTO_REFRESH_SECONDS
)

const val AUTO_REFRESH_SECONDS = 30

class SubwayPositionViewModel(
	private val subwayRepository: SubwayRepository,
	private val diaRepository: DiaRepository,
	private val stationRegistry: SubwayStationRegistry
) : ViewModel() {

	private val _state = MutableStateFlow(SubwayPositionState())
	val state = _state.asStateFlow()

	private var autoRefreshJob: Job? = null
	private var loadJob: Job? = null

	private var myTrainNo: String = ""
	private var myTrainNos: List<String> = emptyList()
	private var line: Int = 0
	private var officeName: String = ""

	/** 사용자가 직접 고른 탭. 30초 갱신이 선택을 되돌리지 않도록 보존한다. */
	private var userSelectedTab: String? = null

	/** 화면 진입 시 1회 호출: 조회 대상 파라미터를 보관한다. */
	fun initialize(myTrainNo: String, line: Int, officeName: String, allTrainNos: String = "") {
		// 근무 열번이 여러 개면 전부 내 열차로 취급한다. 비어 있으면 첫 열번만.
		val nos = allTrainNos.split(',')
			.map { it.trim() }
			.filter { it.isNotBlank() }
			.ifEmpty { listOf(myTrainNo) }
			.distinct()
		_state.update { it.copy(myTrainNo = myTrainNo, myTrainNos = nos, line = line) }
		this.myTrainNo = myTrainNo
		this.myTrainNos = nos
		this.line = line
		this.officeName = officeName
	}

	/** 화면이 보일 때: 즉시 조회 + 자동 갱신 루프 시작. */
	fun start() {
		load(myTrainNo, line, officeName)
		startAutoRefresh(myTrainNo, line, officeName)
	}

	/** 화면이 가려질 때: 진행 중인 조회와 자동 갱신을 모두 중단. */
	fun stop() {
		autoRefreshJob?.cancel()
		autoRefreshJob = null
		loadJob?.cancel()
		loadJob = null
	}

	/** 사용자가 직접 새로고침: 전체 로딩 표시 + 카운트다운 리셋. */
	fun refresh(myTrainNo: String, line: Int, officeName: String) {
		load(myTrainNo, line, officeName)
		startAutoRefresh(myTrainNo, line, officeName)
	}

	/** 하단 탭 선택. 이후 자동 갱신에서도 이 선택을 유지한다. */
	fun selectTab(tabKey: String) {
		userSelectedTab = tabKey
		val s = _state.value
		val tab = s.tabs.firstOrNull { it.key == tabKey } ?: return
		val seg = stationRegistry.segment(s.line, tab.segmentId)
		val stations = stationsForTab(lastTrains, tab, s.myTrainNo, lastPrevTrainNo)
		_state.update {
			it.copy(
				selectedTabKey = tabKey,
				stations = stations,
				myTrainIndex = stations.indexOfFirst { st -> st.trains.any { t -> t.isMine } }
					.takeIf { idx -> idx >= 0 },
				isLoop = seg?.loop == true
			)
		}
	}

	/** 탭 전환 시 재계산에 쓰는 마지막 응답. */
	private var lastTrains: List<SubwayPositionDto> = emptyList()
	private var lastPrevTrainNo: String? = null

	/** 30초마다 카운트다운 후 조용히 갱신하는 루프. */
	private fun startAutoRefresh(myTrainNo: String, line: Int, officeName: String) {
		autoRefreshJob?.cancel()
		autoRefreshJob = viewModelScope.launch {
			while (true) {
				for (remaining in AUTO_REFRESH_SECONDS downTo 1) {
					_state.update { it.copy(secondsUntilRefresh = remaining) }
					delay(1000)
				}
				load(myTrainNo, line, officeName, silent = true)
			}
		}
	}

	private fun load(myTrainNo: String, line: Int, officeName: String, silent: Boolean = false) {
		loadJob?.cancel()
		loadJob = viewModelScope.launch {
			// 자동 갱신(silent)은 전체 로딩 스피너를 띄우지 않고 기존 목록을 유지한다.
			_state.update {
				it.copy(
					isLoading = if (silent) it.isLoading else true,
					errorMessage = null,
					secondsUntilRefresh = AUTO_REFRESH_SECONDS
				)
			}

			// 2호선 홀수 교대 보조: 내 열번이 홀수면 전 열번도 조회.
			val prevTrainNo: String? =
				if (SubwayTrainParser.isOdd(myTrainNo)) previousTrainNo(myTrainNo, officeName) else null

			subwayRepository.getLinePositions(line).fold(
				onSuccess = { list ->
					lastTrains = list
					lastPrevTrainNo = prevTrainNo
					applyResult(list, myTrainNo, line, prevTrainNo)
				},
				onFailure = { e ->
					// 자동 갱신 실패는 기존 목록을 유지하고 에러 화면으로 전환하지 않는다.
					if (silent) {
						_state.update { it.copy(isLoading = false) }
					} else {
						_state.update {
							it.copy(isLoading = false, errorMessage = e.message ?: "조회 실패")
						}
					}
				}
			)
		}
	}

	/** 내 열번 중 지금 실제 운행 중인 것들. */
	private fun runningTrainNos(all: List<SubwayPositionDto>): List<String> =
		myTrainNos.filter { no ->
			all.any { dto -> dto.trainNo?.let { SubwayTrainParser.sameTrain(it, no) } == true }
		}

	private fun applyResult(
		all: List<SubwayPositionDto>,
		myTrainNo: String,
		line: Int,
		prevTrainNo: String?
	) {
		warnUnmappedStations(all, line)

		if (!stationRegistry.isLoaded(line)) {
			// asset에 없는 호선: 기존 방식(열차 있는 역만)으로 폴백.
			applyFallback(all, myTrainNo, prevTrainNo)
			return
		}

		val tabs = buildTabs(all, line)
		if (tabs.isEmpty()) {
			_state.update {
				it.copy(
					isLoading = false, tabs = emptyList(), stations = emptyList(),
					notRunning = true, totalTrainCount = all.size,
					baseTimeText = baseTime(all), stationDataMissing = false
				)
			}
			return
		}

		// 사용자가 고른 탭을 우선 유지하되, 사라졌으면 기본값으로 되돌린다.
		val keep = userSelectedTab?.takeIf { key -> tabs.any { it.key == key } }
		val selected = keep ?: defaultTabKey(all, tabs, myTrainNo, line)
		val tab = tabs.firstOrNull { it.key == selected } ?: tabs.first()
		val stations = stationsForTab(all, tab, myTrainNo, prevTrainNo)
		val seg = stationRegistry.segment(line, tab.segmentId)

		_state.update {
			it.copy(
				isLoading = false,
				tabs = tabs,
				selectedTabKey = tab.key,
				stations = stations,
				myTrainIndex = stations.indexOfFirst { st -> st.trains.any { t -> t.isMine } }
					.takeIf { idx -> idx >= 0 },
				totalTrainCount = all.size,
				baseTimeText = baseTime(all),
				runningTrainNos = runningTrainNos(all),
				isLoop = seg?.loop == true,
				stationDataMissing = false,
				notRunning = all.isEmpty()
			)
		}
	}

	/**
	 * asset에 없는 statnId는 열차가 노선도에서 조용히 사라지는 것을 뜻한다.
	 * 임시 디버깅용이 아니라 상시 경고로 남긴다(역 신설 시 감지).
	 */
	private fun warnUnmappedStations(all: List<SubwayPositionDto>, line: Int) {
		if (!stationRegistry.isLoaded(line)) return
		all.asSequence()
			.filter { stationRegistry.locate(line, it.statnId).isEmpty() }
			.map { "${it.statnId}(${it.statnNm})" }
			.distinct()
			.forEach { Log.w(TAG, "노선도 asset에 없는 역: line=$line $it") }
	}

	/** asset이 없는 호선: 열차가 있는 역만 statnId 순으로 세운다(구 동작). */
	private fun applyFallback(
		all: List<SubwayPositionDto>,
		myTrainNo: String,
		prevTrainNo: String?
	) {
		val mine = all.filter { dto -> isMine(dto.trainNo) }
		val dir = mine.firstOrNull()?.updnLine
		val visible = (if (dir != null) all.filter { it.updnLine == dir } else all)
			.sortedBy { legacySeq(it) }

		val stations = visible
			.groupBy { it.statnId to it.statnNm }
			.map { (k, group) ->
				SubwayStationUi(
					statnId = k.first.orEmpty(),
					name = k.second ?: "-",
					trains = group.map { toUi(it, myTrainNo, prevTrainNo) }
				)
			}

		_state.update {
			it.copy(
				isLoading = false,
				tabs = emptyList(),
				selectedTabKey = null,
				stations = stations,
				myTrainIndex = stations.indexOfFirst { st -> st.trains.any { t -> t.isMine } }
					.takeIf { idx -> idx >= 0 },
				totalTrainCount = all.size,
				baseTimeText = baseTime(all),
				runningTrainNos = runningTrainNos(all),
				isLoop = false,
				stationDataMissing = true,
				notRunning = stations.isEmpty()
			)
		}
	}

	/**
	 * 탭 구성: 본선은 방향별로 하나씩, 지선은 구간당 하나(양방향 함께 표시).
	 * 열차가 한 대도 없는 탭도 노출해 노선도 자체는 항상 볼 수 있게 한다.
	 */
	private fun buildTabs(all: List<SubwayPositionDto>, line: Int): List<SubwayTabUi> {
		val segments = stationRegistry.segments(line)
		val tabs = mutableListOf<SubwayTabUi>()
		segments.forEach { seg ->
			if (seg.id == MAIN_SEGMENT_ID && seg.directions.isNotEmpty()) {
				seg.directions.forEach { dir ->
					tabs += SubwayTabUi(
						key = "${seg.id}:${dir.updnLine}",
						label = dir.label,
						segmentId = seg.id,
						updnLine = dir.updnLine,
						trainCount = all.count { matches(it, line, seg.id, dir.updnLine) }
					)
				}
			} else {
				tabs += SubwayTabUi(
					key = seg.id,
					label = seg.title,
					segmentId = seg.id,
					updnLine = null,
					trainCount = all.count { matches(it, line, seg.id, null) }
				)
			}
		}
		return tabs
	}

	/** DTO가 해당 구간/방향에 속하는지. 분기역은 두 구간 모두에 매칭된다. */
	private fun matches(
		dto: SubwayPositionDto,
		line: Int,
		segmentId: String,
		updnLine: String?
	): Boolean {
		val inSegment = stationRegistry.locate(line, dto.statnId).any { it.segmentId == segmentId }
		if (!inSegment) return false
		return updnLine == null || dto.updnLine == updnLine
	}

	/** 선택 탭의 전체 역 목록에 현재 열차를 얹는다. */
	private fun stationsForTab(
		all: List<SubwayPositionDto>,
		tab: SubwayTabUi,
		myTrainNo: String,
		prevTrainNo: String?
	): List<SubwayStationUi> {
		val seg = stationRegistry.segment(_state.value.line, tab.segmentId) ?: return emptyList()
		val line = _state.value.line

		// statnId -> 이 탭에 표시할 열차들
		val byStation = all
			.filter { matches(it, line, tab.segmentId, tab.updnLine) }
			.groupBy { it.statnId }

		return seg.stations.map { station ->
			SubwayStationUi(
				statnId = station.statnId,
				name = station.name,
				trains = byStation[station.statnId]
					.orEmpty()
					.map { toUi(it, myTrainNo, prevTrainNo) }
			)
		}
	}

	/** 내 열차가 있는 탭을 기본 선택. 없으면 열차가 가장 많은 탭. */
	private fun defaultTabKey(
		all: List<SubwayPositionDto>,
		tabs: List<SubwayTabUi>,
		myTrainNo: String,
		line: Int
	): String {
		// 근무 열번 순서대로 먼저 잡히는 운행 열차의 탭을 연다.
		for (no in myTrainNos) {
			val mine = all.firstOrNull { dto ->
				dto.trainNo?.let { SubwayTrainParser.sameTrain(it, no) } == true
			} ?: continue
			val hit = tabs.firstOrNull { matches(mine, line, it.segmentId, it.updnLine) }
			if (hit != null) return hit.key
		}
		return tabs.maxByOrNull { it.trainCount }?.key ?: tabs.first().key
	}

	/** 이 근무의 열번 중 하나라도 일치하면 내 열차. */
	private fun isMine(trainNo: String?): Boolean {
		if (trainNo == null) return false
		return myTrainNos.any { SubwayTrainParser.sameTrain(trainNo, it) }
	}

	private fun toUi(
		dto: SubwayPositionDto,
		myTrainNo: String,
		prevTrainNo: String?
	): SubwayTrainUi = SubwayTrainUi(
		dto = dto,
		isMine = isMine(dto.trainNo),
		isPrevious = prevTrainNo != null &&
			(dto.trainNo?.let { SubwayTrainParser.sameTrain(it, prevTrainNo) } ?: false),
		seq = legacySeq(dto)
	)

	/** 헤더 기준시각: 기기 시계가 아니라 응답의 수신시각 최댓값(데이터 신선도). */
	private fun baseTime(all: List<SubwayPositionDto>): String =
		all.mapNotNull { it.recptnDt }
			.maxOrNull()
			?.substringAfter(' ')
			?.take(5)
			.orEmpty()

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

	/** 폴백 정렬용 순번. asset이 있는 경로에서는 쓰지 않는다. */
	private fun legacySeq(dto: SubwayPositionDto): Int =
		dto.statnId?.takeLast(4)?.toIntOrNull() ?: 0

	companion object {
		private const val TAG = "SubwayPositionVM"
		private const val MAIN_SEGMENT_ID = "main"
	}
}
