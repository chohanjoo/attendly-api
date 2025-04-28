package com.church.attendly.domain.repository

import com.church.attendly.config.TestQuerydslConfig
import com.church.attendly.domain.entity.*
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@Import(TestQuerydslConfig::class, AttendanceRepositoryImpl::class)
class AttendanceRepositoryTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var attendanceRepository: AttendanceRepository

    private lateinit var department: Department
    private lateinit var village: Village
    private lateinit var gbsGroup: GbsGroup
    private lateinit var leader: User
    private lateinit var member: User
    private lateinit var weekStart: LocalDate
    private lateinit var attendance: Attendance

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
        member = User(
            name = "멤버",
            email = "member@example.com",
            password = "password",
            role = Role.MEMBER,
            department = department,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(member)

        // 출석 정보 생성
        weekStart = LocalDate.of(2023, 5, 1) // 임의의 주 시작일
        attendance = Attendance(
            member = member,
            gbsGroup = gbsGroup,
            weekStart = weekStart,
            worship = WorshipStatus.O,
            qtCount = 3,
            ministry = MinistryStatus.A,
            createdBy = leader,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(attendance)
        entityManager.flush()
    }

    @Test
    fun `GBS 그룹과 주 시작일로 출석 정보를 찾을 수 있다`() {
        // when
        val foundAttendances = attendanceRepository.findByGbsGroupAndWeekStart(gbsGroup, weekStart)

        // then
        assertEquals(1, foundAttendances.size)
        assertEquals(member.id, foundAttendances[0].member.id)
        assertEquals(gbsGroup.id, foundAttendances[0].gbsGroup.id)
        assertEquals(weekStart, foundAttendances[0].weekStart)
    }

    @Test
    fun `멤버와 날짜 범위로 출석 정보를 찾을 수 있다`() {
        // when
        val startDate = LocalDate.of(2023, 4, 1)
        val endDate = LocalDate.of(2023, 6, 1)
        val foundAttendances = attendanceRepository.findByMemberAndWeekStartBetween(member, startDate, endDate)

        // then
        assertEquals(1, foundAttendances.size)
        assertEquals(member.id, foundAttendances[0].member.id)
        assertEquals(weekStart, foundAttendances[0].weekStart)
    }

    @Test
    fun `매칭되는 날짜가 없을 경우 빈 리스트가 반환된다`() {
        // when
        val wrongStartDate = LocalDate.of(2022, 1, 1)
        val wrongEndDate = LocalDate.of(2022, 12, 31)
        val foundAttendances = attendanceRepository.findByMemberAndWeekStartBetween(member, wrongStartDate, wrongEndDate)

        // then
        assertTrue(foundAttendances.isEmpty())
    }

    @Test
    fun `다른 GBS 그룹으로 조회했을 때 결과가 없어야 한다`() {
        // given
        val otherGbsGroup = GbsGroup(
            name = "2GBS",
            village = village,
            termStartDate = LocalDate.of(2023, 1, 1),
            termEndDate = LocalDate.of(2023, 12, 31),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        entityManager.persist(otherGbsGroup)
        entityManager.flush()

        // when
        val foundAttendances = attendanceRepository.findByGbsGroupAndWeekStart(otherGbsGroup, weekStart)

        // then
        assertTrue(foundAttendances.isEmpty())
    }
} 