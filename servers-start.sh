#!/usr/bin/env bash

java -Dakka.remote.netty.tcp.port=2551 -Dchat.server.port=8082 -jar ./target/scala-2.12/chat-assembly.jar
java -Dakka.remote.netty.tcp.port=2552 -Dchat.server.port=8081 -jar ./target/scala-2.12/chat-assembly.jar
java -Dakka.remote.netty.tcp.port=2553 -Dchat.server.port=8080 -jar ./target/scala-2.12/chat-assembly.jar
