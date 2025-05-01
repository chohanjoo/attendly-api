# 교회 출석 관리 시스템 테스트 시나리오 가이드

## 소개

이 문서는 Attendly(교회 출석 관리 시스템)의 HTTP 테스트 시나리오 작성 및 실행에 관한 가이드입니다. 이 가이드는 신규 입사자가 시스템의 API 흐름을 이해하고 새로운 테스트 시나리오를 쉽게 작성할 수 있도록 돕기 위해 작성되었습니다.

## HTTP 파일 기반 테스트란?

HTTP 파일은 API 요청을 순차적으로 정의하고 실행할 수 있는 파일 형식입니다. Visual Studio Code의 REST Client 확장 프로그램이나 IntelliJ IDEA의 HTTP Client 기능을 통해 이 파일을 실행할 수 있습니다. 이를 통해 Postman과 같은 별도의 도구 없이도 API 테스트 자동화가 가능합니다.

## 기본 시나리오 구조

테스트 시나리오 파일(`test-scenario.http`)은 다음과 같은 구조로 구성되어 있습니다:

1. 파일 상단에 시나리오에 대한 설명 주석
2. 각 API 요청 단계 (각 단계는 `###`으로 구분)
3. 각 요청에 대한 설명 및 이름 지정 (`# @name`)
4. 변수 저장 및 이후 요청에서의 재사용 (`@variableName = {{response.body.field}}`)

## 기본 시나리오 설명

현재 구현된 기본 시나리오(`test-scenario.http`)는 다음과 같은 흐름으로 구성되어 있습니다:

1. **계정 관리**
   - ADMIN 권한 계정 생성
   - 로그인 및 액세스 토큰 획득
   - 마을장, 교역자, 리더, 조원 계정 생성

2. **조직 구조 관리**
   - 부서(대학부) 생성
   - 마을 생성
   - GBS 그룹 생성 및 구성원 배정

3. **출석 관리**
   - 각 GBS별 출석 데이터 등록
   - 출석 데이터 조회 및 검증

## HTTP 파일 작성 가이드

### 1. 기본 요청 형식

```
### 요청 설명
# @name 요청이름
METHOD URL
Header-Name: Header-Value

요청 본문(JSON 등)
```

### 2. 변수 저장 및 사용

```
# 응답에서 값 추출하여 변수에 저장
@variableName = {{requestName.response.body.field}}

# 다음 요청에서 변수 사용
{
  "someField": {{variableName}}
}
```

### 3. 인증 토큰 관리

로그인 후 발급받은 토큰을 이후 요청에서 사용:

```
# 로그인 요청
# @name login
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}

# 토큰 저장
@accessToken = {{login.response.body.accessToken}}

# 토큰 사용
GET http://localhost:8080/api/some-endpoint
Authorization: Bearer {{accessToken}}
```

## 새로운 시나리오 추가하기

새로운 테스트 시나리오를 추가하려면 다음 단계를 따르세요:

1. **시나리오 계획하기**
   - 테스트하려는 기능의 흐름 파악
   - 필요한 API 요청 순서 정의
   - 각 요청 간의 데이터 의존성 확인

2. **HTTP 파일 생성하기**
   - `test-scenarios/` 디렉토리에 새 `.http` 파일 생성 (예: `new-feature-test.http`)
   - 파일 상단에 시나리오 설명 주석 추가

3. **요청 작성하기**
   - 각 단계별 요청 작성
   - 요청 간 데이터 전달을 위한 변수 설정
   - 적절한 설명과 이름 부여

4. **테스트 및 문서화**
   - 작성한 시나리오 실행 및 검증
   - 필요시 이 가이드 문서 업데이트

## 자주 사용되는 API 엔드포인트

### 인증 관련 API
- 회원가입: `POST /auth/signup`
- 로그인: `POST /auth/login`
- 토큰 갱신: `POST /auth/refresh`

### 관리자 API
- 사용자 관리: `POST|PUT|GET /api/admin/users`
- 부서 관리: `POST|PUT|GET /api/admin/organization/departments`
- 마을 관리: `POST|PUT|GET /api/admin/organization/villages`
- GBS 그룹 관리: `POST|PUT|GET /api/admin/organization/gbs-groups`

### 출석 관리 API
- 출석 등록: `POST /api/attendance`
- 출석 조회: `GET /api/attendance`
- 마을 출석 현황: `GET /api/village/{id}/attendance`

## 예제: 사용자 시나리오 추가하기

다음은 새로운 시나리오를 작성하는 예제입니다:

1. **시나리오 정의**: "리더 권한 위임 기능 테스트"
2. **HTTP 파일 생성**: `test-scenarios/leader-delegation-test.http`
3. **요청 작성**:

```http
# 리더 권한 위임 테스트 시나리오
# 
# 이 시나리오는 리더가 일시적으로 다른 사용자에게 권한을 위임하는 기능을 테스트합니다.

### 1. 리더 계정 로그인
# @name leaderLogin
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "email": "leader@example.com",
  "password": "password"
}

@leaderToken = {{leaderLogin.response.body.accessToken}}

### 2. 리더 권한 위임 생성
POST http://localhost:8080/api/delegations
Content-Type: application/json
Authorization: Bearer {{leaderToken}}

{
  "delegateToUserId": 5,
  "startDate": "2023-07-01",
  "endDate": "2023-07-10",
  "reason": "휴가"
}

### 3. 위임된 리더로 로그인
# @name delegatedLogin
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "email": "delegate@example.com", 
  "password": "password"
}

@delegatedToken = {{delegatedLogin.response.body.accessToken}}

### 4. 위임된 권한으로 출석 등록 테스트
POST http://localhost:8080/api/attendance
Content-Type: application/json
Authorization: Bearer {{delegatedToken}}

{
  "gbsId": 1,
  "weekStart": "2023-07-02",
  "attendances": [
    {
      "memberId": 10,
      "worship": "O",
      "qtCount": 5,
      "ministry": "A"
    }
  ]
}
```

## 문제 해결

### 일반적인 문제와 해결 방법

1. **응답 변수 추출 실패**
   - 응답 본문의 구조 확인
   - 오타 및 필드명 확인
   - 응답 상태 코드 확인 (오류 응답인 경우 변수 추출 실패)

2. **인증 오류**
   - 토큰 만료 여부 확인
   - 권한 레벨 확인
   - 토큰 형식 확인 (`Bearer` 접두사 포함 여부)

3. **요청 순서 의존성**
   - 요청 간 의존성 있는 경우 순서대로 실행했는지 확인
   - 변수가 올바르게 설정되었는지 확인

## 결론

HTTP 파일 기반 테스트는 API 통합 테스트를 간편하게 수행할 수 있는 효과적인 방법입니다. 이 가이드를 참고하여 새로운 기능이 추가될 때마다 관련 테스트 시나리오를 작성하면, API의 정상 동작을 빠르게 검증하고 회귀 테스트를 수행할 수 있습니다.

## 참고 자료

- [VS Code REST Client 확장 사용법](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
- [IntelliJ HTTP Client 사용법](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
- [Attendly API 문서](/api-documentation.md) 


# 프롬프트
테스트 자동화를 하고 싶어. 시나리오를 줄때니 해당 시나리오대로 실행할 수 있는 http 파일을 만들어줘.
모든 API 는 localhost:8080 으로 호출해.

# 시나리오 
1. ADMIN 권한의 계정을 생성한다. 
  - id : hanjoo@naver.com
  - pw : test123!@# 

2. /api/admin/organization/departments API 으로 "univ4" 부서 생성 
3. /api/admin/organization/villages API 으로 "R" 마을 생성
4. "univ4" 와 "R" 에 속하는 교역자 1명, 리더 2명, 조원 4명 계정을 생성해
4. /api/admin/organization/gbs-groups API 로 리더 1명, 조원 2명씩 묶어줘 
5. 각 GBS 별로 /api/attendance API 로 출석을 등록해 
6. GBS 별로 정상적으로 출석이 등록되었는지 확인해