/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

import sbt._
import Common.isScala3
import Keys._

object Dependencies {

  val CronBuild = sys.env.get("GITHUB_EVENT_NAME").contains("schedule")

  val Scala213 = "2.13.12" // update even in link-validator.conf
  val Scala212 = "2.12.18"
  val Scala3 = "3.3.1"
  val ScalaVersions = Seq(Scala213, Scala212, Scala3)

  val PekkoVersion = PekkoCoreDependency.version
  val PekkoBinaryVersion = PekkoVersion.take(3)

  val InfluxDBJavaVersion = "2.15"

  val AvroVersion = "1.11.3"
  val AwsSdk2Version = "2.23.12"
  val AwsSdk2SqsVersion = "2.20.162" // latest AwsSdk2Version causes us test issues with SQS
  val AwsSpiPekkoHttpVersion = "0.1.0"
  val NettyVersion = "4.1.106.Final"
  // Sync with plugins.sbt
  val PekkoGrpcBinaryVersion = "1.0"
  val PekkoHttpVersion = PekkoHttpDependency.version
  val PekkoHttpBinaryVersion = "1.0"
  val ScalaTestVersion = "3.2.17"
  val TestContainersScalaTestVersion = "0.41.0"
  val mockitoVersion = "4.11.0" // check even https://github.com/scalatest/scalatestplus-mockito/releases
  val protobufJavaVersion = "3.21.12"
  val hoverflyVersion = "0.14.1"
  val scalaCheckVersion = "1.17.0"

  val LogbackForSlf4j1Version = "1.2.13"
  val LogbackForSlf4j2Version = "1.3.14"
  val LogbackVersion = if (PekkoBinaryVersion == "1.0") LogbackForSlf4j1Version else LogbackForSlf4j2Version

  /**
   * Calculates the scalatest version in a format that is used for `org.scalatestplus` scalacheck artifacts
   *
   * @see
   * https://www.scalatest.org/user_guide/property_based_testing
   */
  private def scalaTestPlusScalaCheckVersion(version: String) =
    version.split('.').take(2).mkString("-")

  val scalaTestScalaCheckArtifact = s"scalacheck-${scalaTestPlusScalaCheckVersion(scalaCheckVersion)}"
  val scalaTestScalaCheckVersion = s"$ScalaTestVersion.0"

  val CouchbaseVersion = "2.7.23"
  val CouchbaseVersionForDocs = "2.7"

  val GoogleAuthVersion = "1.22.0"
  val JwtScalaVersion = "10.0.0"

  val log4jOverSlf4jVersion = "1.7.36"
  val jclOverSlf4jVersion = "1.7.36"

  val CommonSettings = Seq(
    // These libraries are added to all modules via the `Common` AutoPlugin
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-stream" % PekkoVersion))

  val testkit = Seq(
    libraryDependencies := Seq(
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.10.0",
      "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion,
      "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % TestContainersScalaTestVersion,
      "com.novocode" % "junit-interface" % "0.11",
      "junit" % "junit" % "4.13.2"))

  val Mockito = Seq(
    "org.mockito" % "mockito-core" % mockitoVersion % Test,
    // https://github.com/scalatest/scalatestplus-mockito/releases
    "org.scalatestplus" %% "mockito-4-11" % (ScalaTestVersion + ".0") % Test)

  // Releases https://github.com/FasterXML/jackson-databind/releases
  // CVE issues https://github.com/FasterXML/jackson-databind/issues?utf8=%E2%9C%93&q=+label%3ACVE
  // This should align with the Jackson minor version used in Pekko 1.0.x
  // https://github.com/apache/incubator-pekko/blob/main/project/Dependencies.scala
  val JacksonDatabindVersion = "2.16.1"
  val JacksonDatabindDependencies = Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % JacksonDatabindVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % JacksonDatabindVersion)

  val Amqp = Seq(
    libraryDependencies ++= Seq(
      "com.rabbitmq" % "amqp-client" % "5.20.0") ++ Mockito)

  val AwsLambda = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      ("com.github.pjfanning" %% "aws-spi-pekko-http" % AwsSpiPekkoHttpVersion).excludeAll(
        ExclusionRule(organization = "org.apache.pekko")),
      ("software.amazon.awssdk" % "lambda" % AwsSdk2Version).excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client"))) ++ Mockito)

  val AzureStorageQueue = Seq(
    libraryDependencies ++= Seq(
      "com.microsoft.azure" % "azure-storage" % "8.6.6"))

  val CassandraVersionInDocs = "4.0"
  val CassandraDriverVersion = "4.17.0"
  val CassandraDriverVersionInDocs = "4.17"

  val Cassandra = Seq(
    libraryDependencies ++= JacksonDatabindDependencies ++ Seq(
      ("com.datastax.oss" % "java-driver-core" % CassandraDriverVersion)
        .exclude("com.github.spotbugs", "spotbugs-annotations")
        .exclude("org.apache.tinkerpop", "*") // https://github.com/akka/alpakka/issues/2200
        .exclude("com.esri.geometry", "esri-geometry-api"), // https://github.com/akka/alpakka/issues/2225
      "io.netty" % "netty-handler" % NettyVersion,
      "org.apache.pekko" %% "pekko-discovery" % PekkoVersion % Provided))

  val Couchbase = Seq(
    libraryDependencies ++= Seq(
      "com.couchbase.client" % "java-client" % CouchbaseVersion,
      "io.reactivex" % "rxjava-reactive-streams" % "1.2.1",
      "org.apache.pekko" %% "pekko-discovery" % PekkoVersion % Provided,
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion % Test,
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonDatabindVersion % Test,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonDatabindVersion % Test))

  val `Doc-examples` = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
      "org.apache.pekko" %% "pekko-connectors-kafka" % "1.0.0" % Test,
      "junit" % "junit" % "4.13.2" % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test))

  val DynamoDB = Seq(
    libraryDependencies ++= Seq(
      ("com.github.pjfanning" %% "aws-spi-pekko-http" % AwsSpiPekkoHttpVersion).excludeAll(
        ExclusionRule(organization = "org.apache.pekko")),
      ("software.amazon.awssdk" % "dynamodb" % AwsSdk2Version).excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion))

  val Elasticsearch = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
      "org.slf4j" % "jcl-over-slf4j" % jclOverSlf4jVersion % Test) ++ JacksonDatabindDependencies)

  val File = Seq(
    libraryDependencies ++= Seq(
      "com.google.jimfs" % "jimfs" % "1.3.0" % Test))

  val avro4sVersion: Def.Initialize[String] = Def.setting {
    if (Common.isScala3.value) "5.0.9" else "4.1.1"
  }

  val AvroParquet = Seq(
    libraryDependencies ++= Seq(
      "org.apache.parquet" % "parquet-avro" % "1.13.1",
      "org.apache.avro" % "avro" % AvroVersion,
      ("org.apache.hadoop" % "hadoop-client" % "3.3.6" % Test).exclude("log4j", "log4j"),
      ("org.apache.hadoop" % "hadoop-common" % "3.3.6" % Test).exclude("log4j", "log4j"),
      "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion.value % Test,
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion % Test,
      "org.specs2" %% "specs2-core" % "4.20.5" % Test,
      "org.slf4j" % "log4j-over-slf4j" % log4jOverSlf4jVersion % Test))

  val Ftp = Seq(
    libraryDependencies ++= Seq(
      "commons-net" % "commons-net" % "3.10.0",
      "com.hierynomus" % "sshj" % "0.38.0",
      "ch.qos.logback" % "logback-classic" % LogbackForSlf4j2Version % Test) ++ Mockito)

  val GeodeVersion = "1.15.1"
  val GeodeVersionForDocs = "115"

  val Geode = Seq(
    libraryDependencies ++= {
      Seq("geode-core", "geode-cq")
        .map("org.apache.geode" % _ % GeodeVersion) ++
      Seq(
        "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % JacksonDatabindVersion,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % JacksonDatabindVersion,
        "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.22.1" % Test,
        "ch.qos.logback" % "logback-classic" % LogbackForSlf4j2Version % Test) ++ JacksonDatabindDependencies ++
      (if (isScala3.value)
         Seq.empty // Equivalent and relevant shapeless functionality has been mainlined into Scala 3 language/stdlib
       else Seq(
         "com.chuusai" %% "shapeless" % "2.3.10"))
    })

  val GoogleCommon = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
      "com.github.jwt-scala" %% "jwt-json-common" % JwtScalaVersion,
      "com.google.auth" % "google-auth-library-credentials" % GoogleAuthVersion,
      "io.specto" % "hoverfly-java" % hoverflyVersion % Test) ++ Mockito)

  val GoogleBigQuery = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-jackson" % PekkoHttpVersion % Provided,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
      "io.spray" %% "spray-json" % "1.3.6",
      "com.fasterxml.jackson.core" % "jackson-annotations" % JacksonDatabindVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % JacksonDatabindVersion % Test,
      "io.specto" % "hoverfly-java" % hoverflyVersion % Test) ++ Mockito)
  val GoogleBigQueryStorage = Seq(
    // see Pekko gRPC version in plugins.sbt
    libraryDependencies ++= Seq(
      // https://github.com/googleapis/java-bigquerystorage/tree/master/proto-google-cloud-bigquerystorage-v1
      "com.google.api.grpc" % "proto-google-cloud-bigquerystorage-v1" % "1.23.2" % "protobuf-src",
      "org.apache.avro" % "avro" % AvroVersion % "provided",
      "org.apache.arrow" % "arrow-vector" % "4.0.1" % "provided",
      "io.grpc" % "grpc-auth" % org.apache.pekko.grpc.gen.BuildInfo.grpcVersion,
      "com.google.protobuf" % "protobuf-java" % protobufJavaVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-core" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-parsing" % PekkoHttpVersion,
      "org.apache.arrow" % "arrow-memory-netty" % "4.0.1" % Test,
      "org.apache.pekko" %% "pekko-discovery" % PekkoVersion) ++ Mockito)

  val GooglePubSub = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
      "com.github.tomakehurst" % "wiremock" % "2.27.2" % Test) ++ Mockito)

  val GooglePubSubGrpc = Seq(
    // see Pekko gRPC version in plugins.sbt
    libraryDependencies ++= Seq(
      // https://github.com/googleapis/java-pubsub/tree/master/proto-google-cloud-pubsub-v1/
      "com.google.cloud" % "google-cloud-pubsub" % "1.112.5" % "protobuf-src",
      "io.grpc" % "grpc-auth" % org.apache.pekko.grpc.gen.BuildInfo.grpcVersion,
      "com.google.auth" % "google-auth-library-oauth2-http" % GoogleAuthVersion,
      "com.google.protobuf" % "protobuf-java" % protobufJavaVersion,
      // pull in Pekko Discovery for our Pekko version
      "org.apache.pekko" %% "pekko-discovery" % PekkoVersion))

  val GoogleFcm = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion) ++ Mockito)

  val GoogleStorage = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
      "io.specto" % "hoverfly-java" % hoverflyVersion % Test) ++ Mockito)

  val HBase = {
    val hbaseVersion = "1.4.14"
    val hadoopVersion = "2.7.7"
    Seq(
      libraryDependencies ++= Seq(
        ("org.apache.hbase" % "hbase-shaded-client" % hbaseVersion).exclude("log4j", "log4j").exclude("org.slf4j",
          "slf4j-log4j12"),
        ("org.apache.hbase" % "hbase-common" % hbaseVersion).exclude("log4j", "log4j").exclude("org.slf4j",
          "slf4j-log4j12"),
        ("org.apache.hadoop" % "hadoop-common" % hadoopVersion).exclude("log4j", "log4j").exclude("org.slf4j",
          "slf4j-log4j12"),
        ("org.apache.hadoop" % "hadoop-mapreduce-client-core" % hadoopVersion).exclude("log4j", "log4j").exclude(
          "org.slf4j", "slf4j-log4j12"),
        "org.slf4j" % "log4j-over-slf4j" % log4jOverSlf4jVersion % Test))
  }

  val HadoopVersion = "3.3.6"
  val Hdfs = Seq(
    libraryDependencies ++= Seq(
      ("org.apache.hadoop" % "hadoop-client" % HadoopVersion).exclude("log4j", "log4j").exclude("org.slf4j",
        "slf4j-log4j12"),
      "org.typelevel" %% "cats-core" % "2.10.0",
      ("org.apache.hadoop" % "hadoop-hdfs" % HadoopVersion % Test).exclude("log4j", "log4j").exclude("org.slf4j",
        "slf4j-log4j12"),
      ("org.apache.hadoop" % "hadoop-common" % HadoopVersion % Test).exclude("log4j", "log4j").exclude("org.slf4j",
        "slf4j-log4j12"),
      ("org.apache.hadoop" % "hadoop-minicluster" % HadoopVersion % Test).exclude("log4j", "log4j").exclude("org.slf4j",
        "slf4j-log4j12"),
      "org.slf4j" % "log4j-over-slf4j" % log4jOverSlf4jVersion % Test) ++ Mockito)

  val HuaweiPushKit = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
      "com.github.jwt-scala" %% "jwt-json-common" % JwtScalaVersion) ++ Mockito)

  val InfluxDB = Seq(
    libraryDependencies ++= Seq(
      "org.influxdb" % "influxdb-java" % InfluxDBJavaVersion))

  val IronMq = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.mdedetrich" %% "pekko-stream-circe" % "1.0.0",
      "org.mdedetrich" %% "pekko-http-circe" % "1.0.0"))

  val Jms = Seq(
    libraryDependencies ++= Seq(
      "javax.jms" % "jms" % "1.1" % Provided,
      "com.ibm.mq" % "com.ibm.mq.allclient" % "9.3.4.1" % Test,
      "org.apache.activemq" % "activemq-broker" % "5.16.7" % Test,
      "org.apache.activemq" % "activemq-client" % "5.16.7" % Test,
      "io.github.sullis" %% "jms-testkit" % "1.0.4" % Test) ++ Mockito,
    // Having JBoss as a first resolver is a workaround for https://github.com/coursier/coursier/issues/200
    externalResolvers := ("jboss".at(
      "https://repository.jboss.org/nexus/content/groups/public")) +: externalResolvers.value)

  val JsonStreaming = Seq(
    libraryDependencies ++= Seq(
      "com.github.jsurfer" % "jsurfer-jackson" % "1.6.5") ++ JacksonDatabindDependencies)

  val Kinesis = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      ("com.github.pjfanning" %% "aws-spi-pekko-http" % AwsSpiPekkoHttpVersion).excludeAll(ExclusionRule(
        organization = "org.apache.pekko"))) ++ Seq(
      "software.amazon.awssdk" % "kinesis" % AwsSdk2Version,
      "software.amazon.awssdk" % "firehose" % AwsSdk2Version,
      "software.amazon.kinesis" % "amazon-kinesis-client" % "2.5.4").map(
      _.excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client"))) ++ Seq(
      "ch.qos.logback" % "logback-classic" % LogbackForSlf4j2Version % Test) ++ Mockito)

  val KuduVersion = "1.7.1"
  val Kudu = Seq(
    libraryDependencies ++= Seq(
      "org.apache.kudu" % "kudu-client-tools" % KuduVersion,
      "org.apache.kudu" % "kudu-client" % KuduVersion % Test))

  val MongoDb = Seq(
    crossScalaVersions -= Scala3,
    libraryDependencies ++= Seq(
      "org.mongodb.scala" %% "mongo-scala-driver" % "4.11.1"))

  val Mqtt = Seq(
    libraryDependencies ++= Seq(
      "org.eclipse.paho" % "org.eclipse.paho.client.mqttv3" % "1.2.5"))

  val MqttStreaming = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
      "org.apache.pekko" %% "pekko-stream-typed" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test))

  val OrientDB = Seq(
    libraryDependencies ++= JacksonDatabindDependencies ++ Seq(
      ("com.orientechnologies" % "orientdb-graphdb" % "3.1.20")
        .exclude("com.tinkerpop.blueprints", "blueprints-core"),
      "com.orientechnologies" % "orientdb-object" % "3.1.20"))

  val PravegaVersion = "0.13.0"
  val PravegaVersionForDocs = s"v$PravegaVersion"

  val Pravega = {
    Seq(
      libraryDependencies ++= Seq(
        "io.pravega" % "pravega-client" % PravegaVersion,
        "org.slf4j" % "log4j-over-slf4j" % log4jOverSlf4jVersion % Test))
  }

  val Reference = Seq(
    // connector specific library dependencies and resolver settings
    libraryDependencies ++= Seq(
    ))

  val S3 = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-xml" % PekkoHttpVersion,
      "software.amazon.awssdk" % "auth" % AwsSdk2Version,
      // in-memory filesystem for file related tests
      "com.google.jimfs" % "jimfs" % "1.3.0" % Test,
      "com.github.tomakehurst" % "wiremock-jre8" % "2.35.1" % Test,
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion % Test,
      "org.scalatestplus" %% scalaTestScalaCheckArtifact % scalaTestScalaCheckVersion % Test))

  val SpringWeb = {
    val SpringVersion = "5.3.31"
    val SpringBootVersion = "2.7.18"
    Seq(
      libraryDependencies ++= Seq(
        "org.springframework" % "spring-core" % SpringVersion,
        "org.springframework" % "spring-context" % SpringVersion,
        "org.springframework.boot" % "spring-boot-autoconfigure" % SpringBootVersion, // TODO should this be provided?
        "org.springframework.boot" % "spring-boot-configuration-processor" % SpringBootVersion % Optional,
        // for examples
        "org.springframework.boot" % "spring-boot-starter-web" % SpringBootVersion % Test))
  }

  val SlickVersion = "3.4.1"
  val Slick = Seq(
    crossScalaVersions -= Scala3,
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % SlickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
      "com.h2database" % "h2" % "2.2.224" % Test))
  val Eventbridge = Seq(
    libraryDependencies ++= Seq(
      ("com.github.pjfanning" %% "aws-spi-pekko-http" % AwsSpiPekkoHttpVersion).excludeAll(
        ExclusionRule(organization = "org.apache.pekko")),
      ("software.amazon.awssdk" % "eventbridge" % AwsSdk2Version).excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion) ++ Mockito)

  val Sns = Seq(
    libraryDependencies ++= Seq(
      ("com.github.pjfanning" %% "aws-spi-pekko-http" % AwsSpiPekkoHttpVersion).excludeAll(
        ExclusionRule(organization = "org.apache.pekko")),
      ("software.amazon.awssdk" % "sns" % AwsSdk2Version).excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion) ++ Mockito)

  val SolrjVersion = "7.7.3"
  val SolrVersionForDocs = "7_7"

  val Solr = Seq(
    libraryDependencies ++= Seq(
      "org.apache.solr" % "solr-solrj" % SolrjVersion,
      ("org.apache.solr" % "solr-test-framework" % SolrjVersion % Test).exclude("org.apache.logging.log4j",
        "log4j-slf4j-impl"),
      "org.slf4j" % "log4j-over-slf4j" % log4jOverSlf4jVersion % Test),
    resolvers += "restlet".at("https://maven.restlet.talend.com"))

  val Sqs = Seq(
    libraryDependencies ++= Seq(
      ("com.github.pjfanning" %% "aws-spi-pekko-http" % AwsSpiPekkoHttpVersion).excludeAll(
        ExclusionRule(organization = "org.apache.pekko")),
      ("software.amazon.awssdk" % "sqs" % AwsSdk2SqsVersion).excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.mockito" % "mockito-inline" % mockitoVersion % Test) ++ Mockito)

  val Sse = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-testkit" % PekkoHttpVersion % Test))

  val UnixDomainSocket = Seq(
    libraryDependencies ++= Seq(
      "com.github.jnr" % "jffi" % "1.3.12", // classifier "complete", // Is the classifier needed anymore?
      "com.github.jnr" % "jnr-unixsocket" % "0.38.21"))

  val Xml = Seq(
    libraryDependencies ++= Seq(
      "com.fasterxml" % "aalto-xml" % "1.3.2"))

}
