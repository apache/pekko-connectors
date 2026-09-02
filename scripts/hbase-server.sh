#!/bin/bash

set -x

# The HBase region server registers itself in ZooKeeper under the container
# hostname "hbase", so the host running the tests must be able to resolve
# that name to localhost: https://github.com/akka/alpakka/issues/2185
echo "127.0.0.1 hbase" | sudo tee -a /etc/hosts

docker compose up -d hbase
