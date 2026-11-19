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

package org.apache.pekko.stream.connectors.solr.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.solr.*;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the Solr messages without a Solr server. */
public class SolrJavaApiTest {

  @Test
  public void writeMessageGetters() {
    Map<String, Map<String, Object>> updates =
        Collections.singletonMap("field", Collections.singletonMap("set", (Object) 1));
    WriteMessage<String, String> message =
        WriteMessage.<String>createUpdateMessage("id", "42", updates)
            .withRoutingFieldValue("routing")
            .withPassThrough("pass");

    assertEquals(Optional.of("id"), message.getIdField());
    assertEquals(Optional.of("42"), message.getIdFieldValue());
    assertEquals(Optional.of("routing"), message.getRoutingFieldValue());
    assertEquals(Optional.empty(), message.getQuery());
    assertEquals(Optional.empty(), message.getSource());
    assertEquals(updates, message.getUpdates());
    assertEquals("pass", message.getPassThrough());
  }

  @Test
  public void writeResultGetters() {
    Map<String, Map<String, Object>> updates =
        Collections.singletonMap("field", Collections.singletonMap("inc", (Object) 2));
    WriteMessage<String, NotUsed> message =
        WriteMessage.<String>createUpdateMessage("id", "7", updates);
    WriteResult<String, NotUsed> result =
        new WriteResult<>(
            message.idField(),
            message.idFieldValue(),
            message.routingFieldValue(),
            message.query(),
            message.source(),
            message.updates(),
            NotUsed.getInstance(),
            0);

    assertEquals(Optional.of("id"), result.getIdField());
    assertEquals(Optional.of("7"), result.getIdFieldValue());
    assertEquals(updates, result.getUpdates());
    assertSame(NotUsed.getInstance(), result.getPassThrough());
    assertEquals(0, result.getStatus());
  }

  @Test
  public void operationsFromJava() {
    assertSame(Upsert.getInstance(), WriteMessage.createUpsertMessage("doc").operation());
    assertSame(DeleteByIds.getInstance(), WriteMessage.createDeleteMessage("id").operation());
    assertSame(
        DeleteByQuery.getInstance(), WriteMessage.createDeleteByQueryMessage("q").operation());
    assertSame(PassThrough.getInstance(), WriteMessage.createPassThrough("p").operation());
    assertSame(
        AtomicUpdate.getInstance(),
        WriteMessage.createUpdateMessage("id", "1", Collections.emptyMap()).operation());
  }
}
