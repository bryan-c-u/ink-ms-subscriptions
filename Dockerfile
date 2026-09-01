FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /app/comprobantes \
    && chown -R app:app /app
COPY --from=build /build/target/*.jar app.jar
USER app

ENV SPRING_PROFILES_ACTIVE=docker
ENV COMPROBANTES_PATH=/app/comprobantes

EXPOSE 3005
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
