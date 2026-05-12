FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app

COPY build/docker-dist/lib lib
COPY build/docker-dist/app app

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED -XX:ActiveProcessorCount=4 -XX:MaxRAMPercentage=80 -XX:+UseCompactObjectHeaders"
CMD ["java", "-cp", "lib/*:app/*", "io.github.kperczynski.MainKt"]
