package com.attendly.config

import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import org.hibernate.engine.jdbc.internal.FormatStyle
import java.text.SimpleDateFormat
import java.util.*

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
            val currentTime = SimpleDateFormat("HH:mm:ss.SSS").format(Date())
            val formattedSql = formatSql(sql)
            
            val separator = "━".repeat(50)
            val result = StringBuilder()
                .append("\n$separator\n")
                .append("⏱️ $currentTime | ⌛ ${elapsed}ms | 🔄 연결ID: $connectionId\n")
                .append("\n🔗 바인딩된 SQL:\n")
                .append(formattedSql)
            
            result.append("\n$separator")
            result.toString()
        }
    }
    
    private fun formatSql(sql: String?): String {
        if (sql == null || sql.trim() == "") return ""
        
        val formattedSql = sql.trim().replace(Regex("\\s+"), " ")
        
        return when {
            formattedSql.startsWith("create", ignoreCase = true) || 
            formattedSql.startsWith("alter", ignoreCase = true) || 
            formattedSql.startsWith("comment", ignoreCase = true) -> FormatStyle.DDL.formatter.format(formattedSql)
            formattedSql.startsWith("select", ignoreCase = true) || 
            formattedSql.startsWith("insert", ignoreCase = true) || 
            formattedSql.startsWith("update", ignoreCase = true) || 
            formattedSql.startsWith("delete", ignoreCase = true) -> FormatStyle.BASIC.formatter.format(formattedSql)
            else -> formattedSql
        }
    }
} 