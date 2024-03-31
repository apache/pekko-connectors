/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.4.7")
addSbtPlugin("com.github.sbt" % "sbt-dynver" % "5.0.1")
addSbtPlugin("net.bzzt" % "sbt-reproducible-builds" % "0.32")
addSbtPlugin("com.github.pjfanning" % "sbt-source-dist" % "0.1.12")
addSbtPlugin("com.github.pjfanning" % "sbt-pekko-build" % "0.3.3")
addSbtPlugin("com.github.sbt" % "sbt-license-report" % "1.6.1")
// discipline
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.10.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.3")
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % "0.8.0")

// docs
addSbtPlugin("org.apache.pekko" % "pekko-sbt-paradox" % "1.0.1")
addSbtPlugin(("com.github.sbt" % "sbt-site-paradox" % "1.6.0").excludeAll(
  "com.lightbend.paradox", "sbt-paradox"))

addSbtPlugin("com.github.sbt" % "sbt-unidoc" % "0.5.0")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings" % "3.0.2")
// Pekko gRPC -- sync with PekkoGrpcBinaryVersion in Dependencies.scala
addSbtPlugin("org.apache.pekko" % "pekko-grpc-sbt-plugin" % "1.0.2")
// templating
addSbtPlugin("com.github.sbt" % "sbt-boilerplate" % "0.7.0")
