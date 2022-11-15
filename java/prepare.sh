#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

#TODO: we won't need "cd .." this once we get a good online url for the openapi json file
cd ..

docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli:v6.1.0 generate \
    -i local/openapi-2022-11-07.json \
    -g java \
    -o local/vault_java_sdk

cd vault_java_sdk

mvn install

cd ../java
mvn clean install
