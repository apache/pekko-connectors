# Release Notes (1.3.x)

## 1.3.0

Release notes for Apache Pekko Connectors 1.3.0. See [GitHub Milestone for 1.2.1](https://github.com/apache/pekko-connectors/milestone/11?closed=1) and [GitHub Milestone for 1.3.0](https://github.com/apache/pekko-connectors/milestone/12?closed=1) for a fuller list of changes.

### Known Issues

Some connectors are not currently tested due to problems with running the tests. Please get in touch if you use these connectors, especially if you have time to help with fixing the issues.

The most notable issues are with:

* HBase ([#61](https://github.com/apache/pekko-connectors/issues/61))
* IronMQ ([#697](https://github.com/apache/pekko-connectors/issues/697))

With OrientDB Connector, it appears that the latest OrientDB client only works with OrientDB 3.2 servers. If you use an older version of OrientDB, you may be better off sticking with Pekko Connectors 1.0.x ([PR361](https://github.com/apache/pekko-connectors/pull/361)).

### Fixes

* Protobuf class conflict: pekko-connectors-google-cloud-pub-sub-grpc bundles unshaded protobuf-java classes ([#1457](https://github.com/apache/pekko-connectors/issues/1457)).
* TarReader: avoid prematurely completing source ([PR1442](https://github.com/apache/pekko-connectors/pull/1442)).
* TarReader: buffer upstream data when subSource is not yet pulled ([PR1475](https://github.com/apache/pekko-connectors/pull/1475)).

### Other Changes

* Avoid silently swallowing the error when SSE stream fails/retries ([PR1205](https://github.com/apache/pekko-connectors/pull/1205)).
* AWS S3: Handle Illegal Headers in Copy Part ([PR1299](https://github.com/apache/pekko-connectors/pull/1299)).
* MQTT v5: expose MQTT 5 user properties on MqttMessage ([#1371](https://github.com/apache/pekko-connectors/issues/1371)).

### Dependency Upgrades

Most dependencies have been upgraded to a recent version that still supports Java 8 as of release time (March 2026).
Exceptions include:

* HBase (see Known Issues above)
* Solr Client was upgraded to v8 (v9 does not support Java 8).
* Spring - we have pinned our dependency to v5 due to Java 8 support. Similarly, Spring Boot is pinned to v2. We expect that you can use newer versions of Spring if you use newer versions of Java Runtime. If you go this route, please test that it works ok before going to production.

Notable upgrades include:

* Netty 4.2.10
* Amazon SDK 2.42.2
* Kinesis client 3.4.1
* Jackson 2.19.4
* amqp-client 5.29.0
* Cassanadra Driver 4.19.2
* Kudu client 1.18.1
* Google Auth 1.43.0
* jwt-scala 11.0.3
