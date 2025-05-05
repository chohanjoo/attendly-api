# JPA 지연 로딩 이슈 해결 - 리더 GBS 히스토리 API

## 발생 일시
2024-05-04 15:28:32

## 장애 현상
`/api/v1/gbs-members/leaders/{leaderId}/history` API 호출 시 다음과 같은 JPA 지연 로딩 관련 예외 발생:

```
org.hibernate.LazyInitializationException: failed to lazily initialize a collection of role: com.attendly.domain.entity.User.gbsLeaderHistories: could not initialize proxy - no Session
```

## 장애 원인 분석

### 1. 문제 코드 분석
GbsMemberService 클래스의 `getLeaderGbsHistories` 메서드에서 `User` 엔티티의 `gbsLeaderHistories` 컬렉션을 직접 접근했으나, 이 시점에 트랜잭션이 종료되어 세션이 닫혀 있었습니다.

```kotlin
val leaderHistories = leader.gbsLeaderHistories.sortedByDescending { it.startDate }
```

User 엔티티에서 gbsLeaderHistories는 다음과 같이 선언되어 있었습니다:

```kotlin
@OneToMany(mappedBy = "leader")
val gbsLeaderHistories: MutableList<GbsLeaderHistory> = mutableListOf()
```

이는 OneToMany 기본 설정인 지연 로딩(FetchType.LAZY)로 설정되어 있어, 해당 컬렉션을 실제로 접근할 때 데이터베이스 조회가 발생합니다. 그러나 이 시점에 이미 트랜잭션이 종료되어 세션이 닫혀 있어 LazyInitializationException이 발생했습니다.

### 2. JPA 지연 로딩 메커니즘
JPA의 지연 로딩은 다음과 같이 작동합니다:
- 엔티티 로드 시 연관 관계에 있는 엔티티는 즉시 로드되지 않고 프록시 객체로 대체됨
- 프록시 객체에 접근 시 실제 데이터베이스 조회가 발생
- 이때 하이버네이트 세션이 열려있어야 하며, 세션이 닫힌 후 접근 시 LazyInitializationException 발생

## 해결 방법

### 1. 레포지토리에서 직접 조회 방식으로 변경
기존 User 엔티티의 컬렉션 필드를 직접 접근하는 대신, 레포지토리에서 조회하는 방식으로 변경했습니다:

```kotlin
// AS-IS: 문제가 있는 코드
val leaderHistories = leader.gbsLeaderHistories.sortedByDescending { it.startDate }

// TO-BE: 해결 방법
val leaderHistories = gbsLeaderHistoryRepository.findByLeaderIdWithDetailsOrderByStartDateDesc(leaderId)
```

### 2. 페치 조인(Fetch Join)을 활용한 N+1 문제 해결
연관 엔티티도 함께 조회하기 위해 JPQL 쿼리에 fetch join을 추가했습니다:

```kotlin
@Query("SELECT h FROM GbsLeaderHistory h " +
       "JOIN FETCH h.gbsGroup g " +
       "JOIN FETCH g.village v " +
       "WHERE h.leader.id = :leaderId " +
       "ORDER BY h.startDate DESC")
fun findByLeaderIdWithDetailsOrderByStartDateDesc(@Param("leaderId") leaderId: Long): List<GbsLeaderHistory>
```

이를 통해 GbsLeaderHistory 엔티티뿐만 아니라 GbsGroup, Village 등 연관 엔티티도 한 번의 쿼리로 함께 로딩할 수 있게 되었습니다.

### 3. 타입 추론 문제 해결
메서드 구현 과정에서 타입 추론 문제가 발생하여 명시적으로 타입을 지정했습니다:

```kotlin
val leaderHistories: List<GbsLeaderHistory> = gbsLeaderHistoryRepository.findByLeaderIdWithDetailsOrderByStartDateDesc(leaderId)

val historyResponses = leaderHistories.map { history: GbsLeaderHistory ->
    // 코드 생략
}
```

## 고려사항

### 1. 즉시 로딩(EAGER) vs 지연 로딩(LAZY)

#### 즉시 로딩 방식의 단점
- 불필요한 데이터까지 항상 조회하여 성능 저하 발생
- 순환 참조 발생 가능성 증가
- N+1 문제 해결책이 되지 못함

#### 지연 로딩 + 페치 조인 방식의 장점
- 필요한 데이터만 선택적으로 로딩 가능
- 명시적인 쿼리를 통해 코드의 의도가 명확해짐
- N+1 문제 해결 가능

### 2. Open Session In View(OSIV) 패턴 검토
OSIV 패턴을 활성화하면 뷰 렌더링(또는 API 응답 생성) 시점까지 하이버네이트 세션을 유지하여 지연 로딩 예외를 방지할 수 있습니다.

```properties
spring.jpa.open-in-view=true
```

그러나 OSIV는 다음과 같은 단점이 있어 사용하지 않기로 결정했습니다:
- 데이터베이스 커넥션을 오래 점유하여 전체 애플리케이션 성능 저하
- 서비스 계층 밖에서 데이터베이스 액세스가 발생하여 코드 예측성 감소
- N+1 문제 해결책이 되지 못함

### 3. DTO 변환 계층 검토
엔티티를 컨트롤러에 반환하지 않고 서비스 계층에서 DTO로 변환하는 방식을 검토했습니다. 현재 구현에서는 이미 LeaderGbsHistoryResponse와 같은 DTO 객체를 사용하고 있어 이 방식을 유지했습니다.

## 학습 및 향후 대책

### 1. 지연 로딩 관련 베스트 프랙티스
- 서비스 계층에서 필요한 모든 데이터를 로딩하고 DTO로 변환
- 가능한 N+1 문제를 방지하기 위해 페치 조인 활용
- JPA 연관 관계는 기본적으로 지연 로딩(LAZY)으로 설정

### 2. 로깅 및 모니터링 개선
- 지연 로딩 관련 예외를 모니터링하기 위한 로깅 추가
- 성능에 영향을 미치는 쿼리에 대한 모니터링 시스템 구축

### 3. 애플리케이션 성능 테스트
- 페치 조인 사용 전후의 성능 비교
- 대용량 데이터에서의 동작 테스트

## 결론
이번 장애를 통해 JPA의 지연 로딩 메커니즘에 대한 이해도를 높이고, 베스트 프랙티스를 적용하여 코드 품질을 향상시켰습니다. 특히 페치 조인을 활용한 N+1 문제 해결 방법을 적용하여 성능도 함께 개선했습니다.

앞으로도 API 설계 및 구현 시 JPA 관련 특성을 고려하여 안정적인 서비스를 제공할 수 있도록 노력하겠습니다. 