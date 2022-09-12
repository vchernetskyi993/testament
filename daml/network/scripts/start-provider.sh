#!/bin/sh
set -e

MAX_WAIT=20
ATTEMPTS=0
FACTORY_CONFIG=/data/factory.json

# FIXME: factory config from previos runs breaks this condition
while [ ! -f $FACTORY_CONFIG ] || [ "$(curl -s -o /dev/null -w '%{http_code}' http://json.provider:7575/readyz)" != "200" ]; do
  if [ $ATTEMPTS = $MAX_WAIT ]; then
    echo "Factory config is not ready. Exiting..."
    exit 1
  else
    echo 'Waiting for factory config...'
    ATTEMPTS=$((ATTEMPTS + 1))
    sleep 3
  fi
done

FACTORY_CONTRACT_ID=$(cat $FACTORY_CONFIG | jq -r .contractId)
export FACTORY_CONTRACT_ID
GOVERNMENT_PARTY=$(cat $FACTORY_CONFIG | jq -r .parties.government)
export GOVERNMENT_PARTY
PROVIDER_PARTY=$(cat $FACTORY_CONFIG | jq -r .parties.provider)
export PROVIDER_PARTY

exec java -jar /app/quarkus-run.jar
