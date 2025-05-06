package com.attendly.config

import com.attendly.api.interceptor.ApiLogInterceptor
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value

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
                    .addParameters(
                        ApiLogInterceptor.X_REQUEST_ID,
                        Parameter()
                            .name(ApiLogInterceptor.X_REQUEST_ID)
                            .description("요청 추적을 위한 고유 식별자")
                            .example("550e8400-e29b-41d4-a716-446655440000")
                            .required(false)
                            .`in`("header")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
    }

    @Bean
    fun userApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("user-api")
            .pathsToMatch("/api/**", "/auth/**")
            .pathsToExclude("/api/admin/**")
            .addOperationCustomizer { operation, _ ->
                operation.addParametersItem(
                    Parameter()
                        .name(ApiLogInterceptor.X_REQUEST_ID)
                        .`in`("header")
                        .`$ref`("#/components/parameters/${ApiLogInterceptor.X_REQUEST_ID}")
                )
            }
            .build()
    }

    @Bean
    fun adminApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
            .group("admin-api")
            .pathsToMatch("/api/admin/**")
            .addOperationCustomizer { operation, _ ->
                operation.addParametersItem(
                    Parameter()
                        .name(ApiLogInterceptor.X_REQUEST_ID)
                        .`in`("header")
                        .`$ref`("#/components/parameters/${ApiLogInterceptor.X_REQUEST_ID}")
                )
            }
            .build()
    }

    private fun apiInfo() = Info()
        .title("Attendly API")
        .description("교회 출석 관리 시스템 API")
        .version("1.0.0")
} 