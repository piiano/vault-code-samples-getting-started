#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# Download Piiano vault open api.
curl -o openapi.json https://piiano.com/docs/assets/openapi.json

# Generate java sdk.
docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli:v6.1.0 generate \
    -i local/openapi.json \
    -g java \
    -o local/vault_java_sdk

# Build and install the Java sdk.
cd vault_java_sdk
ls
mvn clean -X -e compile

# Build and install 'Getting started'.
#cd ..
#mvn clean package
