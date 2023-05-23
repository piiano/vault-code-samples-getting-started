#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

echo "Build and install 'Getting started'"
mvn clean install -DskipTests
