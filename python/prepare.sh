#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

python3 -m venv .venv --prompt vault_getting_started
source .venv/bin/activate

echo "Downloading openapi file"
curl -o openapi.yaml https://docs.piiano.com/assets/openapi.yaml

echo "Running openapi tools to create the SDK"
docker run --rm -u $(id -u):$(id -g) -v "${PWD}:/local" openapitools/openapi-generator-cli:v7.10.0 generate \
    -i local/openapi.yaml \
    -g python \
    -o local/vault_python_sdk

echo "Installing SDK"
cd vault_python_sdk && pip install .