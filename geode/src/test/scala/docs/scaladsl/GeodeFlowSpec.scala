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

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.geode.GeodeSettings
import org.apache.pekko.stream.connectors.geode.scaladsl.Geode
import org.apache.pekko.stream.scaladsl.{ Flow, Sink }

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class GeodeFlowSpec extends GeodeBaseSpec {

  "Alpakka geode" should {
    "create settings" in {
      {
        val hostname = "localhost"
        // #connection
        val geodeSettings = GeodeSettings(hostname, port = 10334)
          .withConfiguration(c => c.setPoolIdleTimeout(10))
        // #connection
        geodeSettings.toString should include("port=10334")
      }
    }

    it { geodeSettings =>
      "flow with shapeless pdx serializer" in {
        // #connection
        val geode = new Geode(geodeSettings)
        system.registerOnTermination(geode.close())
        // #connection

        val source = buildPersonsSource(1 to 10)

        // #flow
        val flow: Flow[Person, Person, NotUsed] = geode.flow(personsRegionSettings)

        val fut = source.via(flow).runWith(Sink.ignore)
        // #flow
        Await.ready(fut, 10 seconds)

        geode.close()
      }

      "flow with explicit pdx serializer" in {
        val geode = new Geode(geodeSettings)
        val source = buildPersonsSource(1 to 20)
        val flow: Flow[Person, Person, NotUsed] = geode.flow(personsRegionSettings, PersonPdxSerializer)
        val fut = source.via(flow).runWith(Sink.ignore)
        Await.ready(fut, 10 seconds)
        geode.close()
      }
    }
  }
}
