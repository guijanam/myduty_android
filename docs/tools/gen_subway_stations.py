#!/usr/bin/env python3
"""
app/src/main/assets/subway_stations.json 생성 스크립트.

서울 열린데이터광장 SearchSTNBySubwayLineInfo API에서 1~9호선 전 역 목록을 받아
실시간 위치 API(realtimePosition)의 statnId로 조회 가능한 노선도 asset을 만든다.

역 신설/개명 시에만 재실행하면 된다(런타임에는 호출하지 않음).

    python3 docs/tools/gen_subway_stations.py

핵심 규칙 — statnId -> FR_CODE 역변환:
    statnId = '1' + subwayId(3) + block(2) + code(4)
    block '00' = 본선, '08' = 지선(1호선 경부/장항, 5호선 마천)

    block '08'              -> 'P{code}'          (P148 등)
    2호선 & code 4자리      -> '211-2' 형태        (성수/신정지선)
    그 외                   -> '{code}'
"""
import json
import urllib.request
import re
import os

API_KEY = "595a517963646576333041576d556d"
MASTER_URL = (
    "http://openapi.seoul.go.kr:8088/"
    f"{API_KEY}/json/SearchSTNBySubwayLineInfo/1/900/"
)

OUT = os.path.join(
    os.path.dirname(__file__), "..", "..",
    "app", "src", "main", "assets", "subway_stations.json",
)

# 호선별 표준 노선 색상 (서울교통공사 CI)
LINE_COLORS = {
    1: "#0052A4", 2: "#00A84D", 3: "#EF7C1C", 4: "#00A5DE", 5: "#996CAC",
    6: "#CD7C2F", 7: "#747F00", 8: "#E6186C", 9: "#BDB092",
}

# 지선 정의: (라인, 분기 major코드 또는 'P') -> (segment id, 표시명)
BRANCH_META = {
    (1, "P"):   ("gyeongbu",  "경부/장항선"),
    (1, "100"): ("gyeongwon", "연천방면"),
    (2, "211"): ("seongsu",   "성수지선"),
    (2, "234"): ("sinjeong",  "신정지선"),
    (5, "P"):   ("macheon",   "마천지선"),
}

FR_RE = re.compile(r"^(P?)(\d+)(?:-(\d+))?$")


def parse_fr(fr):
    """FR_CODE -> (P접두사, major, minor). 정렬 및 지선 판별용."""
    m = FR_RE.match(fr)
    if not m:
        return None
    return m.group(1), int(m.group(2)), int(m.group(3) or 0)


def statn_id(line, fr):
    """FR_CODE -> statnId(10자리). realtimePosition 응답과 조인되는 키."""
    p, major, minor = parse_fr(fr)
    # subwayId 자체가 이미 '1002' 형태(선두 1 포함)라 접두사를 덧붙이지 않는다.
    subway_id = f"{1000 + line}"
    if p == "P":                       # 1·5호선 지선
        # 일반 지선역: 08 + 4자리   (P157 병점 -> 1001080157)
        # 지선의 재지선: 8 + major + minor (P157-1 서동탄 -> 1001801571)
        if minor:
            return f"{subway_id}80{major}{minor}"
        return f"{subway_id}08{major:04d}"
    if minor:                          # 2호선 지선: 211-2 -> 2112
        return f"{subway_id}00{major}{minor}"
    return f"{subway_id}00{major:04d}"


def directions(line, seg_id):
    """구간별 방향 라벨. 2호선 본선만 내선/외선."""
    if line == 2 and seg_id == "main":
        return [
            {"updnLine": "0", "label": "내선순환"},
            {"updnLine": "1", "label": "외선순환"},
        ]
    return [
        {"updnLine": "0", "label": "상행"},
        {"updnLine": "1", "label": "하행"},
    ]


def main():
    with urllib.request.urlopen(MASTER_URL, timeout=30) as r:
        payload = json.load(r)
    rows = payload["SearchSTNBySubwayLineInfo"]["row"]

    lines_out = []
    for line in range(1, 10):
        rows_l = [r for r in rows if r["LINE_NUM"] == f"{line:02d}호선"]
        if not rows_l:
            continue

        # 본선 / 지선 분류
        buckets = {}
        for r in rows_l:
            parsed = parse_fr(r["FR_CODE"])
            if not parsed:
                print(f"  ! FR_CODE 파싱 실패, 건너뜀: {r}")
                continue
            p, major, minor = parsed
            if p == "P":
                key = "P"
            elif minor:
                key = str(major)
            else:
                key = "MAIN"
            buckets.setdefault(key, []).append((major, minor, r))

        segments = []
        for key in sorted(buckets, key=lambda k: (k != "MAIN", k)):
            entries = sorted(buckets[key], key=lambda t: (t[0], t[1]))
            if key == "MAIN":
                seg_id, title = "main", "본선"
            else:
                seg_id, title = BRANCH_META.get(
                    (line, key), (f"branch{key}", f"{key}지선")
                )
            stations = [
                {"statnId": statn_id(line, r["FR_CODE"]), "name": r["STATION_NM"]}
                for _, _, r in entries
            ]
            # 지선은 본선 분기역을 머리에 붙여 연결을 드러낸다.
            if key not in ("MAIN", "P"):
                junction = next(
                    (r for _, _, r in buckets.get("MAIN", []) if r["FR_CODE"] == key),
                    None,
                )
                if junction:
                    stations.insert(0, {
                        "statnId": statn_id(line, junction["FR_CODE"]),
                        "name": junction["STATION_NM"],
                    })
            segments.append({
                "id": seg_id,
                "title": title,
                "loop": line == 2 and seg_id == "main",
                "directions": directions(line, seg_id),
                "stations": stations,
            })

        lines_out.append({
            "line": line,
            "subwayId": f"{1000 + line}",
            "name": f"{line}호선",
            "colorHex": LINE_COLORS[line],
            "segments": segments,
        })
        print(f"line{line}: " + ", ".join(
            f"{s['id']}={len(s['stations'])}" for s in lines_out[-1]["segments"]))

    asset = {"version": 1, "lines": lines_out}
    out = os.path.normpath(OUT)
    with open(out, "w", encoding="utf-8") as f:
        json.dump(asset, f, ensure_ascii=False, indent=1)
    print(f"\nwrote {out} ({os.path.getsize(out)} bytes)")


if __name__ == "__main__":
    main()
