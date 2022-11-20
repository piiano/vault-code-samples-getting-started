#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# Download Piiano vault open api.
curl -o openapi.json https://piiano.com/docs/assets/openapi.json

# Generate java sdk.
docker run --rm -v "${PWD}:/app" openapitools/openapi-generator-cli:v6.2.1 generate \
    -i /app/openapi.json \
    -g java \
    -o /app/vault_java_sdk

# Build and install the Java sdk.
cd vault_java_sdk
mvn clean install

# Build and install 'Getting started'.
cd ..
mvn clean install
