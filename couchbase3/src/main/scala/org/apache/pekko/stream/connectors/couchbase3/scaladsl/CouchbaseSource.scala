/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.couchbase3.scaladsl

import com.couchbase.client.java.{ AsyncCollection, AsyncScope }
import com.couchbase.client.java.analytics.{ AnalyticsOptions, AnalyticsResult }
import com.couchbase.client.java.codec.TypeRef
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv._
import com.couchbase.client.java.manager.query.{ GetAllQueryIndexesOptions, QueryIndex }
import com.couchbase.client.java.query.{ QueryOptions, QueryResult }
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

object CouchbaseSource {

  /**
   * get a document by id from Couchbase collection
   * @param id document id
   * @param options reference to Couchbase options doc
   */
  def get(id: String, options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Source[GetResult, NotUsed] =
    Source.completionStage(asyncCollection.get(id, options))

  /**
   * reference to [[CouchbaseSource.get]] deserialize to Couchbase JsonObject
   */
  def getJson(id: String, options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    get(id, options).map(_.contentAsObject())

  /**
   * reference to [[CouchbaseSource.get]],deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def getObject[T](id: String, target: Class[T], options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    get(id, options).map(_.contentAs(target))

  /**
   * reference to [[CouchbaseSource.getObject]],deserialize to class with Generics
   */
  def getType[T](id: String, target: TypeRef[T], options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] = {
    get(id, options).map(_.contentAs(target))
  }

  /**
   * same to Get option, but reads from all replicas on the active node
   * @param options reference to Couchbase options doc
   * @see [[CouchbaseSource#get]]
   */
  def getAllReplicas(id: String, options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Source[GetReplicaResult, NotUsed] =
    Source
      .completionStage(asyncCollection.getAllReplicas(id, options))
      .flatMapConcat { res =>
        Source.fromJavaStream(() => res.stream())
          .flatMapConcat(Source.completionStage)
      }

  /**
   * reference to [[CouchbaseSource.getAllReplicas]], deserialize to Couchbase JsonObject
   */
  def getAllReplicasJson(id: String, options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    getAllReplicas(id, options)
      .map(_.contentAsObject())

  /**
   * reference to [[CouchbaseSource.getAllReplicas]], deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def getAllReplicasObject[T](id: String, target: Class[T],
      options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    getAllReplicas(id, options)
      .map(_.contentAs(target))

  /**
   * reference to [[CouchbaseSource.getAllReplicasObject]], deserialize to class with Generics
   */
  def getAllReplicasType[T](id: String, target: TypeRef[T],
      options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    getAllReplicas(id, options)
      .map(_.contentAs(target))

  /**
   * like Get[[CouchbaseSource.get]], batch get documents from collection by ScanType[[ScanType]]
   */
  def scan(scanType: ScanType, options: ScanOptions = ScanOptions.scanOptions())(
      implicit asyncCollection: AsyncCollection): Source[ScanResult, NotUsed] =
    Source
      .completionStage(asyncCollection.scan(scanType, options))
      .flatMapConcat(res => Source.fromJavaStream(() => res.stream()))

  /**
   * reference to [[CouchbaseSource.scan]], deserialize to Couchbase JsonObject
   */
  def scanJson(scanType: ScanType, options: ScanOptions = ScanOptions.scanOptions())(
      implicit asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    scan(scanType, options)
      .map(_.contentAsObject())

  /**
   * reference to [[CouchbaseSource.scan]], deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def scanObject[T](scanType: ScanType, target: Class[T],
      options: ScanOptions = ScanOptions.scanOptions())(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    scan(scanType, options)
      .map(_.contentAs(target))

  /**
   * reference to [[CouchbaseSource.scan]], [[CouchbaseSource.scanObject]], deserialize to class with Generics
   */
  def scanType[T](scanType: ScanType, target: TypeRef[T],
      options: ScanOptions = ScanOptions.scanOptions())(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    scan(scanType, options)
      .map(_.contentAs(target))

  /**
   * N1QL query in a Scope.
   *
   * QueryResult contains List<Row>, every Row is a json like
   * <pre>
   *   {
   *     "collectionName": Document
   *   }
   * </pre>
   */
  def query(statement: String, options: QueryOptions = QueryOptions.queryOptions())(
      implicit scope: AsyncScope): Source[QueryResult, NotUsed] = {
    Source
      .completionStage(scope.query(statement, options))
  }

  /**
   * N1QL query in a Scope.
   * @param statement N1QL query sql
   * @see [[CouchbaseSource.query]]
   */
  def queryJson(
      statement: String,
      options: QueryOptions = QueryOptions.queryOptions())(
      implicit scope: AsyncScope): Source[JsonObject, NotUsed] = {
    query(statement, options)
      .flatMapConcat { res =>
        Source.fromJavaStream(() =>
          res.rowsAsObject().stream.flatMap[JsonObject] { json =>
            json.getNames.stream().map(collection => json.getObject(collection))
          })
      }
  }

  /**
   * Performs an Analytics query , QueryResult contains List<Row>
   * <p>
   * val rows: List[JsonObject] = QueryResult.rowsAsObject, every Row is a json with CollectionName key
   * <pre>
   *    {
   *      "collectionName": Document
   *    }
   * </pre>
   * </p>
   * <p>
   *  Warning: couchbase-community not support analyticsQuery, we not test this API
   * </p>
   *
   * @param statement Analytics query sql
   */
  def analyticsQuery(statement: String,
      options: AnalyticsOptions = AnalyticsOptions.analyticsOptions())(
      implicit scope: AsyncScope): Source[AnalyticsResult, NotUsed] =
    Source
      .completionStage(scope.analyticsQuery(statement, options))

  /**
   * <p>
   *  Warning: couchbase-community not support analyticsQuery, we not test this API
   * </p>
   *
   * Performs an Analytics query and convert document row to jsonObject <br>
   * different with analyticsQuery, jsonObject not has the collection Key, but is Document directly<br>
   * @see [[CouchbaseSource.analyticsQuery]]
   */
  def analyticsQueryJson(statement: String,
      options: AnalyticsOptions = AnalyticsOptions.analyticsOptions())(
      implicit scope: AsyncScope): Source[JsonObject, NotUsed] =
    analyticsQuery(statement, options)
      .flatMapConcat { res =>
        Source.fromJavaStream(() =>
          res.rowsAsObject().stream().flatMap[JsonObject] { json =>
            json.getNames.stream().map(collection => json.getObject(collection))
          })
      }

  /**
   * Fetches all indexes from this collection with custom options.
   * @see [[com.couchbase.client.java.manager.query.AsyncCollectionQueryIndexManager#getAllIndexes]]
   */
  def queryAllIndex(options: GetAllQueryIndexesOptions = GetAllQueryIndexesOptions.getAllQueryIndexesOptions)(
      implicit asyncCollection: AsyncCollection): Source[QueryIndex, NotUsed] =
    Source
      .completionStage(asyncCollection.queryIndexes().getAllIndexes(options))
      .flatMapConcat(list => Source.fromJavaStream(() => list.stream()))
}
