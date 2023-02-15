#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

source .venv/bin/activate
python3 pvault_getting_started.py
