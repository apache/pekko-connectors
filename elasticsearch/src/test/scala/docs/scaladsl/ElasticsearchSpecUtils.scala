/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.scaladsl

import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.HttpExt
import pekko.http.scaladsl.model.Uri.Path
import pekko.http.scaladsl.model.{ ContentTypes, HttpMethods, HttpRequest, Uri }
import pekko.stream.connectors.elasticsearch.scaladsl.ElasticsearchSource
import pekko.stream.connectors.elasticsearch.{
  ApiVersionBase,
  ElasticsearchConnectionSettings,
  ElasticsearchParams,
  OpensearchApiVersion,
  OpensearchParams,
  SourceSettingsBase
}
import pekko.stream.scaladsl.Sink
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.immutable
import scala.concurrent.Future

trait ElasticsearchSpecUtils { this: AnyWordSpec with ScalaFutures =>
  implicit def system: ActorSystem

  def http: HttpExt

  // #define-class
  import spray.json._
  import DefaultJsonProtocol._

  case class Book(title: String, shouldSkip: Option[Boolean] = None, price: Int = 10)

  implicit val format: JsonFormat[Book] = jsonFormat3(Book)
  // #define-class

  def register(connectionSettings: ElasticsearchConnectionSettings,
      indexName: String,
      title: String,
      price: Int): Unit = {
    val request = HttpRequest(HttpMethods.POST)
      .withUri(Uri(connectionSettings.baseUrl).withPath(Path(s"/$indexName/_doc")))
      .withEntity(ContentTypes.`application/json`, s"""{"title": "$title", "price": $price}""")
    http.singleRequest(request).futureValue
  }

  def flushAndRefresh(connectionSettings: ElasticsearchConnectionSettings, indexName: String): Unit = {
    val flushRequest = HttpRequest(HttpMethods.POST)
      .withUri(Uri(connectionSettings.baseUrl).withPath(Path(s"/$indexName/_flush")))
    http.singleRequest(flushRequest).futureValue

    val refreshRequest = HttpRequest(HttpMethods.POST)
      .withUri(Uri(connectionSettings.baseUrl).withPath(Path(s"/$indexName/_refresh")))
    http.singleRequest(refreshRequest).futureValue
  }

  def readTitlesFrom(apiVersion: ApiVersionBase,
      sourceSettings: SourceSettingsBase[_, _],
      indexName: String): Future[immutable.Seq[String]] =
    ElasticsearchSource
      .typed[Book](
        constructElasticsearchParams(indexName, "_doc", apiVersion),
        query = """{"match_all": {}}""",
        settings = sourceSettings)
      .map { message =>
        message.source.title
      }
      .runWith(Sink.seq)

  def insertTestData(connectionSettings: ElasticsearchConnectionSettings): Unit = {
    register(connectionSettings, "source", "Akka in Action", 10)
    register(connectionSettings, "source", "Programming in Scala", 20)
    register(connectionSettings, "source", "Learning Scala", 10)
    register(connectionSettings, "source", "Scala for Spark in Production", 5)
    register(connectionSettings, "source", "Scala Puzzlers", 10)
    register(connectionSettings, "source", "Effective Akka", 10)
    register(connectionSettings, "source", "Akka Concurrency", 10)
    flushAndRefresh(connectionSettings, "source")
  }

  def constructElasticsearchParams(indexName: String,
      typeName: String,
      apiVersion: ApiVersionBase): ElasticsearchParams = {
    if (apiVersion == pekko.stream.connectors.elasticsearch.ApiVersion.V5) {
      ElasticsearchParams.V5(indexName, typeName)
    } else if (apiVersion == pekko.stream.connectors.elasticsearch.ApiVersion.V7) {
      ElasticsearchParams.V7(indexName)
    } else if (apiVersion == OpensearchApiVersion.V1) {
      OpensearchParams.V1(indexName)
    } else {
      throw new IllegalArgumentException(s"API version $apiVersion is not supported")
    }
  }
}
