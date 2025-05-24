# HashiCorp Vault 설정 가이드

## 1. HashiCorp Vault란?

HashiCorp Vault는 민감한 정보(secrets)를 안전하게 저장하고 관리하기 위한 도구입니다. 이 문서에서는 Attendly API 애플리케이션에서 Discord Webhook URL과 같은 민감 정보를 안전하게 관리하기 위해 Vault를 사용하는 방법을 설명합니다.

## 2. Vault 사용의 장점

- **보안 강화**: 비밀번호, API 키, 토큰 등과 같은 민감한 정보를 소스 코드나 환경 변수에 직접 노출하지 않습니다.
- **중앙 집중식 관리**: 모든 민감 정보를 한 곳에서 관리할 수 있습니다.
- **접근 제어**: 누가, 어떤 시크릿에 접근할 수 있는지 세밀하게 제어할 수 있습니다.
- **동적 시크릿 생성**: 필요할 때 동적으로 시크릿을 생성할 수 있습니다(예: 데이터베이스 자격 증명).
- **시크릿 교체**: 민감 정보를 주기적으로 교체할 수 있는 기능을 제공합니다.

## 3. Vault 아키텍처

### 3.1 기본 구성요소

1. **Vault 서버**: 민감 정보를 저장하고 관리하는 중앙 서버
2. **Storage Backend**: Vault의 데이터를 저장하는 공간(개발 환경은 메모리, 운영 환경은 보통 Consul, PostgreSQL 등 사용)
3. **Secret Engines**: 다양한 유형의 시크릿을 처리하는 컴포넌트(KV, Database, AWS, 등)
4. **Authentication Methods**: 클라이언트 인증을 처리하는 방법(Token, AppRole, AWS IAM 등)

### 3.2 KV (Key-Value) 시크릿 엔진

Attendly API 프로젝트에서는 기본적인 KV 시크릿 엔진을 사용하여 Discord Webhook URL과 같은 정적 시크릿을 저장합니다. KV 엔진은 KV 버전 2(버전 관리 기능 제공)를 사용합니다.

## 4. Attendly API 프로젝트의 Vault 설정

### 4.1 Docker Compose 설정

Vault 서버를 Docker Compose로 실행하기 위한 설정은 다음과 같습니다:

```yaml
services:
  vault:
    image: hashicorp/vault:latest
    container_name: attendly-api-vault
    ports:
      - "8200:8200"
    environment:
      VAULT_DEV_ROOT_TOKEN_ID: "00000000-0000-0000-0000-000000000000"
      VAULT_DEV_LISTEN_ADDRESS: "0.0.0.0:8200"
      VAULT_ADDR: "http://127.0.0.1:8200"
    cap_add:
      - IPC_LOCK
    volumes:
      - ./vault/data:/vault/data
    command: ["sh", "-c", "vault server -dev && sleep infinity"]
    healthcheck:
      test: ["CMD", "wget", "--spider", "--quiet", "http://127.0.0.1:8200/v1/sys/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  vault-init:
    image: alpine:latest
    container_name: attendly-api-vault-init
    depends_on:
      vault:
        condition: service_healthy
    volumes:
      - ./vault/data:/vault/data
    environment:
      VAULT_ADDR: "http://vault:8200"
      VAULT_TOKEN: "00000000-0000-0000-0000-000000000000"
    command: 
      - "/bin/sh"
      - "-c"
      - |
        apk add --no-cache curl jq unzip
        # Vault CLI 설치
        curl -sSL -o /tmp/vault.zip https://releases.hashicorp.com/vault/1.19.4/vault_1.19.4_linux_amd64.zip
        unzip -o /tmp/vault.zip -d /usr/local/bin
        chmod +x /usr/local/bin/vault
        
        echo "Vault 복원 스크립트 시작..."
        sleep 5
        
        # 백업 파일 목록 확인 (확장자가 .json인 파일만)
        BACKUP_FILES=$(find /vault/data -name "*.json" -not -name "keys.json" | wc -l)
        
        if [ "$BACKUP_FILES" -gt 0 ]; then
          echo "백업 파일이 발견되었습니다. 복원을 시작합니다..."
          
          # keys.json 파일 확인 및 읽기
          if [ -f "/vault/data/keys.json" ]; then
            for KEY in $(cat /vault/data/keys.json | jq -r '.[]'); do
              if [ -f "/vault/data/${KEY}.json" ]; then
                DATA=$(cat "/vault/data/${KEY}.json" | jq -r '.data.data | to_entries | map("\(.key)=\(.value)") | join(" ")')
                vault kv put "secret/$KEY" $DATA
                echo "복원됨: secret/$KEY"
              fi
            done
            echo "Vault 데이터 복원이 완료되었습니다."
          else
            echo "keys.json 파일이 없어 복원할 수 없습니다."
          fi
        else
          echo "백업 파일이 없습니다. 복원 작업을 건너뜁니다."
        fi
        
        echo "Vault 복원 스크립트 종료"
    restart: "no"

  vault-backup:
    image: alpine:latest
    container_name: attendly-api-vault-backup
    depends_on:
      vault-init:
        condition: service_completed_successfully
    volumes:
      - ./vault/data:/vault/data
    environment:
      VAULT_ADDR: "http://vault:8200"
      VAULT_TOKEN: "00000000-0000-0000-0000-000000000000"
    command: 
      - "/bin/sh"
      - "-c"
      - |
        apk add --no-cache curl jq unzip
        # Vault CLI 설치
        curl -sSL -o /tmp/vault.zip https://releases.hashicorp.com/vault/1.19.4/vault_1.19.4_linux_amd64.zip
        unzip -o /tmp/vault.zip -d /usr/local/bin
        chmod +x /usr/local/bin/vault
        
        echo "Vault 데이터 백업 서비스 시작..."
        while true; do
          echo "10초마다 Vault 데이터 백업 중..."
          
          # secret/ 경로의 모든 키 목록 가져오기
          mkdir -p /vault/data
          vault kv list -format=json secret/ 2>/dev/null > /vault/data/keys.json
          
          if [ -s /vault/data/keys.json ]; then
            for KEY in $(cat /vault/data/keys.json | jq -r '.[]'); do
              # 각 키에 대한 값 가져오기
              vault kv get -format=json secret/$KEY > "/vault/data/${KEY}.json"
              echo "백업: secret/$KEY"
            done
          else
            echo "백업할 시크릿이 없습니다."
          fi
          
          sleep 10
        done
    restart: always
```

위 설정의 주요 부분 설명:
- `VAULT_DEV_ROOT_TOKEN_ID`: 개발 환경에서 사용할 루트 토큰 ID (운영 환경에서는 복잡한 토큰 사용 필요)
- `VAULT_DEV_LISTEN_ADDRESS`: Vault 서버가 리스닝할 주소 및 포트
- `cap_add: IPC_LOCK`: Vault가 메모리를 스왑하지 않도록 하는 Linux 기능
- `volumes`: Vault 데이터를 저장하기 위한 볼륨 설정
- `command: ["sh", "-c", "vault server -dev && sleep infinity"]`: 개발 모드로 Vault 서버 실행

### 4.1.1 Vault 데이터 지속성 문제와 해결책

개발 모드(`-dev`)로 실행되는 Vault는 모든 데이터를 메모리에 저장하기 때문에 컨테이너가 재시작될 때마다 데이터가 손실됩니다. 이 문제를 해결하기 위해 다음과 같은 3개의 컨테이너로 구성된 아키텍처를 구현하였습니다:

1. **vault**: 메인 Vault 서버입니다. 개발 모드로 실행되어 빠른 시작과 편리한 사용이 가능합니다.
2. **vault-init**: Vault가 시작된 후 실행되는 컨테이너로, 로컬 디렉토리에 저장된 백업 파일에서 시크릿을 복원합니다.
3. **vault-backup**: Vault가 시작된 후 백그라운드에서 주기적으로 실행되며, Vault의 모든 시크릿을 로컬 디렉토리에 백업합니다.

이 구성을 통해 얻는 장점:

- 개발 모드의 간편함을 유지하면서도 데이터 지속성 확보
- Docker Compose down/up 후에도 시크릿 유지
- 별도의 파일 시스템 스토리지 백엔드 설정 없이 간단한 구성
- 주기적인 자동 백업으로 데이터 손실 최소화

### 4.1.2 Vault 백업 및 복원 작동 원리

1. **백업 프로세스**:
   - `vault-backup` 컨테이너는 10초마다 모든 시크릿을 점검합니다.
   - `vault kv list` 명령으로 모든 시크릿 키를 가져와 `keys.json` 파일에 저장합니다.
   - 각 시크릿에 대해 `vault kv get` 명령을 실행하여 그 값을 개별 JSON 파일로 저장합니다.

2. **복원 프로세스**:
   - `vault-init` 컨테이너는 Vault 서버가 정상적으로 시작된 후 실행됩니다.
   - 저장된 `keys.json` 파일을 읽어 백업된 시크릿 목록을 확인합니다.
   - 각 시크릿에 대한 JSON 파일을 읽고 `vault kv put` 명령으로 Vault에 복원합니다.

3. **의존성 관리**:
   - `vault` → `vault-init` → `vault-backup` → `app` 순서로 시작됩니다.
   - 각 서비스는 이전 서비스의 상태를 체크한 후 시작되어 안정적인 구동을 보장합니다.

### 4.2 Spring Boot 애플리케이션 설정

Spring Cloud Vault를 사용하여 Vault와 통합하기 위한 설정은 다음과 같습니다:

#### 4.2.1 의존성 추가

`build.gradle.kts` 파일에 다음과 같이 의존성을 추가합니다:

```kotlin
ext {
    set("springCloudVersion", "2023.0.0")
}

dependencies {
    // 기존 의존성들...
    
    // Vault 의존성 추가
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${project.ext["springCloudVersion"]}")
    }
}
```

#### 4.2.2 애플리케이션 설정

`application.yml` 파일에 다음과 같이 Vault 연결 설정을 추가합니다:

```yaml
spring:
  application:
    name: attendly-api
  # 다른 설정들...
  cloud:
    vault:
      token: ${SPRING_CLOUD_VAULT_TOKEN:00000000-0000-0000-0000-000000000000}
      scheme: ${SPRING_CLOUD_VAULT_SCHEME:http}
      host: ${SPRING_CLOUD_VAULT_HOST:localhost}
      port: ${SPRING_CLOUD_VAULT_PORT:8200}
      kv:
        enabled: true
        default-context: attendly-api
  config:
    import: vault://
```

주요 설정 항목:
- `spring.cloud.vault.token`: Vault 인증을 위한 토큰
- `spring.cloud.vault.scheme`: Vault 연결 프로토콜(http 또는 https)
- `spring.cloud.vault.host`: Vault 서버 호스트
- `spring.cloud.vault.port`: Vault 서버 포트
- `spring.cloud.vault.kv.enabled`: KV 시크릿 엔진 활성화
- `spring.cloud.vault.kv.default-context`: KV 시크릿 경로의 기본 컨텍스트
- `spring.config.import: vault://`: Spring Boot의 Config Data API를 사용하여 Vault 데이터 가져오기

#### 4.2.3 Docker Compose 환경 설정

애플리케이션 컨테이너에서 Vault를 사용하기 위한 환경 변수 설정:

```yaml
services:
  app:
    # 기존 설정들...
    environment:
      # 기존 환경 변수들...
      SPRING_CONFIG_IMPORT: "vault://"
      SPRING_CLOUD_VAULT_TOKEN: "00000000-0000-0000-0000-000000000000"
      SPRING_CLOUD_VAULT_SCHEME: "http"
      SPRING_CLOUD_VAULT_HOST: "vault"
      SPRING_CLOUD_VAULT_PORT: "8200"
    depends_on:
      db:
        condition: service_healthy
      vault-init:
        condition: service_completed_successfully
```

### 4.3 애플리케이션에서 시크릿 사용

#### 4.3.1 시크릿 참조 방법

`application-dev.yml`와 같은 프로필 설정 파일에서 Vault에 저장된 시크릿 참조:

```yaml
# Discord 웹훅 설정 - Vault에서 가져옴
discord:
  webhook:
    url: ${discord.webhook.url}
    min-level: DEBUG
```

Spring Boot는 애플리케이션이 시작될 때 `discord.webhook.url`을 Vault에서 가져옵니다.

## 5. Vault 사용 방법

### 5.1 Vault에 시크릿 저장

다음 명령어로 Vault에 시크릿을 저장할 수 있습니다:

```bash
# Vault CLI를 사용하여 시크릿 저장
export VAULT_TOKEN="00000000-0000-0000-0000-000000000000"
export VAULT_ADDR="http://127.0.0.1:8200"
vault kv put secret/attendly-api discord.webhook.url="https://discord.com/api/webhooks/your-webhook-url"
```

Docker 환경에서는:

```bash
docker exec -it attendly-api-vault sh -c "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=00000000-0000-0000-0000-000000000000 vault kv put secret/attendly-api discord.webhook.url=https://discord.com/api/webhooks/your-webhook-url"
```

### 5.2 Vault에서 시크릿 확인

다음 명령어로 저장된 시크릿을 확인할 수 있습니다:

```bash
# Vault CLI를 사용하여 시크릿 확인
vault kv get secret/attendly-api
```

Docker 환경에서는:

```bash
docker exec -it attendly-api-vault sh -c "VAULT_ADDR=http://127.0.0.1:8200 VAULT_TOKEN=00000000-0000-0000-0000-000000000000 vault kv get secret/attendly-api"
```

### 5.3 Vault UI 접근

Vault UI는 기본적으로 `http://localhost:8200/ui`에서 접근할 수 있습니다. 개발 환경에서 설정한 루트 토큰을 사용하여 로그인할 수 있습니다.

## 6. 운영 환경 고려사항

개발 환경에서는 편의를 위해 단순화된 설정을 사용하지만, 운영 환경에서는 다음 사항을 고려해야 합니다:

### 6.1 보안 강화

- 강력한 토큰 사용 및 주기적 교체
- TLS/SSL 활성화
- 적절한 권한 설정
- 안전한 Storage Backend 사용(Consul, PostgreSQL 등)

### 6.2 고가용성

- Vault 클러스터 설정
- 적절한 백업 및 복구 전략
- 모니터링 및 알림 설정

### 6.3 토큰 관리

- 짧은 TTL(Time To Live)을 가진 토큰 사용
- 애플리케이션별 토큰 발급
- 권한 범위 제한

## 7. 문제 해결

### 7.1 일반적인 문제

1. **연결 오류**
   - Vault 서버 주소 및 포트가 올바른지 확인
   - 네트워크 설정 확인(특히 Docker 네트워크)
   - 토큰이 유효한지 확인

2. **권한 오류**
   - 사용 중인 토큰에 적절한 권한이 있는지 확인
   - 시크릿 경로가 올바른지 확인

3. **시크릿 로드 실패**
   - 시크릿 경로와 키가 올바른지 확인
   - 애플리케이션 설정이 올바른지 확인

4. **데이터 지속성 문제**
   - 백업 파일이 올바르게 생성되었는지 확인 (`vault/data` 디렉토리 확인)
   - 백업 및 복원 컨테이너의 로그 검토
   - 백업 파일의 권한 문제 확인

### 7.2 디버깅 방법

- Spring Boot 애플리케이션 로그 확인
- Vault 서버 로그 확인: `docker logs attendly-api-vault`
- Vault 백업 컨테이너 로그 확인: `docker logs attendly-api-vault-backup`
- Vault 초기화 컨테이너 로그 확인: `docker logs attendly-api-vault-init`
- 환경 변수 설정 확인

## 8. 추가 자료

- [HashiCorp Vault 공식 문서](https://www.vaultproject.io/docs)
- [Spring Cloud Vault 문서](https://spring.io/projects/spring-cloud-vault)
- [Vault Docker 이미지 문서](https://hub.docker.com/_/vault) 