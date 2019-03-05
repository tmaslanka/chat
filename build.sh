#!/usr/bin/env bash
set -xe

sbt clean test assembly it-test:test

echo "building docker"
sbt docker