FROM amazoncorretto:21 AS builder
WORKDIR /app
COPY . .

# gradlew에 실행 권한 부여
RUN chmod +x ./gradlew

# 메모리 문제 해결을 위한 Gradle 설정
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx512m"
RUN ./gradlew build --no-daemon -x test

FROM amazoncorretto:21
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"] 