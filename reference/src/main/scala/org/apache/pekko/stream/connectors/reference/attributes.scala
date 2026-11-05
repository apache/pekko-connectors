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

package org.apache.pekko.stream.connectors.reference

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.Attributes
import pekko.stream.Attributes.Attribute

object ReferenceAttributes {

  /**
   * Wrap a `Resource` to an attribute so it can be attached to a stream stage.
   */
  def resource(resource: Resource): Attributes = Attributes(new ReferenceResourceValue(resource))
}

final class ReferenceResourceValue @InternalApi private[reference] (val resource: Resource) extends Attribute
