/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

package org.apache.pekko.stream.connectors.huawei.pushkit

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HmsSettingsSpec extends AnyWordSpec with Matchers {
  "HmsSettings" must {
    "have an apply that does not recurse indefinitely" in {
      val hmsSettings = HmsSettings("id1", "secret1", None)
      hmsSettings should equal(HmsSettings(hmsSettings.appId, hmsSettings.appSecret, false, 50, None))
    }
  }
}
