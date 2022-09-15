#!/bin/bash
set -e

MAX_WAIT=20
ATTEMPTS=0

while [ "$(ping -c 1 create-factory)" ]; do
  if [ $ATTEMPTS = $MAX_WAIT ]; then
    echo "Factory config is not ready. Exiting..."
    exit 1
  else
    echo 'Waiting for factory config...'
    ATTEMPTS=$((ATTEMPTS + 1))
    sleep 3
  fi
done

REACT_APP_DAML_PARTY=$(jq -r .parties.government < /data/factory.json)
export REACT_APP_DAML_PARTY
REACT_APP_FACTORY_ID=$(jq -r .contractId < /data/factory.json)
export REACT_APP_FACTORY_ID

cd /app/contracts
daml build
daml codegen js

cd /app/gov-app
npm i --force
npm run build
