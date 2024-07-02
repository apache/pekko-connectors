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

package org.apache.pekko.stream.connectors.jakartams

import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory
import org.apache.activemq.artemis.junit.EmbeddedActiveMQResource

class EmbeddedActiveMQServer(activeMQResource: EmbeddedActiveMQResource) {
  val server: EmbeddedActiveMQResource = activeMQResource

  def createConnectionFactory: ActiveMQConnectionFactory = new ActiveMQConnectionFactory(server.getVmURL)
  def getServer: EmbeddedActiveMQ = server.getServer
  def getMessageCount(queueName: String): Long = server.getMessageCount(queueName)
  def getBrokerUrl: String = server.getVmURL

  def start(): Unit = server.start()
  def stop(): Unit = server.stop()
}
