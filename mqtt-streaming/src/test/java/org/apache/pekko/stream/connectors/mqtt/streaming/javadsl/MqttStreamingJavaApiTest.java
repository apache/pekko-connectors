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

package org.apache.pekko.stream.connectors.mqtt.streaming.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.mqtt.streaming.*;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the MQTT streaming model without a broker. */
public class MqttStreamingJavaApiTest {

  @Test
  public void singletonPackets() {
    Command<Object> disconnect = new Command<>(Disconnect.getInstance());
    assertSame(Disconnect.getInstance(), disconnect.command());
    assertEquals(ControlPacketType.PINGREQ(), PingReq.getInstance().packetType());
    assertEquals(ControlPacketType.PINGRESP(), PingResp.getInstance().packetType());
  }

  @Test
  public void eventCarry() {
    assertEquals(Optional.of("carry"), new Event<>(Disconnect.getInstance(), "carry").getCarry());
    assertEquals(Optional.empty(), new Event<String>(Disconnect.getInstance()).getCarry());
  }

  @Test
  public void publishPacketId() {
    Publish publish = new Publish("topic", ByteString.fromString("payload"));
    assertEquals(Optional.of(0), publish.getPacketId());

    Publish atMostOnce =
        new Publish(PublishQoSFlags.QoSAtMostOnceDelivery(), "topic", ByteString.emptyByteString());
    assertEquals(Optional.empty(), atMostOnce.getPacketId());
  }

  @Test
  public void connectWithersAndGetters() {
    Connect connect =
        new Connect("client", ConnectFlags.CleanSession(), "user", "pass")
            .withKeepAlive(Duration.ofSeconds(5))
            .withWill("willTopic", "willMessage");

    assertEquals(Duration.ofSeconds(5), connect.getKeepAlive());
    assertEquals(Optional.of("willTopic"), connect.getWillTopic());
    assertEquals(Optional.of("willMessage"), connect.getWillMessage());
    assertEquals(Optional.of("user"), connect.getUsername());
    assertEquals(Optional.of("pass"), connect.getPassword());
    assertTrue((connect.connectFlags() & ConnectFlags.WillFlag()) != 0);
  }

  @Test
  public void subscribeAndUnsubscribeLists() {
    List<Pair<String, Integer>> filters =
        Collections.singletonList(Pair.create("topic", SubscribeQoSFlags.QoSAtLeastOnceDelivery()));
    assertEquals(filters, new Subscribe(filters).getTopicFilters());

    List<Integer> codes = Arrays.asList(1, 2);
    assertEquals(codes, new SubAck(7, codes).getReturnCodes());

    assertEquals(
        Arrays.asList("a", "b"), new Unsubscribe(Arrays.asList("a", "b")).getTopicFilters());
  }

  @Test
  public void sessionSettingsDurations() {
    MqttSessionSettings settings =
        MqttSessionSettings.create()
            .withReceiveConnectTimeout(Duration.ofSeconds(1))
            .withReceiveConnAckTimeout(Duration.ofSeconds(2))
            .withReceiveUnsubAckTimeout(Duration.ofSeconds(3));
    assertEquals(Duration.ofSeconds(1), settings.getReceiveConnectTimeout());
    assertEquals(Duration.ofSeconds(2), settings.getReceiveConnAckTimeout());
    assertEquals(Duration.ofSeconds(3), settings.getReceiveUnsubAckTimeout());
    assertEquals(Duration.ofSeconds(30), settings.getConsumerPubRelTimeout());
  }
}
