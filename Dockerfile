FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /build
COPY build/distributions/ktor-frame-1.0.0-SNAPSHOT.tar .
RUN tar xf ktor-frame-1.0.0-SNAPSHOT.tar
RUN mv ktor-frame-1.0.0-SNAPSHOT/lib/ktor-frame-1.0.0-SNAPSHOT.jar .

FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app
COPY --from=builder /build/ktor-frame-1.0.0-SNAPSHOT/lib lib
COPY --from=builder /build/ktor-frame-1.0.0-SNAPSHOT/bin bin
COPY --from=builder /build/ktor-frame-1.0.0-SNAPSHOT.jar lib/ktor-frame-1.0.0-SNAPSHOT.jar
EXPOSE 8080

ENV JAVA_OPTS="--enable-native-access=ALL-UNNAMED -XX:ActiveProcessorCount=4 -XX:MaxRAMPercentage=80 -XX:+UseCompactObjectHeaders"
CMD ["bin/ktor-frame"]
