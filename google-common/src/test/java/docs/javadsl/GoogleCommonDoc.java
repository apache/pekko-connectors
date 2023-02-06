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

package docs.javadsl;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.Graph;
import org.apache.pekko.stream.connectors.google.GoogleAttributes;
import org.apache.pekko.stream.connectors.google.GoogleSettings;
import org.apache.pekko.stream.javadsl.Source;

public class GoogleCommonDoc {

  ActorSystem system = null;
  Graph<?, ?> stream = null;

  void accessingSettings() {
    // #accessing-settings
    GoogleSettings defaultSettings = GoogleSettings.create(system);
    GoogleSettings customSettings = GoogleSettings.create("my-app.custom-google-config", system);
    Source.fromMaterializer(
        (mat, attr) -> {
          GoogleSettings settings = GoogleAttributes.resolveSettings(mat, attr);
          return Source.empty();
        });
    // #accessing-settings
  }

  void customSettings() {
    // #custom-settings
    stream.addAttributes(GoogleAttributes.settingsPath("my-app.custom-google-config"));

    GoogleSettings defaultSettings = GoogleSettings.create(system);
    GoogleSettings customSettings = defaultSettings.withProjectId("my-other-project");
    stream.addAttributes(GoogleAttributes.settings(customSettings));
    // #custom-settings
  }
}
