#!/bin/sh

set -e

infoln() {
  printf "\033[0;34m%s\033[0m\n" "$1"
}

if ! java -version 2>&1 | grep -Eq ".*11\..*\..*"; then
  echo "Java 11 is required to run the project"
  exit 1
fi

infoln "--- Starting the network. ---"
corda-cli network config docker-compose testament-network
corda-cli network deploy -n testament-network -f testament-network.yaml | docker-compose -f - up -d
corda-cli network wait -n testament-network

infoln "--- Deploying package ---"
# we're skipping tests, because they require local network
./gradlew clean build -x test

mkdir -p build/libs
cordapp-builder create \
  --cpk contracts/build/libs/testament-contracts-1.0-SNAPSHOT-cordapp.cpk \
  --cpk workflows/build/libs/testament-workflows-1.0-SNAPSHOT-cordapp.cpk \
  -o build/libs/testament.cpb

corda-cli package install -n testament-network build/libs/testament.cpb
corda-cli network wait -n testament-network

infoln "+++ Cordapp setup verified. Nodes status: +++"
corda-cli network status -n testament-network

infoln "--- Running tests ---"
./gradlew test
