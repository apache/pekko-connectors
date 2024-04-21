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
import com.couchbase.client.java.json.JsonObject
import com.couchbase.client.java.kv._
import com.couchbase.client.java.manager.query.{ GetAllQueryIndexesOptions, QueryIndex }
import com.couchbase.client.java.query.{ QueryOptions, QueryResult }
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source

object CouchbaseSource {

  def get(id: String, options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection) =
    Source.completionStage(asyncCollection.get(id, options))

  def getJson(id: String, options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Source[JsonObject, NotUsed] =
    get(id, options).map(_.contentAsObject())

  def getClass[T](id: String, target: Class[T], options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    Source.completionStage(asyncCollection.get(id, options)).map(_.contentAs(target))

  def getType[T](id: String, target: TypeRef[T], options: GetOptions = GetOptions.getOptions)(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    Source
      .completionStage(asyncCollection.get(id, options)).map(_.contentAs(target))

  def getAllReplicas(id: String, options: GetAllReplicasOptions = GetAllReplicasOptions.getAllReplicasOptions)(
      implicit asyncCollection: AsyncCollection): Source[GetReplicaResult, NotUsed] =
    Source
      .completionStage(asyncCollection.getAllReplicas(id, options))
      .flatMapConcat { res =>
        Source.fromJavaStream(() => res.stream())
          .flatMapConcat(Source.completionStage)
      }

  def getAllReplicasClass[T](id: String, target: Class[T],
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

  def scanClass[T](scanType: ScanType, target: Class[T],
      options: ScanOptions = ScanOptions.scanOptions())(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    scan(scanType, options)
      .map(_.contentAs(target))

  def scanType[T](scanType: ScanType, target: TypeRef[T],
      options: ScanOptions = ScanOptions.scanOptions())(
      implicit asyncCollection: AsyncCollection): Source[T, NotUsed] =
    scan(scanType, options)
      .map(_.contentAs(target))

  def query(statement: String,
      options: QueryOptions = QueryOptions.queryOptions())(
      implicit cluster: AsyncCluster): Source[QueryResult, NotUsed] = {
    Source
      .completionStage(cluster.query(statement, options))
  }

  def queryClass[T](statement: String, target: Class[T], options: QueryOptions = QueryOptions.queryOptions())(
      implicit cluster: AsyncCluster): Source[T, NotUsed] =
    query(statement, options)
      .flatMapConcat(res => Source.fromJavaStream(() => res.rowsAs(target).stream()))

  def queryType[T](statement: String, target: TypeRef[T],
      options: QueryOptions = QueryOptions.queryOptions())(
      implicit cluster: AsyncCluster): Source[T, NotUsed] =
    query(statement, options)
      .flatMapConcat(res => Source.fromJavaStream(() => res.rowsAs(target).stream()))

  def analyticsQuery(statement: String,
      options: AnalyticsOptions = AnalyticsOptions.analyticsOptions())(
      implicit cluster: AsyncCluster): Source[AnalyticsResult, NotUsed] =
    Source
      .completionStage(cluster.analyticsQuery(statement, options))

  def analyticsQueryClass[T](statement: String, target: Class[T], options: AnalyticsOptions)(
      implicit cluster: AsyncCluster): Source[T, NotUsed] =
    analyticsQuery(statement, options)
      .flatMapConcat(res => Source.fromJavaStream(() => res.rowsAs(target).stream()))

  def analyticsQueryType[T](statement: String, target: TypeRef[T],
      options: AnalyticsOptions)(implicit cluster: AsyncCluster): Source[T, NotUsed] =
    analyticsQuery(statement, options)
      .flatMapConcat(res => Source.fromJavaStream(() => res.rowsAs(target).stream()))

  def queryAllIndex(options: GetAllQueryIndexesOptions = GetAllQueryIndexesOptions.getAllQueryIndexesOptions)(
      implicit asyncCollection: AsyncCollection): Source[QueryIndex, NotUsed] =
    Source
      .completionStage(asyncCollection.queryIndexes().getAllIndexes(options))
      .flatMapConcat(list => Source.fromJavaStream(() => list.stream()))
}
