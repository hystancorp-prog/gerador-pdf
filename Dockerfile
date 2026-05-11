FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY demo/pom.xml .
RUN mvn dependency:go-offline -B
COPY demo/src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV PORT=8080
EXPOSE $PORT
ENTRYPOINT ["java", "-jar", "app.jar"]