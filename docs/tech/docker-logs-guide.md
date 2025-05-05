# Docker 컨테이너 로그 모니터링 가이드

Docker 컨테이너의 로그를 확인하고 모니터링하는 다양한 방법에 대한 가이드입니다.

## 기본 로그 확인 명령어

### 1. `docker logs` 명령어

```bash
# 컨테이너의 모든 로그 출력
docker logs [컨테이너_이름/ID]

# 마지막 n줄만 출력
docker logs --tail [숫자] [컨테이너_이름/ID]

# 실시간 로그 모니터링 (follow 모드)
docker logs -f [컨테이너_이름/ID]

# 타임스탬프 표시
docker logs -t [컨테이너_이름/ID]

# 조합 사용 예
docker logs -f -t --tail 100 [컨테이너_이름/ID]
```

예시:
```bash
# church-attendly-app 컨테이너의 실시간 로그 확인
docker logs -f church-attendly-app

# MySQL 컨테이너의 최근 50줄 로그와 타임스탬프 확인
docker logs -t --tail 50 church-attendly-mysql
```

### 2. Docker Compose 로그 확인

```bash
# 모든 서비스의 로그 확인
docker-compose logs

# 특정 서비스의 로그 확인
docker-compose logs [서비스_이름]

# 실시간 로그 모니터링
docker-compose logs -f

# 특정 서비스의 실시간 로그 모니터링
docker-compose logs -f [서비스_이름]

# 마지막 n줄만 출력
docker-compose logs --tail=[숫자]
```

예시:
```bash
# 모든 서비스의 실시간 로그 확인
docker-compose logs -f

# app 서비스의 실시간 로그 확인
docker-compose logs -f app
```

## 고급 로그 모니터링 기법

### 1. 로그 필터링

```bash
# 특정 키워드가 포함된 로그만 표시
docker logs [컨테이너_이름/ID] | grep [키워드]

# 실시간 로그에서 특정 키워드 필터링
docker logs -f [컨테이너_이름/ID] | grep [키워드]
```

예시:
```bash
# ERROR 로그만 필터링
docker logs attendly-api-app | grep ERROR

# 실시간 로그에서 INFO 레벨 메시지만 확인
docker logs -f attendly-api-app | grep INFO
```

### 2. 로그 저장

```bash
# 로그를 파일로 저장
docker logs [컨테이너_이름/ID] > [파일명].log

# 특정 시간대의 로그만 추출하여 저장
docker logs --since=[시간] [컨테이너_이름/ID] > [파일명].log
```

예시:
```bash
# 모든 로그를 파일로 저장
docker logs attendly-api-app > app_logs.log

# 최근 1시간의 로그만 저장
docker logs --since=1h attendly-api-app > recent_logs.log
```

## 로그 드라이버 설정

Docker는 다양한 로그 드라이버를 지원합니다. 기본적으로 `json-file` 드라이버가 사용되지만, 필요에 따라 다른 드라이버로 변경할 수 있습니다.

### docker-compose.yml에서 로그 드라이버 설정 예시

```yaml
version: '3'
services:
  app:
    image: your-app-image
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 주요 로그 드라이버 종류

- **json-file**: 기본 드라이버, JSON 형식으로 로그 저장
- **syslog**: 시스템 로깅 서비스로 로그 전송
- **journald**: systemd 저널로 로그 전송
- **fluentd**: Fluentd 로깅 서비스로 로그 전송
- **awslogs**: AWS CloudWatch로 로그 전송
- **splunk**: Splunk 서비스로 로그 전송
- **gelf**: Graylog 서비스로 로그 전송

## 외부 로그 모니터링 도구 연동

더 강력한 로그 모니터링 및 분석을 위해 다음과 같은 외부 도구와 연동할 수 있습니다:

1. **ELK Stack** (Elasticsearch, Logstash, Kibana)
2. **Grafana Loki**
3. **Datadog**
4. **Prometheus + Grafana**
5. **Fluentd/Fluentbit**

## 팁과 모범 사례

1. 로그 로테이션 설정을 통해 디스크 공간 관리하기
2. 중요 컨테이너는 항상 로그 모니터링 도구와 연동하기
3. 로그 레벨을 적절히 조정하여 불필요한 로그 줄이기
4. 프로덕션 환경에서는 중앙 집중식 로그 관리 시스템 사용하기
5. 컨테이너 재시작 시에도 로그를 유지할 수 있는 방법 고려하기 