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

package org.apache.pekko.stream.connectors.ftp

import org.apache.pekko
import pekko.{ Done, NotUsed }
import pekko.stream.IOResult
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.testkit.TestKit
import pekko.util.ByteString
import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatest.{ Args, BeforeAndAfter, BeforeAndAfterAll, Inside, Status, TestSuite, TestSuiteMixin }

import scala.concurrent.Future
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

trait BaseSpec
    extends TestSuiteMixin
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll
    with ScalaFutures
    with IntegrationPatience
    with Inside
    with PekkoSupport
    with BaseSupport
    with LogCapturing { this: TestSuite =>

  protected def listFilesWithWrongCredentials(basePath: String): Source[FtpFile, NotUsed]

  protected def listFiles(basePath: String): Source[FtpFile, NotUsed]

  protected def listFilesWithFilter(
      basePath: String,
      branchSelector: FtpFile => Boolean,
      emitTraversedDirectories: Boolean = false): Source[FtpFile, NotUsed]

  protected def retrieveFromPath(
      path: String,
      fromRoot: Boolean = false): Source[ByteString, Future[IOResult]]

  protected def retrieveFromPathWithOffset(
      path: String,
      offset: Long): Source[ByteString, Future[IOResult]]

  protected def storeToPath(path: String, append: Boolean): Sink[ByteString, Future[IOResult]]

  protected def remove(): Sink[FtpFile, Future[IOResult]]

  protected def move(destinationPath: FtpFile => String): Sink[FtpFile, Future[IOResult]]

  protected def mkdir(basePath: String, name: String): Source[Done, NotUsed]

  after {
    cleanFiles()
  }

  override protected def afterAll() = {
    TestKit.shutdownActorSystem(getSystem, verifySystemShutdown = true)
    super.afterAll()
  }

  // Allows to run tests n times in a row with a command line argument, useful for debugging sporadic failures
  // e.g. ftp/testOnly *.FtpsStageSpec -- -Dtimes=20
  // https://gist.github.com/dwickern/6ba9c5c505d2325d3737ace059302922
  override protected def runTest(testName: String, args: Args): Status = {
    def run0(times: Int): Status = {
      val status = super.runTest(testName, args)
      if (times <= 1) status else status.thenRun(run0(times - 1))
    }
    run0(args.configMap.getWithDefault("times", "1").toInt)
  }
}
