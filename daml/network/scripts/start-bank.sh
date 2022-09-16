#!/bin/sh
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

FACTORY_CONFIG=/data/factory.json
DAML_FACTORY_ID=$(cat $FACTORY_CONFIG | jq -r .contractId)
export DAML_FACTORY_ID
DAML_GOVERNMENT_PARTY=$(cat $FACTORY_CONFIG | jq -r .parties.government)
export DAML_GOVERNMENT_PARTY
DAML_BANK_PARTY=$(cat $FACTORY_CONFIG | jq -r .parties.bank)
export DAML_BANK_PARTY

exec node /app/dist/index.js
