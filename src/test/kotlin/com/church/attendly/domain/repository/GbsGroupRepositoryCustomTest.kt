package com.church.attendly.domain.repository

import com.church.attendly.config.TestQuerydslConfig
import com.church.attendly.domain.entity.Department
import com.church.attendly.domain.entity.GbsGroup
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
class GbsGroupRepositoryCustomTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var gbsGroupRepository: GbsGroupRepository

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var otherVillage: Village

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

        otherVillage = Village(
            name = "2마을",
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(otherVillage)
    }

    @Test
    fun `findActiveGroupsByVillageId - 특정 마을의 활성화된 GBS 그룹을 조회한다`() {
        // given
        val activeGbsGroup = GbsGroup(
            name = "활성 GBS 그룹",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(activeGbsGroup)

        val inactiveGbsGroup = GbsGroup(
            name = "비활성 GBS 그룹",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 6, 30),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(inactiveGbsGroup)

        val otherVillageGbsGroup = GbsGroup(
            name = "다른 마을 GBS 그룹",
            village = otherVillage,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(otherVillageGbsGroup)

        // when
        val result = gbsGroupRepository.findActiveGroupsByVillageId(village.id!!, LocalDate.of(2023, 7, 1))

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("활성 GBS 그룹")
    }

    @Test
    fun `findByVillageAndTermDate - 특정 마을의 특정 기간 동안의 GBS 그룹을 조회한다`() {
        // given
        val gbsGroup1 = GbsGroup(
            name = "GBS 그룹 1",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup1)

        val gbsGroup2 = GbsGroup(
            name = "GBS 그룹 2",
            village = village,
            termStartDate = LocalDate.of(2023, 7, 1),
            termEndDate = LocalDate.of(2024, 6, 30),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup2)

        val otherVillageGbsGroup = GbsGroup(
            name = "다른 마을 GBS 그룹",
            village = otherVillage,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(otherVillageGbsGroup)

        // when
        val result = gbsGroupRepository.findByVillageAndTermDate(
            village = village,
            startDate = LocalDate.of(2023, 7, 1),
            endDate = LocalDate.of(2023, 12, 31)
        )

        // then
        assertThat(result).hasSize(2)
        assertThat(result).extracting("name").containsExactlyInAnyOrder("GBS 그룹 1", "GBS 그룹 2")
    }
} 