#!/bin/bash
if [[ $1 == "deploy" ]]
then
  docker-compose build
  docker-compose up -d
  sleep 5
  ./test.sh
elif [[ $1 == "destroy" ]]
then 
  docker-compose down -v
else 
  echo "Use deploy or destroy command."
fi
