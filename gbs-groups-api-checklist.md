# GBS 그룹 조회 API 개발 체크리스트

## 🎯 목표
- GET /api/admin/organization/gbs-groups API 개발
- 관리자 페이지에서 모든 GBS 그룹 조회 기능 구현

## 📋 체크리스트

### 1. 사전 조사
- [x] 기존 GBS 관련 엔티티 및 DTO 확인
- [x] AdminOrganizationService 현재 메서드 확인
- [x] 응답 형태 확인 (GbsGroup 인터페이스)

### 2. DTO 작성
- [x] AdminGbsGroupListResponse DTO 생성 (요구사항에 맞게)
- [x] PageResponse 형태로 응답 구성

### 3. Repository 레이어
- [x] GbsGroupRepository에 필요한 쿼리 메서드 확인/추가
- [x] Querydsl을 사용한 복잡한 조회 로직 구현

### 4. Service 레이어
- [x] AdminOrganizationService에 getAllGbsGroups 메서드 추가
- [x] memberCount 계산 로직 포함

### 5. Controller 레이어
- [x] AdminOrganizationController에 GET /gbs-groups 엔드포인트 추가
- [x] 페이징 및 정렬 파라미터 추가
- [x] Swagger 문서화

### 6. 테스트 코드
- [x] AdminOrganizationServiceTest에 getAllGbsGroups 테스트 추가
- [x] AdminOrganizationControllerTest에 GET /gbs-groups 테스트 추가
- [ ] Repository 테스트 필요시 추가

### 7. 실행 및 검증
- [ ] 애플리케이션 빌드 및 실행
- [ ] API 호출 테스트
- [ ] 응답 형태 검증

## 🔍 요구사항 분석
```typescript
interface GbsGroup {
  id: number
  name: string
  villageId: number
  villageName: string
  termStartDate: string
  termEndDate: string
  leaderId?: number
  leaderName?: string
  createdAt: string
  updatedAt: string
  memberCount: number
}
```

## 🎯 주요 고려사항
- @springboot.mdc 규칙 준수
- SOLID, DRY, KISS, YAGNI 원칙 적용
- Querydsl 사용 (Query 어노테이션 사용 금지)
- MockK를 사용한 단위 테스트 작성
- 파라미터 2개 이상 시 DTO 클래스 사용 