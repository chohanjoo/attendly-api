package com.church.attendly.domain.model

import java.time.LocalDate

data class GbsMemberHistorySearchCondition(
    val gbsId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate
) 