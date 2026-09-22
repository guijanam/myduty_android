package com.sonbum.diacalendar2.data.repository

import android.util.Log
import com.sonbum.diacalendar2.data.local.SubwayStationRegistry
import com.sonbum.diacalendar2.data.remote.SubwayApiConfig
import com.sonbum.diacalendar2.data.remote.api.SeoulMetroTrainApi
import com.sonbum.diacalendar2.data.remote.api.SubwayApi
import com.sonbum.diacalendar2.data.remote.dto.SubwayPositionDto
import com.sonbum.diacalendar2.data.remote.parser.SeoulMetroTrainParser
import com.sonbum.diacalendar2.domain.repository.SubwayRepository
import com.sonbum.diacalendar2.domain.util.SubwayTrainParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class SubwayRepositoryImpl(
    private val api: SubwayApi,
    private val seoulMetroApi: SeoulMetroTrainApi,
    private val stationRegistry: SubwayStationRegistry
) : SubwayRepository {

    override suspend fun getLinePositions(line: Int): Result<List<SubwayPositionDto>> =
        coroutineScope {
            val seoulMetroDeferred = async { getSeoulMetroPositions(line) }
            val topisResult = try {
                Result.success(getTopisPositions(line))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            val seoulMetroPositions = seoulMetroDeferred.await()

            topisResult.fold(
                onSuccess = { topisPositions ->
                    Result.success(mergeLinePositions(topisPositions, seoulMetroPositions))
                },
                onFailure = { error ->
                    if (seoulMetroPositions.isNotEmpty()) {
                        Result.success(seoulMetroPositions)
                    } else {
                        Result.failure(error)
                    }
                }
            )
        }

    private suspend fun getTopisPositions(line: Int): List<SubwayPositionDto> {
        val resp = api.getRealtimePosition(
            apiKey = SubwayApiConfig.API_KEY,
            count = SubwayApiConfig.DEFAULT_COUNT,
            lineSeg = "${line}호선"
        )
        // HTTP 200이어도 errorMessage.code로 논리 오류(데이터 없음 등)를 표현.
        // 비정상 코드는 빈 리스트로 처리해 화면에서 "운행 중 아님" 빈 상태를 보이게 한다.
        val code = resp.errorMessage?.code
        return if (code != null && code != "INFO-000") {
            emptyList()
        } else {
            repairUnmappedStationIds(
                positions = latestPositionPerTrain(resp.realtimePositionList.orEmpty()),
                isMapped = { stationId -> stationRegistry.locate(line, stationId).isNotEmpty() },
                stationIdForName = { stationName ->
                    stationRegistry.stationIdForName(line, stationName)
                }
            )
        }
    }

    private suspend fun getSeoulMetroPositions(line: Int): List<SubwayPositionDto> {
        if (line !in SEOUL_METRO_LINES) return emptyList()
        return try {
            val html = seoulMetroApi.getTrainMap(line).string()
            SeoulMetroTrainParser.parse(html, line) { stationName ->
                stationRegistry.stationIdForName(line, stationName)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "서울교통공사 실시간 열차정보 병합 실패: line=$line", e)
            emptyList()
        }
    }

    private companion object {
        val SEOUL_METRO_LINES = 1..8
        const val TAG = "SubwayRepository"
    }
}

/**
 * TOPIS 열번은 첫 자리가 불규칙한 경우가 있어 기존 운용 규칙대로 뒤 3자리로 병합한다.
 * 양쪽에 있는 열차는 서울교통공사의 위치/열번/상태를 사용하되 TOPIS 전용 메타데이터는 보존한다.
 * 2호선은 본선·성수지선·신정지선에서 같은 열번을 동시에 사용할 수 있으므로
 * 서울교통공사 위치는 routeSegmentId까지 구분해 각 운행 구간별로 보존한다.
 */
internal fun mergeLinePositions(
    topisPositions: List<SubwayPositionDto>,
    seoulMetroPositions: List<SubwayPositionDto>
): List<SubwayPositionDto> {
    val topisByKey = linkedMapOf<Pair<String?, String>, SubwayPositionDto>()
    val unidentifiedTopis = mutableListOf<SubwayPositionDto>()

    topisPositions.forEach { position ->
        val key = position.mergeKey()
        if (key == null) unidentifiedTopis += position else topisByKey[key] = position
    }

    val metroByRouteKey = linkedMapOf<Triple<String?, String, String?>, SubwayPositionDto>()
    val unidentifiedMetro = mutableListOf<SubwayPositionDto>()
    seoulMetroPositions.forEach { metro ->
        val routeKey = metro.routeAwareMergeKey()
        if (routeKey == null) {
            unidentifiedMetro += metro
        } else {
            metroByRouteKey[routeKey] = metro
        }
    }

    val metroBaseKeys = metroByRouteKey.values.mapNotNull { it.mergeKey() }.toSet()
    val result = topisByKey
        .filterKeys { it !in metroBaseKeys }
        .values
        .toMutableList()

    metroByRouteKey.values.forEach { metro ->
        val topis = metro.mergeKey()?.let(topisByKey::get)
        result += if (topis == null) {
            metro
        } else {
            metro.copy(
                directAt = topis.directAt,
                lstcarAt = topis.lstcarAt,
                recptnDt = topis.recptnDt
            )
        }
    }

    return result + unidentifiedTopis + unidentifiedMetro
}

private fun SubwayPositionDto.mergeKey(): Pair<String?, String>? {
    val number = trainNo?.trim().orEmpty()
    if (number.isEmpty()) return null
    return subwayId to SubwayTrainParser.matchKey(number)
}

private fun SubwayPositionDto.routeAwareMergeKey(): Triple<String?, String, String?>? {
    val base = mergeKey() ?: return null
    return Triple(base.first, base.second, routeSegmentId)
}

/** TOPIS가 잘못되거나 과거의 statnId를 반환하면 같은 호선의 역명으로 복구한다. */
internal fun repairUnmappedStationIds(
    positions: List<SubwayPositionDto>,
    isMapped: (String?) -> Boolean,
    stationIdForName: (String?) -> String?
): List<SubwayPositionDto> = positions.map { position ->
    if (isMapped(position.statnId)) {
        position
    } else {
        stationIdForName(position.statnNm)
            ?.let { repairedId -> position.copy(statnId = repairedId) }
            ?: position
    }
}

/**
 * TOPIS가 같은 열차의 최신 위치와 이전 위치(또는 완전 중복)를 함께 반환하는 경우가 있어,
 * 호선 ID와 열번이 같은 항목은 수신시각이 가장 최신인 한 건만 남긴다.
 * 열번이 없는 비정상 항목은 서로 다른 열차일 수 있으므로 임의로 합치지 않는다.
 */
internal fun latestPositionPerTrain(
    positions: List<SubwayPositionDto>
): List<SubwayPositionDto> {
    data class IndexedPosition(
        val firstIndex: Int,
        val position: SubwayPositionDto
    )

    val latestByTrain = linkedMapOf<Pair<String?, String>, IndexedPosition>()
    val unidentified = mutableListOf<IndexedPosition>()

    positions.forEachIndexed { index, position ->
        val trainNo = position.trainNo?.trim().orEmpty()
        if (trainNo.isEmpty()) {
            unidentified += IndexedPosition(index, position)
            return@forEachIndexed
        }

        val key = position.subwayId to trainNo
        val existing = latestByTrain[key]
        if (existing == null) {
            latestByTrain[key] = IndexedPosition(index, position)
        } else if (position.recptnDt.orEmpty() > existing.position.recptnDt.orEmpty()) {
            latestByTrain[key] = existing.copy(position = position)
        }
    }

    return (latestByTrain.values + unidentified)
        .sortedBy { it.firstIndex }
        .map { it.position }
}
