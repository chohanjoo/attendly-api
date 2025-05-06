package com.attendly.exception

/**
 * 에러 메시지 생성을 위한 유틸리티 클래스
 */
object ErrorMessageUtils {
    
    /**
     * 기본 에러 메시지에 ID를 추가하여 상세 메시지 생성
     * @param errorMessage 기본 에러 메시지
     * @param id 리소스 ID
     * @return ID가 포함된 상세 에러 메시지
     */
    fun withId(errorMessage: ErrorMessage, id: Long): String {
        return "${errorMessage.message}: ID $id"
    }
    
    /**
     * 기본 에러 메시지에 여러 ID를 추가하여 상세 메시지 생성
     * @param errorMessage 기본 에러 메시지
     * @param ids 여러 리소스 ID 맵 (필드명과 ID 값)
     * @return 여러 ID가 포함된 상세 에러 메시지
     */
    fun withIds(errorMessage: ErrorMessage, vararg ids: Pair<String, Any>): String {
        val idsString = ids.joinToString(", ") { (name, value) -> "$name: $value" }
        return "${errorMessage.message} ($idsString)"
    }
    
    /**
     * 기본 에러 메시지에 날짜와 ID를 추가하여 상세 메시지 생성
     * @param errorMessage 기본 에러 메시지
     * @param id 리소스 ID
     * @param date 날짜
     * @return 날짜와 ID가 포함된 상세 에러 메시지
     */
    fun withIdAndDate(errorMessage: ErrorMessage, id: Long, date: Any): String {
        return "${errorMessage.message}: ID $id, 날짜 $date"
    }
    
    /**
     * 기본 에러 메시지에 대상 필드 정보를 추가하여 상세 메시지 생성
     * @param errorMessage 기본 에러 메시지
     * @param field 대상 필드명
     * @param value 필드 값
     * @return 필드 정보가 포함된 상세 에러 메시지
     */
    fun withField(errorMessage: ErrorMessage, field: String, value: Any?): String {
        return "${errorMessage.message}: $field = ${value ?: "null"}"
    }
    
    /**
     * 기본 에러 메시지에 추가 정보를 map 형태로 받아 상세 메시지 생성
     * @param errorMessage 기본 에러 메시지
     * @param params 추가 정보 맵
     * @return 추가 정보가 포함된 상세 에러 메시지
     */
    fun withParams(errorMessage: ErrorMessage, params: Map<String, Any?>): String {
        val paramsString = params.entries.joinToString(", ") { (key, value) -> 
            "$key: ${value ?: "null"}" 
        }
        return if (paramsString.isNotEmpty()) {
            "${errorMessage.message} ($paramsString)"
        } else {
            errorMessage.message
        }
    }
} 