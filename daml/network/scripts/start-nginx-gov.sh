#!/bin/bash
set -e

echo "Waiting for party..."

REACT_APP_DAML_PARTY=government::12203aade017dee9feecb3d252fa0725fe961c3fe8a32791668d65d9dc4e2c5d5d6e
export REACT_APP_DAML_PARTY

cd /app/gov-app
npm i
npm run build

exec /docker-entrypoint.sh nginx -g "daemon off;"
