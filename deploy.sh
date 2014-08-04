#!/bin/bash

scp shoutout-data-services-core/target/scala-2.10/shoutout.jar root@shiva.neosavvy.com:./
ssh root@shiva.neosavvy.com 'scp ./shoutout.jar root@vm10:/opt/'
