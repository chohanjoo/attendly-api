package com.attendly.api.dto

import com.attendly.domain.entity.Department
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.LeaderDelegation
import com.attendly.domain.entity.Role
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LeaderDelegationResponseTest {

    @Test
    fun `from should convert LeaderDelegation to LeaderDelegationResponse correctly`() {
        // Given
        val department = Department(id = 1L, name = "청년부")
        
        val village = Village(
            id = 1L,
            name = "1마을",
            department = department
        )
        
        val delegator = User(
            id = 1L,
            name = "김위임자",
            role = Role.LEADER,
            department = department
        )
        
        val delegatee = User(
            id = 2L,
            name = "이수임자",
            role = Role.LEADER,
            department = department
        )
        
        val gbsGroup = GbsGroup(
            id = 3L,
            name = "소그룹3",
            village = village,
            termStartDate = LocalDate.of(2023, 3, 1),
            termEndDate = LocalDate.of(2023, 8, 31)
        )
        
        val startDate = LocalDate.of(2023, 7, 1)
        val endDate = LocalDate.of(2023, 7, 31)
        
        val delegation = LeaderDelegation(
            id = 5L,
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = startDate,
            endDate = endDate
        )
        
        // When
        val response = LeaderDelegationResponse.from(delegation)
        
        // Then
        assertEquals(5L, response.id)
        assertEquals(1L, response.delegatorId)
        assertEquals("김위임자", response.delegatorName)
        assertEquals(2L, response.delegateeId)
        assertEquals("이수임자", response.delegateeName)
        assertEquals(3L, response.gbsGroupId)
        assertEquals("소그룹3", response.gbsGroupName)
        assertEquals(startDate, response.startDate)
        assertEquals(endDate, response.endDate)
    }
    
    @Test
    fun `from should handle null endDate`() {
        // Given
        val department = Department(id = 1L, name = "청년부")
        
        val village = Village(
            id = 1L,
            name = "1마을",
            department = department
        )
        
        val delegator = User(
            id = 1L,
            name = "김위임자",
            role = Role.LEADER,
            department = department
        )
        
        val delegatee = User(
            id = 2L,
            name = "이수임자",
            role = Role.LEADER,
            department = department
        )
        
        val gbsGroup = GbsGroup(
            id = 3L,
            name = "소그룹3",
            village = village,
            termStartDate = LocalDate.of(2023, 3, 1),
            termEndDate = LocalDate.of(2023, 8, 31)
        )
        
        val startDate = LocalDate.of(2023, 7, 1)
        
        val delegation = LeaderDelegation(
            id = 5L,
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = startDate,
            endDate = null
        )
        
        // When
        val response = LeaderDelegationResponse.from(delegation)
        
        // Then
        assertEquals(5L, response.id)
        assertEquals(1L, response.delegatorId)
        assertEquals("김위임자", response.delegatorName)
        assertEquals(2L, response.delegateeId)
        assertEquals("이수임자", response.delegateeName)
        assertEquals(3L, response.gbsGroupId)
        assertEquals("소그룹3", response.gbsGroupName)
        assertEquals(startDate, response.startDate)
        assertEquals(null, response.endDate)
    }
} 