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

import com.sksamuel.avro4s.RecordFormat
import org.apache.pekko.testkit.TestKit
import org.scalatest.{ BeforeAndAfterAll, Suite }

import java.io.File
import scala.reflect.io.Directory

trait AbstractAvroParquet extends BeforeAndAfterAll with AbstractAvroParquetBase {
  this: Suite with TestKit =>

  val format: RecordFormat[Document] = RecordFormat[Document]

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    val directory = new Directory(new File(folder))
    directory.deleteRecursively()
  }
}
