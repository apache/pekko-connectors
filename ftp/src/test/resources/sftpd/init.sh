#!/bin/bash

# This script is only meant to be run as part of Docker container.
# See https://github.com/apache/incubator-pekko-connectors/blob/a639063ffc41a9cb8f9ba5c11dfd997a6750b3c1/docker-compose.yml#L235
# Do not run this script on your personal machine. If you do,
# please remember to remove the 2 files from /etc/ssh after you have finished testing.

cp /tmp/ssh_host_ed25519_key /etc/ssh/
cp /tmp/ssh_host_rsa_key /etc/ssh/
chmod 600 /etc/ssh/ssh_host_ed25519_key
chmod 600 /etc/ssh/ssh_host_rsa_key
