/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.hbase

import org.apache.hadoop.hbase.{ HBaseConfiguration, HBaseTestingUtility, HConstants }

/**
 * An HBase cluster (mini ZooKeeper, mini HDFS, master and region server) running inside the test JVM,
 * started once and shared by every suite in that JVM.
 *
 * ZooKeeper is pinned to its default port so that a plain `HBaseConfiguration.create()`, which is what
 * the documentation snippets show, reaches the cluster without any test-specific configuration.
 */
object HBaseMiniCluster {

  private lazy val cluster: HBaseTestingUtility = {
    val conf = HBaseConfiguration.create()
    // HBaseTestingUtility reads the port it should hand to the mini ZooKeeper from this key
    conf.setInt("test.hbase.zookeeper.property.clientPort", HConstants.DEFAULT_ZOOKEEPER_CLIENT_PORT)
    // the master and region server web UIs are of no use here, and their JAXB stack cannot
    // initialize on a JDK that keeps java.lang closed; -1 switches them off
    conf.setInt(HConstants.MASTER_INFO_PORT, -1)
    conf.setInt(HConstants.REGIONSERVER_INFO_PORT, -1)
    // the default asyncfs WAL reflects into protobuf internals in a way that does not survive the
    // shaded hbase/hadoop combination we test against; the filesystem WAL is equivalent for our purposes
    conf.set("hbase.wal.provider", "filesystem")
    conf.set("hbase.wal.meta_provider", "filesystem")
    val util = new HBaseTestingUtility(conf)
    util.startMiniCluster()
    val port = util.getZkCluster.getClientPort
    if (port != HConstants.DEFAULT_ZOOKEEPER_CLIENT_PORT) {
      util.shutdownMiniCluster()
      throw new IllegalStateException(
        s"the mini cluster's ZooKeeper fell back to port $port, so clients built from a plain " +
        s"HBaseConfiguration.create() would not find it; is something else listening on " +
        s"${HConstants.DEFAULT_ZOOKEEPER_CLIENT_PORT}?")
    }
    sys.addShutdownHook(util.shutdownMiniCluster())
    util
  }

  /**
   * Starts the cluster, unless this JVM already has one running. Blocks until it is ready to serve.
   */
  def start(): Unit = {
    cluster
    ()
  }
}
