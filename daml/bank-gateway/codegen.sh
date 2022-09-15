#!/bin/bash
set -e

cd ../contracts
if [ ! -d ".daml/dist" ]; then
  daml build
fi
daml codegen js

TARGET_DIR=bank-gateway/src/daml.js

cd ..
mkdir -p $TARGET_DIR
cp -rf contracts/.daml/js/* $TARGET_DIR
