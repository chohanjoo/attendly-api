# Attendly API 문서

## 목차
1. [인증 API](#인증-api)
2. [출석 API](#출석-api)
3. [통계 API](#통계-api)
4. [리더 위임 API](#리더-위임-api)
5. [관리자 API](#관리자-api)
   - [사용자 관리](#관리자-사용자-관리)
   - [조직 구조 관리](#관리자-조직-구조-관리)
   - [시스템 설정](#관리자-시스템-설정)
   - [배치 작업](#관리자-배치-작업)
   - [로그 관리](#관리자-로그-관리)

## 인증 API
Base URL: `/auth`

| Method | Endpoint | 설명 | 접근 권한 |
|--------|----------|------|-----------|
| POST | `/signup` | 회원가입 | 모든 사용자 |
| POST | `/login` | 로그인 (JWT 토큰 발급) | 모든 사용자 |
| POST | `/refresh` | 토큰 갱신 | 인증된 사용자 |

## 출석 API
Base URL: `/api`

| Method | Endpoint | 설명 | 접근 권한 |
|--------|----------|------|-----------|
| POST | `/attendance` | 출석 일괄 등록 | 리더 |
| GET | `/attendance` | GBS 출석 조회 | 인증된 사용자 |
| GET | `/village/{id}/attendance` | 마을 출석 현황 조회 | 마을 접근 권한 |

## 통계 API
Base URL: `/api`

| Method | Endpoint | 설명 | 접근 권한 |
|--------|----------|------|-----------|
| GET | `/departments/{id}/report` | 부서 출석 통계 조회 | 목회자/관리자 |
| GET | `/villages/{id}/report` | 마을 출석 통계 조회 | 마을 접근 권한 |
| GET | `/gbs/{id}/report` | GBS 출석 통계 조회 | GBS 관리 권한/목회자/관리자 |

## 리더 위임 API
Base URL: `/api/delegations`

| Method | Endpoint | 설명 | 접근 권한 |
|--------|----------|------|-----------|
| POST | `/` | 리더 권한 위임 생성 | 리더 |
| GET | `/active` | 활성화된 위임 권한 조회 | 인증된 사용자 |

## 관리자 API

### 관리자-사용자 관리
Base URL: `/api/admin/users`

| Method | Endpoint | 설명 | 접근 권한 |
|--------|----------|------|-----------|
| POST | `/` | 사용자 생성 | 관리자 |
| PUT | `/{userId}` | 사용자 수정 | 관리자 |
| DELETE | `/{userId}` | 사용자 삭제 | 관리자 |
| GET | `/{userId}` | 사용자 조회 | 관리자 |
| GET | `/` | 사용자 목록 조회 | 관리자 |
| POST | `/{userId}/reset-password` | 비밀번호 초기화 | 관리자 |
| POST | `/bulk` | 사용자 일괄 등록 | 관리자 |

### 관리자-조직 구조 관리
Base URL: `/api/admin/organization`

| Method | Endpoint | 설명 | 접근 권한 |
|--------|----------|------|-----------|
| POST | `/departments` | 부서 생성 | 관리자 |
| PUT | `/departments/{departmentId}` | 부서 수정 | 관리자 |
| DELETE | `/departments/{departmentId}` | 부서 삭제 | 관리자 |
| GET | `/departments/{departmentId}` | 부서 조회 | 관리자 |
| GET | `/departments` | 부서 목록 조회 | 관리자 |
| POST | `/villages` | 마을 생성 | 관리자 |
| PUT | `/villages/{villageId}` | 마을 수정 | 관리자 |
| POST | `/gbs-groups` | GBS 그룹 생성 | 관리자 |
| PUT | `/gbs-groups/{gbsGroupId}` | GBS 그룹 수정 | 관리자 |
| POST | `/gbs-groups/{gbsGroupId}/leaders` | GBS 그룹에 리더 배정 | 관리자 |
| POST | `/gbs-groups/{gbsGroupId}/members` | GBS 그룹에 조원 배정 | 관리자 |
| POST | `/reorganization` | GBS 6개월 주기 재편성 실행 | 관리자 |

### 관리자-시스템 설정
Base URL: `/api/admin/system`

| Method | Endpoint | 설명 | 접근 권한 |
|--------|----------|------|-----------|
| POST | `/settings` | 시스템 설정 저장 | 관리자 |
| DELETE | `/settings/{key}` | 시스템 설정 삭제 | 관리자 |
| GET | `/settings/{key}` | 시스템 설정 조회 | 관리자 |
| GET | `/settings` | 모든 시스템 설정 조회 | 관리자 |
| POST | `/email-settings` | 이메일 설정 저장 | 관리자 |
| GET | `/email-settings` | 이메일 설정 조회 | 관리자 |
| POST | `/slack-settings` | Slack 설정 저장 | 관리자 |
| POST | `/security-policies` | 보안 정책 설정 저장 | 관리자 |
| POST | `/attendance-settings` | 출석 입력 설정 저장 | 관리자 |
| POST | `/batch-settings` | 배치 작업 설정 저장 | 관리자 |

### 관리자-배치 작업
Base URL: `/api/admin/batch`

| Method | Endpoint | 설명 | 접근 권한 |
|--------|----------|------|-----------|
| POST | `/jobs` | 배치 작업 생성 | 관리자 |
| POST | `/jobs/{jobId}/cancel` | 배치 작업 취소 | 관리자 |
| POST | `/jobs/{jobId}/restart` | 배치 작업 재시작 | 관리자 |
| GET | `/jobs/{jobId}` | 배치 작업 조회 | 관리자 |
| GET | `/jobs` | 배치 작업 목록 조회 | 관리자 |
| GET | `/jobs/{jobId}/logs` | 배치 작업 로그 조회 | 관리자 |

### 관리자-로그 관리
Base URL: `/api/admin/logs`

| Method | Endpoint | 설명 | 접근 권한 |
|--------|----------|------|-----------|
| GET | `/` | 시스템 로그 조회 | 관리자 |
| GET | `/{id}` | 시스템 로그 단건 조회 | 관리자 |
| GET | `/categories` | 로그 카테고리 목록 조회 | 관리자 |
| GET | `/levels` | 로그 레벨 목록 조회 | 관리자 | 