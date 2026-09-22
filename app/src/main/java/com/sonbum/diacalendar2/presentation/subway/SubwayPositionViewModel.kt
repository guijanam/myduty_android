package com.sonbum.diacalendar2.presentation.subway

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonbum.diacalendar2.data.local.SubwayStationRegistry
import com.sonbum.diacalendar2.data.local.datastore.SubwayPreferences
import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto
import com.sonbum.diacalendar2.domain.model.StationRef
import com.sonbum.diacalendar2.domain.model.SubwayStationDef
import com.sonbum.diacalendar2.domain.repository.DiaRepository
import com.sonbum.diacalendar2.domain.repository.SubwayRepository
import com.sonbum.diacalendar2.domain.util.SubwayTrainParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
	/** 이 탭의 역 목록과 실시간 열차를 가져올 실제 호선. */
	val sourceLine: Int,
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
	/** 이 근무에서 가장 앞에 있는 열번 하나. */
	val myTrainNos: List<String> = emptyList(),
	/** 가장 앞 열번이 현재 실시간 응답에 있으면 한 건, 없으면 빈 목록. */
	val runningTrainNos: List<String> = emptyList(),
	/** 내 열번이 없을 때 대신 추적 중인 전 열번(API 표기). 내 열차로 취급하지 않는다. */
	val previousTrainNo: String? = null,
	val line: Int = 0,
	val tabs: List<SubwayTabUi> = emptyList(),
	val selectedTabKey: String? = null,
	/** 선택된 탭에 표시할 역(열차 없는 역 포함). */
	val stations: List<SubwayStationUi> = emptyList(),
	/** 내 열차 또는 대신 표시하는 전 열번이 있는 역의 인덱스. */
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
const val DEFAULT_SUBWAY_LINE = 1

internal fun resolveInitialSubwayLine(
	savedLine: Int?,
	dutyLine: Int?,
	fallbackLine: Int
): Int =
	when {
		dutyLine != null && dutyLine in 1..9 -> dutyLine
		savedLine != null && savedLine in 1..9 -> savedLine
		fallbackLine in 1..9 -> fallbackLine
		else -> DEFAULT_SUBWAY_LINE
	}

/** 1호선 화면에는 성수지선 탭을 위해 2호선 실시간 위치도 함께 조회한다. */
internal fun requestedSubwayLines(selectedLine: Int): List<Int> =
	if (selectedLine == 1) listOf(1, 2) else listOf(selectedLine)

internal data class TrackedTrainSelection(
	val current: SubwayPositionDto?,
	val previous: SubwayPositionDto?
) {
	val tracked: SubwayPositionDto? get() = current ?: previous
}

/** 근무 문자열에서는 가장 앞 열번 하나만 현재 근무 열번으로 사용한다. */
internal fun primaryDutyTrainNo(myTrainNo: String, allTrainNos: String): String =
	allTrainNos.split(',')
		.map { it.trim() }
		.firstOrNull { it.isNotBlank() }
		?: myTrainNo.trim()

/**
 * 현재 근무 열번을 우선 선택하고, 현재 열번이 실시간 목록에 없을 때만 전 열번을 선택한다.
 * 완전 일치를 먼저 검사해 뒤 3자리가 같은 다른 열차를 잘못 고르는 경우를 줄인다.
 */
internal fun selectTrackedTrain(
	all: List<SubwayPositionDto>,
	currentTrainNo: String,
	previousTrainNo: String?
): TrackedTrainSelection {
	fun bestMatch(trainNo: String?): SubwayPositionDto? {
		val target = trainNo?.trim().orEmpty()
		if (target.isEmpty()) return null
		return all.firstOrNull { it.trainNo?.trim() == target }
			?: all.firstOrNull { candidate ->
				candidate.trainNo?.let { SubwayTrainParser.sameTrain(it, target) } == true
			}
	}

	val current = bestMatch(currentTrainNo)
	val previous = if (current == null) bestMatch(previousTrainNo) else null
	return TrackedTrainSelection(current = current, previous = previous)
}

/**
 * 분기역은 본선과 지선에 같은 statnId로 등록된다. 서울교통공사 HTML에서 확인한
 * 2호선 구간 힌트로 서로의 열차가 섞이지 않게 하고, 힌트가 없으면 본선에만 둔다.
 */
internal fun matchesSubwaySegment(
	dto: SubwayPositionDto,
	segmentId: String,
	updnLine: String?,
	stationRefs: List<StationRef>
): Boolean {
	if (stationRefs.none { it.segmentId == segmentId }) return false

	val routeSegmentId = dto.routeSegmentId
	if (routeSegmentId != null && routeSegmentId != segmentId) return false
	if (
		routeSegmentId == null &&
		stationRefs.size > 1 &&
		segmentId in STRICT_TWO_LINE_BRANCH_SEGMENTS
	) return false

	return updnLine == null || dto.updnLine == updnLine
}

private val STRICT_TWO_LINE_BRANCH_SEGMENTS = setOf("seongsu", "sinjeong")

class SubwayPositionViewModel(
	private val subwayRepository: SubwayRepository,
	private val diaRepository: DiaRepository,
	private val stationRegistry: SubwayStationRegistry,
	private val subwayPreferences: SubwayPreferences
) : ViewModel() {

	private val _state = MutableStateFlow(SubwayPositionState())
	val state = _state.asStateFlow()

	private var autoRefreshJob: Job? = null
	private var loadJob: Job? = null
	private var initializeJob: Job? = null
	private var initialized = false
	private var isStarted = false

	private var myTrainNo: String = ""
	private var myTrainNos: List<String> = emptyList()
	private var line: Int = 0
	private var officeName: String = ""

	/** 사용자가 직접 고른 탭. 30초 갱신이 선택을 되돌리지 않도록 보존한다. */
	private var userSelectedTab: String? = null

	/** 화면 진입 시 1회 호출: 조회 대상 파라미터를 보관한다. */
	fun initialize(myTrainNo: String, line: Int, officeName: String, allTrainNos: String = "") {
		initializeJob?.cancel()
		loadJob?.cancel()
		autoRefreshJob?.cancel()
		initialized = false

		initializeJob = viewModelScope.launch {
			val primary = primaryDutyTrainNo(myTrainNo, allTrainNos)
			val selectedLine = resolveInitialSubwayLine(
				savedLine = subwayPreferences.selectedLine.first(),
				dutyLine = SubwayTrainParser.line(primary),
				fallbackLine = line
			)
			val nos = listOf(primary).filter { it.isNotBlank() }

			this@SubwayPositionViewModel.myTrainNo = primary
			this@SubwayPositionViewModel.myTrainNos = nos
			this@SubwayPositionViewModel.line = selectedLine
			this@SubwayPositionViewModel.officeName = officeName
			userSelectedTab = null
			lastTrains = emptyList()
			lastCurrentTrainNo = null
			lastPrevTrainNo = null
			initialized = true

			_state.update {
				it.copy(
					isLoading = true,
					errorMessage = null,
					myTrainNo = primary,
					myTrainNos = nos,
					runningTrainNos = emptyList(),
					previousTrainNo = null,
					line = selectedLine,
					tabs = emptyList(),
					selectedTabKey = null,
					stations = emptyList(),
					myTrainIndex = null,
					totalTrainCount = 0,
					baseTimeText = "",
					isLoop = false,
					stationDataMissing = false,
					notRunning = false,
					secondsUntilRefresh = AUTO_REFRESH_SECONDS
				)
			}

			// 최초 진입에서 추론한 기본 호선도 다음 앱 실행 때 복원할 수 있게 저장한다.
			subwayPreferences.saveSelectedLine(selectedLine)
			if (isStarted) startLoading()
		}
	}

	/** 화면이 보일 때: 즉시 조회 + 자동 갱신 루프 시작. */
	fun start() {
		isStarted = true
		if (initialized) startLoading()
	}

	/** 화면이 가려질 때: 진행 중인 조회와 자동 갱신을 모두 중단. */
	fun stop() {
		isStarted = false
		autoRefreshJob?.cancel()
		autoRefreshJob = null
		loadJob?.cancel()
		loadJob = null
	}

	/** 사용자가 직접 새로고침: 전체 로딩 표시 + 카운트다운 리셋. */
	fun refresh() {
		if (!initialized) return
		load(myTrainNo, line, officeName)
		startAutoRefresh(myTrainNo, line, officeName)
	}

	/** 상단 선택기에서 호선을 바꾸고 즉시 조회한다. 선택값은 앱 재실행 후에도 유지한다. */
	fun selectLine(selectedLine: Int) {
		if (selectedLine !in 1..9 || !initialized) return
		viewModelScope.launch { subwayPreferences.saveSelectedLine(selectedLine) }
		if (selectedLine == line) return

		line = selectedLine
		userSelectedTab = null
		lastTrains = emptyList()
		lastCurrentTrainNo = null
		lastPrevTrainNo = null
		_state.update {
			it.copy(
				isLoading = true,
				errorMessage = null,
				line = selectedLine,
				tabs = emptyList(),
				selectedTabKey = null,
				stations = emptyList(),
				myTrainIndex = null,
				totalTrainCount = 0,
				baseTimeText = "",
				isLoop = false,
				stationDataMissing = false,
				notRunning = false,
				secondsUntilRefresh = AUTO_REFRESH_SECONDS
			)
		}

		if (isStarted) startLoading()
	}

	private fun startLoading() {
		load(myTrainNo, line, officeName)
		startAutoRefresh(myTrainNo, line, officeName)
	}

	/** 하단 탭 선택. 이후 자동 갱신에서도 이 선택을 유지한다. */
	fun selectTab(tabKey: String) {
		userSelectedTab = tabKey
		val s = _state.value
		val tab = s.tabs.firstOrNull { it.key == tabKey } ?: return
		val seg = stationRegistry.segment(tab.sourceLine, tab.segmentId)
		val stations = stationsForTab(lastTrains, tab, lastCurrentTrainNo, lastPrevTrainNo)
		val tabLineTrains = positionsForLine(lastTrains, tab.sourceLine)
		val displayedTrains = stations.flatMap { station -> station.trains.map { it.dto } }
		val summaryTrains = if (summarizeDisplayedTrains(line, tab)) displayedTrains else tabLineTrains
		_state.update {
			it.copy(
				selectedTabKey = tabKey,
				stations = stations,
				myTrainIndex = focusTrainIndex(stations)
					.takeIf { idx -> idx >= 0 },
				isLoop = seg?.loop == true,
				totalTrainCount = summaryTrains.size,
				baseTimeText = baseTime(summaryTrains),
				notRunning = summaryTrains.isEmpty()
			)
		}
	}

	/** 탭 전환 시 재계산에 쓰는 마지막 응답. */
	private var lastTrains: List<SubwayPositionDto> = emptyList()
	private var lastCurrentTrainNo: String? = null
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

			getDisplayedLinePositions(line).fold(
				onSuccess = { list ->
					val selectedLineTrains = positionsForLine(list, line)
					val trackableTrains = if (line == 1) {
						selectedLineTrains + positionsForLine(list, 2).filter {
							matches(it, 2, SEONGSU_BRANCH_ID, null)
						}
					} else selectedLineTrains
					val selection = selectTrackedTrain(trackableTrains, myTrainNo, prevTrainNo)
						val activeCurrentTrainNo = selection.current?.trainNo
						val activePreviousTrainNo = selection.previous?.trainNo
						lastTrains = list
						lastCurrentTrainNo = activeCurrentTrainNo
						lastPrevTrainNo = activePreviousTrainNo
						applyResult(
							all = list,
							line = line,
							currentTrainNo = activeCurrentTrainNo,
							prevTrainNo = activePreviousTrainNo,
							trackedTrain = selection.tracked
					)
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

	/** 주 호선 조회 실패는 그대로 전달하고, 성수지선 보조 조회 실패는 빈 목록으로 처리한다. */
	private suspend fun getDisplayedLinePositions(selectedLine: Int): Result<List<SubwayPositionDto>> =
		coroutineScope {
			val requests = requestedSubwayLines(selectedLine).associateWith { requestedLine ->
				async { subwayRepository.getLinePositions(requestedLine) }
			}
			val primary = requests.getValue(selectedLine).await()
			primary.map { primaryPositions ->
				primaryPositions + requests
					.filterKeys { it != selectedLine }
					.values
					.flatMap { it.await().getOrDefault(emptyList()) }
			}
		}

	/** 가장 앞 근무 열번이 실시간 응답에 있을 때만 화면 표시용 목록을 만든다. */
	private fun runningTrainNos(currentTrainNo: String?): List<String> =
		if (currentTrainNo != null) myTrainNos else emptyList()

	private fun applyResult(
		all: List<SubwayPositionDto>,
		line: Int,
		currentTrainNo: String?,
		prevTrainNo: String?,
		trackedTrain: SubwayPositionDto?
	) {
		requestedSubwayLines(line).forEach { sourceLine ->
			warnUnmappedStations(all, sourceLine)
		}

		if (!stationRegistry.isLoaded(line)) {
			// asset에 없는 호선: 기존 방식(열차 있는 역만)으로 폴백.
			applyFallback(all, currentTrainNo, prevTrainNo, trackedTrain)
			return
		}

		val tabs = buildTabs(all, line)
		if (tabs.isEmpty()) {
			_state.update {
				it.copy(
					isLoading = false, tabs = emptyList(), stations = emptyList(),
					notRunning = true, totalTrainCount = all.size,
					baseTimeText = baseTime(all), stationDataMissing = false,
					runningTrainNos = runningTrainNos(currentTrainNo),
					previousTrainNo = prevTrainNo
				)
			}
			return
		}

		// 사용자가 고른 탭을 우선 유지하되, 사라졌으면 기본값으로 되돌린다.
		val keep = userSelectedTab?.let { key ->
			tabs.firstOrNull { it.key == key }
				?.takeIf { tab ->
					// 다른 호선에서 가져온 보조 탭은 사용자가 고른 동안 계속 유지한다.
					tab.sourceLine != line ||
					// 전 열번 폴백 중에는 전 열번이 실제로 있는 탭을 우선한다.
					prevTrainNo == null || trackedTrain == null ||
						matches(trackedTrain, tab)
				}
				?.key
		}
		val selected = keep ?: defaultTabKey(tabs, line, trackedTrain)
		val tab = tabs.firstOrNull { it.key == selected } ?: tabs.first()
		val stations = stationsForTab(all, tab, currentTrainNo, prevTrainNo)
		val seg = stationRegistry.segment(tab.sourceLine, tab.segmentId)
		val tabLineTrains = positionsForLine(all, tab.sourceLine)
		val displayedTrains = stations.flatMap { station -> station.trains.map { it.dto } }
		val summaryTrains = if (summarizeDisplayedTrains(line, tab)) displayedTrains else tabLineTrains

		_state.update {
			it.copy(
				isLoading = false,
				tabs = tabs,
				selectedTabKey = tab.key,
				stations = stations,
				myTrainIndex = focusTrainIndex(stations)
					.takeIf { idx -> idx >= 0 },
				totalTrainCount = summaryTrains.size,
				baseTimeText = baseTime(summaryTrains),
				runningTrainNos = runningTrainNos(currentTrainNo),
				previousTrainNo = prevTrainNo,
				isLoop = seg?.loop == true,
				stationDataMissing = false,
				notRunning = summaryTrains.isEmpty()
			)
		}
	}

	/**
	 * asset에 없는 statnId는 열차가 노선도에서 조용히 사라지는 것을 뜻한다.
	 * 임시 디버깅용이 아니라 상시 경고로 남긴다(역 신설 시 감지).
	 */
	private fun warnUnmappedStations(all: List<SubwayPositionDto>, line: Int) {
		if (!stationRegistry.isLoaded(line)) return
		positionsForLine(all, line).asSequence()
			.filter { stationRegistry.locate(line, it.statnId).isEmpty() }
			.map { "${it.statnId}(${it.statnNm})" }
			.distinct()
			.forEach { Log.w(TAG, "노선도 asset에 없는 역: line=$line $it") }
	}

	/** asset이 없는 호선: 열차가 있는 역만 statnId 순으로 세운다(구 동작). */
	private fun applyFallback(
		all: List<SubwayPositionDto>,
		currentTrainNo: String?,
		prevTrainNo: String?,
		trackedTrain: SubwayPositionDto?
	) {
		val dir = trackedTrain?.updnLine
		val visible = (if (dir != null) all.filter { it.updnLine == dir } else all)
			.sortedBy { legacySeq(it) }

		val stations = visible
			.groupBy { it.statnId to it.statnNm }
			.map { (k, group) ->
				SubwayStationUi(
					statnId = k.first.orEmpty(),
					name = k.second ?: "-",
					trains = group.map { toUi(it, currentTrainNo, prevTrainNo) }
				)
			}

		_state.update {
			it.copy(
				isLoading = false,
				tabs = emptyList(),
				selectedTabKey = null,
				stations = stations,
				myTrainIndex = focusTrainIndex(stations)
					.takeIf { idx -> idx >= 0 },
				totalTrainCount = all.size,
				baseTimeText = baseTime(all),
				runningTrainNos = runningTrainNos(currentTrainNo),
				previousTrainNo = prevTrainNo,
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
		val registeredSegments = stationRegistry.segments(line)
		val segments = if (line == 2) {
			registeredSegments.filterNot { it.id == SEONGSU_BRANCH_ID } +
				registeredSegments.filter { it.id == SEONGSU_BRANCH_ID }
		} else registeredSegments
		val displayedMainIds = if (line == 1) {
			displayedStations(line, line, MAIN_SEGMENT_ID).map { it.statnId }.toSet()
		} else emptySet()
		val tabs = mutableListOf<SubwayTabUi>()
		segments.filter { line != 1 || it.id == MAIN_SEGMENT_ID }.forEach { seg ->
			if (seg.id == MAIN_SEGMENT_ID && seg.directions.isNotEmpty()) {
				seg.directions.forEach { dir ->
					tabs += SubwayTabUi(
						key = "${seg.id}:${dir.updnLine}",
						label = if (line == 1) {
							if (dir.updnLine == "0") "상선" else "하선"
						} else dir.label,
						sourceLine = line,
						segmentId = seg.id,
						updnLine = dir.updnLine,
						trainCount = all.count {
							matches(it, line, seg.id, dir.updnLine) &&
								(line != 1 || it.statnId in displayedMainIds)
						}
					)
				}
			} else {
				tabs += SubwayTabUi(
					key = seg.id,
					label = seg.title,
					sourceLine = line,
					segmentId = seg.id,
					updnLine = null,
					trainCount = all.count { matches(it, line, seg.id, null) }
				)
			}
		}
		if (line == 1) {
			stationRegistry.segment(2, SEONGSU_BRANCH_ID)?.let { seg ->
				tabs += SubwayTabUi(
					key = SEONGSU_TAB_KEY,
					label = seg.title,
					sourceLine = 2,
					segmentId = seg.id,
					updnLine = null,
					trainCount = all.count { matches(it, 2, seg.id, null) }
				)
			}
		}
		return tabs
	}

	private fun matches(dto: SubwayPositionDto, tab: SubwayTabUi): Boolean =
		matches(dto, tab.sourceLine, tab.segmentId, tab.updnLine)

	/** DTO가 해당 구간/방향에 속하는지. 2호선 분기역은 운행 구간 힌트까지 확인한다. */
	private fun matches(
		dto: SubwayPositionDto,
		line: Int,
		segmentId: String,
		updnLine: String?
	): Boolean = matchesSubwaySegment(
		dto = dto,
		segmentId = segmentId,
		updnLine = updnLine,
		stationRefs = stationRegistry.locate(line, dto.statnId)
	)

	/** 1호선 본선은 신설동 교대역을 중심으로 앞뒤 10개 역만 그린다. */
	private fun displayedStations(
		selectedLine: Int,
		sourceLine: Int,
		segmentId: String
	): List<SubwayStationDef> {
		val stations = stationRegistry.segment(sourceLine, segmentId)?.stations.orEmpty()
		if (selectedLine != 1 || sourceLine != 1 || segmentId != MAIN_SEGMENT_ID) return stations
		val changeIndex = stations.indexOfFirst { it.name == "신설동" }
		if (changeIndex < 0) return stations
		return stations.subList(
			(changeIndex - 10).coerceAtLeast(0),
			(changeIndex + 11).coerceAtMost(stations.size)
		)
	}

	/** 선택 탭의 표시 역 목록에 현재 열차를 얹는다. */
	private fun stationsForTab(
		all: List<SubwayPositionDto>,
		tab: SubwayTabUi,
		currentTrainNo: String?,
		prevTrainNo: String?
	): List<SubwayStationUi> {
		val visibleStations = displayedStations(line, tab.sourceLine, tab.segmentId)

		// statnId -> 이 탭에 표시할 열차들
		val byStation = all
			.filter { matches(it, tab) }
			.groupBy { it.statnId }

		return visibleStations.map { station ->
			SubwayStationUi(
				statnId = station.statnId,
				name = station.name,
				trains = byStation[station.statnId]
					.orEmpty()
					.map { toUi(it, currentTrainNo, prevTrainNo) }
			)
		}
	}

	/** 현재 근무 열차, 이어서 전 열번이 있는 탭을 기본 선택한다. */
	private fun defaultTabKey(
		tabs: List<SubwayTabUi>,
		selectedLine: Int,
		trackedTrain: SubwayPositionDto?
	): String {
		val hit = trackedTrain?.let { train ->
			tabs.firstOrNull { matches(train, it) }
		}
		if (hit != null) return hit.key
		return tabs
			.filter { it.sourceLine == selectedLine }
			.maxByOrNull { it.trainCount }
			?.key
			?: tabs.first().key
	}

	/** 현재 근무 열번이 없으면 주황색 전 열번 위치를 스크롤 대상으로 사용한다. */
	private fun focusTrainIndex(stations: List<SubwayStationUi>): Int =
		stations.indexOfFirst { station ->
			station.trains.any { train -> train.isMine || train.isPrevious }
		}

	private fun toUi(
		dto: SubwayPositionDto,
		currentTrainNo: String?,
		prevTrainNo: String?
	): SubwayTrainUi = SubwayTrainUi(
		dto = dto,
		// selectTrackedTrain에서 확정한 API 열번 한 건만 강조한다.
		isMine = currentTrainNo != null && dto.trainNo?.trim() == currentTrainNo.trim(),
		isPrevious = prevTrainNo != null && dto.trainNo?.trim() == prevTrainNo.trim(),
		seq = legacySeq(dto)
	)

	/** 헤더 기준시각: 기기 시계가 아니라 응답의 수신시각 최댓값(데이터 신선도). */
	private fun baseTime(all: List<SubwayPositionDto>): String =
		all.mapNotNull { it.recptnDt }
			.maxOrNull()
			?.substringAfter(' ')
			?.take(5)
			.orEmpty()

	private fun positionsForLine(all: List<SubwayPositionDto>, line: Int): List<SubwayPositionDto> {
		val subwayId = stationRegistry.line(line)?.subwayId ?: (1000 + line).toString()
		return all.filter { dto ->
			dto.subwayId == subwayId || dto.statnId?.startsWith(subwayId) == true
		}
	}

	/** 1호선 표시 구간과 성수지선은 화면에 실제로 그린 열차만 헤더에 집계한다. */
	private fun summarizeDisplayedTrains(selectedLine: Int, tab: SubwayTabUi): Boolean =
		selectedLine == 1 || (tab.sourceLine == 2 && tab.segmentId == SEONGSU_BRANCH_ID)

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
		private const val SEONGSU_BRANCH_ID = "seongsu"
		private const val SEONGSU_TAB_KEY = "2:seongsu"
	}
}
