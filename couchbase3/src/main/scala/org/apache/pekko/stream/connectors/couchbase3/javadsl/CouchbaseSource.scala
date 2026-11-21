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

package org.apache.pekko.stream.connectors.couchbase3.javadsl

import com.couchbase.client.java.{ AsyncCollection, AsyncScope }
import com.couchbase.client.java.analytics.{ AnalyticsOptions, AnalyticsResult }
import com.couchbase.client.java.codec.TypeRef
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv._
import com.couchbase.client.java.manager.query.{ GetAllQueryIndexesOptions, QueryIndex }
import com.couchbase.client.java.query.{ QueryOptions, QueryResult }
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.javadsl.Source
import org.apache.pekko.stream.connectors.couchbase3.scaladsl.{ CouchbaseSource => ScalaCoubaseSource }

object CouchbaseSource {

  /**
   * get a document by id from Couchbase collection
   * @param id document id
   * @param options reference to Couchbase options doc
   */
  def get(id: String, options: GetOptions,
      asyncCollection: AsyncCollection): Source[GetResult, NotUsed] =
    ScalaCoubaseSource.get(id, options)(asyncCollection).asJava

  /**
   * get a document by id from Couchbase collection
   * @param id document id
   *
   * @since 2.0.0
   */
  def get(id: String, asyncCollection: AsyncCollection): Source[GetResult, NotUsed] =
    get(id, GetOptions.getOptions, asyncCollection)

  /**
   * reference to `CouchbaseSource.get` deserialize to Couchbase JsonObject
   */
  def getJson(id: String, options: GetOptions,
      asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    ScalaCoubaseSource.getJson(id, options)(asyncCollection).asJava

  /**
   * reference to `CouchbaseSource.get` deserialize to Couchbase JsonObject
   *
   * @since 2.0.0
   */
  def getJson(id: String, asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    getJson(id, GetOptions.getOptions, asyncCollection)

  /**
   * reference to `CouchbaseSource.get`,deserialize to class
   */
  def getObject[T](id: String, target: Class[T], options: GetOptions,
      asyncCollection: AsyncCollection): Source[T, NotUsed] =
    ScalaCoubaseSource.getObject[T](id, target, options)(asyncCollection).asJava

  /**
   * reference to `CouchbaseSource.get`,deserialize to class
   *
   * @since 2.0.0
   */
  def getObject[T](id: String, target: Class[T], asyncCollection: AsyncCollection): Source[T, NotUsed] =
    getObject[T](id, target, GetOptions.getOptions, asyncCollection)

  /**
   * reference to `CouchbaseSource.getObject`,deserialize to class with Generics
   */
  def getType[T](id: String, target: TypeRef[T], options: GetOptions,
      asyncCollection: AsyncCollection): Source[T, NotUsed] =
    ScalaCoubaseSource.getType[T](id, target, options)(asyncCollection).asJava

  /**
   * reference to `CouchbaseSource.getObject`,deserialize to class with Generics
   *
   * @since 2.0.0
   */
  def getType[T](id: String, target: TypeRef[T], asyncCollection: AsyncCollection): Source[T, NotUsed] =
    getType[T](id, target, GetOptions.getOptions, asyncCollection)

  /**
   * similar to `CouchbaseSource.get` .Reads from replicas or the active node based on the options and returns the results as a list
   * @param options reference to Couchbase options doc
   * @see `CouchbaseSource#get`
   */
  def getAllReplicas(id: String, options: GetAllReplicasOptions,
      asyncCollection: AsyncCollection): Source[GetReplicaResult, NotUsed] =
    ScalaCoubaseSource.getAllReplicas(id, options)(asyncCollection).asJava

  /**
   * similar to `CouchbaseSource.get` .Reads from replicas or the active node based on the options and returns the results as a list
   * @see `CouchbaseSource#get`
   *
   * @since 2.0.0
   */
  def getAllReplicas(id: String, asyncCollection: AsyncCollection): Source[GetReplicaResult, NotUsed] =
    getAllReplicas(id, GetAllReplicasOptions.getAllReplicasOptions, asyncCollection)

  /**
   * reference to `CouchbaseSource.getAllReplicas`, deserialize to Couchbase JsonObject
   */
  def getAllReplicasJson(id: String, options: GetAllReplicasOptions,
      asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    ScalaCoubaseSource.getAllReplicasJson(id, options)(asyncCollection).asJava

  /**
   * reference to `CouchbaseSource.getAllReplicas`, deserialize to Couchbase JsonObject
   *
   * @since 2.0.0
   */
  def getAllReplicasJson(id: String, asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    getAllReplicasJson(id, GetAllReplicasOptions.getAllReplicasOptions, asyncCollection)

  /**
   * reference to `CouchbaseSource.getAllReplicas`, deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def getAllReplicasObject[T](id: String, target: Class[T], options: GetAllReplicasOptions,
      asyncCollection: AsyncCollection): Source[T, NotUsed] =
    ScalaCoubaseSource.getAllReplicasObject[T](id, target, options)(asyncCollection).asJava

  /**
   * reference to `CouchbaseSource.getAllReplicas`, deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   *
   * @since 2.0.0
   */
  def getAllReplicasObject[T](id: String, target: Class[T], asyncCollection: AsyncCollection): Source[T, NotUsed] =
    getAllReplicasObject[T](id, target, GetAllReplicasOptions.getAllReplicasOptions, asyncCollection)

  /**
   * reference to `CouchbaseSource.getAllReplicasObject`, deserialize to class with Generics
   */
  def getAllReplicasType[T](id: String, target: TypeRef[T], options: GetAllReplicasOptions,
      asyncCollection: AsyncCollection): Source[T, NotUsed] =
    ScalaCoubaseSource.getAllReplicasType[T](id, target, options)(asyncCollection).asJava

  /**
   * reference to `CouchbaseSource.getAllReplicasObject`, deserialize to class with Generics
   *
   * @since 2.0.0
   */
  def getAllReplicasType[T](id: String, target: TypeRef[T], asyncCollection: AsyncCollection): Source[T, NotUsed] =
    getAllReplicasType[T](id, target, GetAllReplicasOptions.getAllReplicasOptions, asyncCollection)

  /**
   * similar to Get`CouchbaseSource.get`, batch get documents from collection by ScanType`ScanType`
   */
  def scan(scanType: ScanType, options: ScanOptions,
      asyncCollection: AsyncCollection): Source[ScanResult, NotUsed] =
    ScalaCoubaseSource.scan(scanType, options)(asyncCollection).asJava

  /**
   * similar to Get`CouchbaseSource.get`, batch get documents from collection by ScanType`ScanType`
   *
   * @since 2.0.0
   */
  def scan(scanType: ScanType, asyncCollection: AsyncCollection): Source[ScanResult, NotUsed] =
    scan(scanType, ScanOptions.scanOptions(), asyncCollection)

  /**
   * reference to `CouchbaseSource.scan`, deserialize to Couchbase JsonObject
   */
  def scanJson(scanType: ScanType, options: ScanOptions,
      asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    ScalaCoubaseSource.scanJson(scanType, options)(asyncCollection).asJava

  /**
   * reference to `CouchbaseSource.scan`, deserialize to Couchbase JsonObject
   *
   * @since 2.0.0
   */
  def scanJson(scanType: ScanType, asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    scanJson(scanType, ScanOptions.scanOptions(), asyncCollection)

  /**
   * reference to `CouchbaseSource.scan`, deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   */
  def scanObject[T](scanType: ScanType, target: Class[T], options: ScanOptions,
      asyncCollection: AsyncCollection): Source[T, NotUsed] =
    ScalaCoubaseSource.scanObject[T](scanType, target, options)(asyncCollection).asJava

  /**
   * reference to `CouchbaseSource.scan`, deserialize to class
   * If you add DefaultScalaModule to jackson of couchbase, it could deserialize to scala class
   *
   * @since 2.0.0
   */
  def scanObject[T](scanType: ScanType, target: Class[T], asyncCollection: AsyncCollection): Source[T, NotUsed] =
    scanObject[T](scanType, target, ScanOptions.scanOptions(), asyncCollection)

  /**
   * reference to `CouchbaseSource.scan`, `CouchbaseSource.scanObject`, deserialize to class with Generics
   */
  def scanType[T](scanType: ScanType, target: TypeRef[T], options: ScanOptions,
      asyncCollection: AsyncCollection): Source[T, NotUsed] =
    ScalaCoubaseSource.scanType[T](scanType, target, options)(asyncCollection).asJava

  /**
   * reference to `CouchbaseSource.scan`, `CouchbaseSource.scanObject`, deserialize to class with Generics
   *
   * @since 2.0.0
   */
  def scanType[T](scanType: ScanType, target: TypeRef[T], asyncCollection: AsyncCollection): Source[T, NotUsed] =
    CouchbaseSource.scanType[T](scanType, target, ScanOptions.scanOptions(), asyncCollection)

  /**
   * N1QL query in a Scope.
   *
   * QueryResult contains List<Row>, every Row is a json like
   * <p>
   * <pre>
   *   {
   *     "collectionName": Document
   *   }
   * </pre>
   * </p>
   */
  def query(statement: String, options: QueryOptions,
      scope: AsyncScope): Source[QueryResult, NotUsed] =
    ScalaCoubaseSource.query(statement, options)(scope).asJava

  /**
   * N1QL query in a Scope.
   *
   * QueryResult contains List<Row>, every Row is a json like
   * <p>
   * <pre>
   *   {
   *     "collectionName": Document
   *   }
   * </pre>
   * </p>
   *
   * @since 2.0.0
   */
  def query(statement: String, scope: AsyncScope): Source[QueryResult, NotUsed] =
    query(statement, QueryOptions.queryOptions(), scope)

  /**
   * N1QL query in a Scope.
   * @param statement N1QL query sql
   * @see `CouchbaseSource.query`
   */
  def queryJson(statement: String, options: QueryOptions,
      scope: AsyncScope): Source[JsonObject, NotUsed] =
    ScalaCoubaseSource.queryJson(statement, options)(scope).asJava

  /**
   * N1QL query in a Scope.
   * @param statement N1QL query sql
   * @see `CouchbaseSource.query`
   *
   * @since 2.0.0
   */
  def queryJson(statement: String, scope: AsyncScope): Source[JsonObject, NotUsed] =
    queryJson(statement, QueryOptions.queryOptions(), scope)

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
   *  https://www.couchbase.com/products/editions/server/
   * </p>
   *
   * @param statement Analytics query sql
   */
  def analyticsQuery(statement: String, options: AnalyticsOptions,
      scope: AsyncScope): Source[AnalyticsResult, NotUsed] =
    ScalaCoubaseSource.analyticsQuery(statement, options)(scope).asJava

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
   *  https://www.couchbase.com/products/editions/server/
   * </p>
   *
   * @param statement Analytics query sql
   *
   * @since 2.0.0
   */
  def analyticsQuery(statement: String, scope: AsyncScope): Source[AnalyticsResult, NotUsed] =
    analyticsQuery(statement, AnalyticsOptions.analyticsOptions(), scope)

  /**
   * <p>
   *  Warning: couchbase-community not support analyticsQuery, we not test this API
   * </p>
   *
   * Performs an Analytics query and convert document row to jsonObject <br>
   * different with analyticsQuery, jsonObject not has the collection Key, but is Document directly<br>
   * @see `CouchbaseSource.analyticsQuery`
   */
  def analyticsQueryJson(statement: String, options: AnalyticsOptions,
      scope: AsyncScope): Source[JsonObject, NotUsed] =
    ScalaCoubaseSource.analyticsQueryJson(statement, options)(scope).asJava

  /**
   * <p>
   *  Warning: couchbase-community not support analyticsQuery, we not test this API
   * </p>
   *
   * Performs an Analytics query and convert document row to jsonObject <br>
   * different with analyticsQuery, jsonObject not has the collection Key, but is Document directly<br>
   * @see `CouchbaseSource.analyticsQuery`
   *
   * @since 2.0.0
   */
  def analyticsQueryJson(statement: String, scope: AsyncScope): Source[JsonObject, NotUsed] =
    analyticsQueryJson(statement, AnalyticsOptions.analyticsOptions(), scope)

  /**
   * Fetches all indexes from this collection with custom options.
   * @see `AsyncCollectionQueryIndexManager#getAllIndexes`
   */
  def queryAllIndex(options: GetAllQueryIndexesOptions,
      asyncCollection: AsyncCollection): Source[QueryIndex, NotUsed] =
    ScalaCoubaseSource.queryAllIndex(options)(asyncCollection).asJava

  /**
   * Fetches all indexes from this collection with custom options.
   * @see `AsyncCollectionQueryIndexManager#getAllIndexes`
   *
   * @since 2.0.0
   */
  def queryAllIndex(asyncCollection: AsyncCollection): Source[QueryIndex, NotUsed] =
    queryAllIndex(GetAllQueryIndexesOptions.getAllQueryIndexesOptions, asyncCollection)
}
