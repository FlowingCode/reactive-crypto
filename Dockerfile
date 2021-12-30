FROM openjdk:11-jdk-stretch
EXPOSE 8080
ARG JAR_FILE
COPY target/${JAR_FILE}.jar app.jar
ENTRYPOINT ["java","-Dvaadin.productionMode","-jar","/app.jar"]