# File Uploader
## Description
File uploader is lightfull service that get file and store it on the disk. This service has been written with Java. Also, there is the custom exporter for it, that has been written with Go.

## Test system
- Linux Ubuntu 18 / Alpine docker
- Docker 20.10.7
- Docker-compose 1.27.4
- Java/OpenSdk 16
- Gradle 7.1
- Go 1.16.7

## Quick start

**You need to install docker and docker-compose to your system.**

Then just run that in your console.
```
git clone https://github.com/it-mak/file-uploader
cd file-uploader
./run.sh deploy
```
You will see the message if everything went well.

>Success! File uploader and Exporter works good! Total created files: 1

## Common scheme
Java web server start on 8081 port and wait for requests with any location.

##### Request example:
```
curl -F "file=@file-uploader/src/main/resources/config.properties" 127.0.0.1:8081/upload
```
After that in tempory folder will be created file with random name.

In docker-compose.yaml services will use one shared folder. This shared folder need to calculate requests for the Go exporter. Go exporter have authorization by token. Metrics route is /metrics.

##### Request example:
```
curl -s http://127.0.0.1:2112/metrics -H "Authorization:token"
```
## Project structure
Each project have its own Dockerfile.

- file-uplodaer
- src
- src/main/* - *source code dir*
- src/resources
- src/resources/config.properties - *config for file-uploader app*
- src/resources/log4j2.properties - *config for logger*
- gradle - *gradle wraper*
- build.gradle - *settings for build gradle*
- go_exporter
- main.go - *source code*
- go.mod/go.sum - *packets requirements*
- docker-compose.yaml
- run.sh - *manage script Options: deploy/destroy.*
- test.sh - *smoke tests No args.*
## Settings
You can set settings for Java file-uploader in this file: src/resources/config.properties

- http_port = 8081 - *Port that would be listened for http server*
- file_upload.max_size = 104857600 - *Max file size for sending*
- file_upload.location = /tmp/shared - *Settings for file upload dir*

Go_exporter need 2 env vars:

PATH_FOR_UPLOADED - Location of shared folder for tmp files store

###### Default: "/tmp/shared/"
TOKEN - *Authorization token*

###### Default: "token
If it's not set, default will be used.


