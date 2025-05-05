# Docker 환경 실행 가이드

이 문서는 attendly-api 애플리케이션을 Docker 환경에서 실행하는 방법을 설명합니다. 신규 입사자가 로컬 개발 환경을 쉽게 설정하고 애플리케이션을 실행할 수 있도록 작성되었습니다.

## 사전 요구사항

- [Docker](https://www.docker.com/products/docker-desktop/) 설치
- [Docker Compose](https://docs.docker.com/compose/install/) 설치 (Docker Desktop에 일반적으로 포함됨)
- Git 저장소 복제: `git clone <repository-url>`

## 프로젝트 구조 이해하기

attendly-api 애플리케이션은 다음과 같은 Docker 구성을 사용합니다:

- `app`: Java/Kotlin 기반 Spring Boot 애플리케이션
- `db`: MySQL 8.0 데이터베이스

이 구성은 `docker-compose.yml` 파일에 정의되어 있으며, 데이터베이스 데이터는 `mysql-data` 볼륨에 저장됩니다.

## Docker 환경 실행 방법

### 1. 첫 번째 실행 또는 데이터베이스 초기화가 필요한 경우

데이터베이스를 포함한 모든 것을 처음부터 시작하려면 다음 단계를 따르세요:

```bash
# 1. 프로젝트 디렉토리로 이동
cd /path/to/attendly

# 2. 기존 컨테이너와 볼륨을 모두 중지 및 삭제 (데이터베이스 데이터 초기화)
docker-compose down -v

# 3. 이미지 빌드 및 컨테이너 시작
docker-compose up --build
```

`-v` 옵션은 볼륨을 함께 삭제하는 것으로, 이렇게 하면 데이터베이스가 완전히 초기화됩니다. 새로운 데이터베이스가 생성되고 Flyway 마이그레이션을 통해 스키마가 재생성됩니다.

이 방법은 다음과 같은 상황에서 사용합니다:
- 프로젝트를 처음 설정할 때
- 데이터베이스 스키마를 완전히 초기화하고 싶을 때
- 데이터베이스 관련 문제를 해결할 때
- 테스트 데이터를 초기화하고 싶을 때

### 2. 코드만 변경하고 데이터베이스 데이터를 유지하고 싶은 경우

애플리케이션 코드만 변경하고 기존 데이터베이스 데이터를 유지하고 싶다면 다음 단계를 따르세요:

```bash
# 1. 프로젝트 디렉토리로 이동
cd /path/to/attendly

# 2. 컨테이너만 중지 (볼륨은 유지)
docker-compose down

# 3. 이미지 다시 빌드 및 컨테이너 시작
docker-compose up --build
```

이 방법은 다음과 같은 상황에서 사용합니다:
- 애플리케이션 코드를 변경한 후 변경사항을 테스트할 때
- 기존 데이터베이스 데이터를 유지하면서 애플리케이션을 재시작하고 싶을 때
- 개발 중에 입력한 테스트 데이터를 유지하고 싶을 때

### 3. 백그라운드에서 실행하기

터미널을 계속 열어두지 않고 백그라운드에서 실행하려면 `-d` 옵션을 사용하세요:

```bash
docker-compose up -d --build
```

### 4. 로그 확인하기

애플리케이션 로그를 확인하려면 다음 명령어를 사용하세요:

```bash
# 모든 컨테이너의 로그 보기
docker-compose logs

# 실시간으로 로그 확인 (팔로우 모드)
docker-compose logs -f

# 특정 서비스의 로그만 보기 (예: 애플리케이션)
docker-compose logs app

# 특정 서비스의 로그를 실시간으로 확인
docker-compose logs -f app
```

### 5. 컨테이너 상태 확인하기

실행 중인 컨테이너의 상태를 확인하려면 다음 명령어를 사용하세요:

```bash
docker-compose ps
```

또는 모든 Docker 컨테이너를 확인하려면:

```bash
docker ps
```

## 애플리케이션 접속 방법

애플리케이션이 성공적으로 시작되면 다음 URL로 접속할 수 있습니다:

- **애플리케이션 API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html

## 데이터베이스 직접 접속하기

필요한 경우 MySQL 데이터베이스에 직접 접속할 수 있습니다:

```bash
# Docker 컨테이너 내의 MySQL에 접속
docker exec -it attendly-api-mysql mysql -u root -proot

# 데이터베이스 선택
USE church_attendly;

# 테이블 목록 확인
SHOW TABLES;
```

## 주의사항

1. **볼륨 삭제 (`-v`)**: 이 옵션을 사용하면 모든 데이터베이스 데이터가 삭제됩니다. 중요한 데이터가 있다면 미리 백업하세요.

2. **포트 충돌**: 이미 로컬 시스템에서 8080 포트(애플리케이션) 또는 3306 포트(MySQL)를 사용 중이라면, `docker-compose.yml` 파일에서 포트 매핑을 변경해야 합니다.

3. **메모리 사용량**: Docker가 사용할 수 있는 충분한 메모리가 있는지 확인하세요. 메모리가 부족하면 빌드나 실행 과정에서 문제가 발생할 수 있습니다.

## 문제 해결

### 데이터베이스 연결 오류

애플리케이션이 데이터베이스에 연결할 수 없는 경우:

1. 데이터베이스 컨테이너가 실행 중인지 확인: `docker-compose ps`
2. 데이터베이스 로그 확인: `docker-compose logs db`
3. 환경 변수가 올바르게 설정되었는지 확인: `docker-compose.yml` 파일의 `app` 서비스에 있는 `SPRING_DATASOURCE_URL` 등의 환경 변수

### 빌드 실패

이미지 빌드가 실패하는 경우:

1. 충분한 디스크 공간이 있는지 확인
2. Gradle 래퍼가 실행 권한을 가지고 있는지 확인: `chmod +x ./gradlew`
3. 빌드 로그 자세히 확인: `docker-compose build --no-cache app`

### 서비스 시작 실패

서비스가 시작되지 않는 경우:

1. 로그 확인: `docker-compose logs app`
2. 포트 충돌 여부 확인: `lsof -i :8080` 또는 `lsof -i :3306`
3. 컨테이너 상태 확인: `docker-compose ps`

## 추가 리소스

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Spring Boot Docker 공식 가이드](https://spring.io/guides/topicals/spring-boot-docker/) 