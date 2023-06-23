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

package docs.scaladsl

import org.apache.pekko
import pekko.stream.connectors.geode.scaladsl.Geode
import pekko.stream.scaladsl.Sink
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class GeodeFiniteSourceSpec extends GeodeBaseSpec {

  private val log = LoggerFactory.getLogger(classOf[GeodeFiniteSourceSpec])

  "Geode finite source" should {
    it { geodeSettings =>
      "retrieves finite elements from geode" in {
        // #query
        val geode = new Geode(geodeSettings)
        system.registerOnTermination(geode.close())

        val source =
          geode
            .query[Person](s"select * from /persons order by id")
            .runWith(Sink.foreach(e => log.debug(s"$e")))
        // #query
        Await.ready(source, 10 seconds)

        val animals =
          geode
            .query[Animal](s"select * from /animals order by id")
            .runWith(Sink.foreach(e => log.debug(s"$e")))

        Await.ready(animals, 10 seconds)

        val complexes =
          geode
            .query[Complex](s"select * from /complexes order by id")
            .runWith(Sink.foreach(e => log.debug(s"$e")))

        Await.ready(complexes, 10 seconds)

        geode.close()
      }
    }
  }
}
