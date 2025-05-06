# MDC(Mapped Diagnostic Context)에 RequestId 구현

*작성일: 2024년 5월 12일*

## 개요

로그 추적과 디버깅을 용이하게 하기 위해 모든 프로세스의 MDC(Mapped Diagnostic Context)에 `requestId`를 자동으로 설정하는 기능을 구현했습니다. API 요청을 통해 들어오는 경우 헤더에서 `X-Request-ID`를 가져오고, 없는 경우 UUID를 생성하여 설정합니다. 이를 통해 모든 로그에서 특정 요청이나 작업을 식별할 수 있는 고유 ID를 확인할 수 있습니다.

## 구현 내용

### 1. RequestIdFilter 구현

모든 HTTP 요청을 가로채서 MDC에 requestId를 설정하는 필터를 구현했습니다.

```kotlin
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : GenericFilterBean() {

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val REQUEST_ID_MDC_KEY = "requestId"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        try {
            val httpRequest = request as HttpServletRequest
            val requestId = httpRequest.getHeader(REQUEST_ID_HEADER) ?: UUID.randomUUID().toString()
            MDC.put(REQUEST_ID_MDC_KEY, requestId)
            chain.doFilter(request, response)
        } finally {
            MDC.remove(REQUEST_ID_MDC_KEY)
        }
    }
}
```

- `X-Request-ID` 헤더가 있으면 해당 값을 사용
- 없으면 UUID를 생성
- 필터 처리가 끝나면 MDC에서 requestId 제거

### 2. MdcUtils 유틸리티 클래스 구현

비웹 환경(배치, 스케줄러 등)에서 MDC에 requestId를 관리할 수 있는 유틸리티 클래스를 구현했습니다.

```kotlin
object MdcUtils {
    // 현재 MDC에 설정된 requestId를 가져오거나 없으면 새로 생성
    fun getOrCreateRequestId(): String {
        return MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY) ?: UUID.randomUUID().toString().also {
            MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, it)
            logger.trace("새 requestId 생성: {}", it)
        }
    }

    // 새 requestId 명시적 생성
    fun setNewRequestId(): String {
        val requestId = UUID.randomUUID().toString()
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, requestId)
        logger.trace("새 requestId 설정: {}", requestId)
        return requestId
    }

    // requestId 설정
    fun setRequestId(requestId: String) {
        MDC.put(RequestIdFilter.REQUEST_ID_MDC_KEY, requestId)
    }

    // requestId 제거
    fun clearRequestId() {
        MDC.remove(RequestIdFilter.REQUEST_ID_MDC_KEY)
    }

    // 스레드 간 MDC 컨텍스트 전파 (Callable)
    fun <T> withMdc(callable: Callable<T>): Callable<T> { /* ... */ }

    // 스레드 간 MDC 컨텍스트 전파 (Runnable)
    fun withMdc(runnable: Runnable): Runnable { /* ... */ }

    // 새 requestId로 코드 블록 실행
    fun <T> withNewRequestId(block: () -> T): T { /* ... */ }
}
```

- 다양한 상황에서 requestId를 관리할 수 있는 유틸리티 메서드 제공
- 스레드 간 MDC 컨텍스트 전파 지원
- 코드 블록 실행 동안 임시 requestId 생성 및 이전 상태 복원 기능

### 3. MdcAspect 구현

비동기 작업과 스케줄러 작업에 자동으로 requestId를 설정하는 AOP 어드바이스를 구현했습니다.

```kotlin
@Aspect
@Component
@Order(0)
class MdcAspect {
    // @Async 어노테이션이 붙은 메소드 처리
    @Around("@annotation(org.springframework.scheduling.annotation.Async)")
    fun aroundAsyncMethod(joinPoint: ProceedingJoinPoint): Any? {
        return executeWithNewRequestId(joinPoint, "Async")
    }
    
    // @Scheduled 어노테이션이 붙은 메소드 처리
    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    fun aroundScheduledMethod(joinPoint: ProceedingJoinPoint): Any? {
        return executeWithNewRequestId(joinPoint, "Scheduled")
    }
    
    // 새 requestId로 메소드 실행
    private fun executeWithNewRequestId(joinPoint: ProceedingJoinPoint, type: String): Any? {
        val methodName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"
        
        return MdcUtils.withNewRequestId {
            val requestId = MdcUtils.getOrCreateRequestId()
            logger.debug("$type 메소드 실행: {} (requestId: {})", methodName, requestId)
            
            try {
                joinPoint.proceed()
            } catch (e: Throwable) {
                logger.error("$type 메소드 실행 중 오류 발생: {} (requestId: {})", methodName, requestId, e)
                throw e
            } finally {
                logger.debug("$type 메소드 실행 완료: {} (requestId: {})", methodName, requestId)
            }
        }
    }
}
```

- `@Async` 메소드 실행 시 자동으로 새 requestId 할당
- `@Scheduled` 메소드 실행 시 자동으로 새 requestId 할당
- 메소드 실행 후 자동으로 이전 MDC 상태 복원

## 테스트 구현

각 구현 부분에 대한 단위 테스트를 작성했습니다:

1. `RequestIdFilterTest`: 필터의 동작을 검증
2. `MdcUtilsTest`: 유틸리티 메서드의 동작을 검증
3. `MdcAspectTest`: AOP 어드바이스의 동작을 검증

## 기대 효과

1. **로그 추적성 향상**: 모든 로그에 동일한 requestId가 표시되므로, 특정 요청이나 작업의 로그를 쉽게 추적 가능
2. **비동기 작업 추적**: 비동기 메소드나 스케줄러 작업에서도 고유한 requestId를 통해 로그 추적 가능
3. **다중 스레드 환경 대응**: 스레드 풀 등의 환경에서도 MDC 컨텍스트가 올바르게 전파됨
4. **디버깅 효율성**: 장애 발생 시 관련 로그를 빠르게 찾아 원인 분석 가능

## 사용 방법

### API 요청 시 requestId 지정하기

```bash
curl -H "X-Request-ID: my-custom-id-123" https://api.example.com/endpoint
```

### 배치 작업 등에서 수동으로 requestId 설정하기

```kotlin
// 기존 requestId가 없는 경우 새로 생성하여 반환
val requestId = MdcUtils.getOrCreateRequestId()

// 명시적으로 새 requestId 생성
val newRequestId = MdcUtils.setNewRequestId()

// 코드 블록에 임시 requestId 할당
val result = MdcUtils.withNewRequestId {
    // 이 블록 내에서는 새 requestId가 MDC에 설정됨
    // ...코드...
}
// 블록 종료 후 이전 MDC 상태로 복원
```

### 비동기 작업에서 MDC 컨텍스트 전파하기

```kotlin
val executor = Executors.newSingleThreadExecutor()
val task = MdcUtils.withMdc(Runnable {
    // 이 Runnable 내에서는 원래 스레드의 MDC 컨텍스트가 유지됨
    // ...코드...
})
executor.submit(task)
```

## 주의사항

1. MDC는 스레드 로컬 변수를 사용하므로, 스레드 풀 환경에서는 `MdcUtils.withMdc()`를 통해 명시적으로 MDC 컨텍스트를 전파해야 합니다.
2. 로그백 설정 파일에 `%X{requestId}` 패턴이 포함되어 있어야 로그에 requestId가 출력됩니다.
3. `@Async` 메소드가 다른 스레드 풀을 사용하도록 구성된 경우, MDC 컨텍스트 전파를 위해 `TaskDecorator`를 추가로 구현해야 할 수 있습니다. 