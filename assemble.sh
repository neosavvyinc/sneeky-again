#!/bin/sh
sbt clean assembly;
cp shoutout-data-services-core/target/scala-2.10/shoutout.jar ./ansible/roles/deploy/files/
