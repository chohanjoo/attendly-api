# QueryDSL vs JPQL 비교

## 1. 소개

### 1.1 QueryDSL이란?
QueryDSL은 타입 안전한 쿼리를 작성할 수 있게 해주는 프레임워크입니다. Java와 Kotlin에서 JPA, JDO, SQL 등을 위한 타입 안전한 쿼리를 작성할 수 있게 해줍니다.

### 1.2 JPQL이란?
JPQL(Java Persistence Query Language)은 JPA에서 제공하는 객체지향 쿼리 언어입니다. SQL과 유사하지만, 테이블이 아닌 엔티티 객체를 대상으로 쿼리를 작성합니다.

## 2. QueryDSL의 장점

### 2.1 타입 안전성 (Type Safety)
```kotlin
// JPQL
@Query("SELECT u FROM User u WHERE u.name = :name")
fun findByName(@Param("name") name: String): User?

// QueryDSL
fun findByName(name: String): User? {
    return queryFactory
        .selectFrom(user)
        .where(user.name.eq(name))
        .fetchOne()
}
```
- JPQL은 문자열로 쿼리를 작성하기 때문에 런타임에 오류가 발생할 수 있습니다.
- QueryDSL은 컴파일 시점에 타입 체크가 가능하여 오류를 미리 발견할 수 있습니다.

### 2.2 IDE 지원
- 자동 완성 기능을 통해 쿼리 작성이 용이합니다.
- 메서드 체이닝을 통한 직관적인 쿼리 작성이 가능합니다.
- 리팩토링 시 쿼리도 함께 수정됩니다.

### 2.3 동적 쿼리 작성의 용이성
```kotlin
// 동적 쿼리 예시
fun findUsers(name: String?, age: Int?): List<User> {
    return queryFactory
        .selectFrom(user)
        .where(
            name?.let { user.name.eq(it) },
            age?.let { user.age.eq(it) }
        )
        .fetch()
}
```
- 조건에 따라 동적으로 쿼리를 변경하기 쉽습니다.
- JPQL로 동적 쿼리를 작성하려면 문자열을 조합해야 하는 번거로움이 있습니다.

### 2.4 코드 재사용성
```kotlin
// 공통 조건을 메서드로 추출
private fun activeMembers(gbsId: Long, date: LocalDate): BooleanExpression {
    return gbsMemberHistory.gbsGroup.id.eq(gbsId)
        .and(gbsMemberHistory.startDate.loe(date))
        .and(gbsMemberHistory.endDate.isNull.or(gbsMemberHistory.endDate.goe(date)))
}

// 재사용 예시
fun countActiveMembers(gbsId: Long, date: LocalDate): Long {
    return queryFactory
        .select(gbsMemberHistory.count())
        .from(gbsMemberHistory)
        .where(activeMembers(gbsId, date))
        .fetchOne() ?: 0L
}
```
- 공통 조건을 메서드로 추출하여 재사용할 수 있습니다.
- 쿼리 로직을 모듈화하여 관리가 용이합니다.

## 3. 성능 비교

### 3.1 쿼리 최적화
- QueryDSL은 컴파일 시점에 쿼리가 생성되므로, 런타임 성능이 더 좋습니다.
- JPQL은 런타임에 파싱되어야 하므로 약간의 오버헤드가 있습니다.

### 3.2 캐싱
- QueryDSL은 컴파일된 쿼리를 재사용할 수 있습니다.
- JPQL은 매번 파싱되어야 하므로 캐싱 효과가 제한적입니다.

## 4. 유지보수성

### 4.1 코드 가독성
```kotlin
// JPQL
@Query("""
    SELECT h FROM GbsMemberHistory h
    JOIN FETCH h.member
    WHERE h.gbsGroup.id = :gbsId
    AND h.startDate <= :date
    AND (h.endDate IS NULL OR h.endDate >= :date)
""")

// QueryDSL
fun findActiveMembers(gbsId: Long, date: LocalDate): List<GbsMemberHistory> {
    return queryFactory
        .selectFrom(gbsMemberHistory)
        .join(gbsMemberHistory.member).fetchJoin()
        .where(
            gbsMemberHistory.gbsGroup.id.eq(gbsId),
            gbsMemberHistory.startDate.loe(date),
            gbsMemberHistory.endDate.isNull.or(gbsMemberHistory.endDate.goe(date))
        )
        .fetch()
}
```
- QueryDSL은 메서드 체이닝을 통해 쿼리의 구조를 명확하게 표현할 수 있습니다.
- 복잡한 쿼리도 가독성 있게 작성할 수 있습니다.

### 4.2 디버깅
- QueryDSL은 컴파일 시점에 오류를 발견할 수 있어 디버깅이 용이합니다.
- JPQL은 런타임에 오류가 발생하므로 디버깅이 어려울 수 있습니다.

## 5. 결론

### 5.1 QueryDSL을 선택해야 하는 경우
- 복잡한 동적 쿼리가 필요한 경우
- 타입 안전성이 중요한 경우
- 코드 재사용성이 중요한 경우
- 유지보수성이 중요한 대규모 프로젝트

### 5.2 JPQL을 선택해야 하는 경우
- 단순한 정적 쿼리만 필요한 경우
- 빠른 개발이 필요한 소규모 프로젝트
- QueryDSL의 학습 비용을 감당하기 어려운 경우

## 6. 실습 과제

1. 기존 JPQL 쿼리를 QueryDSL로 변환해보세요.
2. 동적 쿼리를 작성해보세요.
3. 공통 조건을 추출하여 재사용해보세요.
4. 성능 테스트를 통해 QueryDSL과 JPQL의 차이를 측정해보세요. 