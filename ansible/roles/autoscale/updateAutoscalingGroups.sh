#!/bin/bash

AMI_ID=`ec2-create-image i-484fae6b --name "Shoutout v1.0.0 Release Image" --description "Copy of Node 1 for Auto Scaling" | cut -f2`

echo "Created an AMI with ID: " $AMI_ID

# make sure it adds to loadbalancer automatically


LAUNCH_ID=`as-create-launch-config shoutout-launch-config --image-id $AMI_ID --instance-type m3.xlarge --group sg-1f6ab97a`

echo "Created Launch Configuration with ID: " $LAUNCH_ID

as-create-auto-scaling-group shoutout-autoscaling-group --launch-configuration shoutout-launch-config --availability-zones us-east-1c --min-size 1 --max-size 10 --desired-capacity 1 --load-balancers ShoutoutHttpsLoadBalancer

#as-put-scaling-policy one for high one for low
increaseAlarm=`as-put-scaling-policy ShoutoutIncreaseNodes --auto-scaling-group shoutout-autoscaling-group --adjustment=1 --type ChangeInCapacity --cooldown 300 --region us-east-1`
mon-put-metric-alarm shoutout-high-cpu-alarm --comparison-operator GreaterThanThreshold --evaluation-periods 3 --metric-name CPUUtilization --namespace "AWS/EC2" --period 60 --statistic Average --threshold 70 --alarm-actions $increaseAlarm --dimensions "AutoScalingGroupName=shoutout-autoscaling-group" --region us-east-1

decreaseAlarm=`as-put-scaling-policy ShoutoutDecreaseNodes --auto-scaling-group shoutout-autoscaling-group --adjustment=-1 --type ChangeInCapacity --cooldown 300 --region us-east-1`
mon-put-metric-alarm shoutout-low-cpu-alarm --comparison-operator LessThanThreshold --evaluation-periods 3 --metric-name CPUUtilization --namespace "AWS/EC2" --period 60 --statistic Average --threshold 30 --alarm-actions $decreaseAlarm --dimensions "AutoScalingGroupName=shoutout-autoscaling-group" --region us-east-1