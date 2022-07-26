# Corda Testament

[Ethereum Testament](../ethereum) adopted for Corda. 

[//]: # (TODO: include diagram)

Network consists of 3 organisations:
* Provider - issue/update/revoke testaments
* Bank - holds user tokens; distributes them on testament execution
* Government - confirms all operations; triggers testament execution

## Environment Requirements:

1. Download and install Java 11
2. Download and install `cordapp-builder`
3. Download and install `corda-cli`

You can find detailed instructions for steps 2 - 3
at [here](https://docs.r3.com/en/platform/corda/5.0-dev-preview-1/getting-started/overview.html)

## Local start up

Utility script is provided: `./scripts/start.sh`. It starts network nodes, builds and deploys application.

To generate compose file: `corda-cli network deploy -n testament-network -f testament-network.yaml > docker-compose.yaml`.

## Interact with the app

Get node ports: `corda-cli network status -n testament-network`.

Setup variables:
```bash
PROVIDER_PORT=12112
PROVIDER_USER=testamentadmin
PROVIDER_PASSWORD=Password1!
```

Issue testament:
```bash
curl --request POST "https://localhost:$PROVIDER_PORT/api/v1/flowstarter/startflow" \
  -u $PROVIDER_USER:$PROVIDER_PASSWORD \
  --insecure \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "rpcStartFlowRequest": {
        "clientId": "ffeb26aa-f060-4f83-9d59-07074302819f",
        "flowName": "com.example.testament.flows.IssueTestamentFlow",
        "parameters": {
            "parametersInJson": "{\"issuer\":\"0\",\"inheritors\": {\"1\":6000,\"2\":4000}}"
        }
    }
}' | jq
```

Check flow result:
```bash
FLOW_ID=3995299e-83e5-40f7-9456-7f3e47942518 # flowId.uuid from startflow response

curl --request GET "https://localhost:$PROVIDER_PORT/api/v1/flowstarter/flowoutcome/$FLOW_ID" \
  --insecure \
  -u $PROVIDER_USER:$PROVIDER_PASSWORD | jq
```
  
Fetch testament by issuer
```bash
curl --request POST "https://localhost:$PROVIDER_PORT/api/v1/persistence/query" \
  --insecure \
  -u $PROVIDER_USER:$PROVIDER_PASSWORD \
  --header 'Content-Type: application/json' \
  --data-raw '{
    "request": {
        "namedParameters": {
            "issuerId": {
                "parametersInJson": "0"
            }
        },
        "queryName": "TestamentSchemaV1.PersistentTestament.findByIssuerId",
        "postProcessorName": "com.example.testament.states.TestamentPostProcessor"
    },
    "context": {
        "awaitForResultTimeout": "PT15M",
        "currentPosition": -1,
        "maxCount": 10
    }
}' | jq '.positionedValues[0].value.json | fromjson'
```

For API docs consult Swagger: `https://localhost:<port>/api/v1/swagger`.

## Clean up

Stop the network: `./scripts/stop.sh`.
