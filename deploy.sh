#!/bin/bash

scp phantom-data-services-core/target/scala-2.10/phantom.jar root@shiva.neosavvy.com:./
ssh root@shiva.neosavvy.com 'scp ./phantom.jar root@vm10:/opt/'
