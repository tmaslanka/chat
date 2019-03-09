#!/usr/bin/env bash
set -xe

./servers-kill.sh

sbt clean test assembly it-test:test

./servers-kill.sh

#echo "building docker"
#sbt docker