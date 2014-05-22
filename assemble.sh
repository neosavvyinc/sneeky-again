#!/bin/sh
sbt clean assembly; cp phantom-data-services-core/target/scala-2.10/phantom.jar ../SneekyServers/playbooks/deploy-server-app/jar/phantom.jar
