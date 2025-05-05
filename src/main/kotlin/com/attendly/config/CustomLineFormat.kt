package com.attendly.config

import com.p6spy.engine.spy.appender.MessageFormattingStrategy

class CustomLineFormat : MessageFormattingStrategy {
    override fun formatMessage(
        connectionId: Int,
        now: String?,
        elapsed: Long,
        category: String?,
        prepared: String?,
        sql: String?,
        url: String?
    ): String {
        return if (sql.isNullOrEmpty()) {
            ""
        } else {
            // 바인딩된 변수가 있다면 prepared를 표시, 없다면 sql만 표시
            if (!prepared.isNullOrEmpty() && prepared != sql) {
                "실행 SQL: $prepared\n바인딩된 SQL: $sql\n실행시간: ${elapsed}ms"
            } else {
                "실행 SQL: $sql\n실행시간: ${elapsed}ms"
            }
        }
    }
} 