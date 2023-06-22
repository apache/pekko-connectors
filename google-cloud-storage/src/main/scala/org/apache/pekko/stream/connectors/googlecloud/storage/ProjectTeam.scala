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

package org.apache.pekko.stream.connectors.googlecloud.storage

final class ProjectTeam private (projectNumber: String, team: String) {
  def withProjectNumber(projectNumber: String): ProjectTeam =
    copy(projectNumber = projectNumber)
  def withTeam(team: String): ProjectTeam = copy(team = team)

  private def copy(projectNumber: String = projectNumber, team: String = team): ProjectTeam =
    new ProjectTeam(projectNumber, team)

  override def toString: String =
    s"ProjectTeam(projectNumber=$projectNumber, team=$team)"
}

object ProjectTeam {
  def apply(projectNumber: String, team: String): ProjectTeam =
    new ProjectTeam(projectNumber, team)

  def create(projectNumber: String, team: String): ProjectTeam =
    new ProjectTeam(projectNumber, team)
}
