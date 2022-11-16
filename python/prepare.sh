#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

python3 -m venv .venv --prompt vault_getting_started
source .venv/bin/activate

# Download Piiano vault open api.
curl -o openapi.json https://piiano.com/docs/assets/openapi.json

docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli:v6.1.0 generate \
    -i local/openapi.json \
    -g python \
    -o local/vault_python_sdk

cd vault_python_sdk
pip install .
