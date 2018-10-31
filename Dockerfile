FROM gradle:jre11 as build
COPY . .
RUN ["gradle", "jar"]

FROM openjdk:12-alpine
COPY --from=build /home/gradle/build/libs/gradle.jar .
CMD ["java", "-jar", "gradle.jar"]
