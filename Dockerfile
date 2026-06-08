# 1단계: 빌드 환경 (Maven & JDK 17)
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 의존성 정의 및 소스 복사
COPY pom.xml .
COPY src ./src
COPY frontend ./frontend

# 빌드 및 패키징 (테스트 제외 빌드 단축)
RUN mvn clean package -DskipTests

# 2단계: JRE 런타임 환경
FROM eclipse-temurin:17-jre
WORKDIR /app

# builder 단계에서 생성된 JAR 복사
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
