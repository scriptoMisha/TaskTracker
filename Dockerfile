FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
