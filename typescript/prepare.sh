#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

npm install
npm install typescript  --save-dev

echo "Downloading openapi file"
curl -o openapi.yaml https://piiano.com/docs/assets/openapi.yaml

echo "Running openapi tools to create the SDK"
docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli:latest generate \
    -i local/openapi.yaml \
    -g typescript-axios \
    -o local/vault_typescript_sdk \
