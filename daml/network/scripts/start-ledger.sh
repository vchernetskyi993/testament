#!/bin/bash
set -e

MAX_WAIT=5
ATTEMPTS=0

while [ ! -f "/dars/contracts-0.1.0.dar" ]; do
  if [ $ATTEMPTS = $MAX_WAIT ]; then
    echo "DAR is not ready. Exiting..."
    exit 1
  else
    echo 'Waiting for contracts DAR...'
    ATTEMPTS=$((ATTEMPTS + 1))
    sleep 1
  fi
done

echo "Starting $PARTY_NAME ledger node..."

exec /canton/bin/canton daemon \
  -c /configs/features.conf,/configs/ledger-node.conf,/configs/government.conf \
  -Dparty.name="$PARTY_NAME" \
  -Duser="$USER" \
  -Ddar.path=/dars/contracts-0.1.0.dar \
  --bootstrap /scripts/participant.scala
