package com.attendly.config

import com.p6spy.engine.spy.appender.MessageFormattingStrategy

/**
 * 테스트용 간소화된 SQL 포맷터
 */
class TestQuerydslPrettySqlFormatter : MessageFormattingStrategy {
    override fun formatMessage(connectionId: Int, now: String, elapsed: Long, category: String, prepared: String?, sql: String?, url: String?): String {
        if (sql.isNullOrEmpty()) {
            return ""
        }
        
        return "\n--- TEST SQL ---\n$sql\n--- END TEST SQL ---"
    }
} 