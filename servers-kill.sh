#!/usr/bin/env bash

ps -afx | grep java | grep chat | grep assembly | awk '{print $2}' | xargs -I {} kill -9 {}
