package com.attendly.exception

/**
 * 에러 메시지를 관리하는 enum 클래스
 * ErrorCode와 메시지를 분리하여 관리
 */
enum class ErrorMessage(
    val code: ErrorCode,
    val message: String
) {
    // 기본 에러 메시지
    BAD_REQUEST(ErrorCode.BAD_REQUEST, "잘못된 요청입니다"),
    UNAUTHORIZED(ErrorCode.UNAUTHORIZED, "인증이 필요합니다"),
    FORBIDDEN(ErrorCode.FORBIDDEN, "접근 권한이 없습니다"),
    RESOURCE_NOT_FOUND(ErrorCode.RESOURCE_NOT_FOUND, "요청한 리소스를 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(ErrorCode.METHOD_NOT_ALLOWED, "허용되지 않은 메소드입니다"),
    CONFLICT(ErrorCode.CONFLICT, "리소스 충돌이 발생했습니다"),
    UNPROCESSABLE_ENTITY(ErrorCode.UNPROCESSABLE_ENTITY, "요청을 처리할 수 없습니다"),
    
    // 사용자 정의 예외 메시지
    INVALID_INPUT(ErrorCode.INVALID_INPUT, "입력값이 유효하지 않습니다"),
    DUPLICATE_RESOURCE(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 리소스입니다"),
    USER_NOT_FOUND(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"),
    INVALID_CREDENTIALS(ErrorCode.INVALID_CREDENTIALS, "아이디 또는 비밀번호가 일치하지 않습니다"),
    TOKEN_EXPIRED(ErrorCode.TOKEN_EXPIRED, "인증 토큰이 만료되었습니다"),
    INVALID_TOKEN(ErrorCode.INVALID_TOKEN, "유효하지 않은 인증 토큰입니다"),
    
    // 서버 오류 메시지
    INTERNAL_SERVER_ERROR(ErrorCode.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),
    SERVICE_UNAVAILABLE(ErrorCode.SERVICE_UNAVAILABLE, "서비스를 일시적으로 사용할 수 없습니다"),
    
    // 권한 관련 메시지
    ACCESS_DENIED_GBS(ErrorCode.FORBIDDEN, "이 GBS의 멤버 정보를 조회할 권한이 없습니다"),
    ACCESS_DENIED_LEADER_HISTORY(ErrorCode.FORBIDDEN, "다른 리더의 히스토리를 조회할 권한이 없습니다"),
    ACCESS_DENIED_ATTENDANCE(ErrorCode.FORBIDDEN, "이 GBS에 대한 출석 입력 권한이 없습니다"),
    MEMBER_NOT_IN_GBS(ErrorCode.FORBIDDEN, "해당 조원은 이 GBS에 속하지 않습니다"),
    
    // LeaderDelegation 관련 메시지
    DELEGATOR_NOT_FOUND(ErrorCode.USER_NOT_FOUND, "위임자를 찾을 수 없습니다"),
    DELEGATEE_NOT_FOUND(ErrorCode.USER_NOT_FOUND, "수임자를 찾을 수 없습니다"),
    GBS_GROUP_NOT_FOUND(ErrorCode.RESOURCE_NOT_FOUND, "GBS 그룹을 찾을 수 없습니다"),
    INVALID_DELEGATION_DATES(ErrorCode.INVALID_INPUT, "시작일은 종료일보다 이전이거나 같아야 합니다"),
    INVALID_START_DATE(ErrorCode.INVALID_INPUT, "시작일은 현재 날짜 이후여야 합니다"),
    DUPLICATE_DELEGATION(ErrorCode.DUPLICATE_RESOURCE, "이 GBS 그룹에 이미 활성 위임이 존재합니다"),
    
    // GBS 관련 메시지
    NO_CURRENT_GBS_FOR_LEADER(ErrorCode.RESOURCE_NOT_FOUND, "현재 담당하는 GBS가 없습니다"),
    LEADER_NOT_FOUND(ErrorCode.USER_NOT_FOUND, "해당 리더를 찾을 수 없습니다"),
    NO_ACTIVE_LEADER(ErrorCode.RESOURCE_NOT_FOUND, "해당 날짜에 활성화된 GBS 리더를 찾을 수 없습니다"),
    MEMBER_NOT_FOUND(ErrorCode.RESOURCE_NOT_FOUND, "조원을 찾을 수 없습니다"),
    
    // 이메일 관련 메시지
    DUPLICATE_EMAIL(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 이메일입니다"),
    
    // 부서 관련 메시지
    DEPARTMENT_NOT_FOUND(ErrorCode.RESOURCE_NOT_FOUND, "찾을 수 없는 부서입니다"),
    
    // 마을 관련 메시지
    VILLAGE_NOT_FOUND(ErrorCode.RESOURCE_NOT_FOUND, "마을을 찾을 수 없습니다"),
    
    // 배치 작업 관련 메시지
    BATCH_JOB_NOT_FOUND(ErrorCode.RESOURCE_NOT_FOUND, "배치 작업을 찾을 수 없습니다"),
    CANNOT_CANCEL_COMPLETED_JOB(ErrorCode.INVALID_INPUT, "이미 완료되거나 실패한 작업은 취소할 수 없습니다"),
    CANNOT_RESTART_RUNNING_JOB(ErrorCode.INVALID_INPUT, "실행 중인 작업은 재시작할 수 없습니다"),
    
    // 시스템 설정 관련 메시지
    SYSTEM_SETTING_NOT_FOUND(ErrorCode.RESOURCE_NOT_FOUND, "시스템 설정을 찾을 수 없습니다");
    
    companion object {
        /**
         * ErrorCode에 기반한 기본 ErrorMessage 반환
         * 만약 해당 ErrorCode에 맞는 기본 메시지가 없다면 코드 자체의 메시지 사용
         * 
         * @deprecated ErrorMessageUtils 클래스를 사용하세요.
         */
        @Deprecated("ErrorMessageUtils 클래스를 사용하세요.", ReplaceWith("ErrorMessageUtils.getDefaultMessage(errorCode)"))
        fun getDefaultMessage(errorCode: ErrorCode): String {
            return values().firstOrNull { it.code == errorCode }?.message ?: errorCode.message
        }
    }
} 