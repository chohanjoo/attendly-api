FROM amazoncorretto:21-alpine AS builder
WORKDIR /app
COPY . .

# gradlew에 실행 권한 부여
RUN chmod +x ./gradlew

# 메모리 문제 해결을 위한 Gradle 설정
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx512m"
RUN ./gradlew clean bootJar --no-daemon -x test

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# 타임존 설정
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone

# 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"] 