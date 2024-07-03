# Release Notes (1.1.x)

## 1.1.0-M1

Release notes for Apache Pekko Connectors 1.1.0-M1. See [GitHub Milestone](https://github.com/apache/pekko-connectors/milestone/2?closed=1) for a fuller list of changes.
As with all milestone releases, this release is not recommended for production use - it is designed to allow users to try out the changes in a test environment.

### Known Issues

Some connectors are not currently tested due to problems with running the tests. Please get in touch if you use these connectors, especially if you have time to help with fixing the issues.

The most notable issues are with:
* HBase ([#61](https://github.com/apache/pekko-connectors/issues/61))
* IronMQ ([#697](https://github.com/apache/pekko-connectors/issues/697))

With OrientDB Connector, it appears that the latest orientdb client only works with OrientDB 3.2 servers. If you use an older version of OrientDB, you may be better off sticking the Pekko Connectors 1.0.x ([#361](https://github.com/apache/pekko-connectors/pull/361)).

### Additions
* Scala 3 is now support for the Slick Connector.
* New connector `couchbase3` that uses Couchbase Client v3. The pre-existing `couchbase` connector still uses the older v2 Client.
* New connector `jakartams` which is the [Jakarta Messaging](https://jakarta.ee/learn/docs/jakartaee-tutorial/current/messaging/jms-concepts/jms-concepts.html) equivalent of the JMS connector.
* `aws-spi-pekko-http` is now part of Apache Pekko, as opposed to being an externally maintained lib.
* New config for AMQP Connector that allows you to improve performance by reusing byte arrays ([#592](https://github.com/apache/pekko-connectors/pull/592)).

### Changes
* New config for FTP Connector that allows you to choose whether to use the legacy or latest code for FTPS proxies (`useUpdatedFtpsClient`) ([#171](https://github.com/apache/pekko-connectors/pull/171)).
* Google Common: Use scope config for compute-engine auth ([#313](https://github.com/apache/pekko-connectors/pull/313)).

### Dependency Upgrades

Most dependencies have been upgraded to the latest available version that still supports Java 8 as of release time.
Exceptions include:
* HBase (see Known Issues above)
* Solr
* Spring - we have pinned our dependency to v5 due to Java 8 support. Similarly, Spring Boot is pinned to v2. We expect that you can use newer versions of Spring if you use newer versions of Java Runtime. If you go this route, please test that it works ok before going to production.
