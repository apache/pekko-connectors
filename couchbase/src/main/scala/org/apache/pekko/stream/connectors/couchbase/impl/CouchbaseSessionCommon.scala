package org.apache.pekko.stream.connectors.couchbase.impl

import com.couchbase.client.java.{AsyncBucket, AsyncCluster, AsyncCollection, AsyncScope, Collection};

trait CouchbaseSessionCommon {

  def underlying: AsyncCluster

  def bucket(bucketName: String): AsyncBucket

  def scope(bucketName: String): AsyncScope

  def scope(bucketName: String, scopeName: String): AsyncScope

  def collection(bucketName: String): AsyncCollection

  def collection(bucketName: String, collectionName: String): AsyncCollection

  def collection(bucketName: String, scopeName: String, collectionName: String): AsyncCollection
}
