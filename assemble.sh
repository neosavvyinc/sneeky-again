#!/bin/sh
sbt clean assembly;
cp sneeky-data-services-core/target/scala-2.10/sneeky-v2.jar ./ansible/roles/deploy/files/
