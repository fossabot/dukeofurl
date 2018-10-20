FROM gradle:jre11 as build
COPY . .
RUN ["gradle", "build"]
RUN ["tar", "xf", "build/distributions/gradle.tar"]

FROM openjdk:11-jre
COPY --from=build /home/gradle/gradle gradle
CMD ["gradle/bin/gradle"]
