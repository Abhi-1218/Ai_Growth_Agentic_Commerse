FROM eclipse-temurin:22-jdk-alpine
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
COPY src ./src
RUN ./mvnw package -DskipTests
EXPOSE 8080
CMD ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]
