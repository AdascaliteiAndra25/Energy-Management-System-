FROM  maven:3.9.11 AS build

ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
ENV CATALINA_OPTS="-Duser.timezone=UTC"

COPY ../pom.xml /demo/pom.xml
WORKDIR /demo
RUN mvn dependency:go-offline
COPY ./src/ /demo/src
WORKDIR /demo
RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jdk
WORKDIR /demo
COPY --from=build /demo/target/DS2025_30644_Adascalitei_Andra_AuthorizationMicroservice-1.0-SNAPSHOT.jar /demo/demo.jar
EXPOSE 8083
CMD ["java", "-jar", "demo.jar"]
