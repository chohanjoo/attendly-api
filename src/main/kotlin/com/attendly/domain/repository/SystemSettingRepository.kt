package com.attendly.domain.repository

import com.attendly.domain.entity.SystemSetting
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SystemSettingRepository : JpaRepository<SystemSetting, Long> {
    fun findByKey(key: String): Optional<SystemSetting>
    fun findByKeyIn(keys: List<String>): List<SystemSetting>
} 