#!/bin/bash

set -e

MAX_WAIT=5
ATTEMPTS=0

while [ ! -f "/data/${SCRIPT_FILE}" ]; do
  if [ $ATTEMPTS = $MAX_WAIT ]; then
    echo "Script ${SCRIPT_FILE} is not ready. Exiting..."
    exit 1
  else
    echo "Waiting for ${SCRIPT_FILE}..."
    ATTEMPTS=$((ATTEMPTS + 1))
    sleep 1
  fi
done

bash "/data/${SCRIPT_FILE}"
