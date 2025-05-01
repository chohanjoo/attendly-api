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

### Day 8 - 조직 구조 히스토리 & 위임 기능
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| H8-2 | 위임 테이블 추가 (`leader_delegation`) | **A** | H8-1 |
| H8-3 | 히스토리 Entity & Repository 구현 | **B** | H8-1 |
| H8-4 | 위임 관련 API 구현 (`/api/delegations`) | **B** | H8-2, H8-3 |
| H8-5 | 위임 관련 단위/통합 테스트 | **B** | H8-4 |

### Day 9 - 통계 & 리포트
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| R9-1 | 통계 Service 구현 (주간/월간 출석률, 큐티 평균) | **A** | O4-1 |
| R9-2 | Excel/CSV 다운로드 기능 구현 (Apache POI) | **A** | R9-1 |
| R9-3 | 미출석 알람 API 구현 | **B** | R9-1 |
| R9-4 | 통계 API 단위 테스트 | **A** | R9-1, R9-2 |
| R9-5 | 통계 데이터 캐싱 구현 (Redis) | **B** | R9-1 |

### Day 10 - 감사 로그 & 보안 강화
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| L10-1 | Audit Trail 테이블 & Entity 구현 | **A** | H8-1 |
| L10-2 | AOP를 통한 감사 로그 자동화 | **A** | L10-1 |
| L10-3 | HTTPS 설정 & 보안 강화 | **Infra** | - |
| L10-4 | OWASP Top-10 체크리스트 검증 | **Infra** | L10-3 |
| L10-5 | 성능 테스트 (JMeter - 300 동시 사용자) | **Infra** | L10-3 |

### Day 11 - 관리자 기능 구현
| ID | 작업 | 담당 | 의존성 |
|----|------|------|--------|
| A11-1 | 관리자 전용 API 엔드포인트 구현 (`/api/admin/**`) | **A** | - |
| A11-2 | 사용자 관리 CRUD API 구현 | **A** | A11-1 |
| A11-3 | 조직 구조 관리 API 구현 (부서/마을/GBS) | **A** | A11-2 |
| A11-4 | 6개월 주기 배치 작업 구현 | **B** | A11-3 |
| A11-5 | 관리자 대시보드 API 구현 | **B** | A11-4 |
| A11-6 | 시스템 설정 관리 API (알림 설정, 보안 정책 등) | **B** | A11-1 |
| A11-7 | 배치 작업 모니터링 & 로그 API | **Infra** | A11-4 |
| A11-8 | 관리자 API 통합 테스트 | **A** | A11-1 ~ A11-7 |

## 관리자 기능 상세 명세

1. **사용자 관리**
   - 계정 생성/수정/삭제/잠금
   - 역할 부여 및 권한 관리
   - 비밀번호 초기화
   - 사용자 일괄 등록 (CSV 임포트)

2. **조직 구조 관리**
   - 부서/마을/GBS 생성 및 구조 변경
   - 리더/조원 배치 관리
   - 6개월 주기 재편성 배치 작업
   - 조직도 히스토리 관리

3. **시스템 설정**
   - 알림 설정 (이메일/Slack 웹훅)
   - 보안 정책 (비밀번호 정책, 세션 타임아웃)
   - 출석 입력 기간 설정
   - 배치 작업 스케줄링

4. **모니터링 & 로그**
   - 배치 작업 실행 현황
   - 시스템 로그 조회
   - 사용자 접근 로그
   - 데이터 변경 이력

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

## 추가 개선사항 (Week 3+)

1. **모바일 PWA 지원**
   - 오프라인 모드 구현
   - Service Worker 설정
   - IndexedDB 동기화

2. **알림 시스템 고도화**
   - Email 템플릿 구현
   - Slack 웹훅 연동
   - SMS 서비스 연동

3. **SSO 통합**
   - OAuth2/OIDC 설정
   - 교회 계정 연동 flow

4. **모니터링 & 대시보드**
   - Prometheus + Grafana 설정
   - 실시간 WebSocket 통계
   - 커스텀 메트릭 추가

5. **데이터 백업 & 복구**
   - MySQL PITR 설정
   - S3 자동 백업
   - 복구 프로세스 문서화

---
