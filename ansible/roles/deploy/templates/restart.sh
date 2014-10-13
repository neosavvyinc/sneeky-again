#!/bin/bash


kill -9 $(ps aux | grep '[j]ava' | grep sneeky-v2 | awk '{print $2}')
#nohup

echo "sneeky-v2 Process Has Been Started"