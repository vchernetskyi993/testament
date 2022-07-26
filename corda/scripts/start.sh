#!/bin/sh

infoln() {
  printf "\033[0;34m%s\033[0m\n" "$1"
}

if ! java -version 2>&1 | grep -Eq ".*11\..*\..*"; then
  echo "Java 11 is required to run the project"
  exit 1
fi

infoln "--- Configure the network. ---"
corda-cli network config docker-compose testament-network

infoln "--- Creating docker compose yaml file and starting docker containers. ---"
corda-cli network deploy -n testament-network -f testament-network.yaml | docker-compose -f - up -d

infoln "--- Listening to the docker processes. ---"
corda-cli network wait -n testament-network

# Note: we're skipping tests, because they require local network
infoln "--- Building projects. ---"
./gradlew clean build -x test

infoln "--- Creating cpb file. ---"
mkdir -p build/libs
cordapp-builder create \
  --cpk contracts/build/libs/testament-contracts-1.0-SNAPSHOT-cordapp.cpk \
  --cpk workflows/build/libs/testament-workflows-1.0-SNAPSHOT-cordapp.cpk \
  -o build/libs/testament.cpb

infoln "--- Install the cpb file into the network. ---"
corda-cli package install -n testament-network build/libs/testament.cpb

infoln "--- Listening to the docker processes. ---"
corda-cli network wait -n testament-network

infoln "--- Running tests ---"
./gradlew test

infoln "+++ Cordapp Setup Finished, Nodes Status: +++"
corda-cli network status -n testament-network
