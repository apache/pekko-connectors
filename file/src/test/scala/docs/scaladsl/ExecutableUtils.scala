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

import java.io.{ BufferedInputStream, File => JavaFile, InputStream, OutputStream }
import java.nio.file.{ Files, Path, Paths }

import org.apache.pekko.util.ByteString

import scala.annotation.nowarn
import scala.concurrent.Future
import scala.sys.process.{ BasicIO, Process }

object ExecutableUtils {

  def isOnPath(bin: String): Boolean = {
    val paths =
      System.getenv("PATH").split(JavaFile.pathSeparator).map(_.trim).filter(_.nonEmpty).map(p => Paths.get(p)).toList
    paths.exists(path => Files.isExecutable(path.resolve(bin)))
  }

  def run(bin: String, args: Seq[String], cwd: Path, input: ByteString = ByteString.empty): Future[ByteString] = {
    Future {
      val proc = Process(Seq(bin) ++ args, cwd.toFile)
      var stdout = Option.empty[ByteString]
      var stderr = Option.empty[ByteString]
      val io = BasicIO
        .standard(true)
        .withInput { stream =>
          writeStream(stream, input)
        }
        .withOutput { stream =>
          stdout = Some(readStream(stream))
        }
        .withError { stream =>
          stderr = Some(readStream(stream))
        }
      proc.run(io).exitValue() match {
        case 0    => stdout.get
        case code => throw new RuntimeException(s"Subprocess exited with code $code\n\n${stderr.get.utf8String}")
      }
    }(scala.concurrent.ExecutionContext.Implicits.global)
  }

  private def writeStream(stream: OutputStream, content: ByteString): Unit = {
    try stream.write(content.toArray)
    finally stream.close()
  }

  @nowarn("msg=deprecated")
  private def readStream(stream: InputStream): ByteString = {
    val reader = new BufferedInputStream(stream)
    try ByteString(Stream.continually(reader.read).takeWhile(_ != -1).map(_.toByte).toArray)
    finally reader.close()
  }

}
