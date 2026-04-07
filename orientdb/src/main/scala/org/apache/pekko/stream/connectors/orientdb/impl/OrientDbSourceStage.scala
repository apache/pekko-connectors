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
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal
import com.orientechnologies.orient.core.db.ODatabaseSession

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

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
              override protected def runQuery(): util.List[T] = {
                val rs = client.query(q)
                val results = newArrayListWithSize(rs.estimateSize())
                try {
                  while (rs.hasNext) results.add(rs.next().toElement.asInstanceOf[T])
                } finally rs.close()
                results
              }
            }
          case None =>
            new Logic {
              override protected def runQuery(): util.List[T] = {
                val rs = client.query(s"SELECT * FROM $className SKIP ${skip} LIMIT ${settings.limit}")
                val results = newArrayListWithSize(rs.estimateSize())
                try {
                  while (rs.hasNext) results.add(rs.next().toElement.asInstanceOf[T])
                } finally rs.close()
                results
              }
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
                val rs = oObjectClient.query(q)
                val results = newArrayListWithSize(rs.estimateSize())
                try {
                  while (rs.hasNext) {
                    rs.next().getRecord().toScala.foreach { record =>
                      results.add(oObjectClient.getUserObjectByRecord(record, null).asInstanceOf[T])
                    }
                  }
                } finally rs.close()
                results
              }
            }
          case None =>
            new Logic {
              override def preStart(): Unit = {
                super.preStart()
                oObjectClient.getEntityManager.registerEntityClass(c)
              }

              override protected def runQuery(): util.List[T] = {
                val rs =
                  oObjectClient.query(s"SELECT * FROM $className SKIP ${skip} LIMIT ${settings.limit}")
                val results = newArrayListWithSize(rs.estimateSize())
                try {
                  while (rs.hasNext) {
                    rs.next().getRecord().toScala.foreach { record =>
                      results.add(oObjectClient.getUserObjectByRecord(record, null).asInstanceOf[T])
                    }
                  }
                } finally rs.close()
                results
              }
            }
        }

    }

  // if the size is larger than Int.MaxValue, we will just create an ArrayList with a default
  // size of 1000 and let it grow as needed - we assume that estimateSize() is just a hint
  private def newArrayListWithSize(size: Long): util.ArrayList[T] =
    if (size > Int.MaxValue) new util.ArrayList[T](1000)
    else new util.ArrayList[T](math.max(size.toInt, 0))

  private abstract class Logic extends GraphStageLogic(shape) with OutHandler {

    protected var client: ODatabaseSession = _
    protected var oObjectClient: OObjectDatabaseTx = _
    protected var skip = settings.skip

    override def preStart(): Unit = {
      client = settings.oDatabasePool.acquire()
      oObjectClient = new OObjectDatabaseTx(client.asInstanceOf[ODatabaseDocumentInternal])
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
