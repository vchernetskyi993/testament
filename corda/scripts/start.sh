#!/bin/sh

echo "--Step 1: Building projects.--"
./gradlew clean build

echo "--Step 2: Creating cpb file.--"
mkdir -p build/libs
cordapp-builder create \
  --cpk contracts/build/libs/testament-contracts-1.0-SNAPSHOT-cordapp.cpk \
  --cpk workflows/build/libs/testament-workflows-1.0-SNAPSHOT-cordapp.cpk \
  -o build/libs/testament.cpb

echo "--Step 3: Configure the network.--"
corda-cli network config docker-compose testament-network

echo "--Step 4: Creating docker compose yaml file and starting docker containers.--"
corda-cli network deploy -n testament-network -f testament-network.yaml | docker-compose -f - up -d

echo "--Listening to the docker processes.--"
corda-cli network wait -n testament-network

echo "--Step 5: Install the cpb file into the network.--"
corda-cli package install -n testament-network build/libs/testament.cpb

echo "--Listening to the docker processes.--"
corda-cli network wait -n testament-network

echo "++Cordapp Setup Finished, Nodes Status: ++"
corda-cli network status -n testament-network
