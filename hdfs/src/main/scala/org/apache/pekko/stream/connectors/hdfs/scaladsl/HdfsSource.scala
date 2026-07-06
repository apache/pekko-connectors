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

package org.apache.pekko.stream.connectors.hdfs.scaladsl

import java.lang.invoke.{ MethodHandle, MethodHandles, MethodType }
import java.util.concurrent.ConcurrentHashMap

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.ActorAttributes.IODispatcher
import pekko.stream.scaladsl.{ Source, StreamConverters }
import pekko.stream.{ Attributes, IOResult }
import pekko.util.ByteString
import org.apache.hadoop.fs.{ FileSystem, Path }
import org.apache.hadoop.io.compress.CompressionCodec
import org.apache.hadoop.io.{ SequenceFile, Writable }

import scala.concurrent.Future

object HdfsSource {

  private val constructorCache = new ConcurrentHashMap[Class[?], MethodHandle]()
  private val LOOKUP = MethodHandles.lookup()

  private def getNoArgConstructor[T](clazz: Class[T]): MethodHandle = {
    var handle = constructorCache.get(clazz)
    if (handle == null) {
      val lookup = MethodHandles.privateLookupIn(clazz, LOOKUP)
      handle = lookup.findConstructor(clazz, MethodType.methodType(Void.TYPE))
      val existing = constructorCache.putIfAbsent(clazz, handle)
      if (existing != null) handle = existing
    }
    handle
  }

  /**
   * Scala API: creates a `Source` that consumes as `ByteString`
   *
   * @param fs Hadoop file system
   * @param path the file to open
   * @param chunkSize the size of each read operation, defaults to 8192
   */
  def data(
      fs: FileSystem,
      path: Path,
      chunkSize: Int = 8192): Source[ByteString, Future[IOResult]] =
    StreamConverters.fromInputStream(() => fs.open(path), chunkSize)

  /**
   * Scala API: creates a `Source` that consumes as `ByteString`
   *
   * @param fs Hadoop file system
   * @param path the file to open
   * @param codec a streaming compression/decompression pair
   * @param chunkSize the size of each read operation, defaults to 8192
   */
  def compressed(
      fs: FileSystem,
      path: Path,
      codec: CompressionCodec,
      chunkSize: Int = 8192): Source[ByteString, Future[IOResult]] =
    StreamConverters.fromInputStream(() => codec.createInputStream(fs.open(path)), chunkSize)

  /**
   * Scala API: creates a `Source` that consumes as `(K, V)`
   *
   * @param fs Hadoop file system
   * @param path the file to open
   * @param classK a key class
   * @param classV a value class
   */
  def sequence[K <: Writable, V <: Writable](
      fs: FileSystem,
      path: Path,
      classK: Class[K],
      classV: Class[V]): Source[(K, V), NotUsed] = {
    val reader: SequenceFile.Reader = new SequenceFile.Reader(fs.getConf, SequenceFile.Reader.file(path))
    val keyHandle = getNoArgConstructor(classK)
    val valueHandle = getNoArgConstructor(classV)
    val it = Iterator
      .continually {
        val key = keyHandle.invokeWithArguments().asInstanceOf[K]
        val value = valueHandle.invokeWithArguments().asInstanceOf[V]
        val hasCurrent = reader.next(key, value)
        (hasCurrent, (key, value))
      }
      .takeWhile(_._1)
      .map(_._2)
    Source
      .fromIterator(() => it)
      .addAttributes(Attributes(IODispatcher))
  }

}
