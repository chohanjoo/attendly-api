package com.attendly.domain.repository

import com.attendly.config.TestQuerydslConfig
import com.attendly.domain.entity.*
import com.attendly.enums.MinistryStatus
import com.attendly.enums.Role
import com.attendly.enums.WorshipStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@Import(TestQuerydslConfig::class, AttendanceRepositoryImpl::class)
@ActiveProfiles("test")
class AttendanceRepositoryCustomTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var attendanceRepository: AttendanceRepository

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var gbsGroup1: GbsGroup
    private lateinit var gbsGroup2: GbsGroup
    private lateinit var leader: User
    private lateinit var member1: User
    private lateinit var member2: User
    private lateinit var weekStart: LocalDate

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
        gbsGroup1 = GbsGroup(
            name = "1GBS",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup1)

        gbsGroup2 = GbsGroup(
            name = "2GBS",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(gbsGroup2)

        // 리더 생성
        leader = User(
            name = "리더",
            email = "leader@example.com",
            password = "password",
            role = Role.LEADER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(leader)

        // 멤버 생성
        member1 = User(
            name = "멤버1",
            email = "member1@example.com",
            password = "password",
            role = Role.MEMBER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(member1)

        member2 = User(
            name = "멤버2",
            email = "member2@example.com",
            password = "password",
            role = Role.MEMBER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(member2)

        // 출석 정보 생성
        weekStart = LocalDate.of(2023, 5, 1) // 임의의 주 시작일

        // gbsGroup1에 속한 member1의 출석 정보
        val attendance1 = Attendance(
            member = member1,
            gbsGroup = gbsGroup1,
            weekStart = weekStart,
            worship = WorshipStatus.O,
            qtCount = 3,
            ministry = MinistryStatus.A,
            createdBy = leader,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(attendance1)

        // gbsGroup2에 속한 member2의 출석 정보
        val attendance2 = Attendance(
            member = member2,
            gbsGroup = gbsGroup2,
            weekStart = weekStart,
            worship = WorshipStatus.O,
            qtCount = 5,
            ministry = MinistryStatus.B,
            createdBy = leader,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(attendance2)

        // gbsGroup1에 속한 member2의 불참 정보
        val attendance3 = Attendance(
            member = member2,
            gbsGroup = gbsGroup1,
            weekStart = LocalDate.of(2023, 5, 8), // 다른 주차
            worship = WorshipStatus.X,
            qtCount = 0,
            ministry = MinistryStatus.C,
            createdBy = leader,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(attendance3)
        
        entityManager.flush()
    }

    @Test
    fun `GBS ID 목록과 주 시작일로 출석자 수를 계산할 수 있다`() {
        // when
        val gbsIds = listOf(gbsGroup1.id!!, gbsGroup2.id!!)
        val count = attendanceRepository.countAttendancesByGbsIdsAndWeek(gbsIds, weekStart)

        // then
        assertEquals(2L, count)
    }

    @Test
    fun `GBS ID와 주 시작일로 출석 상세 정보를 찾을 수 있다`() {
        // when
        val details = attendanceRepository.findDetailsByGbsIdAndWeek(gbsGroup1.id!!, weekStart)

        // then
        assertEquals(1, details.size)
        assertEquals(member1.id, details[0].member.id)
        assertEquals(gbsGroup1.id, details[0].gbsGroup.id)
        assertEquals(weekStart, details[0].weekStart)
    }

    @Test
    fun `마을 ID와 주 시작일로 출석 정보를 찾을 수 있다`() {
        // when
        val villageAttendances = attendanceRepository.findByVillageIdAndWeek(village.id!!, weekStart)

        // then
        assertEquals(2, villageAttendances.size)
        assertTrue(villageAttendances.any { it.gbsGroup.id == gbsGroup1.id })
        assertTrue(villageAttendances.any { it.gbsGroup.id == gbsGroup2.id })
    }

    @Test
    fun `해당하는 출석 정보가 없는 경우 빈 리스트가 반환된다`() {
        // when
        val nonExistentDate = LocalDate.of(2022, 1, 1)
        val villageAttendances = attendanceRepository.findByVillageIdAndWeek(village.id!!, nonExistentDate)

        // then
        assertTrue(villageAttendances.isEmpty())
    }

    @Test
    fun `다른 마을의 출석 정보는 조회되지 않아야 한다`() {
        // given
        val otherVillage = Village(
            name = "2마을",
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(otherVillage)
        entityManager.flush()

        // when
        val villageAttendances = attendanceRepository.findByVillageIdAndWeek(otherVillage.id!!, weekStart)

        // then
        assertTrue(villageAttendances.isEmpty())
    }

    @Test
    fun `GBS ID와 주 시작일 목록으로 출석 상세 정보를 찾을 수 있다`() {
        // given
        val weekStart2 = LocalDate.of(2023, 5, 8)
        val weekStarts = listOf(weekStart, weekStart2)
        
        // when
        val attendancesByWeek = attendanceRepository.findDetailsByGbsIdAndWeeks(gbsGroup1.id!!, weekStarts)
        
        // then
        assertEquals(2, attendancesByWeek.size) // 두 주차에 대한 맵 엔트리가 있어야 함
        assertEquals(1, attendancesByWeek[weekStart]?.size ?: 0) // 첫 번째 주차에는 출석 1건
        assertEquals(1, attendancesByWeek[weekStart2]?.size ?: 0) // 두 번째 주차에는 출석 1건
        
        // 첫 번째 주차 확인
        val firstWeekAttendances = attendancesByWeek[weekStart]
        assertEquals(member1.id, firstWeekAttendances?.get(0)?.member?.id)
        assertEquals(gbsGroup1.id, firstWeekAttendances?.get(0)?.gbsGroup?.id)
        
        // 두 번째 주차 확인
        val secondWeekAttendances = attendancesByWeek[weekStart2]
        assertEquals(member2.id, secondWeekAttendances?.get(0)?.member?.id)
        assertEquals(gbsGroup1.id, secondWeekAttendances?.get(0)?.gbsGroup?.id)
    }
    
    @Test
    fun `주 시작일 목록이 비어있으면 빈 맵이 반환된다`() {
        // when
        val attendancesByWeek = attendanceRepository.findDetailsByGbsIdAndWeeks(gbsGroup1.id!!, emptyList())
        
        // then
        assertTrue(attendancesByWeek.isEmpty())
    }
    
    @Test
    fun `해당하는 출석 정보가 없는 주차는 맵에 포함되지 않는다`() {
        // given
        val nonExistentDate = LocalDate.of(2022, 1, 1)
        val weekStarts = listOf(nonExistentDate)
        
        // when
        val attendancesByWeek = attendanceRepository.findDetailsByGbsIdAndWeeks(gbsGroup1.id!!, weekStarts)
        
        // then
        assertTrue(attendancesByWeek.isEmpty())
    }
} 