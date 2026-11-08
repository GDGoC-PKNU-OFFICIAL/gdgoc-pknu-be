# Render에는 Java 기본 런타임이 없어 Docker로 배포한다.
# 1단계에서 jar를 만들고, 2단계에는 JRE와 jar만 담아 무료 티어(512MB) 메모리를 아낀다.

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# 의존성 다운로드를 소스와 분리해 소스만 바뀐 빌드는 캐시를 쓴다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
# Windows에서 커밋되어 실행 권한이 없다(100644).
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

COPY src src
# 테스트는 Testcontainers(Docker)가 필요해 이미지 빌드 안에서는 돌리지 않는다. 머지 전에 로컬 · PR에서 확인한다.
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 메모리 한도 기준으로 힙을 잡는다. 오늘 날짜 계산은 ClockConfig가 KST로 고정하고, 이 값은 로그 시각용이다.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -Duser.timezone=Asia/Seoul"
ENV SPRING_PROFILES_ACTIVE=prod

# Render가 PORT를 넘긴다(application.yml의 server.port).
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
