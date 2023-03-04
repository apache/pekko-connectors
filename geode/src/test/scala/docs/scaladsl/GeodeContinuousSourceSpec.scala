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

import akka.NotUsed
import akka.stream.alpakka.geode.scaladsl.{ Geode, PoolSubscription }
import akka.stream.scaladsl.{ Flow, Sink }
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class GeodeContinuousSourceSpec extends GeodeBaseSpec {

  private val log = LoggerFactory.getLogger(classOf[GeodeContinuousSourceSpec])

  "Geode continuousQuery" should {
    it { geodeSettings =>
      "retrieves continuously elements from geode" in {

        // #connection-with-pool
        val geode = new Geode(geodeSettings) with PoolSubscription
        system.registerOnTermination(geode.close())
        // #connection-with-pool

        val flow: Flow[Person, Person, NotUsed] = geode.flow(personsRegionSettings)

        // #continuousQuery
        val source =
          geode
            .continuousQuery[Person](Symbol("test"), s"select * from /persons")
            .runWith(Sink.fold(0) { (c, p) =>
              log.debug(s"$p $c")
              if (c == 19) {
                geode.closeContinuousQuery(Symbol("test")).foreach { _ =>
                  log.debug("test cQuery is closed")
                }

              }
              c + 1
            })
        // #continuousQuery

        val f = buildPersonsSource(1 to 20)
          .via(flow) // geode flow
          .runWith(Sink.ignore)

        Await.result(f, 10 seconds)

        Await.result(source, 5 seconds)
        geode.close()
      }
    }
  }
}
