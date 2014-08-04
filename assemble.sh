#!/bin/sh
sbt clean assembly; cp shoutout-data-services-core/target/scala-2.10/shoutout.jar ../ShoutoutServers/playbooks/deploy-server-app/jar/shoutout.jar
