# Release Notes (1.2.x)

## 1.2.0

Release notes for Apache Pekko Connectors 1.2.0. See [GitHub Milestone for 1.1.1](https://github.com/apache/pekko-connectors/milestone/8?closed=1) and [GitHub Milestone for 1.2.0](https://github.com/apache/pekko-connectors/milestone/9?closed=1) for a fuller list of changes.

### Known Issues

Some connectors are not currently tested due to problems with running the tests. Please get in touch if you use these connectors, especially if you have time to help with fixing the issues.

The most notable issues are with:

* HBase ([#61](https://github.com/apache/pekko-connectors/issues/61))
* IronMQ ([#697](https://github.com/apache/pekko-connectors/issues/697))

With OrientDB Connector, it appears that the latest OrientDB client only works with OrientDB 3.2 servers. If you use an older version of OrientDB, you may be better off sticking with Pekko Connectors 1.0.x ([PR361](https://github.com/apache/pekko-connectors/pull/361)).

### Fixes

* Properly propogate GoogleSettings in addStandardQuery ([PR1026](https://github.com/apache/pekko-connectors/pull/1026)).
* Fix ClassCastException in Google Cloud StorageObject ([PR1031](https://github.com/apache/pekko-connectors/pull/1031)).
* Fix metadata not working with GCStorage ([PR1045](https://github.com/apache/pekko-connectors/pull/1045)).

### Additions

* Slick - Add slick flow functions with error handling using try ([PR949](https://github.com/apache/pekko-connectors/pull/949)).
* Add MQTT v5 support ([PR1035](https://github.com/apache/pekko-connectors/pull/1035)).

### Other Changes

* Missing death watch in UDP.bindFlow(...) ([PR1004](https://github.com/apache/pekko-connectors/pull/1004)).
* Rename none credentials to access-token and remove token from none ([PR1021](https://github.com/apache/pekko-connectors/pull/1021)).
* Make GCS resumableUpload work with empty byte payload ([PR1060](https://github.com/apache/pekko-connectors/pull/1060)).

### Dependency Upgrades

Most dependencies have been upgraded to a recent version that still supports Java 8 as of release time (September 2025).
Exceptions include:

* HBase (see Known Issues above)
* Solr Client was upgraded to v8 (v9 does not support Java 8).
* Spring - we have pinned our dependency to v5 due to Java 8 support. Similarly, Spring Boot is pinned to v2. We expect that you can use newer versions of Spring if you use newer versions of Java Runtime. If you go this route, please test that it works ok before going to production.

Notable upgrades include:

* Netty 4.2.5
* Jackson 2.19.2
* Protobuf-Java 3.25.8
