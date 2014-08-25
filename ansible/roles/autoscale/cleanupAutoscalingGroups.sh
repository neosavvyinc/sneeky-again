#!/bin/bash

# Terminate all spawned Auto Scaling Instances
ec2-terminate-instances `as-describe-auto-scaling-instances | cut -f 3 -d " "`

# Delete the auto-scaling group
as-delete-auto-scaling-group --force-delete --auto-scaling-group `as-describe-auto-scaling-groups | grep AUTO-SCALING-GROUP | cut -f 3 -d " "`

# Delete the launch configuration
as-delete-launch-config `as-describe-launch-configs | cut -f 3 -d " "` -f

# Delete the AMIs
ec2-deregister `ec2-describe-images | cut -f 2 | grep ami`
