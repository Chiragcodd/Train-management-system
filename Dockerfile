# ─── Stage 1: Build ───────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Dependencies pehle copy karo (cache ke liye)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Source code copy karo aur build karo
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Run ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Built JAR copy karo
COPY --from=build /app/target/*.jar app.jar

# Railway PORT env var use karta hai
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]