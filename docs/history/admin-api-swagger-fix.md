# 관리자 API Swagger 노출 문제 해결

## 문제 개요

Swagger UI에서 `/api/admin/*` 경로의 관리자 컨트롤러들이 노출되지 않는 문제가 발생했습니다. 이로 인해 개발 및 테스트 과정에서 관리자 API의 문서를 확인하고 테스트하는 데 어려움이 있었습니다.

## 원인 분석

문제의 원인은 다음과 같았습니다:

1. Swagger 문서 그룹핑 설정이 없어 관리자 API와 일반 사용자 API가 분리되지 않았습니다.
2. 관리자 컨트롤러에는 `@PreAuthorize("hasAuthority('ADMIN')")` 또는 `@PreAuthorize("hasRole('ADMIN')")` 어노테이션이 적용되어 있었으나, Swagger에서 이를 적절히 처리하지 못했습니다.
3. 관리자 컨트롤러의 API 메서드에 `SecurityRequirement` 설정이 누락되어 있었습니다.
4. Spring Security 설정에서 Swagger 문서 접근 경로가 완전히 허용되지 않았습니다.

## 해결 방법

다음과 같은 수정을 통해 문제를 해결했습니다:

### 1. OpenApiConfig 수정

Swagger 문서를 그룹화하여 관리자 API와 일반 사용자 API를 구분했습니다.

```kotlin
@Configuration
class OpenApiConfig {
    // 기존 openAPI() 메서드 유지
    
    @Bean
    fun userApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("user-api")
            .pathsToMatch("/api/v1/**", "/auth/**")
            .build()
    }

    @Bean
    fun adminApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("admin-api")
            .pathsToMatch("/api/admin/**")
            .build()
    }
    
    // 기존 apiInfo() 메서드 유지
}
```

### 2. 관리자 컨트롤러에 보안 요구사항 추가

모든 관리자 컨트롤러 클래스와 API 메서드에 `SecurityRequirement` 어노테이션을 추가했습니다.

```kotlin
@RestController
@RequestMapping("/api/admin/logs")
@Tag(name = "관리자 로그 API", description = "시스템 로그 조회 관련 API")
@PreAuthorize("hasAuthority('ADMIN')")
@SecurityRequirement(name = "bearerAuth") // 클래스 레벨에 추가
class AdminLogController(
    private val systemLogService: SystemLogService
) {
    @Operation(
        summary = "시스템 로그 조회", 
        description = "조건별 시스템 로그를 조회합니다 (관리자 전용)",
        security = [SecurityRequirement(name = "bearerAuth")] // 메서드 레벨에 추가
    )
    @GetMapping
    fun getLogs(...): ResponseEntity<Page<SystemLogResponseDto>> {
        // ...
    }
    
    // 다른 메서드들도 동일하게 수정
}
```

수정한 컨트롤러 목록:
- `AdminLogController`
- `AdminSystemController`
- `AdminOrganizationController`
- `AdminUserController`
- `AdminBatchController`

### 3. Spring Security 설정 수정

`SecurityConfig` 클래스에서 Swagger 문서 접근 경로에 그룹화된 API 문서 경로를 추가했습니다.

```kotlin
@Bean
fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http
        // ...
        .authorizeHttpRequests { auth ->
            auth
                .requestMatchers(
                    "/auth/login",
                    "/auth/signup",
                    "/auth/refresh",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs/admin-api", // 추가
                    "/v3/api-docs/user-api"   // 추가
                ).permitAll()
                // ...
        }
        // ...
}
```

## 결과

이러한 수정을 통해 Swagger UI에서 관리자 API와 일반 사용자 API가 각각의 그룹으로 구분되어 표시되며, 모든 API의 문서를 확인하고 테스트할 수 있게 되었습니다.

Swagger UI에 접속하면 상단에 "user-api"와 "admin-api" 두 개의 그룹을 선택할 수 있으며, 각 그룹에서 해당 API 목록을 확인할 수 있습니다.

## 참고 사항

이 수정은 개발 환경에서 API 테스트를 위한 것으로, 실제 운영 환경에서는 보안 설정을 더 강화하는 것이 좋습니다. 필요한 경우 프로필(profile)에 따라 Swagger 접근을 제한하는 방법을 고려할 수 있습니다. 