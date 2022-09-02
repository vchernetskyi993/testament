#!/bin/bash
set -e

cd "$WORKDIR"
daml clean
daml build
