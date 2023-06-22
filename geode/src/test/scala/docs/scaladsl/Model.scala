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

import java.util.{ Date, UUID }

case class Person(id: Int, name: String, birthDate: Date)
case class Animal(id: Int, name: String, owner: Int)

case class Complex(id: UUID, ints: List[Int], dates: List[Date], ids: Set[UUID] = Set())
