# lily-blog-sample

**배포 플랫폼 검증용 더미 앱** · 소프트뱅크 해커톤 2026 · Team Lily

우리 배포 플랫폼(Vercel 같은 서비스)이 제대로 동작하는지 확인하기 위한 **배포 대상 앱**이다.
간단한 블로그 CRUD API지만, 기능보다는 **플랫폼을 시험할 수 있는 엔드포인트**에 초점을 맞췄다.

## 이 레포의 범위

| 이 레포에 있는 것 | 이 레포에 없는 것 (플랫폼 모듈 담당) |
|---|---|
| 앱 소스 코드, DB 마이그레이션 | CI/CD 파이프라인 |
| `Dockerfile` (빌드 방법) | 블루-그린 / 카나리 배포 구성 |
| 헬스체크·메트릭·장애 주입 엔드포인트 | 로드밸런서, 프록시 설정 |
| | 인프라, 모니터링 설정 |

> 앱 레포는 최대한 단순하게 유지한다. 배포 성공·실패는 모두 **플랫폼의 동작**으로 판단해야 하기 때문이다.

## 플랫폼 기능별 검증 방법

| 플랫폼 기능 | 검증 방법 |
|---|---|
| 배포 | 레포 연결 → 빌드 → 실행 → `GET /api/posts` 가 200인지 확인 |
| 배포 완료 판정 | `GET /actuator/health/readiness` 가 `UP`이 되면 트래픽 전환 |
| 블루-그린 | `APP_COLOR`, `APP_VERSION` 주입 → `GET /version` 으로 전환 확인 |
| 카나리 / LB 분산 | 트래픽 가중치 조정 → `GET /whoami` 반복 호출로 비율 확인 |
| LB 인스턴스 제외 | `POST /chaos/unready` → 해당 인스턴스가 LB에서 빠지는지 확인 |
| 자동 롤백 | `GET /chaos/error` 반복 호출로 에러율 급증 → 롤백 트리거 |
| 타임아웃 | `GET /chaos/slow?ms=10000` |
| DB 마이그레이션 | 배포 시 Flyway `V1`, `V2` 자동 적용 |
| 로그 수집 | `prod` 프로파일에서 JSON 한 줄 로그 출력 (`app`, `version`, `color` 필드 포함) |
| 모니터링 | `GET /actuator/prometheus` 로 메트릭 수집 |
| 무중단 종료 | graceful shutdown 20초. 전환 중 처리 중인 요청이 끊기지 않는지 확인 |

## API

### 플랫폼 검증용

| Method | Path | 설명 |
|---|---|---|
| GET | `/version` | 버전, 색상, 인스턴스, 기동 시각, readiness 상태 |
| GET | `/whoami` | 응답한 인스턴스와 색상 |
| GET | `/chaos/error` | 500 에러 강제 발생 |
| GET | `/chaos/slow?ms=5000` | 지정한 시간만큼 응답 지연 |
| POST | `/chaos/unready` | readiness를 `REFUSING_TRAFFIC`으로 전환 |
| POST | `/chaos/ready` | readiness 복구 |
| GET | `/actuator/health/readiness` | 트래픽 수신 가능 여부 |
| GET | `/actuator/health/liveness` | 재시작 필요 여부 |
| GET | `/actuator/prometheus` | Prometheus 메트릭 |

### 블로그 CRUD

| Method | Path | 설명 |
|---|---|---|
| GET | `/api/posts?page=0&size=20` | 목록 |
| GET | `/api/posts/{id}` | 단건 조회 |
| POST | `/api/posts` | 생성 |
| PUT | `/api/posts/{id}` | 수정 |
| DELETE | `/api/posts/{id}` | 삭제 |

## 플랫폼 연동 규칙

플랫폼이 이 앱을 배포할 때 기준으로 삼는 값이다.

| 항목 | 값 |
|---|---|
| 빌드 | 루트의 `Dockerfile` |
| 포트 | `8080` (`SERVER_PORT` 로 변경 가능) |
| readiness 프로브 | `/actuator/health/readiness` |
| liveness 프로브 | `/actuator/health/liveness` |
| 종료 | `SIGTERM` 을 받으면 최대 20초 동안 처리 중인 요청을 마친 뒤 종료 |

### 환경변수

| 이름 | 기본값 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` (이미지에서는 `prod`) | `local`: H2 인메모리 / `prod`: Postgres + JSON 로그 |
| `APP_VERSION` | `dev` | 배포 버전 식별자 |
| `APP_COLOR` | `blue` | 블루-그린 슬롯 이름 |
| `SERVER_PORT` | `8080` | 서버 포트 |
| `DB_URL` | `jdbc:postgresql://localhost:5432/blog` | `prod` 전용 |
| `DB_USERNAME` / `DB_PASSWORD` | `blog` / `blog` | `prod` 전용 |
| `DB_POOL_SIZE` | `10` | `prod` 전용 |

## 로컬 실행

필요한 것: JDK 21, Gradle 8.12 (또는 Docker)

```bash
# Gradle (H2 인메모리 DB, 별도 DB 필요 없음)
gradle bootRun

# Docker
docker build -t lily-blog-sample .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local lily-blog-sample

# 확인
curl localhost:8080/version
curl localhost:8080/api/posts
```

## 기술 스택

Java 21 · Spring Boot 3.4 · Spring Data JPA · Flyway · H2 / PostgreSQL · Actuator + Micrometer · Logstash Logback Encoder
