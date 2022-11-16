#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

# Run 'Getting started'.
java -ea -jar target/piiano-vault-getting-started-java-0.0.1-SNAPSHOT.jar
