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

package org.apache.pekko.stream.connectors.mongodb.javadsl

import org.apache.pekko
import pekko.NotUsed
import pekko.stream.connectors.mongodb.{ scaladsl, DocumentReplace, DocumentUpdate }
import pekko.stream.connectors.mongodb.scaladsl.MongoFlow.{
  DefaultDeleteOptions,
  DefaultInsertManyOptions,
  DefaultInsertOneOptions,
  DefaultReplaceOptions,
  DefaultUpdateOptions
}
import pekko.stream.javadsl.Flow
import com.mongodb.client.model.{ DeleteOptions, InsertManyOptions, InsertOneOptions, ReplaceOptions, UpdateOptions }
import com.mongodb.client.result.{ DeleteResult, UpdateResult }
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.conversions.Bson

import scala.collection.JavaConverters._

object MongoFlow {

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will insert documents into a collection.
   *
   * @param collection mongo db collection to insert to.
   */
  def insertOne[T](collection: MongoCollection[T]): Flow[T, T, NotUsed] =
    insertOne(collection, DefaultInsertOneOptions)

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will insert documents into a collection.
   *
   * @param collection mongo db collection to insert to.
   * @param options options to apply to the operation
   */
  def insertOne[T](collection: MongoCollection[T], options: InsertOneOptions): Flow[T, T, NotUsed] =
    scaladsl.MongoFlow.insertOne(collection, options).asJava

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will insert batches of documents into a collection.
   *
   * @param collection mongo db collection to insert to.
   */
  def insertMany[T](collection: MongoCollection[T]): Flow[java.util.List[T], java.util.List[T], NotUsed] =
    insertMany(collection, DefaultInsertManyOptions)

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will insert batches of documents into a collection.
   *
   * @param collection mongo db collection to insert to.
   * @param options options to apply to the operation
   */
  def insertMany[T](collection: MongoCollection[T],
      options: InsertManyOptions): Flow[java.util.List[T], java.util.List[T], NotUsed] =
    pekko.stream.scaladsl
      .Flow[java.util.List[T]]
      .map(_.asScala.toIndexedSeq)
      .via(scaladsl.MongoFlow.insertMany(collection, options))
      .map(_.asJava)
      .asJava

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will update documents as defined by a [[DocumentUpdate]].
   *
   * @param collection the mongo db collection to update.
   */
  def updateOne[T](
      collection: MongoCollection[T]): Flow[DocumentUpdate, pekko.japi.Pair[UpdateResult, DocumentUpdate], NotUsed] =
    updateOne(collection, DefaultUpdateOptions)

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will update documents as defined by a [[DocumentUpdate]].
   *
   * @param collection the mongo db collection to update.
   * @param options options to apply to the operation
   */
  def updateOne[T](
      collection: MongoCollection[T],
      options: UpdateOptions): Flow[DocumentUpdate, pekko.japi.Pair[UpdateResult, DocumentUpdate], NotUsed] =
    scaladsl.MongoFlow.updateOne(collection, options).map(fromTupleToPair).asJava

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will update many documents as defined by a [[DocumentUpdate]].
   *
   * @param collection the mongo db collection to update.
   */
  def updateMany[T](
      collection: MongoCollection[T]): Flow[DocumentUpdate, pekko.japi.Pair[UpdateResult, DocumentUpdate], NotUsed] =
    updateMany(collection, DefaultUpdateOptions)

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will update many documents as defined by a [[DocumentUpdate]].
   *
   * @param collection the mongo db collection to update.
   * @param options options to apply to the operation
   */
  def updateMany[T](
      collection: MongoCollection[T],
      options: UpdateOptions = DefaultUpdateOptions)
      : Flow[DocumentUpdate, pekko.japi.Pair[UpdateResult, DocumentUpdate], NotUsed] =
    scaladsl.MongoFlow.updateMany(collection, options).map(fromTupleToPair).asJava

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will delete individual documents as defined by a [[org.bson.conversions.Bson Bson]] filter query.
   *
   * @param collection the mongo db collection to update.
   */
  def deleteOne[T](
      collection: MongoCollection[T]): Flow[Bson, pekko.japi.Pair[DeleteResult, Bson], NotUsed] =
    deleteOne(collection, DefaultDeleteOptions)

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will delete individual documents as defined by a [[org.bson.conversions.Bson Bson]] filter query.
   *
   * @param collection the mongo db collection to update.
   * @param options options to apply to the operation
   */
  def deleteOne[T](collection: MongoCollection[T],
      options: DeleteOptions): Flow[Bson, pekko.japi.Pair[DeleteResult, Bson], NotUsed] =
    scaladsl.MongoFlow.deleteOne(collection, options).map(fromTupleToPair).asJava

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will delete many documents as defined by a [[org.bson.conversions.Bson Bson]] filter query.
   *
   * @param collection the mongo db collection to update.
   */
  def deleteMany[T](
      collection: MongoCollection[T]): Flow[Bson, pekko.japi.Pair[DeleteResult, Bson], NotUsed] =
    deleteMany(collection, DefaultDeleteOptions)

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will delete many documents as defined by a [[org.bson.conversions.Bson Bson]] filter query.
   *
   * @param collection the mongo db collection to update.
   * @param options options to apply to the operation
   */
  def deleteMany[T](collection: MongoCollection[T],
      options: DeleteOptions): Flow[Bson, pekko.japi.Pair[DeleteResult, Bson], NotUsed] =
    scaladsl.MongoFlow.deleteMany(collection, options).map(fromTupleToPair).asJava

  /**
   * A [[pekko.stream.javadsl.Flow Flow]] that will replace document as defined by a [[DocumentReplace]].
   *
   * @param collection the mongo db collection to update.
   * @param options options to apply to the operation
   */
  def replaceOne[T](
      collection: MongoCollection[T],
      options: ReplaceOptions = DefaultReplaceOptions)
      : Flow[DocumentReplace[T], pekko.japi.Pair[UpdateResult, DocumentReplace[T]], NotUsed] =
    scaladsl.MongoFlow.replaceOne(collection, options).map(fromTupleToPair).asJava

  private def fromTupleToPair[T, R] = (pekko.japi.Pair.create[T, R] _).tupled
}
