#!/usr/bin/env bash
# Deletes the cluster of hack/kind/up.sh
set -euo pipefail
kind delete cluster --name car-ledger
