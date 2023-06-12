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

import com.sksamuel.avro4s._
import org.apache.pekko.testkit.TestKit
import org.scalatest.{ BeforeAndAfterAll, Suite }

import java.io.File

trait AbstractAvroParquet extends BeforeAndAfterAll with AbstractAvroParquetBase {
  this: Suite with TestKit =>

  implicit val toRecordDocument: ToRecord[Document] = ToRecord[Document](schema)
  implicit val fromRecordDocument: FromRecord[Document] = FromRecord[Document](schema)
  val format: RecordFormat[Document] = RecordFormat[Document](schema)

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    deleteRecursively(new File(folder))
  }

  private def deleteRecursively(f: File): Boolean = {
    if (f.isDirectory) f.listFiles match {
      case null =>
      case xs   => xs.foreach(deleteRecursively)
    }
    f.delete()
  }
}
