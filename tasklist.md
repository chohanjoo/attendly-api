# 7‑Day Sprint Backlog (MVP v0.1)
**목표**: 1주일 안에 *출석 입력 → 조회*가 실제로 동작하는 최소 기능을 Docker 이미지로 배포한다.  
**팀 구성 가정**: 주니어 2 명(Backend A, Backend B) + 주니어 1 명(Infra/DevOps)  
각 작업은 *≤ 4h* 단위로 세분화했고, 병렬 처리가 가능하도록 의존성을 명시했다.

---

## 공통 기술 스택 (결정 사항)

| 레이어 | 선택 | 이유 / 대중성 |
|--------|------|--------------|
| **언어** | Kotlin 1.9.x |
| **Framework** | Spring Boot 3.2.x |
| **ORM** | Spring Data JPA + Hibernate |
| **DB** | MySQL 8.0 (Docker) |
| **마이그레이션** | Flyway |
| **시큐리티** | Spring Security + jjwt |
| **문서** | springdoc‑openapi 3 |
| **테스트** | JUnit 5, Testcontainers‑MySQL |
| **빌드** | Gradle KTS |
| **컨테이너** | Dockerfile (UBI + JDK 21) |
| **CI** | GitHub Actions |

> **프론트엔드**: 이번 7일 범위 밖 — Swagger UI 및 REST Client로 검증.

---

## 작업 분배 & 일정

### Day 0 (½일) – 킥오프
| ID | 작업 | 담당 | 산출물 |
|----|------|------|--------|
| K0‑1 | Repo 초기화, Git Flow 정의 (`main`/`develop`) | 모두 | Git repo |
| K0‑2 | Spring Initializr – 기본 모듈 생성 (web, security, data‑jpa, flyway, mysql, validation, openapi) | **A** | 빌드 통과 |

### Day 1 – DB 스키마 & 엔티티
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| D1‑1 | Flyway V1 – 스키마/테이블 (`department, village, user, gbs_group, attendance`) *히스토리·위임 제외* | **A** | K0‑2 |
| D1‑2 | 샘플 데이터 insert (1개 부서·마을·GBS, 각 역할 유저) | **A** | D1‑1 |
| D1‑3 | Kotlin @Entity 매핑 & Repository 생성 (5개) | **B** | D1‑1 |

### Day 2 – 인증/인가 (JWT)
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| S2‑1 | `Role` Enum 및 UserDetailsAdapter | **B** | D1‑3 |
| S2‑2 | SecurityConfig – JWT 필터·Provider (jjwt) | **B** | S2‑1 |
| S2‑3 | `/auth/login` → Access/Refresh Token 발급 | **B** | S2‑2 |
| S2‑4 | Postman/Swagger 테스트 케이스 | **B** | S2‑3 |

### Day 3 – 출석 입력/조회 API
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| A3‑1 | `AttendanceDto` & Validator(0‑6, O/X, A/B/C) | **A** | D1‑3 |
| A3‑2 | 리더 권한 체크 Service (`hasLeaderAccess`) | **A** | S2‑2 |
| A3‑3 | **POST** `/api/attendance` (리더 전용) | **A** | A3‑1, A3‑2 |
| A3‑4 | **GET** `/api/attendance?gbsId=&week=` | **B** | A3‑2 |
| A3‑5 | **GET** `/api/village/{id}/attendance?week=` | **B** | A3‑4 |
| A3‑6 | 단위 테스트 (Happy/Unauthorized/ValidationFail) | **A,B** | A3‑3 ~ A3‑5 |

### Day 4 – 조직별 조회 & 권한
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| O4‑1 | `OrganizationService` (캐싱) | **A** | D1‑3 |
| O4‑2 | `@PreAuthorize` (SPEL) | **B** | S2‑2, O4‑1 |
| O4‑3 | 교역자 집계 쿼리 (`COUNT(*) / total`) | **B** | A3‑4 |

### Day 5 – 문서 & 테스트컨테이너 & CI
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| T5‑1 | Testcontainers‑MySQL 설정, 통합 테스트 | **A** | A3‑3 |
| T5‑2 | springdoc‑openapi UI JWT Authorize | **B** | S2‑3 |
| C5‑3 | Dockerfile (JDK 21, JAR layering) | **Infra** | K0‑2 |
| C5‑4 | `docker-compose.yml` (MySQL 8, app) | **Infra** | C5‑3 |
| C5‑5 | GitHub Actions: build → docker push | **Infra** | C5‑3 |

### Day 6 – 검증 & 버그픽스
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| V6‑1 | QA 시나리오 실행 (E2E) | 모두 | A3‑6 |
| V6‑2 | 버그 수정 / 리팩터링 | 해당 | V6‑1 |
| V6‑3 | README : 실행 방법, API 예시 | **Infra** | V6‑1 |

### Day 7 – 릴리스
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| R7‑1 | 태그 `v0.1.0` & Docker Hub 푸시 | **Infra** | V6‑2 |
| R7‑2 | 데모(Zoom) → 기능 시연 | 모두 | R7‑1 |

---

## Definition of Done (MVP)

1. `docker-compose up` → `localhost:8080/swagger-ui.html` 접속  
2. `/auth/login` → JWT 발급  
3. 리더 계정으로 **POST /attendance** 성공  
4. 마을장·교역자 계정으로 조회 API 정상  
5. 통합 테스트 100 % 통과  
6. GitHub Actions green, Docker Hub 이미지 존재  

---

## 후순위 (Week 2+)

- 리더 **권한 위임** API  
- GBS 6개월 히스토리 & 재편성 배치  
- Excel 다운로드 (Apache POI)  
- React 프론트엔드 (Vite + TanStack Query + MUI)  
- 알림 시스템 (Slack)

---
