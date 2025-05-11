package com.attendly.api.dto

import java.time.LocalDateTime

/**
 * API 응답에 대한 공통 형식을 정의하는 클래스
 * 모든 API 응답은 이 클래스를 통해 표준화됩니다.
 *
 * @param T 응답 데이터의 타입
 * @property success 요청 성공 여부
 * @property timestamp 응답 생성 시간
 * @property data 응답 데이터
 * @property message 응답 메시지 (성공 또는 실패 이유)
 * @property code 응답 코드 (성공: 200, 실패: 에러 코드)
 */
data class ApiResponse<T>(
    val success: Boolean = true,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val data: T? = null,
    val message: String = "SUCCESS",
    val code: Int = 200
) {
    companion object {
        /**
         * 성공 응답을 생성하는 팩토리 메서드
         * 
         * @param data 응답 데이터
         * @param message 응답 메시지
         * @return 성공 상태의 ApiResponse 객체
         */
        fun <T> success(data: T, message: String = "SUCCESS"): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                message = message,
                code = 200
            )
        }

        /**
         * 데이터가 없는 성공 응답을 생성하는 팩토리 메서드
         * 
         * @param message 응답 메시지
         * @return 성공 상태의 ApiResponse 객체 (데이터 없음)
         */
        fun <T> successNoData(message: String = "SUCCESS"): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = null,
                message = message,
                code = 200
            )
        }

        /**
         * 실패 응답을 생성하는 팩토리 메서드
         * 
         * @param message 실패 이유
         * @param code 에러 코드
         * @return 실패 상태의 ApiResponse 객체
         */
        fun <T> error(message: String, code: Int = 400): ApiResponse<T> {
            return ApiResponse(
                success = false,
                data = null,
                message = message,
                code = code
            )
        }
    }
}

/**
 * 리스트 데이터를 포함하는 페이지 응답을 위한 클래스
 * 
 * @param T 리스트 내 항목의 타입
 * @property items 리스트 데이터
 * @property totalCount 전체 항목 수 (페이징 정보)
 * @property hasMore 더 많은 데이터가 있는지 여부
 */
data class PageResponse<T>(
    val items: List<T>,
    val totalCount: Long = items.size.toLong(),
    val hasMore: Boolean = false
) 