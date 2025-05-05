# p6spy 가이드 - JPA 쿼리 파라미터 로깅

## 목차

1. [p6spy 소개](#p6spy-소개)
2. [p6spy의 장점](#p6spy의-장점)
3. [적용 방법](#적용-방법)
   - [의존성 추가](#의존성-추가)
   - [설정 파일 생성](#설정-파일-생성)
   - [DataSource 설정](#datasource-설정)
   - [커스텀 포맷 설정](#커스텀-포맷-설정)
4. [사용 예시](#사용-예시)
5. [이슈 해결](#이슈-해결)
6. [참고자료](#참고자료)

## p6spy 소개

p6spy는 JDBC 드라이버를 래핑하여 데이터베이스 쿼리를 가로채고 로깅하는 오픈소스 라이브러리입니다. Spring Boot와 JPA를 사용할 때, 실제 실행되는 SQL 쿼리와 바인딩된 파라미터를 로깅하기 위해 매우 유용합니다.

Hibernate에서 기본적으로 제공하는 SQL 로깅은 쿼리에 바인딩된 파라미터 값을 표시하지 않기 때문에, 실제로 어떤 값이 바인딩되어 실행되었는지 확인하기 어렵습니다. p6spy는 이러한 문제를 해결하여 바인딩된 파라미터 값까지 로깅할 수 있게 해줍니다.

## p6spy의 장점

1. **파라미터 바인딩 로깅**: 가장 큰 장점은 SQL 쿼리에 바인딩된 파라미터 값을 확인할 수 있다는 점입니다.
2. **실행 시간 측정**: 각 쿼리의 실행 시간을 측정하여 로깅합니다.
3. **커스터마이징 가능**: 로깅 형식을 커스터마이징할 수 있어 필요한 정보만 간결하게 볼 수 있습니다.
4. **환경 분리**: 개발 환경과 프로덕션 환경에서 설정을 다르게 할 수 있습니다.
5. **디버깅 용이성**: 복잡한 쿼리나 대량의 데이터를 다룰 때 디버깅이 훨씬 용이해집니다.
6. **성능 분석**: 쿼리 실행 시간을 확인하여 성능 병목 현상을 찾을 수 있습니다.

## 적용 방법

### 의존성 추가

먼저 build.gradle.kts 파일에 p6spy 의존성을 추가합니다.

```kotlin
dependencies {
    // 기존 의존성들...
    
    // P6Spy 의존성 추가
    implementation("p6spy:p6spy:3.9.1")
    
    // 기타 의존성들...
}
```

### 설정 파일 생성

`src/main/resources` 디렉토리 아래에 `spy.properties` 파일을 생성합니다.

```properties
# p6spy 설정
appender=com.p6spy.engine.spy.appender.Slf4JLogger
logMessageFormat=com.attendly.config.CustomLineFormat
customLogMessageFormat=Time: %(executionTime)ms | SQL: %(sql)
dateformat=yyyy-MM-dd HH:mm:ss
excludecategories=info,debug,result,batch,resultset
excluderegexps=\/ping
driverlist=com.mysql.cj.jdbc.Driver
```

- `appender`: 로그를 어디에 출력할지 결정합니다. Slf4J를 사용합니다.
- `logMessageFormat`: 로그 형식을 결정하는 클래스를 지정합니다. 커스텀 포맷 클래스를 사용합니다.
- `customLogMessageFormat`: 기본 로그 형식을 정의합니다.
- `dateformat`: 날짜 형식을 지정합니다.
- `excludecategories`: 로깅에서 제외할 카테고리를 지정합니다.
- `excluderegexps`: 로깅에서 제외할 정규식 패턴을 지정합니다.
- `driverlist`: 실제 JDBC 드라이버 클래스를 지정합니다.

### DataSource 설정

application.yml 또는 application-{profile}.yml 파일에서 DataSource 설정을 변경합니다. p6spy를 사용하기 위해 JDBC URL과 드라이버 클래스를 변경해야 합니다.

```yaml
spring:
  datasource:
    # 기존 URL 앞에 p6spy: 추가
    url: jdbc:p6spy:mysql://localhost:3306/church_attendly?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: root
    password: root
    # 드라이버 클래스를 p6spy 드라이버로 변경
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
```

개발 환경(dev)과 테스트 환경(test) 모두 p6spy를 적용하려면 각 프로필별 설정 파일을 모두 수정해야 합니다.

### 커스텀 포맷 설정

SQL 쿼리 로그를 더 보기 좋게 표시하기 위해 커스텀 포맷 클래스를 만듭니다.

1. 메시지 포맷팅을 위한 클래스를 생성합니다.

```kotlin
package com.attendly.config

import com.p6spy.engine.spy.appender.MessageFormattingStrategy

class CustomLineFormat : MessageFormattingStrategy {
    override fun formatMessage(
        connectionId: Int,
        now: String?,
        elapsed: Long,
        category: String?,
        prepared: String?,
        sql: String?,
        url: String?
    ): String {
        return if (sql.isNullOrEmpty()) {
            ""
        } else {
            // 바인딩된 변수가 있다면 prepared를 표시, 없다면 sql만 표시
            if (!prepared.isNullOrEmpty() && prepared != sql) {
                "실행 SQL: $prepared\n바인딩된 SQL: $sql\n실행시간: ${elapsed}ms"
            } else {
                "실행 SQL: $sql\n실행시간: ${elapsed}ms"
            }
        }
    }
}
```

2. p6spy 설정을 적용하는 설정 클래스를 만듭니다.

```kotlin
package com.attendly.config

import com.p6spy.engine.spy.P6SpyOptions
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class P6spyConfig {
    
    @PostConstruct
    fun setLogMessageFormat() {
        P6SpyOptions.getActiveInstance().logMessageFormat = P6spyLogMessageFormatConfiguration::class.java.name
    }
}
```

3. 추가적으로 포맷 옵션을 더 세밀하게 설정하고 싶을 경우 아래와 같은 형식으로 추가 클래스를 만들 수 있습니다.

```kotlin
package com.attendly.config

import com.p6spy.engine.logging.Category
import com.p6spy.engine.spy.appender.MessageFormattingStrategy
import org.hibernate.engine.jdbc.internal.FormatStyle
import java.text.SimpleDateFormat
import java.util.*

class P6spyLogMessageFormatConfiguration : MessageFormattingStrategy {
    override fun formatMessage(
        connectionId: Int, 
        now: String?, 
        elapsed: Long, 
        category: String?, 
        prepared: String?, 
        sql: String?, 
        url: String?
    ): String {
        val formatSql = formatSql(category, sql)
        return StringBuilder()
            .append("\n\n")
            .append(now)
            .append(" | ")
            .append(elapsed)
            .append("ms | ")
            .append(category)
            .append(" | connection ")
            .append(connectionId)
            .append("\n")
            .append(formatSql)
            .append("\n")
            .toString()
    }

    private fun formatSql(category: String?, sql: String?): String? {
        if (sql == null || sql.trim() == "") return sql

        // Only format Statement, PreparedStatement and Batch
        val isStatement = Category.STATEMENT.name == category
        val isPreparedStatement = "statement" == category
        val isBatch = Category.BATCH.name == category

        if (!isStatement && !isPreparedStatement && !isBatch) {
            return sql
        }

        val formattedSql = sql.trim().replace(Regex("\\s+"), " ")

        return when {
            formattedSql.startsWith("create") || formattedSql.startsWith("alter") || formattedSql.startsWith("comment") -> FormatStyle.DDL.formatter.format(formattedSql)
            else -> FormatStyle.BASIC.formatter.format(formattedSql)
        }
    }

    private fun getCurrentTimeFormatted(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
    }
}
```

## 사용 예시

p6spy를 설정한 후 애플리케이션을 실행하면 SQL 쿼리 로그가 다음과 같이 출력됩니다:

```
실행 SQL: select u.* from users u where u.email = 'example@example.com'
바인딩된 SQL: select u.* from users u where u.email = ?
실행시간: 5ms
```

또는 보다 상세한 정보를 포함한 로그:

```

2023-05-19 14:30:22 | 10ms | statement | connection 1
select
    u.id,
    u.email,
    u.username,
    u.created_at
from
    users u
where
    u.email = 'example@example.com'

```

## 이슈 해결

### 1. p6spy 카테고리 클래스 오류

p6spy 라이브러리의 `Category` 클래스에는 일반적으로 예상할 수 있는 `PREPARED_STATEMENT` 상수가 존재하지 않습니다. 대신 `statement` 카테고리만 존재합니다. 이로 인해 다음과 같은 코드는 컴파일 에러를 발생시킵니다:

```kotlin
val isPreparedStatement = Category.PREPARED_STATEMENT.name == category
```

해결 방법은 문자열로 직접 비교하는 것입니다:

```kotlin
val isPreparedStatement = "statement" == category
```

### 2. 로그 포맷 클래스 등록 오류

spy.properties 파일에서 커스텀 로그 포맷 클래스를 등록할 때 전체 패키지 경로를 정확히 명시해야 합니다:

```
logMessageFormat=com.attendly.config.CustomLineFormat
```

### 3. 프로덕션 환경에서의 성능 고려

p6spy는 개발 및 테스트 환경에서는 매우 유용하지만, 프로덕션 환경에서는 성능 오버헤드를 발생시킬 수 있습니다. 따라서 프로덕션 환경에서는 비활성화하는 것이 좋습니다. 이를 위해 프로필별로 다른 DataSource 설정을 사용하는 것이 좋습니다.

## 참고자료

- [p6spy 공식 GitHub](https://github.com/p6spy/p6spy)
- [p6spy 공식 문서](https://p6spy.readthedocs.io/en/latest/)
- [Spring Boot와 p6spy 통합 가이드](https://github.com/gavlyukovskiy/spring-boot-data-source-decorator) 