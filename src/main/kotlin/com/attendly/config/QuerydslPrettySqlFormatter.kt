package com.attendly.config

import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import org.hibernate.engine.jdbc.internal.FormatStyle

class QuerydslPrettySqlFormatter : MessageFormattingStrategy {
    
    companion object {
        private val NEW_LINE = System.lineSeparator()
        private const val QUERYDSL_PACKAGE = "com.attendly.domain.repository"
    }
    
    override fun formatMessage(connectionId: Int, now: String, elapsed: Long, category: String, prepared: String?, sql: String?, url: String?): String {
        if (sql.isNullOrEmpty()) {
            return ""
        }
        
        val formattedSql = formatSql(sql)
        val methodName = extractMethodName()
        
        val separator = "━".repeat(80)
        return buildString {
            append("\n$separator\n")
            append("⏱️ $now | ⌛ ${elapsed}ms | 🔄 Connection ID: $connectionId\n")
            if (methodName.isNotEmpty()) {
                append("📝 Method: $methodName\n")
            }
            append("\n🔗 바인딩된 SQL:\n")
            append(formattedSql)
            append("\n$separator")
        }
    }
    
    private fun formatSql(sql: String): String {
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
    
    private fun extractMethodName(): String {
        val stackTraceElements = Thread.currentThread().stackTrace
        
        // QueryDSL 레포지토리 메소드 찾기
        for (element in stackTraceElements) {
            if (element.className.startsWith(QUERYDSL_PACKAGE) && 
                element.className.contains("RepositoryImpl") && 
                !element.methodName.equals("findById") && 
                !element.methodName.equals("findAll") && 
                !element.methodName.equals("save") && 
                !element.methodName.equals("delete")) {
                
                return "${element.className.substringAfterLast(".")}.${element.methodName}()"
            }
        }
        
        return ""
    }
} 