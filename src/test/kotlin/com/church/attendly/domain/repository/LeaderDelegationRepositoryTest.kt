package com.church.attendly.domain.repository

import com.church.attendly.config.TestQuerydslConfig
import com.church.attendly.domain.entity.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@Import(TestQuerydslConfig::class)
class LeaderDelegationRepositoryTest {

    @Autowired
    private lateinit var leaderDelegationRepository: LeaderDelegationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var villageRepository: VillageRepository

    @Autowired
    private lateinit var gbsGroupRepository: GbsGroupRepository

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var gbsGroup: GbsGroup
    private lateinit var delegator: User
    private lateinit var delegatee: User

    @BeforeEach
    fun setup() {
        // 부서 생성
        department = departmentRepository.save(
            Department(
                name = "청년부"
            )
        )

        // 마을 생성
        village = villageRepository.save(
            Village(
                name = "1마을",
                department = department
            )
        )

        // GBS 그룹 생성
        gbsGroup = gbsGroupRepository.save(
            GbsGroup(
                name = "1GBS",
                village = village,
                termStartDate = LocalDate.now().minusMonths(1),
                termEndDate = LocalDate.now().plusMonths(5)
            )
        )

        // 위임자(리더) 생성
        delegator = userRepository.save(
            User(
                name = "리더",
                role = Role.LEADER,
                department = department
            )
        )

        // 피위임자 생성
        delegatee = userRepository.save(
            User(
                name = "부리더",
                role = Role.MEMBER,
                department = department
            )
        )
    }

    @Test
    fun `현재 활성화된 위임 관계를 찾을 수 있다`() {
        // given
        val today = LocalDate.now()
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = today.minusDays(1),
            endDate = today.plusDays(1)
        )
        leaderDelegationRepository.save(delegation)

        // when
        val activeDelegations = leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(
            delegateeId = delegatee.id!!,
            gbsId = gbsGroup.id!!,
            date = today
        )

        // then
        assertThat(activeDelegations).hasSize(1)
        assertThat(activeDelegations[0].delegator.id).isEqualTo(delegator.id)
        assertThat(activeDelegations[0].delegatee.id).isEqualTo(delegatee.id)
        assertThat(activeDelegations[0].gbsGroup.id).isEqualTo(gbsGroup.id)
    }

    @Test
    fun `종료일이 없는 위임 관계도 찾을 수 있다`() {
        // given
        val today = LocalDate.now()
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = today.minusDays(1)
        )
        leaderDelegationRepository.save(delegation)

        // when
        val activeDelegations = leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(
            delegateeId = delegatee.id!!,
            gbsId = gbsGroup.id!!,
            date = today
        )

        // then
        assertThat(activeDelegations).hasSize(1)
        assertThat(activeDelegations[0].endDate).isNull()
    }

    @Test
    fun `종료된 위임 관계는 찾지 않는다`() {
        // given
        val today = LocalDate.now()
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = today.minusDays(2),
            endDate = today.minusDays(1)
        )
        leaderDelegationRepository.save(delegation)

        // when
        val activeDelegations = leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(
            delegateeId = delegatee.id!!,
            gbsId = gbsGroup.id!!,
            date = today
        )

        // then
        assertThat(activeDelegations).isEmpty()
    }

    @Test
    fun `시작되지 않은 위임 관계는 찾지 않는다`() {
        // given
        val today = LocalDate.now()
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = today.plusDays(1),
            endDate = today.plusDays(2)
        )
        leaderDelegationRepository.save(delegation)

        // when
        val activeDelegations = leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(
            delegateeId = delegatee.id!!,
            gbsId = gbsGroup.id!!,
            date = today
        )

        // then
        assertThat(activeDelegations).isEmpty()
    }
} 