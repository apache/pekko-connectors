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

package org.apache.pekko.stream.connectors.xml.javadsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.connectors.xml.Attribute;
import org.apache.pekko.stream.connectors.xml.Namespace;
import org.apache.pekko.stream.connectors.xml.ParseEvent;
import org.apache.pekko.stream.connectors.xml.StartElement;
import org.apache.pekko.stream.javadsl.FlowWithContext;
import org.apache.pekko.util.ByteString;
import org.junit.jupiter.api.Test;

/** Exercises the Java API of the XML model. */
public class XmlJavaApiTest {

  @Test
  public void startElementAttributes() {
    Attribute attribute = Attribute.create("id", "1", Optional.of("p"), Optional.of("urn:ns"));
    StartElement element =
        StartElement.create(
            "element", Collections.singletonList(attribute), Optional.empty(), Optional.empty());

    assertEquals(Optional.of(attribute), element.getAttribute("id"));
    assertFalse(element.getAttribute("missing").isPresent());
    assertEquals(Collections.singletonList(attribute), element.getAttributesList());
  }

  @Test
  public void attributeListAndNamespaceFactories() {
    List<Attribute> attributes =
        StartElement.createAttributeList(
            Collections.singletonMap("a", "b"), Optional.of("p"), Optional.empty());
    assertEquals(
        Collections.singletonList(
            new Attribute("a", "b", scala.Option.apply("p"), scala.Option.empty())),
        attributes);

    assertEquals(Optional.empty(), Namespace.create("urn:ns").getPrefix());
  }

  /** Compile-only check: parserWithContext can configure the factory without ignoreInvalidChars. */
  @SuppressWarnings("unused")
  private static void parserWithContextCompiles() {
    FlowWithContext<ByteString, String, ParseEvent, String, NotUsed> flow =
        XmlParsing.parserWithContext(factory -> {});
  }
}
