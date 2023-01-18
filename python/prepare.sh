#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

python3 -m venv .venv --prompt vault_getting_started
source .venv/bin/activate

echo "Downloading openapi file"
curl -o openapi.yaml https://piiano.com/docs/assets/openapi.yaml

echo "Running openapi tools to create the SDK"
docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli:v6.1.0 generate \
    -i local/openapi.yaml \
    -g python \
    -o local/vault_python_sdk

echo "Installing SDK"
cd vault_python_sdk && pip install .
