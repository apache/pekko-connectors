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
import com.github.sbt.junit.jupiter.sbt.Import.JupiterKeys

object Dependencies {

  val CronBuild = sys.env.get("GITHUB_EVENT_NAME").contains("schedule")

  val Scala213 = "2.13.15" // update even in link-validator.conf
  val Scala212 = "2.12.20"
  val Scala3 = "3.3.4"
  val ScalaVersions = Seq(Scala213, Scala212, Scala3)

  val PekkoVersion = PekkoCoreDependency.version
  val PekkoBinaryVersion = PekkoCoreDependency.default.link

  val InfluxDBJavaVersion = "2.23"

  val AvroVersion = "1.11.4"
  val AwsSdk2Version = "2.29.29"
  val NettyVersion = "4.1.115.Final"
  // Sync with plugins.sbt
  val PekkoGrpcBinaryVersion = "1.1"
  val PekkoHttpVersion = PekkoHttpDependency.version
  val PekkoStreamsCirceVersion = "1.1.0"
  val PekkoHttpBinaryVersion = PekkoHttpDependency.default.link
  val ScalaTestVersion = "3.2.19"
  val TestContainersScalaTestVersion = "0.41.4"
  val mockitoVersion = "4.11.0" // check even https://github.com/scalatest/scalatestplus-mockito/releases
  val protobufJavaVersion = "3.25.5"
  val hoverflyVersion = "0.19.1"
  val scalaCheckVersion = "1.18.1"

  // Legacy versions support Slf4J v1 for compatibility with older libs
  val Slf4jVersion = "2.0.16"
  val Slf4jLegacyVersion = "1.7.36"
  val LogbackVersion = "1.3.14"
  val LogbackLegacyVersion = "1.2.13"

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
  val scalaTestMockitoVersion = "3.2.18.0"

  val CouchbaseVersion = "2.7.23"
  val Couchbase3Version = "3.6.0"
  val CouchbaseVersionForDocs = "2.7"

  val GoogleAuthVersion = "1.30.0"
  val JwtScalaVersion = "10.0.1"
  val Log4jVersion = "2.23.1"

  // Releases https://github.com/FasterXML/jackson-databind/releases
  // CVE issues https://github.com/FasterXML/jackson-databind/issues?utf8=%E2%9C%93&q=+label%3ACVE
  // This should align with the Jackson minor version used in Pekko 1.1.x
  // https://github.com/apache/pekko/blob/main/project/Dependencies.scala
  val JacksonVersion = "2.17.3"
  val JacksonDatabindDependencies = Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % JacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion)

  // wiremock has very outdated, CVE vulnerable dependencies
  private val jettyVersion = "9.4.56.v20240826"
  private val guavaVersion = "33.3.1-jre"
  private val wireMockDependencies = Seq(
    "com.github.tomakehurst" % "wiremock-jre8" % "2.35.2" % Test,
    "org.eclipse.jetty" % "jetty-server" % jettyVersion % Test,
    "org.eclipse.jetty" % "jetty-servlet" % jettyVersion % Test,
    "org.eclipse.jetty" % "jetty-servlets" % jettyVersion % Test,
    "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % Test,
    "org.eclipse.jetty" % "jetty-proxy" % jettyVersion % Test,
    "org.eclipse.jetty" % "jetty-alpn-server" % jettyVersion % Test,
    "org.eclipse.jetty" % "jetty-alpn-java-server" % jettyVersion % Test,
    "org.eclipse.jetty" % "jetty-alpn-openjdk8-server" % jettyVersion % Test,
    "org.eclipse.jetty" % "jetty-alpn-java-client" % jettyVersion % Test,
    "org.eclipse.jetty" % "jetty-alpn-openjdk8-client" % jettyVersion % Test,
    "org.eclipse.jetty.http2" % "http2-server" % jettyVersion % Test,
    "com.google.guava" % "guava" % guavaVersion % Test,
    "com.fasterxml.jackson.core" % "jackson-core" % JacksonVersion % Test,
    "com.fasterxml.jackson.core" % "jackson-annotations" % JacksonVersion % Test,
    "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion % Test,
    "commons-io" % "commons-io" % "2.18.0" % Test,
    "commons-fileupload" % "commons-fileupload" % "1.5" % Test,
    "com.jayway.jsonpath" % "json-path" % "2.9.0" % Test)

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
      "org.slf4j" % "slf4j-api" % Slf4jVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % TestContainersScalaTestVersion,
      "com.novocode" % "junit-interface" % "0.11",
      "junit" % "junit" % "4.13.2"))

  val Mockito = Seq(
    "org.mockito" % "mockito-core" % mockitoVersion % Test,
    // https://github.com/scalatest/scalatestplus-mockito/releases
    "org.scalatestplus" %% "mockito-4-11" % scalaTestMockitoVersion % Test)

  val Amqp = Seq(
    libraryDependencies ++= Seq(
      "com.rabbitmq" % "amqp-client" % "5.24.0",
      "org.scalatestplus" %% scalaTestScalaCheckArtifact % scalaTestScalaCheckVersion % Test) ++ Mockito)

  val AwsSpiPekkoHttp = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "software.amazon.awssdk" % "http-client-spi" % AwsSdk2Version,
      ("software.amazon.awssdk" % "dynamodb" % AwsSdk2Version % "it,test").excludeAll(
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      ("software.amazon.awssdk" % "kinesis" % AwsSdk2Version % "it,test").excludeAll(
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      ("software.amazon.awssdk" % "sns" % AwsSdk2Version % "it,test").excludeAll(
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      ("software.amazon.awssdk" % "sqs" % AwsSdk2Version % "it,test").excludeAll(
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      ("software.amazon.awssdk" % "s3" % AwsSdk2Version % "it,test").excludeAll(
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      ("software.amazon.awssdk" % "http-client-tests" % AwsSdk2Version % "it,test").excludeAll(
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      "com.dimafeng" %% "testcontainers-scala" % TestContainersScalaTestVersion % Test,
      "com.github.sbt.junit" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "it,test",
      "ch.qos.logback" % "logback-classic" % LogbackVersion % "it,test"))

  val AwsLambda = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      ("software.amazon.awssdk" % "lambda" % AwsSdk2Version).excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client"))) ++ Mockito)

  val AzureStorageQueue = Seq(
    libraryDependencies ++= Seq(
      "com.microsoft.azure" % "azure-storage" % "8.6.6"))

  val CassandraVersionInDocs = "4.0"
  val CassandraDriverVersion = "4.18.1"
  val CassandraDriverVersionInDocs = "4.17"

  val Cassandra = Seq(
    libraryDependencies ++= JacksonDatabindDependencies ++ Seq(
      "org.apache.cassandra" % "java-driver-core" % CassandraDriverVersion,
      "io.netty" % "netty-handler" % NettyVersion,
      "org.apache.pekko" %% "pekko-discovery" % PekkoVersion % Provided))

  val Couchbase = Seq(
    libraryDependencies ++= Seq(
      "com.couchbase.client" % "java-client" % CouchbaseVersion,
      "io.reactivex" % "rxjava-reactive-streams" % "1.2.1",
      "org.apache.pekko" %% "pekko-discovery" % PekkoVersion % Provided,
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion % Test,
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion % Test,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion % Test))

  val Couchbase3 = Seq(
    libraryDependencies ++= Seq(
      "com.couchbase.client" % "java-client" % Couchbase3Version,
      "org.apache.pekko" %% "pekko-discovery" % PekkoVersion % Provided,
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion % Test,
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion % Test,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion % Test))

  val `Doc-examples` = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion,
      "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
      "org.apache.pekko" %% "pekko-connectors-kafka" % "1.1.0" % Test,
      "junit" % "junit" % "4.13.2" % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test))

  val DynamoDB = Seq(
    libraryDependencies ++= Seq(
      ("software.amazon.awssdk" % "dynamodb" % AwsSdk2Version).excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion))

  val Elasticsearch = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
      "org.slf4j" % "jcl-over-slf4j" % Slf4jVersion % Test) ++ JacksonDatabindDependencies)

  val File = Seq(
    libraryDependencies ++= Seq(
      "com.google.jimfs" % "jimfs" % "1.3.0" % Test))

  val avro4sVersion: Def.Initialize[String] = Def.setting {
    if (Common.isScala3.value) "5.0.9" else "4.1.1"
  }

  val AvroParquet = Seq(
    libraryDependencies ++= Seq(
      "org.apache.parquet" % "parquet-avro" % "1.15.0",
      "org.apache.avro" % "avro" % AvroVersion,
      ("org.apache.hadoop" % "hadoop-client" % "3.3.6" % Test).exclude("log4j", "log4j"),
      ("org.apache.hadoop" % "hadoop-common" % "3.3.6" % Test).exclude("log4j", "log4j"),
      "com.sksamuel.avro4s" %% "avro4s-core" % avro4sVersion.value % Test,
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion % Test,
      "org.specs2" %% "specs2-core" % "4.20.9" % Test,
      "org.slf4j" % "slf4j-api" % Slf4jVersion % Test,
      "org.slf4j" % "log4j-over-slf4j" % Slf4jVersion % Test))

  val Ftp = Seq(
    libraryDependencies ++= Seq(
      "commons-net" % "commons-net" % "3.11.1",
      "com.hierynomus" % "sshj" % "0.39.0",
      "org.slf4j" % "slf4j-api" % Slf4jVersion % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion % Test) ++ Mockito)

  val GeodeVersion = "1.15.1"
  val GeodeVersionForDocs = "115"

  val Geode = Seq(
    libraryDependencies ++= {
      Seq("geode-core", "geode-cq")
        .map("org.apache.geode" % _ % GeodeVersion) ++
      Seq(
        "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % JacksonVersion,
        "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % JacksonVersion,
        "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.24.3" % Test,
        "org.slf4j" % "slf4j-api" % Slf4jVersion % Test,
        "ch.qos.logback" % "logback-classic" % LogbackVersion % Test) ++ JacksonDatabindDependencies ++
      (if (isScala3.value)
         Seq.empty // Equivalent and relevant shapeless functionality has been mainlined into Scala 3 language/stdlib
       else Seq(
         "com.chuusai" %% "shapeless" % "2.3.12"))
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
      "com.fasterxml.jackson.core" % "jackson-annotations" % JacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % JacksonVersion % Test,
      "io.specto" % "hoverfly-java" % hoverflyVersion % Test) ++ Mockito)

  val ArrowVersion = "16.0.0"
  val GoogleBigQueryStorage = Seq(
    // see Pekko gRPC version in plugins.sbt
    libraryDependencies ++= Seq(
      // https://github.com/googleapis/java-bigquerystorage/tree/master/proto-google-cloud-bigquerystorage-v1
      "com.google.api.grpc" % "proto-google-cloud-bigquerystorage-v1" % "3.10.3" % "protobuf-src",
      "org.apache.avro" % "avro" % AvroVersion % "provided",
      "org.apache.arrow" % "arrow-vector" % ArrowVersion % "provided",
      "io.grpc" % "grpc-auth" % org.apache.pekko.grpc.gen.BuildInfo.grpcVersion,
      "com.google.protobuf" % "protobuf-java" % protobufJavaVersion % Runtime,
      "org.apache.pekko" %% "pekko-discovery" % PekkoVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-core" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-parsing" % PekkoHttpVersion,
      "org.apache.arrow" % "arrow-memory-netty" % ArrowVersion % Test,
      "org.slf4j" % "slf4j-api" % Slf4jVersion % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion % Test) ++ Mockito)

  val GooglePubSub = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion) ++
    Mockito ++ wireMockDependencies)

  val GooglePubSubGrpc = Seq(
    // see Pekko gRPC version in plugins.sbt
    libraryDependencies ++= Seq(
      // https://github.com/googleapis/java-pubsub/tree/master/proto-google-cloud-pubsub-v1/
      "com.google.cloud" % "google-cloud-pubsub" % "1.133.1" % "protobuf-src",
      "io.grpc" % "grpc-auth" % org.apache.pekko.grpc.gen.BuildInfo.grpcVersion,
      "com.google.auth" % "google-auth-library-oauth2-http" % GoogleAuthVersion,
      "com.google.protobuf" % "protobuf-java" % protobufJavaVersion % Runtime,
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
        "org.slf4j" % "log4j-over-slf4j" % Slf4jVersion % Test))
  }

  val HadoopVersion = "3.3.6"
  val Hdfs = Seq(
    libraryDependencies ++= Seq(
      ("org.apache.hadoop" % "hadoop-client" % HadoopVersion).exclude("log4j", "log4j").exclude("org.slf4j",
        "slf4j-log4j12"),
      "org.typelevel" %% "cats-core" % "2.12.0",
      ("org.apache.hadoop" % "hadoop-hdfs" % HadoopVersion % Test).exclude("log4j", "log4j").exclude("org.slf4j",
        "slf4j-log4j12"),
      ("org.apache.hadoop" % "hadoop-common" % HadoopVersion % Test).exclude("log4j", "log4j").exclude("org.slf4j",
        "slf4j-log4j12"),
      ("org.apache.hadoop" % "hadoop-minicluster" % HadoopVersion % Test).exclude("log4j", "log4j").exclude("org.slf4j",
        "slf4j-log4j12"),
      "org.slf4j" % "log4j-over-slf4j" % Slf4jVersion % Test) ++ Mockito)

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
      "org.mdedetrich" %% "pekko-stream-circe" % PekkoStreamsCirceVersion,
      "org.mdedetrich" %% "pekko-http-circe" % PekkoStreamsCirceVersion))

  val JakartaMs = {
    val artemisVersion = "2.19.1"
    Seq(
      libraryDependencies ++= Seq(
        "jakarta.jms" % "jakarta.jms-api" % "3.0.0" % Provided,
        "com.ibm.mq" % "com.ibm.mq.jakarta.client" % "9.4.1.0" % Test,
        "org.apache.activemq" % "artemis-server" % artemisVersion % Test,
        "org.apache.activemq" % "artemis-jakarta-client" % artemisVersion % Test,
        "org.apache.activemq" % "artemis-junit" % artemisVersion % Test,
        "com.github.pjfanning" % "jakartamswrapper" % "0.1.0" % Test) ++ Mockito)
  }

  val Jms = Seq(
    libraryDependencies ++= Seq(
      "javax.jms" % "javax.jms-api" % "2.0.1" % Provided,
      "com.ibm.mq" % "com.ibm.mq.allclient" % "9.4.1.0" % Test,
      "org.apache.activemq" % "activemq-broker" % "5.16.7" % Test,
      "org.apache.activemq" % "activemq-client" % "5.16.7" % Test,
      "io.github.sullis" %% "jms-testkit" % "1.0.4" % Test,
      "com.github.pjfanning" % "jmswrapper" % "0.1.0" % Test) ++ Mockito)

  val JsonStreaming = Seq(
    libraryDependencies ++= Seq(
      "com.github.jsurfer" % "jsurfer-jackson" % "1.6.5") ++ JacksonDatabindDependencies)

  val Kinesis = Seq(
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
      "software.amazon.awssdk" % "kinesis" % AwsSdk2Version,
      "software.amazon.awssdk" % "firehose" % AwsSdk2Version,
      "software.amazon.kinesis" % "amazon-kinesis-client" % "3.0.1").map(
      _.excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client"))) ++ Seq(
      "org.slf4j" % "slf4j-api" % Slf4jVersion % Test,
      "ch.qos.logback" % "logback-classic" % LogbackVersion % Test) ++ Mockito)

  val KuduVersion = "1.17.1"
  val Kudu = Seq(
    libraryDependencies ++= Seq(
      "org.apache.kudu" % "kudu-client" % KuduVersion,
      "org.apache.logging.log4j" % "log4j-to-slf4j" % Log4jVersion % Test))

  val MongoDb = Seq(
    crossScalaVersions -= Scala3,
    libraryDependencies ++= Seq(
      "org.mongodb.scala" %% "mongo-scala-driver" % "5.2.1"))

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
      ("com.orientechnologies" % "orientdb-graphdb" % "3.2.36")
        .exclude("com.tinkerpop.blueprints", "blueprints-core"),
      "com.orientechnologies" % "orientdb-object" % "3.2.36"))

  val PravegaVersion = "0.13.0"
  val PravegaVersionForDocs = "latest"

  val Pravega = {
    Seq(
      libraryDependencies ++= Seq(
        "io.pravega" % "pravega-client" % PravegaVersion,
        "org.slf4j" % "slf4j-api" % Slf4jVersion % Test,
        "org.slf4j" % "log4j-over-slf4j" % Slf4jVersion % Test))
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
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion % Test,
      "org.scalatestplus" %% scalaTestScalaCheckArtifact % scalaTestScalaCheckVersion % Test) ++
    wireMockDependencies)

  val SpringWeb = {
    val SpringVersion = "5.3.39"
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

  val SlickVersion = "3.5.1"
  val Slick = Seq(
    // Transitive dependency `scala-reflect` to avoid `NoClassDefFoundError`.
    // See: https://github.com/slick/slick/issues/2933
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq("org.scala-lang" % "scala-reflect" % scalaVersion.value)
      case _            => Nil
    }),
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % SlickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % SlickVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion % Test,
      "com.h2database" % "h2" % "2.2.224" % Test))

  val Eventbridge = Seq(
    libraryDependencies ++= Seq(
      ("software.amazon.awssdk" % "eventbridge" % AwsSdk2Version).excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion) ++ Mockito)

  val Sns = Seq(
    libraryDependencies ++= Seq(
      ("software.amazon.awssdk" % "sns" % AwsSdk2Version).excludeAll(
        ExclusionRule("software.amazon.awssdk", "apache-client"),
        ExclusionRule("software.amazon.awssdk", "netty-nio-client")),
      "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion) ++ Mockito)

  val SolrjVersion = "8.11.4"
  val SolrVersionForDocs = "8_11"

  val Solr = Seq(
    libraryDependencies ++= Seq(
      "org.apache.solr" % "solr-solrj" % SolrjVersion,
      ("org.apache.solr" % "solr-test-framework" % SolrjVersion % Test).exclude("org.apache.logging.log4j",
        "log4j-slf4j-impl"),
      "org.slf4j" % "log4j-over-slf4j" % Slf4jLegacyVersion % Test),
    dependencyOverrides ++= Seq(
      "org.slf4j" % "slf4j-api" % Slf4jLegacyVersion,
      "ch.qos.logback" % "logback-classic" % LogbackLegacyVersion))

  val Sqs = Seq(
    libraryDependencies ++= Seq(
      ("software.amazon.awssdk" % "sqs" % AwsSdk2Version).excludeAll(
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
      "com.github.jnr" % "jffi" % "1.3.13", // classifier "complete", // Is the classifier needed anymore?
      "com.github.jnr" % "jnr-unixsocket" % "0.38.23"))

  val Xml = Seq(
    libraryDependencies ++= Seq(
      "com.fasterxml" % "aalto-xml" % "1.3.3"))

}
