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

import com.couchbase.client.java.{ AsyncCluster, AsyncCollection }
import com.couchbase.client.java.analytics.{ AnalyticsOptions, AnalyticsResult }
import com.couchbase.client.java.codec.TypeRef
import com.couchbase.client.java.kv._
import com.couchbase.client.java.manager.query.{ GetAllQueryIndexesOptions, QueryIndex }
import com.couchbase.client.java.query.{ QueryOptions, QueryResult }
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.connectors.couchbase3.scaladsl
import org.apache.pekko.stream.javadsl.Source

object CouchbaseSource {

  def get(collection: AsyncCollection, id: String,
      options: GetOptions = GetOptions.getOptions): Source[GetResult, NotUsed] =
    scaladsl.CouchbaseSource.get(collection, id, options).asJava

  def get[T](collection: AsyncCollection, id: String, target: Class[T],
      options: GetOptions = GetOptions.getOptions): Source[T, NotUsed] =
    scaladsl.CouchbaseSource.get(collection, id, target, options).asJava

  def get[T](collection: AsyncCollection, id: String, target: TypeRef[T],
      options: GetOptions = GetOptions.getOptions): Source[T, NotUsed] =
    scaladsl.CouchbaseSource.get(collection, id, target, options).asJava

  def getAllReplicas(collection: AsyncCollection, id: String,
      options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions): Source[GetReplicaResult, NotUsed] =
    scaladsl.CouchbaseSource.getAllReplicas(collection, id, options).asJava

  def getAllReplicas[T](collection: AsyncCollection, id: String, target: Class[T],
      options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions): Source[T, NotUsed] =
    scaladsl.CouchbaseSource.getAllReplicas(collection, id, target, options).asJava

  def getAllReplicas[T](collection: AsyncCollection, id: String, target: TypeRef[T],
      options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions): Source[T, NotUsed] =
    scaladsl.CouchbaseSource.getAllReplicas(collection, id, target, options).asJava

  def scan(collection: AsyncCollection, scanType: ScanType,
      options: ScanOptions = ScanOptions.scanOptions()): Source[ScanResult, NotUsed] =
    scaladsl.CouchbaseSource.scan(collection, scanType, options).asJava

  def scan[T](collection: AsyncCollection, scanType: ScanType, target: Class[T],
      options: ScanOptions = ScanOptions.scanOptions()): Source[T, NotUsed] =
    scaladsl.CouchbaseSource.scan(collection, scanType, target, options).asJava

  def scan[T](collection: AsyncCollection, scanType: ScanType, target: TypeRef[T],
      options: ScanOptions = ScanOptions.scanOptions()): Source[T, NotUsed] =
    scaladsl.CouchbaseSource.scan(collection, scanType, target, options).asJava

  def query(cluster: AsyncCluster, statement: String,
      options: QueryOptions): Source[QueryResult, NotUsed] =
    scaladsl.CouchbaseSource.query(cluster, statement, options).asJava

  def query[T](cluster: AsyncCluster, statement: String, target: Class[T],
      options: QueryOptions = QueryOptions.queryOptions()): Source[T, NotUsed] =
    query(cluster, statement, options)
      .flatMapConcat(res => Source.from(res.rowsAs(target)))

  def query[T](cluster: AsyncCluster, statement: String, target: TypeRef[T],
      options: QueryOptions = QueryOptions.queryOptions()): Source[T, NotUsed] =
    query(cluster, statement, options)
      .flatMapConcat(res => Source.from(res.rowsAs(target)))

  def analyticsQuery(cluster: AsyncCluster, statement: String,
      options: AnalyticsOptions = AnalyticsOptions.analyticsOptions()): Source[AnalyticsResult, NotUsed] =
    Source
      .completionStage(cluster.analyticsQuery(statement, options))

  def analyticsQuery[T](cluster: AsyncCluster, statement: String, target: Class[T],
      options: AnalyticsOptions = AnalyticsOptions.analyticsOptions()): Source[T, NotUsed] =
    analyticsQuery(cluster, statement, options)
      .flatMapConcat(res => Source.from(res.rowsAs(target)))

  def analyticsQuery[T](cluster: AsyncCluster, statement: String, target: TypeRef[T],
      options: AnalyticsOptions = AnalyticsOptions.analyticsOptions()): Source[T, NotUsed] =
    analyticsQuery(cluster, statement, options)
      .flatMapConcat(res => Source.from(res.rowsAs(target)))

  def queryAllIndex(collection: AsyncCollection,
      options: GetAllQueryIndexesOptions = GetAllQueryIndexesOptions.getAllQueryIndexesOptions)
      : Source[QueryIndex, NotUsed] =
    Source
      .completionStage(collection.queryIndexes().getAllIndexes(options))
      .flatMapConcat(Source.from)
}
