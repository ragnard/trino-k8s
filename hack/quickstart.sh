#!/usr/bin/env bash

set -euo pipefail

DIR="$(dirname "$(realpath "$0")")"
IMAGE_NAME=ghcr.io/ragnard/trino-k8s
TAG=0.9.4
IMAGE="${IMAGE_NAME}:${TAG}"

# Start Trino server
echo "Starting trino-server container"
docker run \
       -d \
       --rm \
       --network=host \
       --name trino-server \
       -v "${HOME}/.kube/config:/home/trino/.kube/config" \
       -v "${DIR}/kubernetes.properties:/etc/trino/catalog/kubernetes.properties" \
       ${IMAGE}

cleanup() {
    echo "Cleaning up before exit..."
    docker stop trino-server
}

trap cleanup EXIT

echo -n "Waiting for trino-server to become ready"
while ! curl -s localhost:8080; do
    echo -n "."
    sleep 1
done
echo " done!"

echo "Starting interactive trino-client container"
docker run \
       -it \
       --network=host \
       --name trino-client \
       --rm \
       ${IMAGE} trino http://localhost:8080
