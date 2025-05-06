# CapturingFilterChain 기술 문서

## 개요

`CapturingFilterChain`은 Spring의 필터 메커니즘에서 필터 실행 중간에 특정 상태나 값을 캡처하기 위해 사용되는 테스트 유틸리티 클래스입니다. 
특히 필터가 실행되는 동안 특정 컨텍스트 변수의 값을 확인하는 테스트에서 유용하게 활용됩니다.

## 배경

Spring 애플리케이션에서 필터는 요청과 응답을 가로채 처리하는 중요한 컴포넌트입니다. 
필터의 올바른 동작을 검증하기 위해서는 필터 체인이 실행되는 동안 특정 값이 올바르게 설정되었는지 확인해야 할 때가 있습니다.
그러나 필터 실행 후 이러한 값들이 정리되거나 변경될 수 있어, 표준 `MockFilterChain`으로는 중간 상태를 확인하기 어려울 수 있습니다.

## CapturingFilterChain의 구현

`CapturingFilterChain`은 Spring의 `FilterChain` 인터페이스를 구현한 사용자 정의 클래스입니다:

```kotlin
private inner class CapturingFilterChain : FilterChain {
    override fun doFilter(request: ServletRequest, response: ServletResponse) {
        capturedRequestId = MDC.get("requestId")
    }
}
```

위 구현에서는 `doFilter` 메서드를 오버라이드하여 호출 시점에 MDC(Mapped Diagnostic Context)에서 "requestId" 값을 캡처합니다.

## 활용 사례

주요 활용 사례는 다음과 같습니다:

1. **MDC 값 캡처**: 로깅 프레임워크의 MDC에 설정된 값을 캡처하여 검증
2. **필터 중간 상태 테스트**: 필터 실행 중간에 설정된 특정 속성이나 상태 확인
3. **컨텍스트 전파 검증**: 필터가 스레드 로컬 또는 다른 컨텍스트 메커니즘을 통해 값을 올바르게 전파하는지 확인

## 예제 코드

`RequestIdFilter`의 동작을 테스트하는 예제:

```kotlin
@Test
fun `헤더에 Request-ID가 있을 때 해당 값을 MDC에 설정한다`() {
    // given
    val expectedRequestId = "test-request-id-123"
    mockRequest.addHeader("X-Request-ID", expectedRequestId)
    val capturingFilterChain = CapturingFilterChain()

    // when
    requestIdFilter.doFilter(mockRequest, mockResponse, capturingFilterChain)

    // then
    assertThat(capturedRequestId).isEqualTo(expectedRequestId)
    // 필터 종료 후 MDC에서 제거되었는지 확인
    assertThat(MDC.get("requestId")).isNull()
}
```

위 테스트에서는 `CapturingFilterChain`을 사용하여 필터가 설정한 MDC 값을 캡처하고, 필터 메서드 실행 후 해당 값이 제거되었는지도 검증합니다.

## 장점

- **중간 상태 포착**: 필터 체인 실행 중간에 설정된 값을 포착할 수 있음
- **격리된 테스트**: 특정 필터의 기능만 격리하여 테스트 가능
- **간결한 구현**: 필요한 기능만 구현하여 테스트 코드를 간결하게 유지

## 단점

- **제한된 시뮬레이션**: 실제 필터 체인의 모든 동작을 시뮬레이션하지는 않음
- **단일 목적**: 특정 값을 캡처하는 등 제한된 목적으로만 사용 가능

## 결론

`CapturingFilterChain`은 Spring 필터의 테스트에서 중간 상태나 값을 검증해야 할 때 유용한 패턴입니다.
특히 MDC와 같은 스레드 로컬 저장소에 값을 설정하고 나중에 제거하는 필터를 테스트할 때 효과적으로 활용할 수 있습니다.
이 패턴을 활용하면 필터의 핵심 동작을 격리하여 효율적으로 테스트할 수 있습니다. 