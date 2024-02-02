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

import com.couchbase.client.java.{ AsyncCluster, AsyncCollection }
import com.couchbase.client.java.analytics.{ AnalyticsOptions, AnalyticsResult }
import com.couchbase.client.java.codec.TypeRef
import com.couchbase.client.java.kv._
import com.couchbase.client.java.manager.query.{ GetAllQueryIndexesOptions, QueryIndex }
import com.couchbase.client.java.query.{ QueryOptions, QueryResult }
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

object CouchbaseSource {

  def get(asyncCollection: AsyncCollection, id: String,
      options: GetOptions = GetOptions.getOptions): Source[GetResult, NotUsed] =
    Source
      .completionStage(asyncCollection.get(id, options))

  def get[T](asyncCollection: AsyncCollection, id: String, target: Class[T],
      options: GetOptions = GetOptions.getOptions): Source[T, NotUsed] =
    get(asyncCollection, id, options).map(_.contentAs(target))

  def get[T](asyncCollection: AsyncCollection, id: String, target: TypeRef[T],
      options: GetOptions = GetOptions.getOptions): Source[T, NotUsed] =
    get(asyncCollection, id, options).map(_.contentAs(target))

  def getAllReplicas(asyncCollection: AsyncCollection, id: String,
      options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions): Source[GetReplicaResult, NotUsed] =
    Source
      .completionStage(asyncCollection.getAllReplicas(id, options))
      .flatMapConcat { res =>
        Source.fromJavaStream(() => res.stream())
          .flatMapConcat(Source.completionStage)
      }

  def getAllReplicas[T](asyncCollection: AsyncCollection, id: String, target: Class[T],
      options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions): Source[T, NotUsed] =
    getAllReplicas(asyncCollection, id, options)
      .map(_.contentAs(target))

  def getAllReplicas[T](asyncCollection: AsyncCollection, id: String, target: TypeRef[T],
      options: GetAllReplicasOptions =
        GetAllReplicasOptions.getAllReplicasOptions): Source[T, NotUsed] =
    getAllReplicas(asyncCollection, id, options)
      .map(_.contentAs(target))

  def scan(asyncCollection: AsyncCollection, scanType: ScanType,
      options: ScanOptions = ScanOptions.scanOptions()): Source[ScanResult, NotUsed] =
    Source
      .completionStage(asyncCollection.scan(scanType, options))
      .flatMapConcat(res => Source.fromJavaStream(() => res.stream()))

  def scan[T](asyncCollection: AsyncCollection, scanType: ScanType, target: Class[T],
      options: ScanOptions = ScanOptions.scanOptions()): Source[T, NotUsed] =
    scan(asyncCollection, scanType, options)
      .map(_.contentAs(target))

  def scan[T](asyncCollection: AsyncCollection, scanType: ScanType, target: TypeRef[T],
      options: ScanOptions = ScanOptions.scanOptions()): Source[T, NotUsed] =
    scan(asyncCollection, scanType, options)
      .map(_.contentAs(target))

  def query(cluster: AsyncCluster, statement: String,
      options: QueryOptions): Source[QueryResult, NotUsed] = {
    Source
      .completionStage(cluster.query(statement, options))
  }

  def query[T](cluster: AsyncCluster, statement: String, target: Class[T],
      options: QueryOptions = QueryOptions.queryOptions()): Source[T, NotUsed] =
    query(cluster, statement, options)
      .flatMapConcat(res => Source.fromJavaStream(() => res.rowsAs(target).stream()))

  def query[T](cluster: AsyncCluster, statement: String, target: TypeRef[T],
      options: QueryOptions = QueryOptions.queryOptions()): Source[T, NotUsed] =
    query(cluster, statement, options)
      .flatMapConcat(res => Source.fromJavaStream(() => res.rowsAs(target).stream()))

  def analyticsQuery(
      cluster: AsyncCluster, statement: String, options: AnalyticsOptions): Source[AnalyticsResult, NotUsed] =
    Source
      .completionStage(cluster.analyticsQuery(statement, options))

  def analyticsQuery[T](cluster: AsyncCluster, statement: String, target: Class[T],
      options: AnalyticsOptions = AnalyticsOptions.analyticsOptions()): Source[T, NotUsed] =
    analyticsQuery(cluster, statement, options)
      .flatMapConcat(res => Source.fromJavaStream(() => res.rowsAs(target).stream()))

  def analyticsQuery[T](cluster: AsyncCluster, statement: String, target: TypeRef[T],
      options: AnalyticsOptions = AnalyticsOptions.analyticsOptions()): Source[T, NotUsed] =
    analyticsQuery(cluster, statement, options)
      .flatMapConcat(res => Source.fromJavaStream(() => res.rowsAs(target).stream()))

  def queryAllIndex(asyncCollection: AsyncCollection,
      options: GetAllQueryIndexesOptions = GetAllQueryIndexesOptions.getAllQueryIndexesOptions)
      : Source[QueryIndex, NotUsed] =
    Source
      .completionStage(asyncCollection.queryIndexes().getAllIndexes(options))
      .flatMapConcat(list => Source.fromJavaStream(() => list.stream()))
}
