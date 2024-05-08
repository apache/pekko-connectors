/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import net.bzzt.reproduciblebuilds.ReproducibleBuildsPlugin.reproducibleBuildsCheckResolver

sourceDistName := "apache-pekko-connectors"
sourceDistIncubating := false
ThisBuild / resolvers += Resolver.ApacheMavenSnapshotsRepo

ThisBuild / reproducibleBuildsCheckResolver := Resolver.ApacheMavenStagingRepo

lazy val userProjects: Seq[ProjectReference] = List[ProjectReference](
  amqp,
  avroparquet,
  awsSpiPekkoHttp,
  awslambda,
  azureStorageQueue,
  cassandra,
  couchbase,
  couchbase3,
  csv,
  dynamodb,
  elasticsearch,
  eventbridge,
  files,
  ftp,
  geode,
  googleCommon,
  googleCloudBigQuery,
  googleCloudBigQueryStorage,
  googleCloudPubSub,
  googleCloudPubSubGrpc,
  googleCloudStorage,
  googleFcm,
  hbase,
  hdfs,
  huaweiPushKit,
  influxdb,
  ironmq,
  jms,
  jsonStreaming,
  kinesis,
  kudu,
  mongodb,
  mqtt,
  mqttStreaming,
  orientdb,
  pravega,
  reference,
  s3,
  springWeb,
  simpleCodecs,
  slick,
  sns,
  solr,
  sqs,
  sse,
  text,
  udp,
  unixdomainsocket,
  xml)

lazy val `pekko-connectors` = project
  .in(file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .disablePlugins(MimaPlugin, SitePlugin)
  .aggregate(userProjects: _*)
  .aggregate(`doc-examples`, billOfMaterials)
  .settings(
    name := "pekko-connectors-root",
    onLoadMessage :=
      """
        |** Welcome to the sbt build definition for Pekko Connectors! **
        |
        |Useful sbt tasks:
        |
        |  docs/previewSite - builds Paradox and Scaladoc documentation,
        |    starts a webserver and opens a new browser window
        |
        |  test - runs all the tests for all of the connectors.
        |    Make sure to run `docker-compose up` first.
        |
        |  mqtt/testOnly *.MqttSourceSpec - runs a single test
        |
        |  mimaReportBinaryIssues - checks whether this current API
        |    is binary compatible with the released version
        |
        |  checkCodeStyle - checks that the codebase follows code
        |    style
        |
        |  applyCodeStyle - applies code style to the codebase
      """.stripMargin,
    // unidoc combines sources and jars from all connectors and that
    // might include some incompatible ones. Depending on the
    // classpath order that might lead to scaladoc compilation errors.
    // Therefore some versions are excluded here.
    ScalaUnidoc / unidoc / fullClasspath := {
      (ScalaUnidoc / unidoc / fullClasspath).value
        .filterNot(_.data.getAbsolutePath.contains("protobuf-java-2.5.0.jar"))
        .filterNot(_.data.getAbsolutePath.contains("guava-28.1-android.jar"))
        .filterNot(_.data.getAbsolutePath.contains("commons-net-3.1.jar"))
        .filterNot(_.data.getAbsolutePath.contains("protobuf-java-2.6.1.jar"))
    },
    ScalaUnidoc / unidoc / unidocProjectFilter := inAnyProject
    -- inProjects(
      `doc-examples`,
      csvBench,
      mqttStreamingBench,
      // googleCloudPubSubGrpc and googleCloudBigQueryStorage contain the same gRPC generated classes
      // don't include ScalaDocs for googleCloudBigQueryStorage to make it work
      googleCloudBigQueryStorage,
      // springWeb triggers an esoteric ScalaDoc bug (from Java code)
      springWeb),
    licenses := List(License.Apache2),
    crossScalaVersions := List() // workaround for https://github.com/sbt/sbt/issues/3465
  )

addCommandAlias("applyCodeStyle", ";scalafmtAll; scalafmtSbt; javafmtAll; +headerCreateAll")
addCommandAlias("checkCodeStyle", ";+headerCheckAll; scalafmtCheckAll; scalafmtSbtCheck; javafmtCheckAll")

lazy val amqp = pekkoConnectorProject("amqp", "amqp", Dependencies.Amqp)

lazy val avroparquet =
  pekkoConnectorProject("avroparquet", "avroparquet", Dependencies.AvroParquet)

lazy val awsSpiPekkoHttp =
  pekkoConnectorProject("aws-spi-pekko-http", "aws.api.pekko.http", Dependencies.AwsSpiPekkoHttp)
    .configs(IntegrationTest)

lazy val awslambda = pekkoConnectorProject("awslambda", "aws.api.pekko.http", Dependencies.AwsLambda)
  .dependsOn(awsSpiPekkoHttp)

lazy val azureStorageQueue = pekkoConnectorProject(
  "azure-storage-queue",
  "azure.storagequeue",
  Dependencies.AzureStorageQueue)

lazy val cassandra =
  pekkoConnectorProject("cassandra", "cassandra", Dependencies.Cassandra)

lazy val couchbase =
  pekkoConnectorProject("couchbase", "couchbase", Dependencies.Couchbase)

lazy val couchbase3 =
  pekkoConnectorProject("couchbase3", "couchbase3", Dependencies.Couchbase3)

lazy val csv = pekkoConnectorProject("csv", "csv")

lazy val csvBench = internalProject("csv-bench")
  .dependsOn(csv)
  .enablePlugins(JmhPlugin)

lazy val dynamodb = pekkoConnectorProject("dynamodb", "aws.dynamodb", Dependencies.DynamoDB)
  .dependsOn(awsSpiPekkoHttp)

lazy val elasticsearch = pekkoConnectorProject(
  "elasticsearch",
  "elasticsearch",
  Dependencies.Elasticsearch)

// The name 'file' is taken by `sbt.file`, hence 'files'
lazy val files = pekkoConnectorProject("file", "file", Dependencies.File)

lazy val ftp = pekkoConnectorProject(
  "ftp",
  "ftp",
  Dependencies.Ftp,
  MetaInfLicenseNoticeCopy.ftpSettings,
  Test / fork := true,
  // To avoid potential blocking in machines with low entropy (default is `/dev/random`)
  Test / javaOptions += "-Djava.security.egd=file:/dev/./urandom")

lazy val geode =
  pekkoConnectorProject(
    "geode",
    "geode",
    Dependencies.Geode,
    Test / fork := true,
    Test / scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n == 13 =>
          // https://github.com/scala/bug/issues/12072
          List("-Xlint:-byname-implicit")
        case Some((2, n)) if n == 12 =>
          List.empty
        case Some((3, _)) =>
          List.empty
      }
    })

lazy val googleCommon = pekkoConnectorProject(
  "google-common",
  "google.common",
  Dependencies.GoogleCommon,
  MetaInfLicenseNoticeCopy.googleCommonSettings,
  Test / fork := true)

lazy val googleCloudBigQuery = pekkoConnectorProject(
  "google-cloud-bigquery",
  "google.cloud.bigquery",
  Dependencies.GoogleBigQuery,
  Test / fork := true,
  Compile / scalacOptions += "-Wconf:src=src_managed/.+:s").dependsOn(googleCommon).enablePlugins(
  spray.boilerplate.BoilerplatePlugin)

lazy val googleCloudBigQueryStorage = pekkoConnectorProject(
  "google-cloud-bigquery-storage",
  "google.cloud.bigquery.storage",
  Dependencies.GoogleBigQueryStorage,
  pekkoGrpcCodeGeneratorSettings ~= { _.filterNot(_ == "flat_package") },
  pekkoGrpcCodeGeneratorSettings += "server_power_apis",
  // FIXME only generate the server for the tests again
  pekkoGrpcGeneratedSources := Seq(PekkoGrpc.Client, PekkoGrpc.Server),
  // Test / pekkoGrpcGeneratedSources := Seq(PekkoGrpc.Server),
  pekkoGrpcGeneratedLanguages := Seq(PekkoGrpc.Scala, PekkoGrpc.Java),
  Compile / scalacOptions ++= Seq(
    "-Wconf:src=.+/pekko-grpc/main/.+:s",
    "-Wconf:src=.+/pekko-grpc/test/.+:s"),
  compile / javacOptions := (compile / javacOptions).value.filterNot(_ == "-Xlint:deprecation")).dependsOn(
  googleCommon).enablePlugins(PekkoGrpcPlugin)

lazy val googleCloudPubSub = pekkoConnectorProject(
  "google-cloud-pub-sub",
  "google.cloud.pubsub",
  Dependencies.GooglePubSub,
  Test / fork := true,
  // See docker-compose.yml gcloud-pubsub-emulator_prep
  Test / envVars := Map("PUBSUB_EMULATOR_HOST" -> "localhost", "PUBSUB_EMULATOR_PORT" -> "8538")).dependsOn(
  googleCommon)

lazy val googleCloudPubSubGrpc = pekkoConnectorProject(
  "google-cloud-pub-sub-grpc",
  "google.cloud.pubsub.grpc",
  Dependencies.GooglePubSubGrpc,
  pekkoGrpcCodeGeneratorSettings ~= { _.filterNot(_ == "flat_package") },
  pekkoGrpcGeneratedSources := Seq(PekkoGrpc.Client),
  pekkoGrpcGeneratedLanguages := Seq(PekkoGrpc.Scala, PekkoGrpc.Java),
  // for the ExampleApp in the tests
  run / connectInput := true,
  Compile / scalacOptions ++= Seq(
    "-Wconf:src=.+/pekko-grpc/main/.+:s",
    "-Wconf:src=.+/pekko-grpc/test/.+:s"),
  compile / javacOptions := (compile / javacOptions).value.filterNot(_ == "-Xlint:deprecation")).enablePlugins(
  PekkoGrpcPlugin).dependsOn(googleCommon)

lazy val googleCloudStorage = pekkoConnectorProject(
  "google-cloud-storage",
  "google.cloud.storage",
  Test / fork := true,
  Dependencies.GoogleStorage).dependsOn(googleCommon)

lazy val googleFcm =
  pekkoConnectorProject("google-fcm", "google.firebase.fcm", Dependencies.GoogleFcm, Test / fork := true)
    .dependsOn(googleCommon)

lazy val hbase = pekkoConnectorProject("hbase", "hbase", Dependencies.HBase, Test / fork := true)

lazy val hdfs = pekkoConnectorProject("hdfs", "hdfs", Dependencies.Hdfs)

lazy val huaweiPushKit =
  pekkoConnectorProject("huawei-push-kit", "huawei.pushkit", Dependencies.HuaweiPushKit)

lazy val influxdb = pekkoConnectorProject(
  "influxdb",
  "influxdb",
  Dependencies.InfluxDB,
  Compile / scalacOptions ++= Seq(
    // JDK 11: method isAccessible in class AccessibleObject is deprecated
    "-Wconf:cat=deprecation:s"))

lazy val ironmq = pekkoConnectorProject(
  "ironmq",
  "ironmq",
  Dependencies.IronMq,
  Test / fork := true)

lazy val jms = pekkoConnectorProject("jms", "jms", Dependencies.Jms)

lazy val jsonStreaming = pekkoConnectorProject("json-streaming", "json.streaming", Dependencies.JsonStreaming)

lazy val kinesis = pekkoConnectorProject("kinesis", "aws.kinesis", Dependencies.Kinesis)
  .dependsOn(awsSpiPekkoHttp)

lazy val kudu = pekkoConnectorProject("kudu", "kudu", Dependencies.Kudu)

lazy val mongodb = pekkoConnectorProject("mongodb", "mongodb", Dependencies.MongoDb)

lazy val mqtt = pekkoConnectorProject("mqtt", "mqtt", Dependencies.Mqtt)

lazy val mqttStreaming =
  pekkoConnectorProject("mqtt-streaming", "mqttStreaming", Dependencies.MqttStreaming,
    MetaInfLicenseNoticeCopy.mqttStreamingSettings)

lazy val mqttStreamingBench = internalProject("mqtt-streaming-bench")
  .enablePlugins(JmhPlugin)
  .dependsOn(mqtt, mqttStreaming)

lazy val orientdb =
  pekkoConnectorProject(
    "orientdb",
    "orientdb",
    Dependencies.OrientDB,
    Test / fork := true,
    // note: orientdb client needs to be refactored to move off deprecated calls
    fatalWarnings := false)

lazy val reference = internalProject("reference", Dependencies.Reference)
  .dependsOn(testkit % Test)

lazy val s3 = pekkoConnectorProject("s3", "aws.s3", Dependencies.S3,
  MetaInfLicenseNoticeCopy.s3Settings)
  .dependsOn(awsSpiPekkoHttp)

lazy val pravega = pekkoConnectorProject(
  "pravega",
  "pravega",
  Dependencies.Pravega,
  Test / fork := true)

lazy val springWeb = pekkoConnectorProject(
  "spring-web",
  "spring.web",
  Dependencies.SpringWeb)

lazy val simpleCodecs = pekkoConnectorProject("simple-codecs", "simplecodecs")

lazy val slick = pekkoConnectorProject("slick", "slick", Dependencies.Slick)

lazy val eventbridge = pekkoConnectorProject("aws-event-bridge", "aws.eventbridge",
  Dependencies.Eventbridge)
  .dependsOn(awsSpiPekkoHttp)

lazy val sns = pekkoConnectorProject("sns", "aws.sns", Dependencies.Sns)
  .dependsOn(awsSpiPekkoHttp)

// Solrj has some deprecated methods
lazy val solr = pekkoConnectorProject("solr", "solr", Dependencies.Solr,
  fatalWarnings := false)

lazy val sqs = pekkoConnectorProject("sqs", "aws.sqs", Dependencies.Sqs)
  .dependsOn(awsSpiPekkoHttp)

lazy val sse = pekkoConnectorProject("sse", "sse", Dependencies.Sse)

lazy val text = pekkoConnectorProject("text", "text")

lazy val udp = pekkoConnectorProject("udp", "udp")

lazy val unixdomainsocket =
  pekkoConnectorProject("unix-domain-socket", "unixdomainsocket", Dependencies.UnixDomainSocket)

lazy val xml = pekkoConnectorProject("xml", "xml", Dependencies.Xml)

lazy val docs = project
  .enablePlugins(PekkoParadoxPlugin, ParadoxPlugin, ParadoxSitePlugin, PreprocessPlugin)
  .disablePlugins(MimaPlugin)
  .settings(
    Compile / paradox / name := "Apache Pekko Connectors",
    publish / skip := true,
    pekkoParadoxGithub := Some("https://github.com/apache/pekko-connectors"),
    makeSite := makeSite.dependsOn(LocalRootProject / ScalaUnidoc / doc).value,
    previewPath := (Paradox / siteSubdirName).value,
    Preprocess / siteSubdirName := s"api/pekko-connectors/${projectInfoVersion.value}",
    Preprocess / sourceDirectory := (LocalRootProject / ScalaUnidoc / unidoc / target).value,
    Preprocess / preprocessRules := Seq(
      ("http://www\\.eclipse\\.org/".r, _ => "https://www\\.eclipse\\.org/"),
      ("http://pravega\\.io/".r, _ => "https://pravega\\.io/"),
      ("http://www\\.scala-lang\\.org/".r, _ => "https://www\\.scala-lang\\.org/"),
      ("https://javadoc\\.io/page/".r, _ => "https://javadoc\\.io/static/")),
    Paradox / siteSubdirName := s"docs/pekko-connectors/${projectInfoVersion.value}",
    Global / pekkoParadoxIncubatorNotice := None,
    paradoxProperties ++= Map(
      "pekko.version" -> Dependencies.PekkoVersion,
      "pekko-http.version" -> Dependencies.PekkoHttpVersion,
      "hadoop.version" -> Dependencies.HadoopVersion,
      "extref.github.base_url" -> s"https://github.com/apache/pekko-connectors/tree/${if (isSnapshot.value) "main"
        else "v" + version.value}/%s",
      "extref.pekko.base_url" -> s"https://pekko.apache.org/docs/pekko/current/%s",
      "scaladoc.org.apache.pekko.base_url" -> s"https://pekko.apache.org/api/pekko/${Dependencies.PekkoBinaryVersion}",
      "javadoc.org.apache.pekko.base_url" -> s"https://pekko.apache.org/japi/pekko/${Dependencies.PekkoBinaryVersion}/",
      "javadoc.org.apache.pekko.link_style" -> "direct",
      "extref.pekko-http.base_url" -> s"https://pekko.apache.org/docs/pekko-http/${Dependencies.PekkoHttpBinaryVersion}/%s",
      "scaladoc.org.apache.pekko.http.base_url" -> s"https://pekko.apache.org/api/pekko-http/${Dependencies.PekkoHttpBinaryVersion}/",
      "javadoc.org.apache.pekko.http.base_url" -> s"https://pekko.apache.org/japi/pekko-http/${Dependencies.PekkoHttpBinaryVersion}/",
      // Pekko gRPC
      "pekko-grpc.version" -> Dependencies.PekkoGrpcBinaryVersion,
      "extref.pekko-grpc.base_url" -> s"https://pekko.apache.org/docs/pekko-grpc/${Dependencies.PekkoGrpcBinaryVersion}/%s",
      "scaladoc.org.apache.pekko.gprc.base_url" -> s"https://pekko.apache.org/api/pekko-grpc/${Dependencies.PekkoGrpcBinaryVersion}/",
      // Couchbase
      "couchbase.version" -> Dependencies.CouchbaseVersion,
      "extref.couchbase.base_url" -> s"https://docs.couchbase.com/java-sdk/${Dependencies.CouchbaseVersionForDocs}/%s",
      // Java
      "extref.java-api.base_url" -> "https://docs.oracle.com/javase/8/docs/api/index.html?%s.html",
      "extref.geode.base_url" -> s"https://geode.apache.org/docs/guide/${Dependencies.GeodeVersionForDocs}/%s",
      "extref.javaee-api.base_url" -> "https://docs.oracle.com/javaee/7/api/index.html?%s.html",
      "extref.paho-api.base_url" -> "https://www.eclipse.org/paho/files/javadoc/index.html?%s.html",
      "extref.pravega.base_url" -> s"https://cncf.pravega.io/docs/${Dependencies.PravegaVersionForDocs}/%s",
      "extref.slick.base_url" -> s"https://scala-slick.org/doc/${Dependencies.SlickVersion}/%s",
      // Cassandra
      "extref.cassandra.base_url" -> s"https://cassandra.apache.org/doc/${Dependencies.CassandraVersionInDocs}/%s",
      "extref.cassandra-driver.base_url" -> s"https://docs.datastax.com/en/developer/java-driver/${Dependencies.CassandraDriverVersionInDocs}/%s",
      "javadoc.com.datastax.oss.base_url" -> s"https://docs.datastax.com/en/drivers/java/${Dependencies.CassandraDriverVersionInDocs}/",
      // Solr
      "extref.solr.base_url" -> s"https://solr.apache.org/guide/${Dependencies.SolrVersionForDocs}/%s",
      "javadoc.org.apache.solr.base_url" -> s"https://solr.apache.org/docs/${Dependencies.SolrVersionForDocs}_0/solr-solrj/",
      // Java
      "javadoc.base_url" -> "https://docs.oracle.com/javase/8/docs/api/",
      "javadoc.javax.jms.base_url" -> "https://docs.oracle.com/javaee/7/api/",
      "javadoc.com.couchbase.base_url" -> s"https://docs.couchbase.com/sdk-api/couchbase-java-client-${Dependencies.CouchbaseVersion}/",
      "javadoc.io.pravega.base_url" -> s"http://pravega.io/docs/${Dependencies.PravegaVersionForDocs}/javadoc/clients/",
      "javadoc.org.apache.kudu.base_url" -> s"https://kudu.apache.org/releases/${Dependencies.KuduVersion}/apidocs/",
      "javadoc.org.apache.hadoop.base_url" -> s"https://hadoop.apache.org/docs/r${Dependencies.HadoopVersion}/api/",
      "javadoc.software.amazon.awssdk.base_url" -> "https://sdk.amazonaws.com/java/api/latest/",
      "javadoc.com.google.auth.base_url" -> "https://www.javadoc.io/doc/com.google.auth/google-auth-library-credentials/latest/",
      "javadoc.com.google.auth.link_style" -> "direct",
      "javadoc.com.fasterxml.jackson.annotation.base_url" -> "https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-annotations/latest/",
      "javadoc.com.fasterxml.jackson.annotation.link_style" -> "direct",
      // Scala
      "scaladoc.spray.json.base_url" -> s"https://javadoc.io/doc/io.spray/spray-json_${scalaBinaryVersion.value}/latest/",
      // Eclipse Paho client for MQTT
      "javadoc.org.eclipse.paho.client.mqttv3.base_url" -> "https://www.eclipse.org/paho/files/javadoc/",
      "javadoc.org.bson.codecs.configuration.base_url" -> "https://mongodb.github.io/mongo-java-driver/3.7/javadoc/",
      "scaladoc.scala.base_url" -> s"https://www.scala-lang.org/api/${scalaBinaryVersion.value}.x/",
      "scaladoc.org.apache.pekko.stream.connectors.base_url" -> s"/${(Preprocess / siteSubdirName).value}/",
      "javadoc.org.apache.pekko.stream.connectors.base_url" -> ""),
    paradoxGroups := Map("Language" -> Seq("Java", "Scala")),
    paradoxRoots := List("examples/elasticsearch-samples.html",
      "examples/ftp-samples.html",
      "examples/jms-samples.html",
      "examples/mqtt-samples.html",
      "index.html"),
    apidocRootPackage := "org.apache.pekko",
    Compile / paradoxMarkdownToHtml / sourceGenerators += Def.taskDyn {
      val targetFile = (Compile / paradox / sourceManaged).value / "license-report.md"

      (LocalRootProject / dumpLicenseReportAggregate).map { dir =>
        IO.copy(List(dir / "pekko-connectors-root-licenses.md" -> targetFile)).toList
      }
    }.taskValue)

lazy val testkit = internalProject("testkit", Dependencies.testkit)

lazy val `doc-examples` = project
  .enablePlugins(AutomateHeaderPlugin)
  .disablePlugins(MimaPlugin, SitePlugin)
  .settings(
    name := s"pekko-connectors-doc-examples",
    publish / skip := true,
    Dependencies.`Doc-examples`)

lazy val billOfMaterials = Project("bill-of-materials", file("bill-of-materials"))
  .enablePlugins(BillOfMaterialsPlugin)
  .disablePlugins(MimaPlugin, SitePlugin)
  .settings(
    name := "pekko-connectors-bom",
    licenses := List(License.Apache2),
    libraryDependencies := Seq.empty,
    bomIncludeProjects := userProjects,
    description := s"${description.value} (depending on Scala ${CrossVersion.binaryScalaVersion(scalaVersion.value)})")

val mimaCompareVersion = "1.0.2"

def pekkoConnectorProject(projectId: String,
    moduleName: String,
    additionalSettings: sbt.Def.SettingsDefinition*): Project = {
  import com.typesafe.tools.mima.core._
  Project(id = projectId, base = file(projectId))
    .enablePlugins(AutomateHeaderPlugin, ReproducibleBuildsPlugin)
    .disablePlugins(SitePlugin)
    .settings(
      name := s"pekko-connectors-$projectId",
      licenses := List(License.Apache2),
      AutomaticModuleName.settings(s"pekko.stream.connectors.$moduleName"),
      mimaPreviousArtifacts := {
        if (moduleName == "slick" || moduleName == "couchbase3") {
          Set.empty
        } else {
          Set(organization.value %% name.value % mimaCompareVersion)
        }
      },
      mimaBinaryIssueFilters ++= Seq(
        ProblemFilters.exclude[Problem]("*.impl.*"),
        // generated code
        ProblemFilters.exclude[Problem]("com.google.*")),
      Test / parallelExecution := false)
    .settings(additionalSettings: _*)
    .dependsOn(testkit % Test)
}

def internalProject(projectId: String, additionalSettings: sbt.Def.SettingsDefinition*): Project =
  Project(id = projectId, base = file(projectId))
    .enablePlugins(AutomateHeaderPlugin)
    .disablePlugins(SitePlugin, MimaPlugin)
    .settings(name := s"pekko-connectors-$projectId", publish / skip := true)
    .settings(additionalSettings: _*)

Global / onLoad := (Global / onLoad).value.andThen { s =>
  val v = version.value
  val log = sLog.value
  log.info(
    s"Building Pekko Connectors $v against Pekko ${Dependencies.PekkoVersion} and Pekko HTTP ${Dependencies.PekkoHttpVersion} on Scala ${(googleCommon / scalaVersion).value}")
  if (dynverGitDescribeOutput.value.hasNoTags)
    log.error(
      s"Failed to derive version from git tags. Maybe run `git fetch --unshallow` or `git fetch upstream` on a fresh git clone from a fork? Derived version: $v")
  s
}
