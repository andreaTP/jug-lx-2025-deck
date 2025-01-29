#!/bin/bash
set -euox pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

wasm-interp ${SCRIPT_DIR}/demo.wasm -r "operation" -a "i32:8" -a "i32:5"
