# Spring Security 권한 관리 실전 튜토리얼

## Spring Security에서 권한을 관리하는 방법

안녕하세요! 이 튜토리얼에서는 Spring Security에서 사용자 권한을 관리하는 실제 예제와 함께 `hasRole`과 `hasAuthority`의 차이점을 알아보겠습니다.

## 기본 개념 이해하기

Spring Security에서 사용자 권한은 크게 두 가지 방식으로 나눌 수 있습니다:

- **Role (역할)**: 사용자의 직책이나 직위를 나타냅니다.
- **Authority (권한)**: 사용자가 할 수 있는 구체적인 작업을 나타냅니다.

```
🎭 Role: ADMIN, USER, MANAGER
🔑 Authority: READ_DATA, MODIFY_USER, DELETE_POST
```

## 예제 프로젝트: 학생 성적 관리 시스템

간단한 학생 성적 관리 시스템을 예로 들어보겠습니다:

1. **사용자 역할:**
   - ADMIN: 시스템 관리자
   - TEACHER: 교사
   - STUDENT: 학생

2. **권한:**
   - VIEW_GRADES: 성적 조회 가능
   - MODIFY_GRADES: 성적 수정 가능
   - MANAGE_USERS: 사용자 계정 관리 가능

## 1. 사용자 정보와 권한 설정하기

먼저 사용자 엔티티를 생성해봅시다:

```kotlin
// 사용자 역할 정의
enum class UserRole {
    ADMIN, TEACHER, STUDENT
}

// 사용자 엔티티
@Entity
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    val username: String,
    
    val password: String,
    
    @Enumerated(EnumType.STRING)
    val role: UserRole,
    
    @ElementCollection(fetch = FetchType.EAGER)
    val permissions: Set<String> = emptySet()
)
```

그리고 `UserDetails`를 구현한 클래스를 만들어 Spring Security에 사용자 정보를 제공합니다:

```kotlin
class SecurityUser(private val user: User) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        // 1. 역할 기반 권한 (ROLE_ 접두사 사용)
        val roleAuthority = SimpleGrantedAuthority("ROLE_${user.role.name}")
        
        // 2. 추가 권한 (접두사 없음)
        val permissions = user.permissions.map { 
            SimpleGrantedAuthority(it) 
        }
        
        // 역할과 권한을 모두 포함
        return listOf(roleAuthority) + permissions
    }
    
    // 기타 UserDetails 메서드 구현
    override fun getPassword(): String = user.password
    override fun getUsername(): String = user.username
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true
}
```

## 2. 컨트롤러에 권한 적용하기

이제 컨트롤러에 권한을 적용해봅시다:

```kotlin
@RestController
@RequestMapping("/api/grades")
class GradeController(private val gradeService: GradeService) {

    // 모든 학생 성적 조회 - 관리자와 교사만 가능
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    fun getAllGrades(): List<GradeDTO> {
        return gradeService.getAllGrades()
    }
    
    // 특정 학생의 성적 조회 - 해당 학생, 교사, 관리자만 가능
    @GetMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or (hasRole('STUDENT') and #studentId == authentication.principal.user.id)")
    fun getStudentGrades(@PathVariable studentId: Long): List<GradeDTO> {
        return gradeService.getStudentGrades(studentId)
    }
    
    // 성적 수정 - 성적 수정 권한이 있는 사용자만 가능
    @PutMapping("/{gradeId}")
    @PreAuthorize("hasAuthority('MODIFY_GRADES')")
    fun updateGrade(@PathVariable gradeId: Long, @RequestBody gradeDTO: GradeDTO): GradeDTO {
        return gradeService.updateGrade(gradeId, gradeDTO)
    }
    
    // 시스템 설정 변경 - 관리자만 가능
    @PutMapping("/settings")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('MANAGE_SYSTEM')")
    fun updateSettings(@RequestBody settings: SystemSettings): SystemSettings {
        return gradeService.updateSettings(settings)
    }
}
```

## 3. 권한 부여 이해하기: hasRole vs hasAuthority

```kotlin
// 예제 1: hasRole 사용
@PreAuthorize("hasRole('ADMIN')")
fun adminFunction() { ... }
// Spring은 내부적으로 'ROLE_ADMIN' 권한을 확인합니다

// 예제 2: hasAuthority 사용
@PreAuthorize("hasAuthority('MODIFY_GRADES')")
fun modifyGradesFunction() { ... }
// 정확히 'MODIFY_GRADES' 문자열과 일치하는 권한을 확인합니다
```

### hasRole과 hasAuthority의 차이점 실습

1. **hasRole의 동작 방식:**
```kotlin
@PreAuthorize("hasRole('ADMIN')")
// 내부적으로 'ROLE_ADMIN' 문자열을 찾습니다
```

2. **hasAuthority의 동작 방식:**
```kotlin
@PreAuthorize("hasAuthority('MODIFY_GRADES')")
// 정확히 'MODIFY_GRADES' 문자열을 찾습니다
```

3. **권한 문자열 직접 확인:**
```kotlin
// 'ROLE_' 접두사를 포함한 권한 확인
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
// hasRole('ADMIN')과 동일

// 'ROLE_' 접두사 없이 확인 (실패할 수 있음!)
@PreAuthorize("hasRole('MODIFY_GRADES')")
// 실제로는 'ROLE_MODIFY_GRADES'를 찾음 (의도와 다름)
```

## 4. 실전 문제 해결 시나리오

### 시나리오 1: 403 Forbidden 오류 발생

문제: 관리자 권한이 있는 사용자가 관리자 페이지에 접근할 때 403 오류가 발생합니다.

```kotlin
// 컨트롤러:
@PreAuthorize("hasAuthority('ADMIN')")
@GetMapping("/admin/dashboard")
fun adminDashboard() { ... }

// UserDetailsAdapter:
override fun getAuthorities(): Collection<GrantedAuthority> {
    return listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
}
```

원인: `hasAuthority('ADMIN')`는 정확히 "ADMIN" 문자열을 찾지만, 실제 부여된 권한은 "ROLE_ADMIN"입니다.

해결 방법:
```kotlin
// 방법 1: 컨트롤러 수정
@PreAuthorize("hasAuthority('ROLE_ADMIN')")

// 방법 2: 컨트롤러 수정 (더 권장됨)
@PreAuthorize("hasRole('ADMIN')")

// 방법 3: UserDetailsAdapter 수정
override fun getAuthorities(): Collection<GrantedAuthority> {
    // role 기반 권한과 추가 권한 모두 설정
    return listOf(
        SimpleGrantedAuthority("ROLE_ADMIN"),
        SimpleGrantedAuthority("ADMIN")
    )
}
```

### 시나리오 2: 복잡한 권한 검사 구현

문제: 교사는 자신이 담당하는 학급의 학생 성적만 수정할 수 있어야 합니다.

해결 방법: 사용자 정의 권한 검사 메서드 사용

```kotlin
@Component("gradeSecurityExpressions")
class GradeSecurityExpressions {
    @Autowired
    private lateinit var classService: ClassService
    
    fun canModifyStudentGrade(studentId: Long): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = (authentication.principal as SecurityUser).user
        
        // 관리자는 모든 학생의 성적을 수정할 수 있음
        if (user.role == UserRole.ADMIN) {
            return true
        }
        
        // 교사는 자신의 학급 학생의 성적만 수정할 수 있음
        if (user.role == UserRole.TEACHER) {
            return classService.isStudentInTeacherClass(studentId, user.id!!)
        }
        
        return false
    }
}

// 컨트롤러에서 사용
@PutMapping("/students/{studentId}/grades")
@PreAuthorize("@gradeSecurityExpressions.canModifyStudentGrade(#studentId)")
fun updateStudentGrades(@PathVariable studentId: Long, @RequestBody grades: List<GradeDTO>) {
    // ...
}
```

## 5. 모범 사례 및 팁

### 권한 설계 원칙

1. **역할 기반 접근 제어(RBAC):**
   - 일반적인 접근 제어는 역할(Role)로 구현
   - 예: `hasRole('ADMIN')`, `hasRole('TEACHER')`

2. **권한 기반 접근 제어(PBAC):**
   - 세부적인 작업 권한은 Authority로 구현
   - 예: `hasAuthority('MODIFY_GRADES')`, `hasAuthority('VIEW_REPORTS')`

3. **역할과 권한의 조합:**
   ```kotlin
   @PreAuthorize("hasRole('ADMIN') or (hasRole('TEACHER') and hasAuthority('MODIFY_GRADES'))")
   ```

### 명명 규칙 제안

```
// 역할 (ROLE_ 접두사는 Spring Security가 자동으로 추가)
ADMIN, USER, TEACHER, STUDENT

// 권한 (동사_명사 형식 권장)
VIEW_GRADES, MODIFY_USER, CREATE_COURSE, DELETE_COMMENT
```

## 6. 테스트하기

Spring Security 권한을 테스트하는 방법을 알아봅시다:

```kotlin
@WebMvcTest(GradeController::class)
@WithMockUser(username = "admin", roles = ["ADMIN"])
class GradeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @MockBean
    private lateinit var gradeService: GradeService
    
    @Test
    fun `관리자는 모든 성적을 조회할 수 있다`() {
        // given
        val grades = listOf(GradeDTO(1L, 1L, "수학", 95))
        given(gradeService.getAllGrades()).willReturn(grades)
        
        // when & then
        mockMvc.perform(get("/api/grades/all"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].score").value(95))
    }
    
    @Test
    @WithMockUser(username = "student", roles = ["STUDENT"])
    fun `학생은 모든 성적을 조회할 수 없다`() {
        // when & then
        mockMvc.perform(get("/api/grades/all"))
            .andExpect(status().isForbidden)
    }
    
    @Test
    @WithMockUser(username = "teacher", authorities = ["MODIFY_GRADES"])
    fun `성적 수정 권한이 있는 사용자는 성적을 수정할 수 있다`() {
        // given
        val updatedGrade = GradeDTO(1L, 1L, "수학", 90)
        given(gradeService.updateGrade(1L, updatedGrade)).willReturn(updatedGrade)
        
        // when & then
        mockMvc.perform(put("/api/grades/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(ObjectMapper().writeValueAsString(updatedGrade)))
            .andExpect(status().isOk)
    }
}
```

## 결론

Spring Security에서 권한 관리는 `hasRole`과 `hasAuthority`의 차이를 이해하는 것부터 시작합니다:

- **hasRole**: 사용자의 역할에 따른 접근 제어 (내부적으로 'ROLE_' 접두사 추가)
- **hasAuthority**: 특정 작업에 대한 권한 검사 (정확한 문자열 비교)

권한 관리의 핵심은 일관성입니다. `hasRole`과 `hasAuthority`를 목적에 맞게 구분하여 사용하고, 명확한 명명 규칙을 따르면 보안 관련 버그를 줄이고 유지보수가 쉬운 시스템을 구축할 수 있습니다. 