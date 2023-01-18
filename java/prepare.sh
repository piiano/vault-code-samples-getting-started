#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "Download openapi file"
curl -o openapi.yaml https://piiano.com/docs/assets/openapi.yaml

echo "Delete the old generated java SDK"
[ -d "vault_java_sdk" ] && rm -r vault_java_sdk

echo "Run openapi tools to create the java SDK"
docker run --rm -u $(id -u):$(id -g) -v "${PWD}:/local" openapitools/openapi-generator-cli:v6.2.1 generate \
    -i local/openapi.yaml \
    -g java \
    -o local/vault_java_sdk

echo "Install java SDK"
cd vault_java_sdk && mvn clean install

echo "Build and install 'Getting started'"
cd .. && mvn clean install -DskipTests
