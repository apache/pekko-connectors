#InfluxDB

The Apache Pekko Connectors InfluxDb connector provides Apache Pekko Streams integration for InfluxDB.

For more information about InfluxDB, please visit the [InfluxDB Documentation](https://docs.influxdata.com/)

@@project-info{ projectId="influxdb" }

@@@note { title="Official Apache Pekko Streams client" }

## Influxdata, the makers of InfluxDB now offer an Apache Pekko Streams-aware client library in https://github.com/influxdata/influxdb-client-java/tree/master/client-scala 

"The reference Scala client that allows query and write for the InfluxDB 2.0 by Apache Pekko Streams."

@@@


@@@warning { title="API may change" }

Apache Pekko Connectors InfluxDB is marked as "API may change". Please try it out and suggest improvements.

Furthermore, the major InfluxDB update to [version 2.0](https://www.influxdata.com/products/influxdb) is expected to bring API and dependency changes to Apache Pekko Connectors InfluxDB.

@@@




## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=org.apache.pekko
  artifact=pekko-connectors-influxdb_$scala.binary.version$
  version=$project.version$
  symbol2=PekkoVersion
  value2=$akka.version$
  group2=org.apache.pekko
  artifact2=pekko-stream_$scala.binary.version$
  version2=PekkoVersion
}

The table below shows direct dependencies of this module and the second tab shows all libraries it depends on transitively.

@@dependencies { projectId="influxdb" }

## Set up InfluxDB client

Sources, Flows and Sinks provided by this connector need a prepared `org.influxdb.InfluxDB` to
access to InfluxDB.

Scala
: @@snip [snip](/influxdb/src/test/scala/docs/scaladsl/InfluxDbSpec.scala) { #init-client }

Java
: @@snip [snip](/influxdb/src/test/java/docs/javadsl/TestUtils.java) { #init-client }

## InfluxDB as Source and Sink

Now we can stream messages from or to InfluxDB by providing the `InfluxDB` to the
@scala[@scaladoc[InfluxDbSource](akka.stream.alpakka.influxdb.scaladsl.InfluxDbSource$)]
@java[@scaladoc[InfluxDbSource](akka.stream.alpakka.influxdb.javadsl.InfluxDbSource$)]
or the
@scala[@scaladoc[InfluxDbSink](akka.stream.alpakka.influxdb.scaladsl.InfluxDbSink$).]
@java[@scaladoc[InfluxDbSink](akka.stream.alpakka.influxdb.javadsl.InfluxDbSink$).]


Scala
: @@snip [snip](/influxdb/src/test/scala/docs/scaladsl/InfluxDbSpecCpu.java) { #define-class }

Java
: @@snip [snip](/influxdb/src/test/java/docs/javadsl/InfluxDbCpu.java) { #define-class }

### With typed source

Use `InfluxDbSource.typed` and `InfluxDbSink.typed` to create source and sink.
@scala[The data is converted by InfluxDBMapper.]
@java[The data is converted by InfluxDBMapper.]

Scala
: @@snip [snip](/influxdb/src/test/scala/docs/scaladsl/InfluxDbSpec.scala) { #run-typed }

Java
: @@snip [snip](/influxdb/src/test/java/docs/javadsl/InfluxDbTest.java) { #run-typed }

### With `QueryResult` source

Use `InfluxDbSource.create` and `InfluxDbSink.create` to create source and sink.

Scala
: @@snip [snip](/influxdb/src/test/scala/docs/scaladsl/InfluxDbSpec.scala) { #run-query-result}

Java
: @@snip [snip](/influxdb/src/test/java/docs/javadsl/InfluxDbTest.java) { #run-query-result}

TODO

### Writing to InfluxDB

You can also build flow stages. 
@scala[@scaladoc[InfluxDbFlow](akka.stream.alpakka.influxdb.scaladsl.InfluxDbFlow$).]
@java[@scaladoc[InfluxDbFlow](akka.stream.alpakka.influxdb.javadsl.InfluxDbFlow$).]
The API is similar to creating Sinks.

Scala
: @@snip [snip](/influxdb/src/test/scala/docs/scaladsl/FlowSpec.scala) { #run-flow }

Java
: @@snip [snip](/influxdb/src/test/java/docs/javadsl/InfluxDbTest.java) { #run-flow }

### Passing data through InfluxDbFlow 

When streaming documents from Kafka, you might want to commit to Kafka **AFTER** the document has been written to InfluxDB.

Scala
: @@snip [snip](/influxdb/src/test/scala/docs/scaladsl/FlowSpec.scala) { #kafka-example }

Java
: @@snip [snip](/influxdb/src/test/java/docs/javadsl/InfluxDbTest.java) { #kafka-example }


