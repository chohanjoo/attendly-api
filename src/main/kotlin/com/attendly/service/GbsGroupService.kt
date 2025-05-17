package com.attendly.service

import com.attendly.api.dto.GbsGroupInfo
import com.attendly.api.dto.GbsGroupListResponse
import com.attendly.domain.repository.GbsGroupRepository
import com.attendly.domain.repository.VillageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class GbsGroupService(
    private val gbsGroupRepository: GbsGroupRepository,
    private val villageRepository: VillageRepository
) {

    /**
     * 특정 마을의 GBS 그룹 목록을 조회합니다.
     * 현재 유효한(termEndDate가 현재 날짜 이후인) GBS 그룹만 반환합니다.
     *
     * @param villageId 마을 ID
     * @return GBS 그룹 목록
     */
    @Transactional(readOnly = true)
    fun getGbsGroups(villageId: Long): GbsGroupListResponse {
        // 마을 존재 여부 확인
        villageRepository.findById(villageId).orElseThrow {
            IllegalArgumentException("존재하지 않는 마을입니다. ID: $villageId")
        }
        
        // GBS 그룹 목록 조회 - 현재 날짜를 기준으로 유효한 그룹만 조회
        val currentDate = LocalDate.now()
        val gbsGroups = gbsGroupRepository.findActiveGroupsByVillageId(villageId, currentDate)
        
        // DTO 변환
        val groupInfos = gbsGroups.map { group ->
            GbsGroupInfo(
                id = group.id!!,
                name = group.name,
                description = "", // GbsGroup 엔티티에 description 필드가 없으므로 빈 문자열 사용
                color = "#000000", // GbsGroup 엔티티에 color 필드가 없으므로 기본색상 사용
                isActive = group.termEndDate.isAfter(LocalDate.now()) // 현재 날짜보다 종료일이 이후인 경우 활성 상태로 간주
            )
        }
        
        return GbsGroupListResponse(groups = groupInfos)
    }
} 