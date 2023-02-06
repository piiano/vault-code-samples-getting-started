#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

npm install

echo "Downloading openapi file"
curl -o openapi.yaml https://piiano.com/docs/assets/openapi.yaml

echo "Running openapi typescript codegen to create the SDK"
npx -y openapi-typescript-codegen \
    --input openapi.yaml \
    --output vault_typescript_sdk \
    --client axios
