#!/bin/bash
set -e

MAX_WAIT=5
ATTEMPTS=0

while [ ! -d "/contracts/.daml/dist/" ]; do
  if [ $ATTEMPTS = $MAX_WAIT ]; then
    echo "DAR is not ready. Exiting..."
    exit 1
  else
    echo 'Waiting for contracts DAR...'
    ATTEMPTS=$((ATTEMPTS + 1))
    sleep 1
  fi
done

cp /contracts/.daml/dist/*.dar contracts.dar 

echo "Starting $PARTY_NAME ledger node..."

exec /canton/bin/canton daemon \
  -c /configs/features.conf,/configs/ledger-node.conf,/configs/government.conf \
  -Dparty.name="$PARTY_NAME" \
  -Duser="$PARTY_USER" \
  -Ddar.path=contracts.dar \
  --bootstrap /scripts/participant.scala
