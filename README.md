# lily-blog-sample

소프트뱅크 해커톤 2026 — **배포 플랫폼 검증용 더미 애플리케이션**

Spring Boot 기반의 최소 블로그 CRUD 서비스. 이 앱 자체는 중요하지 않고,
**우리 배포 플랫폼이 제대로 동작하는지 확인하기 위한 표적(target)** 으로 사용한다.

## 왜 이 앱인가

플랫폼의 각 모듈이 검증해야 하는 것을 이 앱 하나로 전부 찌를 수 있도록 구성했다.

| 플랫폼 모듈 | 이 앱에서 검증하는 방법 |
|---|---|
| 원클릭 배포 | 레포 연결 → 빌드 → 실행 → `GET /api/posts` 200 |
| CI/CD (블루-그린) | `APP_COLOR`, `APP_VERSION` 주입 → `GET /version` 으로 전환 확인 |
| 카나리 | nginx `weight` 조정 → `GET /whoami` 반복 호출로 분산 비율 확인 |
| DB 마이그레이션 | Flyway `V1`, `V2` → 배포 시 자동 적용, 버전 테이블 확인 |
| 로깅 | `prod` 프로파일에서 JSON 한 줄 로그 출력 (그대로 수집 가능) |
| 롤백 | `GET /chaos/error` 로 에러율 급증 → 자동 롤백 트리거 |
| 로드밸런서 | `POST /chaos/unready` 로 readiness 차단 → LB 제외 여부 확인 |
| 모니터링 | `/actuator/prometheus` 에서 CPU·메모리·요청 지표 수집 |

## API

### 블로그 CRUD

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/posts` | 목록 (page, size 지원) |
| GET | `/api/posts/{id}` | 단건 조회 |
| POST | `/api/posts` | 생성 |
| PUT | `/api/posts/{id}` | 수정 |
| DELETE | `/api/posts/{id}` | 삭제 |

### 플랫폼 검증용

| Method | Path | 설명 |
|---|---|---|
| GET | `/version` | 버전·색상·인스턴스·기동시각 — **블루-그린 전환 확인** |
| GET | `/whoami` | 인스턴스 식별만 반환 — **LB 분산 비율 확인** |
| GET | `/chaos/error` | 500 에러 강제 발생 — **자동 롤백 트리거** |
| GET | `/chaos/slow?ms=5000` | 응답 지연 — **타임아웃 시연** |
| POST | `/chaos/unready` | readiness 차단 — **LB에서 제외되는지 확인** |
| POST | `/chaos/ready` | readiness 복구 |
| GET | `/actuator/health/readiness` | 배포 완료 판정용 프로브 |
| GET | `/actuator/health/liveness` | 재시작 판정용 프로브 |
| GET | `/actuator/prometheus` | 메트릭 수집 |

## 실행

### 로컬 (H2 인메모리)

```bash
gradle wrapper          # 최초 1회, gradlew 생성
./gradlew bootRun
curl http://localhost:8080/api/posts
```

### Docker Compose (Postgres + 블루/그린 2대 + nginx)

```bash
docker compose up --build
curl http://localhost:8080/api/posts

# LB 분산 확인 — blue 9 : green 1 로 섞여 나와야 함
for i in $(seq 1 20); do curl -s localhost:8080/whoami; echo; done

# 장애 주입 → 롤백 시연
curl -s localhost:8080/chaos/error
```

## 환경변수

| 이름 | 기본값 | 용도 |
|---|---|---|
| `APP_VERSION` | `dev` | 배포 버전 식별 |
| `APP_COLOR` | `blue` | 블루-그린 슬롯 |
| `SPRING_PROFILES_ACTIVE` | `local` | `local`(H2) / `prod`(Postgres) |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | — | prod 프로파일 DB 접속 |
| `SERVER_PORT` | `8080` | 포트 |

## 참고

- graceful shutdown 20초 설정 — 블루-그린 전환 시 인플라이트 요청 보호
- `ddl-auto: validate` — 스키마는 Flyway만 변경. 마이그레이션 모듈 검증 목적
