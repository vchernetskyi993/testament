#!/bin/bash
set -e

cd ../contracts
if [ ! -d ".daml/dist" ]; then
  daml build
fi
daml codegen java

TARGET_DIR=provider-gateway/build/generated/source/daml/main/

cd ..
mkdir -p $TARGET_DIR
cp -rf contracts/.daml/java $TARGET_DIR
