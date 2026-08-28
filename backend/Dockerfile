FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system casacontas && useradd --system --gid casacontas --home-dir /app casacontas
COPY --from=build --chown=casacontas:casacontas /workspace/target/backend-*.jar app.jar

USER casacontas
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/urandom"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
