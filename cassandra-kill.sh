#!/usr/bin/env bash

ps -afx  | grep test-embedded-cassandra.yaml | grep java | awk '{print $2}' | xargs -I {} kill -9 {}
