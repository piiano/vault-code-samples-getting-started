#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "Running tests"
mvn test
