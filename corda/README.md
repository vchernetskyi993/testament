# Corda Testament

[Ethereum Testament](../ethereum) adopted for Corda.

![diagram](./diagram.png)

Network consists of 3 organisations:

* Provider - issue/update/revoke testaments
* Bank - holds user tokens; distributes them on testament execution
* Government - confirms all operations; announces testament execution

## Environment Requirements:

1. Download and install Java 11
2. Download and install `cordapp-builder`
3. Download and install `corda-cli`

You can find detailed instructions for steps 2 - 3
[here](https://docs.r3.com/en/platform/corda/5.0-dev-preview-1/getting-started/overview.html)

## Local network

Utility script is provided: `./scripts/start.sh`. It starts network nodes, builds and deploys application.

To generate compose
file: `corda-cli network deploy -n testament-network -f testament-network.yaml > docker-compose.yaml`.

### Clean up

Stop the network: `./scripts/stop.sh`.

## Interact with the app

Setup variables:

* for credentials check out [network config](./testament-network.yaml)
* ports are printed on network start up (or issue `corda-cli network status -n testament-network`)

```bash
# testament provider node
PROVIDER_PORT=12112
PROVIDER_USER=testamentadmin
PROVIDER_PASSWORD=Password1!
# bank node
BANK_PORT=12116
BANK_USER=bankadmin
BANK_PASSWORD=Password1!
# government node
GOV_PORT=12120
GOV_USER=govadmin
GOV_PASSWORD=Password1!
# testament issuer & account holder
USER_ID=3
```

For API docs consult Swagger: `https://localhost:$PROVIDER_PORT/api/v1/swagger`.

Issue testament:

```bash
jq -nc --arg clientId $(uuidgen) --arg userId $USER_ID '{
  "issuer": $userId,
  "inheritors": {
    "1": 6000,
    "2": 4000
  }
} | tostring as $params | {
  "rpcStartFlowRequest": {
    "clientId": $clientId,
    "flowName": "com.example.testament.flows.IssueTestamentFlow",
    "parameters": {
      "parametersInJson": $params
    }
  }
}' | curl --request POST "https://localhost:$PROVIDER_PORT/api/v1/flowstarter/startflow" \
  -u $PROVIDER_USER:$PROVIDER_PASSWORD \
  --insecure \
  --header 'Content-Type: application/json' \
  --data-binary @- | jq
```

Check flow result:

```bash
FLOW_ID=3995299e-83e5-40f7-9456-7f3e47942518 # flowId.uuid from startflow response

curl --request GET "https://localhost:$PROVIDER_PORT/api/v1/flowstarter/flowoutcome/$FLOW_ID" \
  --insecure \
  -u $PROVIDER_USER:$PROVIDER_PASSWORD | jq
  
# OR for Bank:
curl --request GET "https://localhost:$BANK_PORT/api/v1/flowstarter/flowoutcome/$FLOW_ID" \
  --insecure \
  -u $BANK_USER:$BANK_PASSWORD | jq
  
# OR Government:
curl --request GET "https://localhost:$GOV_PORT/api/v1/flowstarter/flowoutcome/$FLOW_ID" \
  --insecure \
  -u $GOV_USER:$GOV_PASSWORD | jq
```

Fetch testament by issuer:

```bash
jq -nc --arg userId $USER_ID '{
  "request": {
    "namedParameters": {
      "issuerId": {
        "parametersInJson": $userId
      }
    },
    "queryName": "TestamentSchemaV1.PersistentTestament.findByIssuerId",
    "postProcessorName": "com.example.testament.processor.TestamentPostProcessor"
  },
  "context": {
    "awaitForResultTimeout": "PT15M",
    "currentPosition": -1,
    "maxCount": 100
  }
}' | curl --request POST "https://localhost:$PROVIDER_PORT/api/v1/persistence/query" \
  --insecure \
  -u $PROVIDER_USER:$PROVIDER_PASSWORD \
  --header 'Content-Type: application/json' \
  --data-binary @- | jq '.positionedValues[-1].value.json | fromjson'
```

Update testament:

```bash
jq -nc --arg clientId $(uuidgen) --arg userId $USER_ID '{
  "issuer": $userId,
  "inheritors": {
    "1": 6000,
    "2": 2000,
    "3": 2000
  }
} | tostring as $params | {
  "rpcStartFlowRequest": {
    "clientId": $clientId,
    "flowName": "com.example.testament.flows.UpdateTestamentFlow",
    "parameters": {
      "parametersInJson": $params
    }
  }
}' | curl --request POST "https://localhost:$PROVIDER_PORT/api/v1/flowstarter/startflow" \
  -u $PROVIDER_USER:$PROVIDER_PASSWORD \
  --insecure \
  --header 'Content-Type: application/json' \
  --data-binary @- | jq
```

Revoke testament:

```bash
jq -nc --arg clientId $(uuidgen) --arg userId $USER_ID '{
  "issuer": $userId
} | tostring as $params | {
  "rpcStartFlowRequest": {
    "clientId": $clientId,
    "flowName": "com.example.testament.flows.RevokeTestamentFlow",
    "parameters": {
      "parametersInJson": $params
    }
  }
}' | curl --request POST "https://localhost:$PROVIDER_PORT/api/v1/flowstarter/startflow" \
  -u $PROVIDER_USER:$PROVIDER_PASSWORD \
  --insecure \
  --header 'Content-Type: application/json' \
  --data-binary @- | jq
```

Announce testament:

```bash
jq -nc --arg clientId $(uuidgen) --arg userId $USER_ID '{
  "issuer": $userId
} | tostring as $params | {
  "rpcStartFlowRequest": {
    "clientId": $clientId,
    "flowName": "com.example.testament.flows.AnnounceTestamentFlow",
    "parameters": {
      "parametersInJson": $params
    }
  }
}' | curl --request POST "https://localhost:$GOV_PORT/api/v1/flowstarter/startflow" \
  -u $GOV_USER:$GOV_PASSWORD \
  --insecure \
  --header 'Content-Type: application/json' \
  --data-binary @- | jq
```

Execute testament:

```bash
jq -nc --arg clientId $(uuidgen) --arg userId $USER_ID '{
  "issuer": $userId
} | tostring as $params | {
  "rpcStartFlowRequest": {
    "clientId": $clientId,
    "flowName": "com.example.testament.flows.ExecuteTestamentFlow",
    "parameters": {
      "parametersInJson": $params
    }
  }
}' | curl --request POST "https://localhost:$BANK_PORT/api/v1/flowstarter/startflow" \
  -u $BANK_USER:$BANK_PASSWORD \
  --insecure \
  --header 'Content-Type: application/json' \
  --data-binary @- | jq
```

Store gold to Bank:

```bash
jq -nc --arg clientId $(uuidgen) --arg userId $USER_ID '{
  "holder": $userId,
  "amount": "3000"
} | tostring as $params | {
  "rpcStartFlowRequest": {
    "clientId": $clientId,
    "flowName": "com.example.testament.flows.StoreGoldFlow",
    "parameters": {
      "parametersInJson": $params
    }
  }
}' | curl --request POST "https://localhost:$BANK_PORT/api/v1/flowstarter/startflow" \
  --insecure \
  -u $BANK_USER:$BANK_PASSWORD \
  --header 'Content-Type: application/json' \
  --data-binary @- | jq
```

Retrieve bank account state:

```bash
jq -nc --arg userId $USER_ID '{
  "request": {
    "namedParameters": {
      "holderId": {
        "parametersInJson": $userId
      }
    },
    "queryName": "AccountSchemaV1.PersistentAccount.findByHolderId",
    "postProcessorName": "com.example.testament.processor.AccountPostProcessor"
  },
  "context": {
    "awaitForResultTimeout": "PT15M",
    "currentPosition": -1,
    "maxCount": 100
  }
}' | curl --request POST "https://localhost:$BANK_PORT/api/v1/persistence/query" \
  --insecure \
  -u $BANK_USER:$BANK_PASSWORD \
  --header 'Content-Type: application/json' \
  --data-binary @- | jq '.positionedValues[-1].value.json | fromjson'
```

Withdraw gold from Bank:

```bash
jq -nc --arg clientId $(uuidgen) --arg userId $USER_ID '{
  "holder": $userId,
  "amount": "2000"
} | tostring as $params | {
  "rpcStartFlowRequest": {
    "clientId": $clientId,
    "flowName": "com.example.testament.flows.WithdrawGoldFlow",
    "parameters": {
      "parametersInJson": $params
    }
  }
}' | curl --request POST "https://localhost:$BANK_PORT/api/v1/flowstarter/startflow" \
  --insecure \
  -u $BANK_USER:$BANK_PASSWORD \
  --header 'Content-Type: application/json' \
  --data-binary @- | jq
```

## Testing

We use only integration tests. They require local network running and artifact deployed.
Check out `scripts/start.sh` for details.
