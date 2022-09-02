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

DARS=(/contracts/.daml/dist/*)

echo "Starting $PARTICIPANT_NAME ledger node..."

exec /canton/bin/canton daemon \
  -c /configs/features.conf,/configs/"$PARTICIPANT_NAME".conf \
  -Dparticipant.name="$PARTICIPANT_NAME" \
  -Dparticipant.user="$PARTICIPANT_USER" \
  -Ddar.path="${DARS[0]}" \
  --bootstrap /scripts/participant.scala
