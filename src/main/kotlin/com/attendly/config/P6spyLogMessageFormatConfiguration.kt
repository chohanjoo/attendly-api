package com.attendly.config

import com.p6spy.engine.logging.Category
import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import org.hibernate.engine.jdbc.internal.FormatStyle
import java.text.SimpleDateFormat
import java.util.*

class P6spyLogMessageFormatConfiguration : MessageFormattingStrategy {
    override fun formatMessage(
        connectionId: Int, 
        now: String?, 
        elapsed: Long, 
        category: String?, 
        prepared: String?, 
        sql: String?, 
        url: String?
    ): String {
        val formatSql = formatSql(category, sql)
        return StringBuilder()
            .append("\n\n")
            .append(now)
            .append(" | ")
            .append(elapsed)
            .append("ms | ")
            .append(category)
            .append(" | connection ")
            .append(connectionId)
            .append("\n")
            .append(formatSql)
            .append("\n")
            .toString()
    }

    private fun formatSql(category: String?, sql: String?): String? {
        if (sql == null || sql.trim() == "") return sql

        // Only format Statement and Batch
        val isStatement = Category.STATEMENT.name == category
        val isPreparedStatement = "statement" == category
        val isBatch = Category.BATCH.name == category

        if (!isStatement && !isPreparedStatement && !isBatch) {
            return sql
        }

        val formattedSql = sql.trim().replace(Regex("\\s+"), " ")

        return when {
            formattedSql.startsWith("create") || formattedSql.startsWith("alter") || formattedSql.startsWith("comment") -> FormatStyle.DDL.formatter.format(formattedSql)
            else -> FormatStyle.BASIC.formatter.format(formattedSql)
        }
    }

    private fun getCurrentTimeFormatted(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
    }
} 