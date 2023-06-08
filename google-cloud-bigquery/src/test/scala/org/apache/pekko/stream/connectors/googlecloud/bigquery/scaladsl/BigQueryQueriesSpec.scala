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

package org.apache.pekko.stream.connectors.googlecloud.bigquery.scaladsl

import _root_.spray.json._
import org.apache.pekko
import pekko.actor.ActorSystem
import pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import pekko.stream.connectors.google.auth.NoCredentials
import pekko.stream.connectors.google.{ GoogleAttributes, GoogleSettings }
import pekko.stream.connectors.googlecloud.bigquery.model.JobReference
import pekko.stream.connectors.googlecloud.bigquery.model.QueryResponse
import pekko.stream.connectors.googlecloud.bigquery.{ BigQueryEndpoints, HoverflySupport }
import pekko.stream.scaladsl.Sink
import pekko.testkit.TestKit
import io.specto.hoverfly.junit.core.SimulationSource.dsl
import io.specto.hoverfly.junit.dsl.HoverflyDsl.service
import io.specto.hoverfly.junit.dsl.ResponseCreators.success
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpecLike

class BigQueryQueriesSpec
    extends TestKit(ActorSystem("BigQueryQueriesSpec"))
    with AsyncWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with HoverflySupport
    with BigQueryRest
    with BigQueryQueries {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    super.afterAll()
  }

  implicit def queryResponseFormat[T: JsonFormat]: RootJsonFormat[QueryResponse[T]] = {
    import DefaultJsonProtocol._
    jsonFormat10(QueryResponse[T])
  }

  implicit val settings: GoogleSettings = GoogleSettings().copy(credentials = NoCredentials("", ""))

  val jobId = "jobId"
  val pageToken = "pageToken"

  val incompleteQuery = QueryResponse[JsValue](
    None,
    JobReference(Some(settings.projectId), Some(jobId), None),
    None,
    None,
    None,
    None,
    false,
    None,
    None,
    None)

  val completeQuery = incompleteQuery.copy[JsValue](
    jobComplete = true,
    rows = Some(JsString("firstPage") :: Nil))

  val completeQueryWith2ndPage = completeQuery.copy[JsValue](
    pageToken = Some(pageToken))

  val query2ndPage = completeQuery.copy[JsValue](
    rows = Some(JsString("secondPage") :: Nil))

  val completeQueryWithoutJobId = completeQuery.copy[JsValue](
    jobReference = JobReference(None, None, None))

  "BigQueryQueries" should {

    "get query results" when {
      import DefaultJsonProtocol._

      "completes immediately and has one page" in {

        hoverfly.reset()
        hoverfly.simulate(
          dsl(
            service(BigQueryEndpoints.queries(settings.projectId).authority.host.address())
              .post(BigQueryEndpoints.queries(settings.projectId).path.toString)
              .queryParam("prettyPrint", "false")
              .anyBody()
              .willReturn(success(completeQuery.toJson.compactPrint, "application/json"))))

        query[JsValue]("SQL")
          .addAttributes(GoogleAttributes.settings(settings))
          .runWith(Sink.seq[JsValue])
          .map(_ shouldEqual Seq(JsString("firstPage")))
      }

      "completes immediately and has two pages" in {

        hoverfly.reset()
        hoverfly.simulate(
          dsl(
            service(BigQueryEndpoints.queries(settings.projectId).authority.host.address())
              .post(BigQueryEndpoints.queries(settings.projectId).path.toString)
              .queryParam("prettyPrint", "false")
              .anyBody()
              .willReturn(success(completeQueryWith2ndPage.toJson.compactPrint, "application/json"))
              .get(BigQueryEndpoints.query(settings.projectId, jobId).path.toString)
              .queryParam("pageToken", pageToken)
              .queryParam("prettyPrint", "false")
              .willReturn(success(query2ndPage.toJson.toString, "application/json"))))

        query[JsValue]("SQL")
          .addAttributes(GoogleAttributes.settings(settings))
          .runWith(Sink.seq[JsValue])
          .map(_ shouldEqual Seq(JsString("firstPage"), JsString("secondPage")))
      }

      "completes on 2nd attempt and has one page" in {

        hoverfly.reset()
        hoverfly.simulate(
          dsl(
            service(BigQueryEndpoints.queries(settings.projectId).authority.host.address())
              .post(BigQueryEndpoints.queries(settings.projectId).path.toString)
              .queryParam("prettyPrint", "false")
              .anyBody()
              .willReturn(success(incompleteQuery.toJson.compactPrint, "application/json"))
              .get(BigQueryEndpoints.query(settings.projectId, jobId).path.toString)
              .queryParam("prettyPrint", "false")
              .willReturn(success(completeQuery.toJson.toString, "application/json"))))

        query[JsValue]("SQL")
          .addAttributes(GoogleAttributes.settings(settings))
          .runWith(Sink.seq[JsValue])
          .map(_ shouldEqual Seq(JsString("firstPage")))
      }

      "completes on 2nd attempt and has two pages" in {

        hoverfly.reset()
        hoverfly.simulate(
          dsl(
            service(BigQueryEndpoints.queries(settings.projectId).authority.host.address())
              .post(BigQueryEndpoints.queries(settings.projectId).path.toString)
              .queryParam("prettyPrint", "false")
              .anyBody()
              .willReturn(success(incompleteQuery.toJson.compactPrint, "application/json"))
              .get(BigQueryEndpoints.query(settings.projectId, jobId).path.toString)
              .queryParam("prettyPrint", "false")
              .willReturn(success(completeQueryWith2ndPage.toJson.toString, "application/json"))
              .get(BigQueryEndpoints.query(settings.projectId, jobId).path.toString)
              .queryParam("pageToken", pageToken)
              .queryParam("prettyPrint", "false")
              .willReturn(success(query2ndPage.toJson.toString, "application/json"))))

        query[JsValue]("SQL")
          .addAttributes(GoogleAttributes.settings(settings))
          .runWith(Sink.seq[JsValue])
          .map(_ shouldEqual Seq(JsString("firstPage"), JsString("secondPage")))
      }

      "completes immediately without job id" in {

        hoverfly.reset()
        hoverfly.simulate(
          dsl(
            service(BigQueryEndpoints.queries(settings.projectId).authority.host.address())
              .post(BigQueryEndpoints.queries(settings.projectId).path.toString)
              .queryParam("prettyPrint", "false")
              .anyBody()
              .willReturn(success(completeQueryWithoutJobId.toJson.compactPrint, "application/json"))))

        query[JsValue]("SQL")
          .addAttributes(GoogleAttributes.settings(settings))
          .runWith(Sink.seq[JsValue])
          .map(_ shouldEqual Seq(JsString("firstPage")))
      }

    }

    "fail" when {

      "parser is broken" in {

        class BrokenParserException extends Exception

        implicit object brokenFormat extends JsonFormat[JsValue] {
          override def write(obj: JsValue): JsValue = obj
          override def read(json: JsValue): JsValue = throw new BrokenParserException
        }

        hoverfly.reset()
        hoverfly.simulate(
          dsl(
            service(BigQueryEndpoints.queries(settings.projectId).authority.host.address())
              .post(BigQueryEndpoints.queries(settings.projectId).path.toString)
              .queryParam("prettyPrint", "false")
              .anyBody()
              .willReturn(success(completeQuery.toJson.compactPrint, "application/json"))))

        recoverToSucceededIf[BrokenParserException] {
          query[JsValue]("SQL")
            .addAttributes(GoogleAttributes.settings(settings))
            .runWith(Sink.seq[JsValue])
        }
      }
    }

  }

}
