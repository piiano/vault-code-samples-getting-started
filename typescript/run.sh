#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

npm run build
npm run start
