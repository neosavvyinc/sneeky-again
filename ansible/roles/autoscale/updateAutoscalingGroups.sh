#!/bin/bash

AMI_ID=`ec2-create-image i-484fae6b --name "Shoutout v1.0.0 Release Image" --description "Copy of Node 1 for Auto Scaling" | cut -f2`

echo "Created an AMI with ID: " $AMI_ID

LAUNCH_ID=`as-create-launch-config shoutout-launch-config --image-id $AMI_ID --instance-type m3.xlarge --group sg-1f6ab97a`

echo "Created Launch Configuration with ID: " $LAUNCH_ID

as-create-auto-scaling-group shoutout-autoscaling-group --launch-configuration shoutout-launch-config --availability-zones us-east-1c --min-size 1 --max-size 10 --desired-capacity 1
