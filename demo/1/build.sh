#!/bin/bash
set -euox pipefail

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

rm -f demo.wasm

docker run --rm \
    -v ${SCRIPT_DIR}/:/src \
    -w /src tinygo/tinygo:0.34.0 bash \
    -c "tinygo build --no-debug -target=wasm-unknown -o /tmp/tmp.wasm operation.go && cat /tmp/tmp.wasm" > \
    ${SCRIPT_DIR}/demo.wasm
