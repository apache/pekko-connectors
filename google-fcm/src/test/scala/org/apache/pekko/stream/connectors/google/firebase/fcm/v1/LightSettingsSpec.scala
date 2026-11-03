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

package org.apache.pekko.stream.connectors.google.firebase.fcm.v1

import org.apache.pekko
import pekko.stream.connectors.google.firebase.fcm.v1.models.LightSettings
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LightSettingsSpec extends AnyWordSpec with Matchers {

  "LightSettings" should {

    "set light_on_duration with withLightOnDuration" in {
      val settings = LightSettings.empty.withLightOnDuration("1s")
      settings.light_on_duration shouldBe Some("1s")
      settings.light_off_duration shouldBe None
    }

    "set light_off_duration with withLightOffDuration" in {
      val settings = LightSettings.empty.withLightOffDuration("2s")
      settings.light_off_duration shouldBe Some("2s")
      settings.light_on_duration shouldBe None
    }

    "set light_off_duration with the deprecated light_off_duration setter" in {
      @annotation.nowarn("cat=deprecation")
      val settings = LightSettings.empty.light_off_duration("2s")
      settings.light_off_duration shouldBe Some("2s")
      settings.light_on_duration shouldBe None
    }
  }
}
