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

import java.util.{ Date, UUID }

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.stream.connectors.geode.{ GeodeSettings, RegionSettings }
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.Source
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class GeodeBaseSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with LogCapturing {

  implicit val system = ActorSystem("test")

  // #region
  val personsRegionSettings: RegionSettings[Int, Person] = RegionSettings("persons", (p: Person) => p.id)
  val animalsRegionSettings: RegionSettings[Int, Animal] = RegionSettings("animals", (a: Animal) => a.id)
  val complexesRegionSettings: RegionSettings[UUID, Complex] = RegionSettings("complexes", (a: Complex) => a.id)

  // #region

  /**
   * Run IT test only if geode is available.
   * @param f
   */
  def it(f: GeodeSettings => Unit): Unit =
    f(GeodeSettings(sys.env.get("IT_GEODE_HOSTNAME").getOrElse("localhost")))

  protected def buildPersonsSource(range: Range): Source[Person, Any] =
    Source(range).map(i => Person(i, s"Person Scala $i", new Date()))

  protected def buildAnimalsSource(range: Range): Source[Animal, Any] =
    Source(range).map(i => Animal(i, s"Animal Scala $i", 1))

  protected def buildComplexesSource(range: Range): Source[Complex, Any] =
    Source(range).map(i => Complex(UUID.randomUUID(), List(1, 2, 3), List(new Date()), Set(UUID.randomUUID())))

  override protected def afterAll(): Unit =
    Await.result(system.terminate(), 10 seconds)
}
