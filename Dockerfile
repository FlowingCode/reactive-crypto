FROM openjdk:11-jdk-stretch
EXPOSE 8080
ARG JAR_FILE
COPY target/${JAR_FILE}.jar app.jar
ARG TOKEN
ENV TOKEN=$TOKEN
ENTRYPOINT ["java","-Dvaadin.productionMode", "-Dapi.finnhub.endpoint=wss://ws.finnhub.io?token=$TOKEN", "-Dapi.finnhub.token=$TOKEN","-jar","/app.jar"]