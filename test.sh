#!/usr/bin/env bash
rm -rf /tmp/uploaded*.tmp
export TOKEN=token
upload_response_code=$(curl -o /dev/null -s -w "%{http_code}\n" -F "file=@file-uploader/src/main/resources/config.properties" 127.0.0.1:8081/upload)
created_files_count=$(sudo ls /var/lib/docker/volumes/file-uploader_shared-volume/_data | wc -l)
sleep 2
metrics_increase=$(curl -s http://127.0.0.1:2112/metrics -H "Authorization: $TOKEN" | grep '^files_uploaded' | cut -d ' ' -f2)


SUCCESS_RESPONSE_CODE=201

if [[ $upload_response_code != $SUCCESS_RESPONSE_CODE ]]
then
  echo "Error: Uploader app response code is not $SUCCESS_RESPONSE_CODE, bad code is $upload_response_code"; exit 1 
elif [[ $created_files_count == 0 ]]
then
  echo "Error: Files have not been uploaded."; exit 1
elif [[ $metrics_increase == 0 ]]
then
  echo "Error: Metrics not increased."; exit 1
else
  echo "Success! File uploader and Exporter works good! Total created files: $metrics_increase"; exit 0 
fi
