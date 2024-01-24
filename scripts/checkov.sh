#!/bin/bash

set -e

TERRAFORM_CONFIG_DIR="$(pwd)/infra"
CHECKOV_OUTPUT_DIR="$(pwd)/checkov/results"

mkdir -p "${CHECKOV_OUTPUT_DIR}"

cd "${TERRAFORM_CONFIG_DIR}" || exit

checkov -d . -o cli -o sarif --output-file-path console,"${CHECKOV_OUTPUT_DIR}/checkov_results.json"

exit 0
