#!/bin/bash
set +x

# playground available here: https://play.openpolicyagent.org/p/0FjgydMuVo

opa build -t wasm -o bundle.tar.gz -e application policy.rego
tar xzf bundle.tar.gz /policy.wasm
rm -f bundle.tar.gz
