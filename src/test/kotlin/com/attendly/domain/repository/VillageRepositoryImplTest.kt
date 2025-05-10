package com.attendly.domain.repository

import com.attendly.config.TestQuerydslConfig
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.Village
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
class VillageRepositoryImplTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var villageRepository: VillageRepository

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
    fun `findVillageWithActiveGbsGroups - 특정 마을과 해당 날짜에 활성화된 GBS 그룹을 함께 조회한다`() {
        // given
        val activeGbsGroup1 = GbsGroup(
            name = "활성 GBS 그룹 1",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(activeGbsGroup1)

        val activeGbsGroup2 = GbsGroup(
            name = "활성 GBS 그룹 2",
            village = village,
            termStartDate = LocalDate.of(2023, 3, 1),
            termEndDate = LocalDate.of(2023, 10, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(activeGbsGroup2)

        val inactiveGbsGroup = GbsGroup(
            name = "비활성 GBS 그룹",
            village = village,
            termStartDate = LocalDate.of(2022, 1, 1),
            termEndDate = LocalDate.of(2022, 12, 31),
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

        entityManager.flush()
        entityManager.clear()

        // when
        val targetDate = LocalDate.of(2023, 7, 1)
        val result = villageRepository.findVillageWithActiveGbsGroups(village.id!!, targetDate)

        // then
        assertThat(result).isNotNull
        assertThat(result!!.id).isEqualTo(village.id)
        assertThat(result.name).isEqualTo("1마을")
        
        // 활성 GBS 그룹만 포함되어 있는지 확인
        val gbsGroupNames = result.gbsGroups.map { it.name }
        assertThat(gbsGroupNames).hasSize(2)
        assertThat(gbsGroupNames).containsExactlyInAnyOrder("활성 GBS 그룹 1", "활성 GBS 그룹 2")
    }
    
    @Test
    fun `findVillageWithActiveGbsGroups - 해당 날짜에 활성화된 GBS 그룹이 없으면 null을 반환한다`() {
        // given
        val inactiveGbsGroup = GbsGroup(
            name = "비활성 GBS 그룹",
            village = village,
            termStartDate = LocalDate.of(2022, 1, 1),
            termEndDate = LocalDate.of(2022, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(inactiveGbsGroup)

        val futureGbsGroup = GbsGroup(
            name = "미래 GBS 그룹",
            village = village,
            termStartDate = LocalDate.of(2024, 1, 1),
            termEndDate = LocalDate.of(2024, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(futureGbsGroup)

        entityManager.flush()
        entityManager.clear()

        // when
        val targetDate = LocalDate.of(2023, 7, 1)
        val result = villageRepository.findVillageWithActiveGbsGroups(village.id!!, targetDate)

        // then
        assertThat(result).isNull()
    }
    
    @Test
    fun `findVillageWithActiveGbsGroups - 존재하지 않는 마을 ID로 조회하면 null을 반환한다`() {
        // given
        val nonExistingVillageId = 9999L

        // when
        val result = villageRepository.findVillageWithActiveGbsGroups(nonExistingVillageId, LocalDate.now())

        // then
        assertThat(result).isNull()
    }
} 