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
} 