package com.church.attendly.domain.repository

import com.church.attendly.config.TestQuerydslConfig
import com.church.attendly.domain.entity.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
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

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var gbsGroup: GbsGroup
    private lateinit var delegator: User
    private lateinit var delegatee: User
    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate

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

        // 기본 테스트 데이터 생성
        startDate = LocalDate.now()
        endDate = LocalDate.now().plusDays(7)
        
        entityManager.flush()
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
        assertEquals(1, activeDelegations.size)
        assertEquals(delegator.id, activeDelegations[0].delegator.id)
        assertEquals(delegatee.id, activeDelegations[0].delegatee.id)
        assertEquals(gbsGroup.id, activeDelegations[0].gbsGroup.id)
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
        assertEquals(1, activeDelegations.size)
        assertEquals(null, activeDelegations[0].endDate)
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
        assertEquals(0, activeDelegations.size)
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
        assertEquals(0, activeDelegations.size)
    }
    
    @Test
    fun `GBS 그룹 ID와 날짜로 활성화된 위임 관계를 찾을 수 있다`() {
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
        val activeDelegation = leaderDelegationRepository.findActiveByGbsGroupIdAndDate(
            gbsGroupId = gbsGroup.id!!,
            date = today
        )

        // then
        assertNotNull(activeDelegation)
        assertEquals(delegator.id, activeDelegation?.delegator?.id)
        assertEquals(delegatee.id, activeDelegation?.delegatee?.id)
        assertEquals(gbsGroup.id, activeDelegation?.gbsGroup?.id)
    }
    
    @Test
    fun `GBS 그룹에 활성화된 위임 관계가 없으면 null을 반환한다`() {
        // given
        val today = LocalDate.now()
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = today.plusDays(1),  // 아직 시작되지 않음
            endDate = today.plusDays(2)
        )
        leaderDelegationRepository.save(delegation)

        // when
        val activeDelegation = leaderDelegationRepository.findActiveByGbsGroupIdAndDate(
            gbsGroupId = gbsGroup.id!!,
            date = today
        )

        // then
        assertNull(activeDelegation)
    }
    
    @Test
    fun `위임받은 사용자 ID와 날짜로 모든 활성화된 위임 관계를 찾을 수 있다`() {
        // given
        val today = LocalDate.now()
        
        // 첫 번째 GBS 그룹 위임 관계
        val delegation1 = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = today.minusDays(1),
            endDate = today.plusDays(1)
        )
        leaderDelegationRepository.save(delegation1)
        
        // 두 번째 GBS 그룹 생성
        val gbsGroup2 = gbsGroupRepository.save(
            GbsGroup(
                name = "2GBS",
                village = village,
                termStartDate = LocalDate.now().minusMonths(1),
                termEndDate = LocalDate.now().plusMonths(5)
            )
        )
        
        // 두 번째 위임 관계
        val delegation2 = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup2,
            startDate = today.minusDays(2)
        )
        leaderDelegationRepository.save(delegation2)

        // when
        val activeDelegations = leaderDelegationRepository.findActiveByDelegateIdAndDate(
            userId = delegatee.id!!,
            date = today
        )

        // then
        assertEquals(2, activeDelegations.size)
        
        // 결과에 두 개의 다른 GBS 그룹이 포함되어 있는지 확인
        val gbsGroupIds = activeDelegations.map { it.gbsGroup.id }
        assertTrue(gbsGroupIds.contains(gbsGroup.id))
        assertTrue(gbsGroupIds.contains(gbsGroup2.id))
    }
    
    @Test
    fun `위임받은 사용자에게 활성화된 위임 관계가 없으면 빈 리스트를 반환한다`() {
        // given
        val today = LocalDate.now()
        
        // 모든 위임 관계가 만료됨
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = today.minusDays(5),
            endDate = today.minusDays(1)
        )
        leaderDelegationRepository.save(delegation)

        // when
        val activeDelegations = leaderDelegationRepository.findActiveByDelegateIdAndDate(
            userId = delegatee.id!!,
            date = today
        )

        // then
        assertTrue(activeDelegations.isEmpty())
    }

    @Test
    fun `findActiveByGbsGroupIdAndDate should return delegation when active delegation exists for the GBS group and date`() {
        // Given
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = startDate,
            endDate = endDate
        )
        entityManager.persist(delegation)
        entityManager.flush()
        
        // When
        val result = leaderDelegationRepository.findActiveByGbsGroupIdAndDate(gbsGroup.id!!, startDate.plusDays(1))
        
        // Then
        assertNotNull(result)
        assertEquals(delegator.id, result?.delegator?.id)
        assertEquals(delegatee.id, result?.delegatee?.id)
        assertEquals(gbsGroup.id, result?.gbsGroup?.id)
    }
    
    @Test
    fun `findActiveByGbsGroupIdAndDate should return null when no active delegation exists for the GBS group and date`() {
        // Given
        val pastEndDate = LocalDate.now().minusDays(1)
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = startDate.minusDays(10),
            endDate = pastEndDate
        )
        entityManager.persist(delegation)
        entityManager.flush()
        
        // When
        val result = leaderDelegationRepository.findActiveByGbsGroupIdAndDate(gbsGroup.id!!, startDate.plusDays(1))
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `findActiveByDelegateIdAndDate should return delegations when active delegations exist for the delegate and date`() {
        // Given
        val delegation1 = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = startDate,
            endDate = endDate
        )
        
        val village2 = Village(name = "2마을", department = delegatee.department)
        entityManager.persist(village2)
        
        val gbsGroup2 = GbsGroup(
            name = "GBS 그룹 2",
            village = village2,
            termStartDate = LocalDate.now(),
            termEndDate = LocalDate.now().plusMonths(6)
        )
        entityManager.persist(gbsGroup2)
        
        val delegation2 = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup2,
            startDate = startDate,
            endDate = endDate
        )
        
        entityManager.persist(delegation1)
        entityManager.persist(delegation2)
        entityManager.flush()
        
        // When
        val result = leaderDelegationRepository.findActiveByDelegateIdAndDate(delegatee.id!!, startDate.plusDays(1))
        
        // Then
        assertEquals(2, result.size)
        assertEquals(delegation1.id, result[0].id)
        assertEquals(delegation2.id, result[1].id)
    }
    
    @Test
    fun `findActiveByDelegateIdAndDate should return empty list when no active delegations exist for the delegate and date`() {
        // Given
        val pastEndDate = LocalDate.now().minusDays(1)
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = startDate.minusDays(10),
            endDate = pastEndDate
        )
        
        entityManager.persist(delegation)
        entityManager.flush()
        
        // When
        val result = leaderDelegationRepository.findActiveByDelegateIdAndDate(delegatee.id!!, startDate.plusDays(1))
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `findActiveDelegationsByDelegateeAndGbs should return delegations when active delegations exist for delegatee and GBS`() {
        // Given
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = startDate,
            endDate = endDate
        )
        
        entityManager.persist(delegation)
        entityManager.flush()
        
        // When
        val result = leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(
            delegateeId = delegatee.id!!,
            gbsId = gbsGroup.id!!,
            date = startDate.plusDays(1)
        )
        
        // Then
        assertEquals(1, result.size)
        assertEquals(delegation.id, result[0].id)
    }
    
    @Test
    fun `findActiveDelegationsByDelegateeAndGbs should return empty list when no active delegations exist`() {
        // Given
        val pastEndDate = LocalDate.now().minusDays(1)
        val delegation = LeaderDelegation(
            delegator = delegator,
            delegatee = delegatee,
            gbsGroup = gbsGroup,
            startDate = startDate.minusDays(10),
            endDate = pastEndDate
        )
        
        entityManager.persist(delegation)
        entityManager.flush()
        
        // When
        val result = leaderDelegationRepository.findActiveDelegationsByDelegateeAndGbs(
            delegateeId = delegatee.id!!,
            gbsId = gbsGroup.id!!,
            date = startDate.plusDays(1)
        )
        
        // Then
        assertTrue(result.isEmpty())
    }
} 