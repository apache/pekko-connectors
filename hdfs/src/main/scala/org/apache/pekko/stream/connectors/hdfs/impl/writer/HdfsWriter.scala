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

package org.apache.pekko.stream.connectors.hdfs.impl.writer

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.hdfs.FilePathGenerator
import pekko.stream.connectors.hdfs.impl.writer.HdfsWriter._
import org.apache.hadoop.fs.{ FileSystem, Path }

/**
 * Internal API
 */
@InternalApi
private[hdfs] trait HdfsWriter[W, I] {

  // `outputOpened` records whether the lazy `output` was forced, so `close`
  // can skip it rather than creating a file only to close it.
  private var outputOpened = false

  protected lazy val output: W = {
    outputOpened = true
    create(fs, temp)
  }

  /** Whether `output` has been forced. */
  protected def isOutputOpened: Boolean = outputOpened

  protected lazy val temp: Path = tempFromTarget(pathGenerator, target)

  def moveToTarget(): Boolean = {
    if (!fs.exists(target.getParent))
      fs.mkdirs(target.getParent)
    // mimics FileContext#rename(temp, target, Options.Rename.Overwrite) semantics
    if (overwrite) fs.delete(target, false)
    fs.rename(temp, target)
  }

  def sync(): Unit

  def targetPath: String = target.toString

  def write(input: I, separator: Option[Array[Byte]]): Long

  def rotate(rotationCount: Long): HdfsWriter[W, I]

  /**
   * Release the underlying output without moving anything to the target.
   *
   * Called when the stage stops without completing normally, where `rotate` is
   * never reached and the output would otherwise stay open on an HDFS lease.
   * Must be safe to call when nothing was ever written: `output` is lazy, so
   * implementations have to avoid forcing it.
   */
  def close(): Unit

  protected def target: Path

  protected def fs: FileSystem

  protected def overwrite: Boolean

  protected def pathGenerator: FilePathGenerator

  protected def create(fs: FileSystem, file: Path): W

}

/**
 * Internal API
 */
@InternalApi
private[writer] object HdfsWriter {

  def createTargetPath(generator: FilePathGenerator, c: Long): Path =
    generator(c, System.currentTimeMillis / 1000)

  def tempFromTarget(generator: FilePathGenerator, target: Path): Path =
    new Path(generator.tempDirectory, target.getName)

  def getOrCreatePath(maybePath: Option[Path], default: => Path): Path =
    maybePath.getOrElse(default)

}
