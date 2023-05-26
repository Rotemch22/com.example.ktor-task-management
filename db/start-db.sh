#!/usr/bin/env bash
set -e
BASEDIR=$(dirname "$0")

docker-compose -f ${BASEDIR}/docker-compose.yml up -d
