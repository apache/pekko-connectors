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

import com.couchbase.client.java.{AsyncCollection, AsyncScope}
import com.couchbase.client.java.analytics.{AnalyticsOptions, AnalyticsResult}
import com.couchbase.client.java.codec.TypeRef
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv._
import com.couchbase.client.java.manager.query.{GetAllQueryIndexesOptions, QueryIndex}
import com.couchbase.client.java.query.{QueryOptions, QueryResult}
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

object CouchbaseSource {

  def get(id: String, options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection) =
    Source.completionStage(asyncCollection.get(id, options))

  def getJson(id: String, options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    get(id, options).map(_.contentAsObject())

  def getObject[T](id: String, target: Class[T], options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    get(id, options).map(_.contentAs(target))

  def getType[T](id: String, target: TypeRef[T], options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] = {
    get(id, options).map(_.contentAs(target))
  }

  def getAllReplicas(id: String, options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Source[GetReplicaResult, NotUsed] =
    Source
      .completionStage(asyncCollection.getAllReplicas(id, options))
      .flatMapConcat { res =>
        Source.fromJavaStream(() => res.stream())
          .flatMapConcat(Source.completionStage)
      }

  def getAllReplicasObject[T](id: String, target: Class[T],
      options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    getAllReplicas(id, options)
      .map(_.contentAs(target))

  def getAllReplicasType[T](id: String, target: TypeRef[T],
      options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    getAllReplicas(id, options)
      .map(_.contentAs(target))

  def scan(scanType: ScanType, options: ScanOptions = ScanOptions.scanOptions())(
      implicit asyncCollection: AsyncCollection): Source[ScanResult, NotUsed] =
    Source
      .completionStage(asyncCollection.scan(scanType, options))
      .flatMapConcat(res => Source.fromJavaStream(() => res.stream()))

  def scanJson(scanType: ScanType, options: ScanOptions = ScanOptions.scanOptions())(
      implicit asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    scan(scanType, options)
      .map(_.contentAsObject())

  def scanObject[T](scanType: ScanType, target: Class[T],
      options: ScanOptions = ScanOptions.scanOptions())(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    scan(scanType, options)
      .map(_.contentAs(target))

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
   * @see
   */
  def queryJson(
      statement: String,
      options: QueryOptions = QueryOptions.queryOptions())(
      implicit scope: AsyncScope): Source[JsonObject, NotUsed] = {
    query(statement, options)
      .flatMapConcat { res =>
        Source.fromJavaStream(() =>
          res.rowsAsObject().stream.flatMap { json =>
            json.getNames.stream().map(collection => json.getObject(collection))
          })
      }
  }

  /**
   * Performs an Analytics query , QueryResult contains List<Row>
   * <P>
   * val rows: List[JsonObject] = QueryResult.rowsAsObject, every Row is a json with CollectionName key
   * <pre>
   *    {
   *      "collectionName": Document
   *    }
   * </pre>
   *
   * @param statement Analytics query sql
   * warning: couchbase-community not support analyticsQuery,we not test this api
   */
  def analyticsQuery(statement: String,
      options: AnalyticsOptions = AnalyticsOptions.analyticsOptions())(
      implicit scope: AsyncScope): Source[AnalyticsResult, NotUsed] =
    Source
      .completionStage(scope.analyticsQuery(statement, options))

  /**
   * Performs an Analytics query and convert document row to jsonObject
   * warning: different with analyticsQuery, jsonObject not has the collection Key
   * warning: couchbase-community not support analyticsQuery,we not test this api
   * @see [[org.apache.pekko.stream.connectors.couchbase3.scaladsl.CouchbaseSource#analyticsQuery]]
   */
  def analyticsQueryJson(statement: String,
      options: AnalyticsOptions = AnalyticsOptions.analyticsOptions())(
      implicit scope: AsyncScope): Source[JsonObject, NotUsed] =
    analyticsQuery(statement, options)
      .flatMapConcat { res =>
        Source.fromJavaStream(() =>
          res.rowsAsObject().stream().flatMap { json =>
            json.getNames.stream().map(collection => json.getObject(collection))
          })
      }

  def queryAllIndex(options: GetAllQueryIndexesOptions = GetAllQueryIndexesOptions.getAllQueryIndexesOptions)(
      implicit asyncCollection: AsyncCollection): Source[QueryIndex, NotUsed] =
    Source
      .completionStage(asyncCollection.queryIndexes().getAllIndexes(options))
      .flatMapConcat(list => Source.fromJavaStream(() => list.stream()))
}
