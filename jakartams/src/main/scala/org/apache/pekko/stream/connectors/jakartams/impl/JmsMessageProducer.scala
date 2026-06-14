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

package org.apache.pekko.stream.connectors.jakartams.impl

import jakarta.jms
import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.stream.connectors.jakartams._

/**
 * Internal API.
 */
@InternalApi
private class JmsMessageProducer(jmsProducer: jms.MessageProducer, jmsSession: JmsProducerSession, val epoch: Int) {

  private val defaultDestination = jmsSession.jmsDestination

  private val destinationCache = new SoftReferenceCache[Destination, jms.Destination]()

  def send(elem: JmsEnvelope[?]): Unit = {
    val message: jms.Message = createMessage(elem)
    populateMessageProperties(message, elem)

    val (sendHeaders, headersBeforeSend: Set[JmsHeader]) = elem.headers.partition(_.usedDuringSend)
    populateMessageHeader(message, headersBeforeSend)

    val deliveryMode = sendHeaders
      .collectFirst { case x: JmsDeliveryMode => x.deliveryMode }
      .getOrElse(jmsProducer.getDeliveryMode)

    val priority = sendHeaders
      .collectFirst { case x: JmsPriority => x.priority }
      .getOrElse(jmsProducer.getPriority)

    val timeToLive = sendHeaders
      .collectFirst { case x: JmsTimeToLive => x.timeInMillis }
      .getOrElse(jmsProducer.getTimeToLive)

    val destination = elem.destination match {
      case Some(messageDestination) => lookup(messageDestination)
      case None                     => defaultDestination
    }
    jmsProducer.send(destination, message, deliveryMode, priority, timeToLive)
  }

  private def lookup(dest: Destination) = destinationCache.lookup(dest, dest.create(jmsSession.session))

  private[jakartams] def createMessage(element: JmsEnvelope[?]): jms.Message =
    element match {

      case textMessage: JmsTextMessagePassThrough[?] => jmsSession.session.createTextMessage(textMessage.body)

      case byteMessage: JmsByteMessagePassThrough[?] =>
        val newMessage = jmsSession.session.createBytesMessage()
        newMessage.writeBytes(byteMessage.bytes)
        newMessage

      case byteStringMessage: JmsByteStringMessagePassThrough[?] =>
        val newMessage = jmsSession.session.createBytesMessage()
        newMessage.writeBytes(byteStringMessage.bytes.toArray)
        newMessage

      case mapMessage: JmsMapMessagePassThrough[?] =>
        val newMessage = jmsSession.session.createMapMessage()
        populateMapMessage(newMessage, mapMessage)
        newMessage

      case objectMessage: JmsObjectMessagePassThrough[?] =>
        jmsSession.session.createObjectMessage(objectMessage.serializable)

      case pt: JmsPassThrough[?] => throw new IllegalArgumentException("can't create message for JmsPassThrough")

    }

  private[jakartams] def populateMessageProperties(message: jakarta.jms.Message, jmsMessage: JmsEnvelope[?]): Unit =
    jmsMessage.properties.foreach {
      case (key, v) =>
        v match {
          case v: String      => message.setStringProperty(key, v)
          case v: Int         => message.setIntProperty(key, v)
          case v: Boolean     => message.setBooleanProperty(key, v)
          case v: Byte        => message.setByteProperty(key, v)
          case v: Short       => message.setShortProperty(key, v)
          case v: Float       => message.setFloatProperty(key, v)
          case v: Long        => message.setLongProperty(key, v)
          case v: Double      => message.setDoubleProperty(key, v)
          case v: Array[Byte] => message.setObjectProperty(key, v)
          case null           => message.setObjectProperty(key, null)
          case _              => throw UnsupportedMessagePropertyType(key, v, jmsMessage)
        }
    }

  private def populateMapMessage(message: jakarta.jms.MapMessage, jmsMessage: JmsMapMessagePassThrough[?]): Unit =
    jmsMessage.body.foreach {
      case (key, v) =>
        v match {
          case v: String      => message.setString(key, v)
          case v: Int         => message.setInt(key, v)
          case v: Boolean     => message.setBoolean(key, v)
          case v: Byte        => message.setByte(key, v)
          case v: Short       => message.setShort(key, v)
          case v: Float       => message.setFloat(key, v)
          case v: Long        => message.setLong(key, v)
          case v: Double      => message.setDouble(key, v)
          case v: Array[Byte] => message.setBytes(key, v)
          case null           => message.setObject(key, v)
          case _              => throw UnsupportedMapMessageEntryType(key, v, jmsMessage)
        }
    }

  private def populateMessageHeader(message: jakarta.jms.Message, headers: Set[JmsHeader]): Unit =
    headers.foreach {
      case JmsType(jmsType)                   => message.setJMSType(jmsType)
      case JmsReplyTo(destination)            => message.setJMSReplyTo(destination.create(jmsSession.session))
      case JmsCorrelationId(jmsCorrelationId) => message.setJMSCorrelationID(jmsCorrelationId)
      case JmsExpiration(jmsExpiration)       => message.setJMSExpiration(jmsExpiration)
      case JmsDeliveryMode(_) | JmsPriority(_) | JmsTimeToLive(_) | JmsTimestamp(_) | JmsRedelivered(_) |
          JmsMessageId(_) => // see #send(JmsMessage)
    }
}

/**
 * Internal API.
 */
@InternalApi
private[impl] object JmsMessageProducer {
  def apply(jmsSession: JmsProducerSession, settings: JmsProducerSettings, epoch: Int): JmsMessageProducer = {
    val producer = jmsSession.session.createProducer(null)
    if (settings.timeToLive.nonEmpty) {
      producer.setTimeToLive(settings.timeToLive.get.toMillis)
    }
    new JmsMessageProducer(producer, jmsSession, epoch)
  }
}
