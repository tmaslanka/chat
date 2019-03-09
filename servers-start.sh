#!/usr/bin/env bash

./servers-kill.sh

start() {
    akka_port=$1
    http_port=$2
    java -Dakka.remote.netty.tcp.port=$akka_port -Dchat.server.port=$http_port -jar ./target/scala-2.12/chat-assembly.jar > target/server-http-$http_port.log
}
start 2551 8082 &
start 2552 8081 &
start 2553 8080 &
