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

package akka.stream.alpakka.azure.storagequeue

sealed abstract class DeleteOrUpdateMessage
object DeleteOrUpdateMessage {

  sealed abstract class Delete extends DeleteOrUpdateMessage
  case object Delete extends Delete

  /**
   * Java API
   */
  def createDelete(): Delete = Delete

  final class UpdateVisibility private (val timeout: Int) extends DeleteOrUpdateMessage {
    override def toString: String =
      s"DeleteOrUpdateMessage.UpdateVisibility(timeout=$timeout)"
  }

  object UpdateVisibility {
    def apply(timeout: Int) =
      new UpdateVisibility(timeout)
  }

  /**
   * Java API
   */
  def createUpdateVisibility(timeout: Int): UpdateVisibility = UpdateVisibility(timeout)
}
