# Release Notes (1.1.x)

## 1.1.0

Release notes for Apache Pekko Connectors 1.1.0. See [GitHub Milestone for 1.1.0-M1](https://github.com/apache/pekko-connectors/milestone/2?closed=1) and [GitHub Milestone for 1.1.0](https://github.com/apache/pekko-connectors/milestone/7?closed=1) for a fuller list of changes.

### Known Issues

Some connectors are not currently tested due to problems with running the tests. Please get in touch if you use these connectors, especially if you have time to help with fixing the issues.

The most notable issues are with:

* HBase ([#61](https://github.com/apache/pekko-connectors/issues/61))
* IronMQ ([#697](https://github.com/apache/pekko-connectors/issues/697))

With OrientDB Connector, it appears that the latest OrientDB client only works with OrientDB 3.2 servers. If you use an older version of OrientDB, you may be better off sticking with Pekko Connectors 1.0.x ([PR361](https://github.com/apache/pekko-connectors/pull/361)).

### Potentially Breaking Change

* Remove duplicate scopes value in reference.conf ([PR314](https://github.com/apache/pekko-connectors/pull/314)).
    * this could break applications that were loading the scope from the `service-account.scopes`

### Fixes

* Close JMS sessions when exceptions happen ([PR485](https://github.com/apache/pekko-connectors/pull/485)).
* SQS: delaySeconds parameter is not populated in SendMessageBatchRequestEntry ([#759](https://github.com/apache/pekko-connectors/issues/759)). (not in v1.1.0-M1)

### Additions
* Scala 3 is now supported for the Slick Connector.
* New connector `couchbase3` that uses Couchbase Client v3. The pre-existing `couchbase` connector still uses the older v2 Client.
* New connector `jakartams` which is the [Jakarta Messaging](https://jakarta.ee/learn/docs/jakartaee-tutorial/current/messaging/jms-concepts/jms-concepts.html) equivalent of the JMS connector.
* `aws-spi-pekko-http` is now part of Apache Pekko, as opposed to being an externally maintained lib.
* aws-spi-pekko-http: Allow using SdkHttpConfigurationOption over default pekko-http connection settings ([PR827](https://github.com/apache/pekko-connectors/pull/827)). (not in v1.1.0-M1)
* New pekko-connectors-bom ([PR633](https://github.com/apache/pekko-connectors/pull/633)).

### Other Changes
* New config for FTP Connector that allows you to choose whether to use the legacy or latest code for FTPS proxies (`useUpdatedFtpsClient`) ([PR171](https://github.com/apache/pekko-connectors/pull/171)).
* Kinesis: use stage materializer with IODispatcher instead of injected EC ([PR226](https://github.com/apache/pekko-connectors/pull/226)).
* Add support for FTPS implicit mode ([PR311](https://github.com/apache/pekko-connectors/pull/311)).
* Google Common: Use scope config for compute-engine auth ([PR313](https://github.com/apache/pekko-connectors/pull/313)).
* Google Common: Remove duplicate scopes value in reference.conf ([PR314](https://github.com/apache/pekko-connectors/pull/314)).
* New config for AMQP Connector that allows you to improve performance by reusing byte arrays ([PR592](https://github.com/apache/pekko-connectors/pull/592)).
* Google Common: Improve error message in case of response failure ([PR799](https://github.com/apache/pekko-connectors/pull/799)). (not in v1.1.0-M1)

### Dependency Upgrades

Most dependencies have been upgraded to the latest available version that still supports Java 8 as of release time (October 2024).
Exceptions include:

* HBase (see Known Issues above)
* Solr Client was upgraded to v8 (v9 does not support Java 8).
* Spring - we have pinned our dependency to v5 due to Java 8 support. Similarly, Spring Boot is pinned to v2. We expect that you can use newer versions of Spring if you use newer versions of Java Runtime. If you go this route, please test that it works ok before going to production.
