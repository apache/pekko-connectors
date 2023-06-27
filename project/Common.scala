/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import de.heikoseeberger.sbtheader._
import com.lightbend.paradox.projectinfo.ParadoxProjectInfoPluginKeys._
import com.lightbend.sbt.JavaFormatterPlugin.autoImport.javafmtOnCompile
import com.typesafe.tools.mima.plugin.MimaKeys._
import org.mdedetrich.apache.sonatype.ApacheSonatypePlugin
import sbtdynver.DynVerPlugin
import sbtdynver.DynVerPlugin.autoImport.dynverSonatypeSnapshots

object Common extends AutoPlugin {

  object autoImport {
    val fatalWarnings = settingKey[Boolean]("Warnings stop compilation with an error")
  }
  import autoImport._

  override def trigger = allRequirements

  override def requires = JvmPlugin && HeaderPlugin && ApacheSonatypePlugin && DynVerPlugin

  val isScala3 = Def.setting(scalaBinaryVersion.value == "3")

  override def globalSettings = Seq(
    scmInfo := Some(ScmInfo(url("https://github.com/apache/incubator-pekko-connectors"),
      "git@github.com:apache/incubator-pekko-connectors.git")),
    developers += Developer("contributors",
      "Contributors",
      "dev@pekko.apache.org",
      url("https://github.com/apache/incubator-pekko-connectors/graphs/contributors")),
    description := "Apache Pekko Connectors is a Reactive Enterprise Integration library for Java and Scala, based on Reactive Streams and Pekko.",
    fatalWarnings := true,
    mimaReportSignatureProblems := true,
    // Ignore unused keys which affect documentation
    excludeLintKeys ++= Set(scmInfo, projectInfoVersion, autoAPIMappings))

  val packagesToSkip = "org.apache.pekko.pattern:" + // for some reason Scaladoc creates this
    "org.mongodb.scala:" + // this one is a mystery as well
    // excluding generated grpc classes, except the model ones (com.google.pubsub)
    "com.google.api:com.google.cloud:com.google.iam:com.google.logging:" +
    "com.google.longrunning:com.google.protobuf:com.google.rpc:com.google.type"

  override lazy val projectSettings = Dependencies.CommonSettings ++ Seq(
    projectInfoVersion := (if (isSnapshot.value) "snapshot" else version.value),
    crossVersion := CrossVersion.binary,
    crossScalaVersions := Dependencies.ScalaVersions,
    scalaVersion := Dependencies.Scala213,
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Werror",
      "-Wconf:cat=unused-nowarn:s",
      "-release:8"),
    Compile / doc / scalacOptions := scalacOptions.value ++ Seq(
      "-doc-title",
      "Apache Pekko Connectors",
      "-doc-version",
      version.value,
      "-sourcepath",
      (ThisBuild / baseDirectory).value.toString),
    Compile / doc / scalacOptions := scalacOptions.value,
    Compile / doc / scalacOptions ++=
      Seq(
        "-doc-source-url", {
          val branch = if (isSnapshot.value) "main" else s"v${version.value}"
          s"https://github.com/apache/incubator-pekko-connectors/tree/${branch}€{FILE_PATH_EXT}#L€{FILE_LINE}"
        },
        "-doc-canonical-base-url",
        "https://pekko.apache.org/api/pekko-connectors/current/"),
    Compile / doc / scalacOptions ++= {
      if (isScala3.value) {
        Seq("-skip-packages:" + packagesToSkip)
      } else {
        Seq("-skip-packages", packagesToSkip)
      }
    },
    Compile / doc / scalacOptions -= "-Werror",
    compile / javacOptions ++= Seq(
      "-Xlint:cast",
      "-Xlint:deprecation",
      "-Xlint:dep-ann",
      "-Xlint:empty",
      "-Xlint:fallthrough",
      "-Xlint:finally",
      "-Xlint:overloads",
      "-Xlint:overrides",
      "-Xlint:rawtypes",
      // JDK 11 "-Xlint:removal",
      "-Xlint:static",
      "-Xlint:try",
      "-Xlint:unchecked",
      "-Xlint:varargs"),
    compile / javacOptions ++= (scalaVersion.value match {
      case Dependencies.Scala213 if insideCI.value && fatalWarnings.value && !Dependencies.CronBuild =>
        Seq("-Werror")
      case _ => Seq.empty[String]
    }),
    autoAPIMappings := true,
    apiURL := Some(url(
      s"https://pekko.apache.org/api/pekko-connectors/${version.value}/org/apache/pekko/stream/connectors/index.html")),
    // show full stack traces and test case durations
    Test / testOptions += Tests.Argument("-oDF"),
    // -a Show stack traces and exception class name for AssertionErrors.
    // -v Log "test run started" / "test started" / "test run finished" events on log level "info" instead of "debug".
    // -q Suppress stdout for successful tests.
    Test / testOptions += Tests.Argument(TestFrameworks.JUnit, "-a", "-v", "-q"),
    // By default scalatest futures time out in 150 ms, dilate that to 600ms.
    // This should not impact the total test time as we don't expect to hit this
    // timeout.
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-F", "4"),
    scalafmtOnCompile := false,
    javafmtOnCompile := false)

  override lazy val buildSettings = Seq(
    dynverSonatypeSnapshots := true)
}
