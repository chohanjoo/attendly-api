package com.church.attendly.domain.repository

import com.church.attendly.config.TestQuerydslConfig
import com.church.attendly.domain.entity.Department
import com.church.attendly.domain.entity.GbsGroup
import com.church.attendly.domain.entity.GbsLeaderHistory
import com.church.attendly.domain.entity.User
import com.church.attendly.domain.entity.Role
import com.church.attendly.domain.entity.Village
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@Import(TestQuerydslConfig::class)
class GbsLeaderHistoryRepositoryCustomTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var gbsLeaderHistoryRepository: GbsLeaderHistoryRepository

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var gbsGroup: GbsGroup
    private lateinit var leader: User
    private lateinit var currentLeaderHistory: GbsLeaderHistory
    private lateinit var pastLeaderHistory: GbsLeaderHistory

    @BeforeEach
    fun setUp() {
        // 부서 생성
        department = Department(
            name = "청년부",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(department)

        // 마을 생성
        village = Village(
            name = "1마을",
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(village)

        // GBS 그룹 생성
        gbsGroup = GbsGroup(
            name = "1GBS",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup)

        // 리더 생성
        leader = User(
            email = "leader@example.com",
            password = "password",
            name = "홍길동",
            role = Role.LEADER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(leader)

        // 현재 리더 히스토리 생성
        currentLeaderHistory = GbsLeaderHistory(
            gbsGroup = gbsGroup,
            leader = leader,
            startDate = LocalDate.of(2023, 1, 1),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(currentLeaderHistory)

        // 과거 리더 히스토리 생성
        pastLeaderHistory = GbsLeaderHistory(
            gbsGroup = gbsGroup,
            leader = leader,
            startDate = LocalDate.of(2022, 1, 1),
            endDate = LocalDate.of(2022, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(pastLeaderHistory)

        entityManager.flush()
        entityManager.clear()
    }

    @Test
    fun `findCurrentLeaderByGbsId 현재 리더를 찾아야 함`() {
        // when
        val result = gbsLeaderHistoryRepository.findCurrentLeaderByGbsId(gbsGroup.id!!)

        // then
        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(leader.id)
        assertThat(result?.name).isEqualTo("홍길동")
    }

    @Test
    fun `findByGbsGroupIdAndLeaderIdAndEndDateIsNull 현재 리더 히스토리를 찾아야 함`() {
        // when
        val result = gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsGroup.id!!, leader.id!!)

        // then
        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(currentLeaderHistory.id)
        assertThat(result?.leader?.id).isEqualTo(leader.id)
        assertThat(result?.endDate).isNull()
    }

    @Test
    fun `findByGbsGroupIdAndLeaderIdAndEndDateIsNull 과거 리더는 찾지 않아야 함`() {
        // when
        val result = gbsLeaderHistoryRepository.findByGbsGroupIdAndLeaderIdAndEndDateIsNull(gbsGroup.id!!, leader.id!!)

        // then
        assertThat(result).isNotNull
        assertThat(result?.id).isNotEqualTo(pastLeaderHistory.id)
    }
} 