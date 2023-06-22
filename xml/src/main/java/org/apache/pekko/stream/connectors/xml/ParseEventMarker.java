/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.stream.connectors.xml;

/**
 * Mirrors the sub-classes of [[ParseEvent]] to allow use with Java switch statements instead of
 * chained `instanceOf` tests.
 */
public enum ParseEventMarker {
  XMLStartDocument,
  XMLEndDocument,
  XMLStartElement,
  XMLEndElement,
  XMLCharacters,
  XMLProcessingInstruction,
  XMLComment,
  XMLCData,
}
