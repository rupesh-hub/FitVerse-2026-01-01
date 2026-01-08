#Build stage
FROM maven:3.8.7-openjdk-18 AS build

MAINTAINER Rupesh Dulal<dulalrupesh77@gmail.com>
LABEL app=FitVerseBackend

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

#Runtime stage
FROM amazoncorretto:17
WORKDIR /app
COPY --from=build /build/target/fitverse-backend-*.jar /app/

ARG PROJECT_VERSION=1.0.0
ARG ACTIVE_PROFILE=default

EXPOSE 8181

ENV VERSION=${PROJECT_VERSION}
ENV PROFILE=${ACTIVE_PROFILE}
ENV DATABASE_URL=jdbc:mysql://mysql:3306/fitverse

CMD java -jar -Dspring.profiles.active=${PROFILE} -Dspring.datasource.url=${DATABASE_URL} fitverse-backend-${VERSION}.jar