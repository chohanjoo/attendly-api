package com.attendly.api.util

import com.attendly.api.dto.ApiResponse
import com.attendly.api.dto.PageResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * ResponseEntity 생성을 위한 확장 함수들을 제공하는 유틸리티 클래스
 */
object ResponseUtil {
    
    /**
     * 단일 객체 데이터로 성공 응답을 생성합니다.
     * 
     * @param data 응답 데이터
     * @param message 응답 메시지
     * @param status HTTP 상태
     * @return ApiResponse를 담은 ResponseEntity
     */
    fun <T> success(
        data: T,
        message: String = "SUCCESS",
        status: HttpStatus = HttpStatus.OK
    ): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity
            .status(status)
            .body(ApiResponse.success(data, message))
    }
    
    /**
     * 데이터 없이 성공 응답을 생성합니다.
     * 
     * @param message 응답 메시지
     * @param status HTTP 상태
     * @return 데이터가 없는 ApiResponse를 담은 ResponseEntity
     */
    fun <T> successNoData(
        message: String = "SUCCESS",
        status: HttpStatus = HttpStatus.OK
    ): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity
            .status(status)
            .body(ApiResponse.successNoData(message))
    }
    
    /**
     * 리스트 데이터로 응답을 생성합니다.
     * 
     * @param items 리스트 데이터
     * @param totalCount 전체 항목 수
     * @param hasMore 더 많은 데이터가 있는지 여부
     * @param message 응답 메시지
     * @param status HTTP 상태
     * @return PageResponse를 담은 ApiResponse
     */
    fun <T> successList(
        items: List<T>,
        totalCount: Long = items.size.toLong(),
        hasMore: Boolean = false,
        message: String = "SUCCESS",
        status: HttpStatus = HttpStatus.OK
    ): ResponseEntity<ApiResponse<PageResponse<T>>> {
        val pageResponse = PageResponse(items, totalCount, hasMore)
        return ResponseEntity
            .status(status)
            .body(ApiResponse.success(pageResponse, message))
    }
    
    /**
     * 에러 응답을 생성합니다.
     * 
     * @param message 에러 메시지
     * @param errorCode 에러 코드
     * @param status HTTP 상태
     * @return 에러 정보를 담은 ApiResponse
     */
    fun <T> error(
        message: String,
        errorCode: Int = 400,
        status: HttpStatus = HttpStatus.BAD_REQUEST
    ): ResponseEntity<ApiResponse<T>> {
        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(message, errorCode))
    }
    
    /**
     * 생성 성공(201) 응답을 생성합니다.
     * 
     * @param data 생성된 데이터
     * @param message 응답 메시지
     * @return HTTP 201 상태와 ApiResponse를 담은 ResponseEntity
     */
    fun <T> created(
        data: T,
        message: String = "CREATED"
    ): ResponseEntity<ApiResponse<T>> {
        return success(data, message, HttpStatus.CREATED)
    }
} 