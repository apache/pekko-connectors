/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.scaladsl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.Graph
import pekko.stream.connectors.google.{ GoogleAttributes, GoogleSettings }
import pekko.stream.scaladsl.Source

import scala.annotation.nowarn

@nowarn("msg=never used")
@nowarn("msg=dead code")
class GoogleCommonDoc {

  implicit val system: ActorSystem = ???
  val stream: Graph[Nothing, Nothing] = ???

  { // #accessing-settings
    val defaultSettings = GoogleSettings()
    val customSettings = GoogleSettings("my-app.custom-google-config")
    Source.fromMaterializer { (mat, attr) =>
      val settings: GoogleSettings = GoogleAttributes.resolveSettings(mat, attr)
      Source.empty
    }
    // #accessing-settings
  }

  {
    // #custom-settings
    stream.addAttributes(GoogleAttributes.settingsPath("my-app.custom-google-config"))

    val defaultSettings = GoogleSettings()
    val customSettings = defaultSettings.withProjectId("my-other-project")
    stream.addAttributes(GoogleAttributes.settings(customSettings))
    // #custom-settings
  }

}
