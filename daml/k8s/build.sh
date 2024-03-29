#!/bin/sh

echo "--- Building Docker images ---"

docker build ../auth-server/ -t testament/auth-server
docker build .. -f ../network/Dockerfile.ledger -t testament/ledger
docker build ../network/ -f ../network/Dockerfile.json -t testament/json

echo "--- Loading images to Minikube ---"

minikube image load testament/auth-server
minikube image load testament/ledger
minikube image load testament/json
