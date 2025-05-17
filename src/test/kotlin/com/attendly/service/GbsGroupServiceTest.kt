package com.attendly.service

import com.attendly.api.dto.GbsGroupInfo
import com.attendly.api.dto.GbsGroupListResponse
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.Village
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.VillageRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.Optional

class GbsGroupServiceTest {

    private lateinit var gbsGroupRepository: GbsGroupRepository
    private lateinit var villageRepository: VillageRepository
    private lateinit var gbsGroupService: GbsGroupService

    @BeforeEach
    fun setUp() {
        gbsGroupRepository = mockk()
        villageRepository = mockk()
        gbsGroupService = GbsGroupService(gbsGroupRepository, villageRepository)
    }

    @Test
    fun `getGbsGroups는 유효한 마을 ID로 유효한 GBS 그룹 목록을 반환해야 한다`() {
        // given
        val villageId = 1L
        val currentDate = LocalDate.now()
        
        val village = mockk<Village>()
        
        val gbsGroup1 = mockk<GbsGroup>()
        every { gbsGroup1.id } returns 1L
        every { gbsGroup1.name } returns "그룹1"
        every { gbsGroup1.termEndDate } returns LocalDate.now().plusDays(30) // 종료일이 30일 후
        
        val gbsGroup2 = mockk<GbsGroup>()
        every { gbsGroup2.id } returns 2L
        every { gbsGroup2.name } returns "그룹2"
        every { gbsGroup2.termEndDate } returns LocalDate.now().plusDays(60) // 종료일이 60일 후
        
        every { villageRepository.findById(villageId) } returns Optional.of(village)
        every { gbsGroupRepository.findActiveGroupsByVillageId(villageId, any()) } returns listOf(gbsGroup1, gbsGroup2)
        
        // when
        val result = gbsGroupService.getGbsGroups(villageId)
        
        // then
        verify(exactly = 1) { villageRepository.findById(villageId) }
        verify(exactly = 1) { gbsGroupRepository.findActiveGroupsByVillageId(villageId, any()) }
        
        assertEquals(2, result.groups.size)
        assertEquals(1L, result.groups[0].id)
        assertEquals("그룹1", result.groups[0].name)
        assertEquals("", result.groups[0].description) // 빈 문자열로 변경
        assertEquals("#000000", result.groups[0].color) // 기본 색상으로 변경
        assertEquals(true, result.groups[0].isActive) // 종료일이 미래이므로 true
        
        assertEquals(2L, result.groups[1].id)
        assertEquals("그룹2", result.groups[1].name)
        assertEquals("", result.groups[1].description) // 빈 문자열로 변경
        assertEquals("#000000", result.groups[1].color) // 기본 색상으로 변경
        assertEquals(true, result.groups[1].isActive) // 종료일이 미래이므로 true
    }
    
    @Test
    fun `getGbsGroups는 존재하지 않는 마을 ID로 예외를 던져야 한다`() {
        // given
        val nonExistentVillageId = 999L
        every { villageRepository.findById(nonExistentVillageId) } returns Optional.empty()
        
        // when & then
        val exception = assertThrows<IllegalArgumentException> {
            gbsGroupService.getGbsGroups(nonExistentVillageId)
        }
        
        assertEquals("존재하지 않는 마을입니다. ID: $nonExistentVillageId", exception.message)
    }
} 