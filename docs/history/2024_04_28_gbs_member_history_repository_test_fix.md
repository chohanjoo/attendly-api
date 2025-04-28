# GbsMemberHistory Repository 테스트 오류 수정

## 발생 일시
- 2024년 4월 28일

## 문제 상황
GbsMemberHistoryRepositoryCustom 인터페이스의 테스트 코드 실행 시 다음과 같은 오류가 발생했습니다:

1. `NoSuchBeanDefinitionException` 오류
   - 원인: QueryDSL의 `JPAQueryFactory` 빈이 테스트 환경에서 제공되지 않음
   - 영향: 모든 테스트 케이스 실패

2. `findByGbsGroupIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual` 메서드 테스트 실패
   - 원인: 기간 조회 조건이 잘못 설정됨
   - 영향: 특정 기간 동안의 활성 멤버 조회 결과가 부정확함

## 해결 방법

### 1. JPAQueryFactory 빈 등록 문제
1. `@Repository` 어노테이션 추가
   ```kotlin
   @Repository
   class GbsMemberHistoryRepositoryImpl(
       private val queryFactory: JPAQueryFactory
   )
   ```

2. 테스트 설정 클래스 추가
   ```kotlin
   @Import(GbsMemberHistoryRepositoryCustomTest.TestConfig::class)
   class GbsMemberHistoryRepositoryCustomTest {
       class TestConfig {
           @Bean
           fun jpaQueryFactory(entityManager: EntityManager): JPAQueryFactory {
               return JPAQueryFactory(entityManager)
           }
       }
   }
   ```

### 2. 기간 조회 조건 수정
1. 수정 전 코드
   ```kotlin
   gbsMemberHistory.startDate.loe(startDate),
   gbsMemberHistory.endDate.isNull.or(gbsMemberHistory.endDate.goe(endDate))
   ```

2. 수정 후 코드
   ```kotlin
   gbsMemberHistory.startDate.loe(endDate),
   gbsMemberHistory.endDate.isNull.or(gbsMemberHistory.endDate.goe(startDate))
   ```

3. 수정 이유
   - 기존 코드: 멤버의 시작일이 조회 시작일보다 이전이고, 종료일이 조회 종료일보다 이후인 경우만 조회
   - 수정된 코드: 멤버의 시작일이 조회 종료일보다 이전이고, 종료일이 조회 시작일보다 이후인 경우를 조회
   - 이를 통해 주어진 기간과 겹치는 모든 멤버를 정확하게 조회할 수 있음

## 영향 및 결과
1. 모든 테스트 케이스가 성공적으로 통과
2. 기간 조회 기능이 정확하게 동작하여 GBS 멤버 통계 및 조회 기능의 신뢰성 향상
3. 테스트 환경에서 QueryDSL 관련 설정이 올바르게 동작하도록 개선

## 교훈
1. QueryDSL을 사용하는 테스트 환경 설정 시 필요한 빈을 명시적으로 등록해야 함
2. 기간 조회 조건 설정 시 시작일과 종료일의 관계를 정확하게 고려해야 함
3. 테스트 실패 시 구체적인 원인 파악을 위해 단계적인 문제 해결 접근이 필요 