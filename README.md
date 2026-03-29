# optimal-loads-secom

BlueMap SECOM Wrapper Server  
**Optimal-LOADS 프로젝트** - NOAA GFS 기상 데이터를 SECOM 표준으로 제공

---

## 구조

```
optimal-loads-secom/
├── pom.xml
└── src/main/java/dev/bluemap/secom/
    ├── SecomApplication.java          # Spring Boot 메인
    ├── controller/
    │   ├── PingController.java        # GET /v1/ping
    │   ├── CapabilityController.java  # GET /v1/capability
    │   └── GetController.java         # GET /v1/object  ← 핵심
    ├── service/
    │   └── WeatherApiService.java     # 기상 API 호출
    └── provider/
        └── SecomSignatureProviderImpl.java  # 서명 (로컬: 더미)
```

---

## 엔드포인트

| 메서드 | 경로 | 역할 |
|--------|------|------|
| GET | `/api/secom/v1/ping` | 상태 확인 (MSR 모니터링용) |
| GET | `/api/secom/v1/capability` | 서비스 기능 선언 |
| GET | `/api/secom/v1/object` | 기상 데이터 조회 |

---

## 실행 방법

### 사전 요건
- Java 17+
- Maven 3.8+

### 빌드 & 실행
```bash
cd optimal-loads-secom
mvn spring-boot:run
```

### 테스트
```bash
# Ping 테스트
curl http://localhost:8766/api/secom/v1/ping

# Capability 확인
curl http://localhost:8766/api/secom/v1/capability

# 기상 데이터 조회 (부산 인근)
curl "http://localhost:8766/api/secom/v1/object?geometry=POINT(129.0%2035.0)"
```

---

## TODO (단계별)

### Phase 1 - 로컬 테스트 (지금)
- [x] 기본 SECOM 엔드포인트 구현
- [x] 기상 API 연결
- [ ] 로컬 실행 확인
- [ ] geometry 파싱 개선 (POLYGON 지원)

### Phase 2 - MSR 등록 준비
- [ ] KRISO MCP에서 MRN 발급
- [ ] 인증서 다운로드
- [ ] `application.properties`에 keystore 설정
- [ ] `SecomSignatureProviderImpl` 실제 서명 로직 구현

### Phase 3 - 배포 & MSR 등록
- [ ] AWS 서버 배포
- [ ] KRISO MSR에 서비스 인스턴스 등록
- [ ] MSR ping 모니터링 확인

---

## 기상 API 연결 정보

기존 서버: `http://52.78.244.211/api/griddata`

파라미터 예시:
```
source=noaa
dataset_code=original
model=gfs
variable=DIRPW      # 파도 방향
run_time_utc=2025-07-01T00:00:00Z
step_hours=0
lat=35.0
lon=129.0
buffer_km=50.0
```

지원 variable 목록 (추후 확장):
- `DIRPW`: 파도 방향
- `HTSGW`: 유의파고
- `PERPW`: 파주기
- `UGRD`: U방향 풍속
- `VGRD`: V방향 풍속
