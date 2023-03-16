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

package org.apache.pekko.stream.connectors.mongodb.scaladsl

import org.apache.pekko
import pekko.stream.scaladsl.{ Flow, Source }
import pekko.NotUsed
import pekko.annotation.InternalApi
import pekko.stream.connectors.mongodb.{ DocumentReplace, DocumentUpdate }
import com.mongodb.client.model.{ DeleteOptions, InsertManyOptions, InsertOneOptions, ReplaceOptions, UpdateOptions }
import com.mongodb.client.result.{ DeleteResult, UpdateResult }
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.conversions.Bson

import scala.collection.JavaConverters._

object MongoFlow {

  /** Internal Api */
  @InternalApi private[mongodb] val DefaultInsertOneOptions = new InsertOneOptions()

  /** Internal Api */
  @InternalApi private[mongodb] val DefaultInsertManyOptions = new InsertManyOptions()

  /** Internal Api */
  @InternalApi private[mongodb] val DefaultUpdateOptions = new UpdateOptions()

  /** Internal Api */
  @InternalApi private[mongodb] val DefaultDeleteOptions = new DeleteOptions()

  /** Internal Api */
  @InternalApi private[mongodb] val DefaultReplaceOptions = new ReplaceOptions()

  /**
   * A [[pekko.stream.scaladsl.Flow Flow]] that will insert documents into a collection.
   *
   * @param collection mongo db collection to insert to.
   * @param options options to apply to the operation
   */
  def insertOne[T](collection: MongoCollection[T],
      options: InsertOneOptions = DefaultInsertOneOptions): Flow[T, T, NotUsed] =
    Flow[T]
      .flatMapConcat(doc => Source.fromPublisher(collection.insertOne(doc, options)).map(_ => doc))

  /**
   * A [[pekko.stream.scaladsl.Flow Flow]] that will insert batches documents into a collection.
   *
   * @param collection mongo db collection to insert to.
   * @param options options to apply to the operation
   */
  def insertMany[T](collection: MongoCollection[T],
      options: InsertManyOptions = DefaultInsertManyOptions): Flow[Seq[T], Seq[T], NotUsed] =
    Flow[Seq[T]].flatMapConcat(docs => Source.fromPublisher(collection.insertMany(docs.asJava, options)).map(_ => docs))

  /**
   * A [[pekko.stream.scaladsl.Flow Flow]] that will update documents as defined by a [[DocumentUpdate]].
   *
   * @param collection the mongo db collection to update.
   * @param options options to apply to the operation
   */
  def updateOne[T](
      collection: MongoCollection[T],
      options: UpdateOptions = DefaultUpdateOptions): Flow[DocumentUpdate, (UpdateResult, DocumentUpdate), NotUsed] =
    Flow[DocumentUpdate].flatMapConcat(documentUpdate =>
      Source
        .fromPublisher(collection.updateOne(documentUpdate.filter, documentUpdate.update, options))
        .map(_ -> documentUpdate))

  /**
   * A [[pekko.stream.scaladsl.Flow Flow]] that will update many documents as defined by a [[DocumentUpdate]].
   *
   * @param collection the mongo db collection to update.
   * @param options options to apply to the operation
   */
  def updateMany[T](
      collection: MongoCollection[T],
      options: UpdateOptions = DefaultUpdateOptions): Flow[DocumentUpdate, (UpdateResult, DocumentUpdate), NotUsed] =
    Flow[DocumentUpdate].flatMapConcat(documentUpdate =>
      Source
        .fromPublisher(collection.updateMany(documentUpdate.filter, documentUpdate.update, options))
        .map(_ -> documentUpdate))

  /**
   * A [[pekko.stream.scaladsl.Flow Flow]] that will delete individual documents as defined by a [[org.bson.conversions.Bson Bson]] filter query.
   *
   * @param collection the mongo db collection to update.
   * @param options options to apply to the operation
   */
  def deleteOne[T](collection: MongoCollection[T],
      options: DeleteOptions = DefaultDeleteOptions): Flow[Bson, (DeleteResult, Bson), NotUsed] =
    Flow[Bson].flatMapConcat(bson => Source.fromPublisher(collection.deleteOne(bson, options)).map(_ -> bson))

  /**
   * A [[pekko.stream.scaladsl.Flow Flow]] that will delete many documents as defined by a [[org.bson.conversions.Bson Bson]] filter query.
   *
   * @param collection the mongo db collection to update.
   * @param options options to apply to the operation
   */
  def deleteMany[T](collection: MongoCollection[T],
      options: DeleteOptions = DefaultDeleteOptions): Flow[Bson, (DeleteResult, Bson), NotUsed] =
    Flow[Bson].flatMapConcat(bson => Source.fromPublisher(collection.deleteMany(bson, options)).map(_ -> bson))

  /**
   * A [[pekko.stream.scaladsl.Flow Flow]] that will replace document as defined by a [[DocumentReplace]].
   *
   * @param collection the mongo db collection to update.
   * @param options options to apply to the operation
   */
  def replaceOne[T](
      collection: MongoCollection[T],
      options: ReplaceOptions = DefaultReplaceOptions)
      : Flow[DocumentReplace[T], (UpdateResult, DocumentReplace[T]), NotUsed] =
    Flow[DocumentReplace[T]].flatMapConcat(documentReplace =>
      Source
        .fromPublisher(collection.replaceOne(documentReplace.filter, documentReplace.replacement, options))
        .map(_ -> documentReplace))
}
