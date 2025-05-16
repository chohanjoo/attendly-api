package com.attendly.exception

import org.springframework.http.HttpStatus

/**
 * 에러 메시지를 관리하는 enum 클래스
 * 상태 코드, 에러 코드, 메시지를 통합하여 관리
 * 
 * 에러 코드 체계:
 * - E400-E599: 기본 HTTP 에러
 * - E1000-E1099: 인증/인가 관련
 * - E2000-E2099: GBS 관련
 * - E3000-E3099: 리더 위임 관련
 * - E4000-E4099: 출석 관련
 * - E5000-E5099: 사용자/멤버 관련
 * - E9000-E9099: 시스템/배치 관련
 */
enum class ErrorMessage(
    val status: HttpStatus,
    val code: String,
    val message: String
) {
    // 기본 HTTP 에러 (E400-E599)
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "E400", "잘못된 요청입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "접근 권한이 없습니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "요청한 리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "E405", "허용되지 않은 메소드입니다"),
    CONFLICT(HttpStatus.CONFLICT, "E409", "리소스 충돌이 발생했습니다"),
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_ENTITY, "E422", "요청을 처리할 수 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "서버 내부 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "E503", "서비스를 일시적으로 사용할 수 없습니다"),
    
    // 인증/인가 관련 (E1000-E1099)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "E1000", "아이디 또는 비밀번호가 일치하지 않습니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "E1001", "인증 토큰이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "E1002", "유효하지 않은 인증 토큰입니다"),
    ACCESS_DENIED_GBS(HttpStatus.FORBIDDEN, "E1010", "이 GBS의 멤버 정보를 조회할 권한이 없습니다"),
    ACCESS_DENIED_LEADER_HISTORY(HttpStatus.FORBIDDEN, "E1011", "다른 리더의 히스토리를 조회할 권한이 없습니다"),
    ACCESS_DENIED_ATTENDANCE(HttpStatus.FORBIDDEN, "E1012", "이 GBS에 대한 출석 입력 권한이 없습니다"),
    
    // GBS 관련 (E2000-E2099)
    GBS_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "E2000", "GBS 그룹을 찾을 수 없습니다"),
    NO_CURRENT_GBS_FOR_LEADER(HttpStatus.NOT_FOUND, "E2001", "현재 담당하는 GBS가 없습니다"),
    NO_ACTIVE_LEADER(HttpStatus.NOT_FOUND, "E2002", "해당 날짜에 활성화된 GBS 리더를 찾을 수 없습니다"),
    MEMBER_NOT_IN_GBS(HttpStatus.BAD_REQUEST, "E2003", "해당 멤버는 GBS에 속하지 않습니다"),
    GBS_GROUP_NOT_IN_VILLAGE(HttpStatus.BAD_REQUEST, "E2004", "GBS 그룹이 해당 마을에 속하지 않습니다"),
    
    // 리더 위임 관련 (E3000-E3099)
    DELEGATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "E3000", "위임자를 찾을 수 없습니다"),
    DELEGATEE_NOT_FOUND(HttpStatus.NOT_FOUND, "E3001", "수임자를 찾을 수 없습니다"),
    INVALID_DELEGATION_DATES(HttpStatus.BAD_REQUEST, "E3002", "시작일은 종료일보다 이전이거나 같아야 합니다"),
    INVALID_START_DATE(HttpStatus.BAD_REQUEST, "E3003", "시작일은 현재 날짜 이후여야 합니다"),
    DUPLICATE_DELEGATION(HttpStatus.CONFLICT, "E3004", "이 GBS 그룹에 이미 활성 위임이 존재합니다"),
    
    // 출석 관련 (E4000-E4099)
    WORSHIP_NOT_FOUND(HttpStatus.NOT_FOUND, "E4000", "예배 정보를 찾을 수 없습니다"),
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E4001", "출석 정보를 찾을 수 없습니다"),
    GBS_WEEK_NOT_FOUND(HttpStatus.NOT_FOUND, "E4002", "해당 주차의 GBS 정보를 찾을 수 없습니다"),
    INVALID_WEEK_START(HttpStatus.BAD_REQUEST, "E4003", "주 시작일은 일요일이어야 합니다"),
    
    // 사용자/멤버 관련 (E5000-E5099)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "E5000", "사용자를 찾을 수 없습니다"),
    LEADER_NOT_FOUND(HttpStatus.NOT_FOUND, "E5001", "해당 리더를 찾을 수 없습니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "E5002", "조원을 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "E5003", "이미 사용 중인 이메일입니다"),
    USER_NOT_ASSIGNED_TO_VILLAGE(HttpStatus.BAD_REQUEST, "E5004", "사용자가 마을에 배정되지 않았습니다"),
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "E5005", "찾을 수 없는 부서입니다"),
    VILLAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "E5006", "마을을 찾을 수 없습니다"),
    VILLAGE_LEADER_NOT_FOUND(HttpStatus.NOT_FOUND, "E5007", "마을장을 찾을 수 없습니다"),
    VILLAGE_NOT_IN_DEPARTMENT(HttpStatus.BAD_REQUEST, "E5008", "마을이 해당 부서에 속하지 않습니다"),
    
    // 시스템/배치 관련 (E9000-E9099)
    BATCH_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "E9000", "배치 작업을 찾을 수 없습니다"),
    CANNOT_CANCEL_COMPLETED_JOB(HttpStatus.BAD_REQUEST, "E9001", "이미 완료되거나 실패한 작업은 취소할 수 없습니다"),
    CANNOT_RESTART_RUNNING_JOB(HttpStatus.BAD_REQUEST, "E9002", "실행 중인 작업은 재시작할 수 없습니다"),
    SYSTEM_SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "E9003", "시스템 설정을 찾을 수 없습니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "E9004", "입력값이 유효하지 않습니다"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "E9005", "이미 존재하는 리소스입니다");
    
    companion object {
        /**
         * 에러 코드에 기반한 ErrorMessage 조회
         */
        fun fromCode(code: String): ErrorMessage {
            return values().firstOrNull { it.code == code } 
                ?: throw IllegalArgumentException("Invalid error code: $code")
        }
        
        /**
         * 상태 코드에 기반한 기본 ErrorMessage 조회
         */
        fun fromStatus(status: HttpStatus): ErrorMessage {
            return when(status) {
                HttpStatus.BAD_REQUEST -> BAD_REQUEST
                HttpStatus.UNAUTHORIZED -> UNAUTHORIZED
                HttpStatus.FORBIDDEN -> FORBIDDEN
                HttpStatus.NOT_FOUND -> RESOURCE_NOT_FOUND
                HttpStatus.METHOD_NOT_ALLOWED -> METHOD_NOT_ALLOWED
                HttpStatus.CONFLICT -> CONFLICT
                HttpStatus.UNPROCESSABLE_ENTITY -> UNPROCESSABLE_ENTITY
                HttpStatus.INTERNAL_SERVER_ERROR -> INTERNAL_SERVER_ERROR
                HttpStatus.SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE
                else -> INTERNAL_SERVER_ERROR
            }
        }
    }
} 