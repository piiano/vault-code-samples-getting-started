#!/bin/bash
set -euo pipefail

# Like npm install, but uses package-lock.json instead of package.json.
# This is important because we want to use the exact versions of the dependencies.
npm ci
