#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

source .venv/bin/activate
python pvault_getting_started.py
