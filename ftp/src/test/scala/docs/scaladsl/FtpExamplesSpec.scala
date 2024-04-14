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
import java.io.PrintWriter
import java.net.InetAddress

import org.apache.pekko
import pekko.stream.Materializer
import pekko.stream.connectors.ftp.{ BaseFtpSupport, FtpSettings }
import pekko.stream.connectors.testkit.scaladsl.LogCapturing
import pekko.stream.scaladsl.Source
import pekko.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import pekko.testkit.TestKit
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class FtpExamplesSpec
    extends BaseFtpSupport
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with LogCapturing {

  implicit val materializer: Materializer = getMaterializer

  override protected def afterAll(): Unit = {
    getRootDir.resolve("file.txt").toFile.delete()
    getRootDir.resolve("file.txt.gz").toFile.delete()
    TestKit.shutdownActorSystem(getSystem)
    super.afterAll()
  }

  def ftpSettings = {
    // #create-settings
    val ftpSettings = FtpSettings
      .create(InetAddress.getByName(HOSTNAME))
      .withPort(PORT)
      .withCredentials(CREDENTIALS)
      .withBinary(true)
      .withPassiveMode(true)
      // only useful for debugging
      .withConfigureConnection { (ftpClient: FTPClient) =>
        ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true))
      }
    // #create-settings
    ftpSettings
  }

  "a file" should {
    "be stored" in assertAllStagesStopped {
      // #storing
      import org.apache.pekko
      import pekko.stream.IOResult
      import pekko.stream.connectors.ftp.scaladsl.Ftp
      import pekko.util.ByteString
      import scala.concurrent.Future

      val result: Future[IOResult] = Source
        .single(ByteString("this is the file contents"))
        .runWith(Ftp.toPath("file.txt", ftpSettings))
      // #storing

      val ioResult = result.futureValue(timeout(Span(1, Seconds)))
      ioResult should be(IOResult.createSuccessful(25))

      val p = fileExists("file.txt")
      p should be(true)

    }

    "be gzipped" in assertAllStagesStopped {
      import pekko.stream.IOResult
      import pekko.stream.connectors.ftp.scaladsl.Ftp
      import pekko.util.ByteString
      import scala.concurrent.Future

      // #storing

      // Create a gzipped target file
      import org.apache.pekko.stream.scaladsl.Compression
      val result: Future[IOResult] = Source
        .single(ByteString("this is the file contents" * 50))
        .via(Compression.gzip)
        .runWith(Ftp.toPath("file.txt.gz", ftpSettings))
      // #storing

      val ioResult = result.futureValue(timeout(Span(1, Seconds)))
      ioResult should be(IOResult.createSuccessful(61))

      val p = fileExists("file.txt.gz")
      p should be(true)

    }
  }

}
