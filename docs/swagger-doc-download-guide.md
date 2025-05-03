# Swagger 문서 다운로드 가이드

이 문서는 Attendly 프로젝트의 Swagger API 문서를 다운로드하고 활용하는 방법을 설명합니다.

## 사전 준비

프로젝트가 실행 중이어야 합니다. 다음 명령어로 프로젝트를 실행하세요:

```bash
./gradlew bootRun
```

## JSON 형식으로 다운로드

### 전체 API 문서 다운로드

```bash
curl -o api-docs.json http://localhost:8080/api-docs
```

### 사용자 API 문서 다운로드

```bash
curl -o user-api-docs.json http://localhost:8080/api-docs/user-api
```

### 관리자 API 문서 다운로드

```bash
curl -o admin-api-docs.json http://localhost:8080/api-docs/admin-api
```

## YAML 형식으로 변환

YAML 형식의 문서를 생성하려면 Redocly CLI를 설치한 후 변환할 수 있습니다:

```bash
# Redocly CLI 설치
npm install -g @redocly/cli

# JSON을 YAML로 변환
redocly bundle api-docs.json -o openapi.yaml
```

## HTML 문서 생성

브라우저에서 바로 볼 수 있는 HTML 문서를 생성하려면:

```bash
# Redocly CLI 설치 (위에서 이미 설치했다면 생략)
npm install -g @redocly/cli

# HTML 문서 생성
redocly build-docs api-docs.json -o swagger-docs.html
```

## 생성된 파일 활용 방법

1. **JSON/YAML 파일**: 
   - [Swagger Editor](https://editor.swagger.io/)에 불러와서 확인
   - API 클라이언트 도구(Postman, Insomnia 등)에 가져와서 API 테스트

2. **HTML 문서**: 
   - 브라우저에서 바로 열어서 확인
   
3. **웹 브라우저에서 직접 확인**:
   - 프로젝트가 실행 중일 때 `http://localhost:8080/swagger-ui.html`에 접속

## 프론트엔드 개발자를 위한 API 문서 공유 방법

프론트엔드 개발자에게 API 문서를 공유할 때는 다음 파일들을 전달하는 것이 좋습니다:

1. `api-docs.json` 또는 `openapi.yaml` - API 클라이언트 생성 및 테스트 도구와의 연동을 위한 원본 문서
2. `swagger-docs.html` - 브라우저에서 바로 확인할 수 있는 문서

## 추가 정보

- 문서의 API 요청을 테스트하기 위해서는 Swagger UI(`http://localhost:8080/swagger-ui.html`)를 사용하는 것이 가장 편리합니다.
- 인증이 필요한 API를 테스트할 때는 먼저 `/auth/login` 엔드포인트를 통해 토큰을 획득한 후, Swagger UI의 'Authorize' 버튼을 클릭하여 해당 토큰을 설정해야 합니다. 