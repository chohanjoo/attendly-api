# Swagger UI 보안 버그 수정

## 버그 개요

Swagger UI에서 토큰을 설정해도 API 요청 시 토큰이 전송되지 않는 문제가 발생했습니다. 이로 인해 Swagger UI에서 인증이 필요한 API를 테스트할 때 항상 `403 Forbidden` 응답을 받는 상황이었습니다.

## 원인 분석

문제의 원인은 두 가지 구성 요소 간의 불일치였습니다:

1. **OpenApiConfig.kt**에서 JWT 보안 스키마 이름이 `bearerAuth`로 설정되어 있었습니다:

```kotlin
@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "bearerAuth"
        
        return OpenAPI()
            .info(apiInfo())
            .components(
                Components()
                    .addSecuritySchemes(
                        securitySchemeName,
                        SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
    }
    
    // ...
}
```

2. 반면 컨트롤러 클래스의 `@Operation` 어노테이션에서는 `Bearer Authentication`이라는 다른 이름으로 보안 요구 사항을 정의하고 있었습니다:

```kotlin
@GetMapping("/attendance")
@Operation(
    summary = "GBS 출석 조회",
    description = "특정 GBS의 특정 주차 출석 데이터를 조회합니다.",
    security = [SecurityRequirement(name = "Bearer Authentication")]
)
@PreAuthorize("isAuthenticated()")
fun getAttendancesByGbs(
    // ...
)
```

이 불일치로 인해 Swagger UI에서 토큰을 설정해도 실제 API 요청 시 토큰이 전송되지 않았습니다. OpenAPI 규격에서는 `@Operation` 어노테이션의 `security` 매개변수에 지정된 이름이 반드시 `OpenApiConfig`에서 정의한 보안 스키마 이름과 일치해야 합니다.

## 해결 방법

문제를 해결하기 위해 모든 컨트롤러 클래스에서 `@Operation` 어노테이션의 `security` 매개변수에 지정된 보안 스키마 이름을 `bearerAuth`로 변경했습니다:

1. **AttendanceController.kt** 수정:

```kotlin
@GetMapping("/attendance")
@Operation(
    summary = "GBS 출석 조회",
    description = "특정 GBS의 특정 주차 출석 데이터를 조회합니다.",
    security = [SecurityRequirement(name = "bearerAuth")]
)
@PreAuthorize("isAuthenticated()")
fun getAttendancesByGbs(
    // ...
)
```

2. **StatisticsController.kt** 수정:

```kotlin
@GetMapping("/departments/{id}/report")
@Operation(
    summary = "부서 출석 통계 조회",
    description = "특정 부서의 출석 통계를 조회합니다.",
    security = [SecurityRequirement(name = "bearerAuth")]
)
@PreAuthorize("@methodSecurityExpressions.isMinister(authentication) or hasRole('ADMIN')")
fun getDepartmentStatistics(
    // ...
)
```

## 결과

보안 스키마 이름을 일관되게 수정한 후, Swagger UI에서 JWT 토큰을 설정하면 API 요청 시 해당 토큰이 정상적으로 전송됩니다. 이제 Swagger UI에서 인증이 필요한 API를 테스트할 때 `403 Forbidden` 오류가 발생하지 않고 정상적으로 API를 호출할 수 있습니다.

## 교훈

1. OpenAPI/Swagger 문서화 시 보안 스키마 이름은 항상 일관되게 유지해야 합니다.
2. SecurityRequirement의 `name` 속성은 반드시 SecurityScheme에서 정의한 이름과 일치해야 합니다.
3. API 문서화 및 테스트 도구를 설정할 때는 인증/인가 메커니즘도 함께 검증해야 합니다.

## 관련 파일

- `src/main/kotlin/com/church/attendly/config/OpenApiConfig.kt`
- `src/main/kotlin/com/church/attendly/api/controller/AttendanceController.kt`
- `src/main/kotlin/com/church/attendly/api/controller/StatisticsController.kt` 