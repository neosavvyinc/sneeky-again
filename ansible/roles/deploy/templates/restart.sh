#!/bin/bash

kill -9 $(ps aux | grep '[j]ava' | grep shoutout | awk '{print $2}')
nohup java -Dconfig.file=/opt/shoutout/production.conf -Dlogback.configurationFile=/opt/shoutout/logback.xml -jar /opt/shoutout/shoutout.jar