package com.sonbum.diacalendar2.data.remote

/**
 * 서울 열린데이터 광장 지하철 실시간 위치 API 설정.
 * HTTP(cleartext) 엔드포인트 — 매니페스트에 usesCleartextTraffic 이미 허용됨.
 */
object SubwayApiConfig {
    const val BASE_URL = "http://swopenAPI.seoul.go.kr/"
    const val API_KEY = "595a517963646576333041576d556d"
    // 1호선 실측 80대. total이 이 값을 넘으면 열차가 조용히 누락되므로 여유를 둔다.
    const val DEFAULT_COUNT = 200
}
