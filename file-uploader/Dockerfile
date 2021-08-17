FROM openjdk:16-jdk-alpine AS build
WORKDIR /file-uploader/
ADD . /file-uploader/
RUN ./gradlew clean build --stacktrace
RUN tar -xf /file-uploader/build/distributions/*.tar -C /file-uploader/build   
RUN ls /file-uploader/build/file-uploader/lib

FROM openjdk:16-jdk-alpine
WORKDIR /opt/apt
COPY --from=build /file-uploader/build/file-uploader/ /opt/app/
EXPOSE 8081
ENTRYPOINT ["/opt/app/bin/file-uploader"]
