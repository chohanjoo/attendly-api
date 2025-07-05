package com.attendly.domain.repository

import com.attendly.config.TestQuerydslConfig
import com.attendly.domain.entity.Department
import com.attendly.domain.entity.GbsGroup
import com.attendly.domain.entity.GbsLeaderHistory
import com.attendly.enums.Role
import com.attendly.domain.entity.User
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
class GbsGroupRepositoryCustomTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var gbsGroupRepository: GbsGroupRepository

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var otherVillage: Village
    private lateinit var leader: User

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
        
        // 리더 생성
        leader = User(
            name = "리더",
            role = Role.LEADER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(leader)
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

    @Test
    fun `findWithCurrentLeader - GBS 그룹과 현재 리더를 함께 조회한다`() {
        // given
        val gbsGroup = GbsGroup(
            name = "GBS 그룹",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup)
        
        val leaderHistory = GbsLeaderHistory(
            gbsGroup = gbsGroup,
            leader = leader,
            startDate = LocalDate.of(2023, 1, 1),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(leaderHistory)
        
        // when
        val result = gbsGroupRepository.findWithCurrentLeader(gbsGroup.id!!)
        
        // then
        assertThat(result).isNotNull
        assertThat(result!!.gbsGroup.id).isEqualTo(gbsGroup.id)
        assertThat(result.gbsGroup.name).isEqualTo("GBS 그룹")
        assertThat(result.leader).isNotNull
        assertThat(result.leader!!.name).isEqualTo("리더")
    }
    
    @Test
    fun `findWithCurrentLeader - 리더가 없는 경우 GBS 그룹만 조회된다`() {
        // given
        val gbsGroup = GbsGroup(
            name = "리더 없는 GBS 그룹",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup)
        
        // when
        val result = gbsGroupRepository.findWithCurrentLeader(gbsGroup.id!!)
        
        // then
        assertThat(result).isNotNull
        assertThat(result!!.gbsGroup.id).isEqualTo(gbsGroup.id)
        assertThat(result.gbsGroup.name).isEqualTo("리더 없는 GBS 그룹")
        assertThat(result.leader).isNull()
    }
    
    @Test
    fun `findWithCurrentLeader - 종료된 리더 이력은 조회되지 않는다`() {
        // given
        val gbsGroup = GbsGroup(
            name = "GBS 그룹",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup)
        
        val endedLeaderHistory = GbsLeaderHistory(
            gbsGroup = gbsGroup,
            leader = leader,
            startDate = LocalDate.of(2023, 1, 1),
            endDate = LocalDate.of(2023, 6, 30), // 종료된 리더 이력
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(endedLeaderHistory)
        
        // when
        val result = gbsGroupRepository.findWithCurrentLeader(gbsGroup.id!!)
        
        // then
        assertThat(result).isNotNull
        assertThat(result!!.gbsGroup.id).isEqualTo(gbsGroup.id)
        assertThat(result.gbsGroup.name).isEqualTo("GBS 그룹")
        assertThat(result.leader).isNull()
    }

    @Test
    fun `findVillageGbsWithLeaderInfo - 마을의 모든 GBS와 리더 정보를 한 번에 조회한다`() {
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
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup2)

        val leader1 = User(
            name = "리더 1",
            role = Role.LEADER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(leader1)

        val leaderHistory1 = GbsLeaderHistory(
            gbsGroup = gbsGroup1,
            leader = leader1,
            startDate = LocalDate.of(2023, 1, 1),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(leaderHistory1)

        // GBS 그룹 2는 리더가 없음

        // when
        val result = gbsGroupRepository.findVillageGbsWithLeaderInfo(village.id!!, LocalDate.of(2023, 7, 1))

        // then
        assertThat(result).hasSize(2)
        
        val gbsInfo1 = result.find { it.gbsName == "GBS 그룹 1" }
        assertThat(gbsInfo1).isNotNull
        assertThat(gbsInfo1!!.gbsId).isEqualTo(gbsGroup1.id)
        assertThat(gbsInfo1.leaderName).isEqualTo("리더 1")
        assertThat(gbsInfo1.leaderId).isEqualTo(leader1.id)
        
        val gbsInfo2 = result.find { it.gbsName == "GBS 그룹 2" }
        assertThat(gbsInfo2).isNotNull
        assertThat(gbsInfo2!!.gbsId).isEqualTo(gbsGroup2.id)
        assertThat(gbsInfo2.leaderName).isNull()
        assertThat(gbsInfo2.leaderId).isNull()
    }
    
    @Test
    fun `findVillageGbsWithLeaderInfo - 활성 GBS만 조회한다`() {
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

        // when
        val result = gbsGroupRepository.findVillageGbsWithLeaderInfo(village.id!!, LocalDate.of(2023, 7, 1))

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].gbsName).isEqualTo("활성 GBS 그룹")
    }
    
    @Test
    fun `findVillageGbsWithLeaderInfo - 다른 마을의 GBS는 조회되지 않는다`() {
        // given
        val gbsGroup1 = GbsGroup(
            name = "1마을 GBS 그룹",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup1)

        val gbsGroup2 = GbsGroup(
            name = "2마을 GBS 그룹",
            village = otherVillage,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup2)

        // when
        val result = gbsGroupRepository.findVillageGbsWithLeaderInfo(village.id!!, LocalDate.of(2023, 7, 1))

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].gbsName).isEqualTo("1마을 GBS 그룹")
    }

    @Test
    fun `findVillageGbsWithLeaderInfo - 존재하지 않는 마을의 경우 빈 리스트를 반환한다`() {
        // given
        val nonExistentVillageId = 999L

        // when
        val result = gbsGroupRepository.findVillageGbsWithLeaderInfo(nonExistentVillageId, LocalDate.of(2023, 7, 1))

        // then
        assertThat(result).isEmpty()
    }
} 