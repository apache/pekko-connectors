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
import pekko.stream.connectors.ftp.{ FtpFile, FtpSettings, SftpSettings }

object scalaExamples {

  object sshConfigure {
    // #configure-custom-ssh-client
    import org.apache.pekko.stream.connectors.ftp.scaladsl.{ Sftp, SftpApi }
    import net.schmizz.sshj.{ DefaultConfig, SSHClient }

    val sshClient: SSHClient = new SSHClient(new DefaultConfig)
    val configuredClient: SftpApi = Sftp(sshClient)
    // #configure-custom-ssh-client
  }

  object traversing {
    // #traversing
    import org.apache.pekko
    import pekko.NotUsed
    import pekko.stream.connectors.ftp.scaladsl.Ftp
    import pekko.stream.scaladsl.Source

    def listFiles(basePath: String, settings: FtpSettings): Source[FtpFile, NotUsed] =
      Ftp.ls(basePath, settings)

    // #traversing
  }

  object retrieving {
    // #retrieving
    import org.apache.pekko
    import pekko.stream.IOResult
    import pekko.stream.connectors.ftp.scaladsl.Ftp
    import pekko.stream.scaladsl.Source
    import pekko.util.ByteString

    import scala.concurrent.Future

    def retrieveFromPath(path: String, settings: FtpSettings): Source[ByteString, Future[IOResult]] =
      Ftp.fromPath(path, settings)

    // #retrieving
  }

  object retrievingUnconfirmedReads {
    // #retrieving-with-unconfirmed-reads
    import org.apache.pekko
    import pekko.stream.IOResult
    import pekko.stream.connectors.ftp.scaladsl.Sftp
    import pekko.stream.scaladsl.Source
    import pekko.util.ByteString

    import scala.concurrent.Future

    def retrieveFromPath(path: String, settings: SftpSettings): Source[ByteString, Future[IOResult]] =
      Sftp.fromPath(path, settings.withMaxUnconfirmedReads(64))

    // #retrieving-with-unconfirmed-reads
  }

  object removing {
    // #removing
    import org.apache.pekko
    import pekko.stream.IOResult
    import pekko.stream.connectors.ftp.scaladsl.Ftp
    import pekko.stream.scaladsl.Sink

    import scala.concurrent.Future

    def remove(settings: FtpSettings): Sink[FtpFile, Future[IOResult]] =
      Ftp.remove(settings)
    // #removing
  }

  object move {
    // #moving
    import org.apache.pekko
    import pekko.stream.IOResult
    import pekko.stream.connectors.ftp.scaladsl.Ftp
    import pekko.stream.scaladsl.Sink

    import scala.concurrent.Future

    def move(destinationPath: FtpFile => String, settings: FtpSettings): Sink[FtpFile, Future[IOResult]] =
      Ftp.move(destinationPath, settings)
    // #moving
  }

  object mkdir {
    // #mkdir-source

    import org.apache.pekko
    import pekko.NotUsed
    import pekko.stream.scaladsl.Source
    import pekko.stream.connectors.ftp.scaladsl.Ftp
    import pekko.Done

    def mkdir(basePath: String, directoryName: String, settings: FtpSettings): Source[Done, NotUsed] =
      Ftp.mkdir(basePath, directoryName, settings)

    // #mkdir-source
  }

  object processAndMove {
    // #processAndMove
    import java.nio.file.Files

    import org.apache.pekko
    import pekko.NotUsed
    import pekko.stream.connectors.ftp.scaladsl.Ftp
    import pekko.stream.scaladsl.{ FileIO, RunnableGraph }

    def processAndMove(sourcePath: String,
        destinationPath: FtpFile => String,
        settings: FtpSettings): RunnableGraph[NotUsed] =
      Ftp
        .ls(sourcePath, settings)
        .flatMapConcat(ftpFile => Ftp.fromPath(ftpFile.path, settings).map((_, ftpFile)))
        .alsoTo(FileIO.toPath(Files.createTempFile("downloaded", "tmp")).contramap(_._1))
        .to(Ftp.move(destinationPath, settings).contramap(_._2))
    // #processAndMove
  }
}
