#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

python3 -m venv .venv --prompt vault_getting_started
source .venv/bin/activate

#TODO: we won't need "cd .." this once we get the a good online url for the openapi json file
cd ..

docker run --rm -v "${PWD}:/local" openapitools/openapi-generator-cli:v6.1.0 generate \
    -i local/openapi-2022-11-07.json \
    -g python \
    -o local/vault_python_sdk

cd vault_python_sdk
pip install .
