#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "Downloading openapi file"
curl -o openapi.json https://piiano.com/docs/assets/openapi.json

echo "Running openapi tools to create the SDK"
docker run --rm -u $(id -u):$(id -g) -v "${PWD}:/local" openapitools/openapi-generator-cli:v6.2.1 generate \
    -i local/openapi.json \
    -g java \
    -o local/vault_java_sdk

echo "Installing SDK"
cd vault_java_sdk && mvn clean install

echo "Build and install 'Getting started'"
cd .. && mvn clean install
