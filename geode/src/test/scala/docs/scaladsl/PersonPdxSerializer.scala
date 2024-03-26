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

import java.util.Date

import org.apache.pekko.stream.connectors.geode.PekkoPdxSerializer
import org.apache.geode.pdx.{ PdxReader, PdxWriter }

//#person-pdx-serializer
object PersonPdxSerializer extends PekkoPdxSerializer[Person] {
  override def clazz: Class[Person] = classOf[Person]

  override def toData(o: scala.Any, out: PdxWriter): Boolean =
    o match {
      case p: Person =>
        out.writeInt("id", p.id)
        out.writeString("name", p.name)
        out.writeDate("birthDate", p.birthDate)
        true
      case _ => false
    }

  override def fromData(clazz: Class[_], in: PdxReader): AnyRef = {
    val id: Int = in.readInt("id")
    val name: String = in.readString("name")
    val birthDate: Date = in.readDate("birthDate")
    Person(id, name, birthDate)
  }
}
//#person-pdx-serializer
