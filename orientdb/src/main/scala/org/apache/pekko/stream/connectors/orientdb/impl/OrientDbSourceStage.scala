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

package org.apache.pekko.stream.connectors.orientdb.impl

import java.util

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.orientdb.{ OrientDbReadResult, OrientDbSourceSettings }
import pekko.stream.stage.{ GraphStage, GraphStageLogic, OutHandler }
import pekko.stream.{ ActorAttributes, Attributes, Outlet, SourceShape }
import com.orientechnologies.orient.`object`.db.OObjectDatabaseTx
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery

import scala.jdk.CollectionConverters._

/**
 * INTERNAL API
 */
@InternalApi
private[orientdb] final class OrientDbSourceStage[T](className: String,
    query: Option[String],
    settings: OrientDbSourceSettings,
    clazz: Option[Class[T]] = None)
    extends GraphStage[SourceShape[OrientDbReadResult[T]]] {

  val out: Outlet[OrientDbReadResult[T]] = Outlet("OrientDBSource.out")
  override val shape = SourceShape(out)
  override def initialAttributes: Attributes =
    // see https://orientdb.com/docs/last/Java-Multi-Threading.html
    super.initialAttributes.and(ActorAttributes.Dispatcher("pekko.connectors.orientdb.pinned-dispatcher"))

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    clazz match {
      case None =>
        query match {
          case Some(q) =>
            new Logic {
              override protected def runQuery(): util.List[T] =
                client.query[util.List[T]](new OSQLSynchQuery[T](q))

            }
          case None =>
            new Logic {
              override protected def runQuery(): util.List[T] =
                client.query[util.List[T]](
                  new OSQLSynchQuery[T](s"SELECT * FROM $className SKIP ${skip} LIMIT ${settings.limit}"))
            }
        }

      case Some(c) =>
        query match {
          case Some(q) =>
            new Logic {
              override def preStart(): Unit = {
                super.preStart()
                oObjectClient.getEntityManager.registerEntityClass(c)
              }

              override protected def runQuery(): util.List[T] = {
                client.setDatabaseOwner(oObjectClient)
                oObjectClient.getEntityManager.registerEntityClass(c)
                oObjectClient.query[util.List[T]](new OSQLSynchQuery[T](q))
              }
            }
          case None =>
            new Logic {
              override def preStart(): Unit = {
                super.preStart()
                oObjectClient.getEntityManager.registerEntityClass(c)
              }

              override protected def runQuery(): util.List[T] =
                oObjectClient
                  .query[util.List[T]](
                    new OSQLSynchQuery[T](
                      s"SELECT * FROM $className SKIP ${skip} LIMIT ${settings.limit}"))
            }
        }

    }

  private abstract class Logic extends GraphStageLogic(shape) with OutHandler {

    protected var client: ODatabaseDocumentTx = _
    protected var oObjectClient: OObjectDatabaseTx = _
    protected var skip = settings.skip

    override def preStart(): Unit = {
      client = settings.oDatabasePool.acquire()
      oObjectClient = new OObjectDatabaseTx(client)
      client.setDatabaseOwner(oObjectClient)
    }

    override def postStop(): Unit =
      if (client != null) {
        if (oObjectClient != null) oObjectClient.close()
        client.close()
      }

    setHandler(out, this)

    override def onPull(): Unit = {
      val data = runQuery().asScala.toList
      if (data.isEmpty)
        completeStage()
      else {
        skip += settings.limit
        emitMultiple(out, data.map(OrientDbReadResult(_)).iterator)
      }
    }

    protected def runQuery(): java.util.List[T]
  }

}
