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

package org.apache.pekko.stream.connectors.couchbase.impl

import com.couchbase.client.java.{ AsyncBucket, AsyncCluster, AsyncCollection, AsyncScope };

trait CouchbaseSessionCommon {

  def underlying: AsyncCluster

  def bucket(bucketName: String): AsyncBucket

  def scope(bucketName: String): AsyncScope

  def scope(bucketName: String, scopeName: String): AsyncScope

  def collection(bucketName: String): AsyncCollection

  def collection(bucketName: String, collectionName: String): AsyncCollection

  def collection(bucketName: String, scopeName: String, collectionName: String): AsyncCollection
}
