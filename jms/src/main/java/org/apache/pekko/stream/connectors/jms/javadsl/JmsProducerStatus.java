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

package org.apache.pekko.stream.connectors.jms.javadsl;

import org.apache.pekko.NotUsed;
import org.apache.pekko.stream.javadsl.Source;

public interface JmsProducerStatus {

    /**
     * source that provides connector status change information.
     * Only the most recent connector state is buffered if the source is not consumed.
     */
    Source<JmsConnectorState, NotUsed> connectorState();
}
