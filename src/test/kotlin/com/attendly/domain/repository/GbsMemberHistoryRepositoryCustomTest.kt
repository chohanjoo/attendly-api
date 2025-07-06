package com.attendly.domain.repository

import com.attendly.config.TestQuerydslConfig
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.GbsMemberHistory
import com.attendly.enums.Role
import com.attendly.domain.entity.User
import com.attendly.domain.entity.Village
import com.attendly.domain.model.GbsMemberHistorySearchCondition
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@Import(TestQuerydslConfig::class)
class GbsMemberHistoryRepositoryCustomTest {

    @Autowired
    private lateinit var gbsMemberHistoryRepository: GbsMemberHistoryRepository

    @Autowired
    private lateinit var gbsGroupRepository: GbsGroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var villageRepository: VillageRepository

    @Test
    fun `countActiveMembers는 특정 날짜의 활성 멤버 수를 반환한다`() {
        // given
        val department = departmentRepository.save(Department(name = "테스트 부서"))
        val village = villageRepository.save(Village(name = "테스트 마을", department = department))
        
        val gbsGroup = gbsGroupRepository.save(GbsGroup(
            name = "테스트 GBS",
            village = village,
            termStartDate = LocalDate.now().minusDays(1),
            termEndDate = LocalDate.now().plusDays(1)
        ))
        
        val member = userRepository.save(User(
            name = "테스트 멤버",
            role = Role.MEMBER,
            department = department
        ))
        
        val activeMember = GbsMemberHistory(
            gbsGroup = gbsGroup,
            member = member,
            startDate = LocalDate.now().minusDays(1),
            endDate = LocalDate.now().plusDays(1)
        )
        val inactiveMember = GbsMemberHistory(
            gbsGroup = gbsGroup,
            member = member,
            startDate = LocalDate.now().minusDays(2),
            endDate = LocalDate.now().minusDays(1)
        )
        gbsMemberHistoryRepository.save(activeMember)
        gbsMemberHistoryRepository.save(inactiveMember)

        // when
        val count = gbsMemberHistoryRepository.countActiveMembers(gbsGroup.id!!, LocalDate.now())

        // then
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `findActiveMembers는 특정 날짜의 활성 멤버 목록을 반환한다`() {
        // given
        val department = departmentRepository.save(Department(name = "테스트 부서"))
        val village = villageRepository.save(Village(name = "테스트 마을", department = department))
        
        val gbsGroup = gbsGroupRepository.save(GbsGroup(
            name = "테스트 GBS",
            village = village,
            termStartDate = LocalDate.now().minusDays(1),
            termEndDate = LocalDate.now().plusDays(1)
        ))
        
        val member = userRepository.save(User(
            name = "테스트 멤버",
            role = Role.MEMBER,
            department = department
        ))
        
        val activeMember = GbsMemberHistory(
            gbsGroup = gbsGroup,
            member = member,
            startDate = LocalDate.now().minusDays(1),
            endDate = LocalDate.now().plusDays(1)
        )
        val inactiveMember = GbsMemberHistory(
            gbsGroup = gbsGroup,
            member = member,
            startDate = LocalDate.now().minusDays(2),
            endDate = LocalDate.now().minusDays(1)
        )
        gbsMemberHistoryRepository.save(activeMember)
        gbsMemberHistoryRepository.save(inactiveMember)

        // when
        val activeMembers = gbsMemberHistoryRepository.findActiveMembers(
            GbsMemberHistorySearchCondition(
                gbsId = gbsGroup.id!!,
                startDate = LocalDate.now(),
                endDate = LocalDate.now()
            )
        )

        // then
        assertThat(activeMembers).hasSize(1)
        assertThat(activeMembers[0].id).isEqualTo(activeMember.id)
    }

    @Test
    fun `findActiveMembers는 주어진 기간 동안의 멤버 목록을 반환한다`() {
        // given
        val department = departmentRepository.save(Department(name = "테스트 부서"))
        val village = villageRepository.save(Village(name = "테스트 마을", department = department))
        
        val gbsGroup = gbsGroupRepository.save(GbsGroup(
            name = "테스트 GBS",
            village = village,
            termStartDate = LocalDate.now().minusDays(1),
            termEndDate = LocalDate.now().plusDays(1)
        ))
        
        val member = userRepository.save(User(
            name = "테스트 멤버",
            role = Role.MEMBER,
            department = department
        ))
        
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(2)
        
        val member1 = GbsMemberHistory(
            gbsGroup = gbsGroup,
            member = member,
            startDate = startDate.minusDays(1),
            endDate = endDate.plusDays(1)
        )
        val member2 = GbsMemberHistory(
            gbsGroup = gbsGroup,
            member = member,
            startDate = startDate.plusDays(1),
            endDate = endDate.plusDays(1)
        )
        val member3 = GbsMemberHistory(
            gbsGroup = gbsGroup,
            member = member,
            startDate = startDate.minusDays(2),
            endDate = startDate.minusDays(1)
        )
        
        gbsMemberHistoryRepository.save(member1)
        gbsMemberHistoryRepository.save(member2)
        gbsMemberHistoryRepository.save(member3)

        // when
        val members = gbsMemberHistoryRepository.findActiveMembers(
            GbsMemberHistorySearchCondition(
                gbsId = gbsGroup.id!!,
                startDate = startDate,
                endDate = endDate
            )
        )

        // then
        assertThat(members).hasSize(2)
        assertThat(members.map { it.id }).contains(member1.id, member2.id)
    }

    @Test
    fun `findActiveMembersByVillageGbsIds는 여러 GBS의 활성 멤버들을 한 번에 조회한다`() {
        // given
        val department = departmentRepository.save(Department(name = "테스트 부서"))
        val village = villageRepository.save(Village(name = "테스트 마을", department = department))
        
        val gbsGroup1 = gbsGroupRepository.save(GbsGroup(
            name = "테스트 GBS 1",
            village = village,
            termStartDate = LocalDate.now().minusDays(10),
            termEndDate = LocalDate.now().plusDays(10)
        ))
        
        val gbsGroup2 = gbsGroupRepository.save(GbsGroup(
            name = "테스트 GBS 2",
            village = village,
            termStartDate = LocalDate.now().minusDays(10),
            termEndDate = LocalDate.now().plusDays(10)
        ))
        
        val member1 = userRepository.save(User(
            name = "테스트 멤버 1",
            role = Role.MEMBER,
            department = department
        ))
        
        val member2 = userRepository.save(User(
            name = "테스트 멤버 2",
            role = Role.MEMBER,
            department = department
        ))
        
        val member3 = userRepository.save(User(
            name = "테스트 멤버 3",
            role = Role.MEMBER,
            department = department
        ))
        
        val activeMember1 = gbsMemberHistoryRepository.save(GbsMemberHistory(
            gbsGroup = gbsGroup1,
            member = member1,
            startDate = LocalDate.now().minusDays(5),
            endDate = null // 현재 활성
        ))
        
        val activeMember2 = gbsMemberHistoryRepository.save(GbsMemberHistory(
            gbsGroup = gbsGroup1,
            member = member2,
            startDate = LocalDate.now().minusDays(3),
            endDate = LocalDate.now().plusDays(3) // 현재 활성
        ))
        
        val activeMember3 = gbsMemberHistoryRepository.save(GbsMemberHistory(
            gbsGroup = gbsGroup2,
            member = member3,
            startDate = LocalDate.now().minusDays(2),
            endDate = null // 현재 활성
        ))
        
        // 비활성 멤버 (종료됨)
        gbsMemberHistoryRepository.save(GbsMemberHistory(
            gbsGroup = gbsGroup2,
            member = member1,
            startDate = LocalDate.now().minusDays(5),
            endDate = LocalDate.now().minusDays(1) // 종료됨
        ))

        // when
        val result = gbsMemberHistoryRepository.findActiveMembersByVillageGbsIds(
            listOf(gbsGroup1.id!!, gbsGroup2.id!!), 
            LocalDate.now()
        )

        // then
        assertThat(result).hasSize(3)
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(
            activeMember1.id, activeMember2.id, activeMember3.id
        )
        
        // GBS별로 그룹화했을 때 정확히 조회되는지 확인
        val membersByGbs = result.groupBy { it.gbsGroup.id }
        assertThat(membersByGbs[gbsGroup1.id]).hasSize(2)
        assertThat(membersByGbs[gbsGroup2.id]).hasSize(1)
    }

    @Test
    fun `findActiveMembersByVillageGbsIds는 빈 GBS ID 리스트에 대해 빈 리스트를 반환한다`() {
        // when
        val result = gbsMemberHistoryRepository.findActiveMembersByVillageGbsIds(
            emptyList(), 
            LocalDate.now()
        )

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `findActiveMembersByVillageGbsIds는 존재하지 않는 GBS ID에 대해 빈 리스트를 반환한다`() {
        // when
        val result = gbsMemberHistoryRepository.findActiveMembersByVillageGbsIds(
            listOf(999L, 1000L), 
            LocalDate.now()
        )

        // then
        assertThat(result).isEmpty()
    }
    
    @Test
    fun `findActiveMembersByVillageGbsIds는 특정 날짜에 활성인 멤버만 조회한다`() {
        // given
        val department = departmentRepository.save(Department(name = "테스트 부서"))
        val village = villageRepository.save(Village(name = "테스트 마을", department = department))
        
        val gbsGroup = gbsGroupRepository.save(GbsGroup(
            name = "테스트 GBS",
            village = village,
            termStartDate = LocalDate.now().minusDays(10),
            termEndDate = LocalDate.now().plusDays(10)
        ))
        
        val member = userRepository.save(User(
            name = "테스트 멤버",
            role = Role.MEMBER,
            department = department
        ))
        
        // 2023년 1월 1일부터 2023년 6월 30일까지 활성
        val memberHistory = gbsMemberHistoryRepository.save(GbsMemberHistory(
            gbsGroup = gbsGroup,
            member = member,
            startDate = LocalDate.of(2023, 1, 1),
            endDate = LocalDate.of(2023, 6, 30)
        ))

        // when - 활성 기간 중인 날짜로 조회
        val resultActive = gbsMemberHistoryRepository.findActiveMembersByVillageGbsIds(
            listOf(gbsGroup.id!!), 
            LocalDate.of(2023, 3, 15)
        )
        
        // when - 활성 기간이 아닌 날짜로 조회
        val resultInactive = gbsMemberHistoryRepository.findActiveMembersByVillageGbsIds(
            listOf(gbsGroup.id!!), 
            LocalDate.of(2023, 7, 1)
        )

        // then
        assertThat(resultActive).hasSize(1)
        assertThat(resultActive[0].id).isEqualTo(memberHistory.id)
        
        assertThat(resultInactive).isEmpty()
    }
} 