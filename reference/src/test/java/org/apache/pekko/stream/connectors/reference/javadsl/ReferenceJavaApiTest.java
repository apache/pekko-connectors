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

package org.apache.pekko.stream.connectors.reference.javadsl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.Attributes;
import org.apache.pekko.stream.connectors.reference.ReferenceAttributes;
import org.apache.pekko.stream.connectors.reference.Resource;
import org.apache.pekko.stream.connectors.reference.ResourceSettings;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the reference connector's resource. */
public class ReferenceJavaApiTest {

  @Test
  public void resourceConnectionIsAJavaFlow() {
    Resource resource = Resource.create(ResourceSettings.create("message"));
    Flow<ByteString, ByteString, NotUsed> connection = resource.getConnection();
    assertNotNull(connection);

    Attributes attributes = ReferenceAttributes.resource(resource);
    assertNotNull(attributes);
  }
}
