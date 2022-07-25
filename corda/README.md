# Corda5 Cordapp Template 

[//]: # (TODO: add app overview)

## Environment Requirements: 
1. Download and install Java 11
2. Download and install `cordapp-builder` 
3. Download and install `corda-cli` 

You can find detailed instructions for steps 2 - 3 at [here](https://docs.r3.com/en/platform/corda/5.0-dev-preview-1/getting-started/overview.html)

## Local start up

Utility script is provided: `./scripts/start.sh`.
./scripts/start.sh
It starts network nodes, builds and deploys application. 

To generate compose file: `corda-cli network deploy -n testament-network -f testament-network.yaml > docker-compose.yaml`.

## Interact with the app 

Get node ports: `corda-cli network status -n testament-network`.

[//]: # (TODO: add HTTP examples)

For API docs consult Swagger: `https://localhost:<port>/api/v1/swagger`.

## Clean up

Stop the network: `./scripts/stop.sh`.

If you want to remove old network state: `rm -rf ~/.corda/testament-network`.
