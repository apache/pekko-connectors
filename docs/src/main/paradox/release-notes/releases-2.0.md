# Release Notes (2.0.x)

## 2.0.0-M1

This is milestone release and is aimed at testing this new major version by early adopters. This is experimental. This release should not be used in production.

Java 17 is the minimum supported version for Java. Scala 2.12 support has been removed.

See the [GitHub Milestone for 2.0.0-M1](https://github.com/apache/pekko-connectors/milestone/4?closed=1) for a fuller list of changes.

It is likely that you will need to recompile your code to use the 2.x libs.

### Known Issues

Some connectors are not currently tested due to problems with running the tests. Please get in touch if you use these connectors, especially if you have time to help with fixing the issues.

The most notable issues are with:

* HBase ([#61](https://github.com/apache/pekko-connectors/issues/61))
* IronMQ ([#697](https://github.com/apache/pekko-connectors/issues/697))

With OrientDB Connector, it appears that the latest OrientDB client only works with OrientDB 3.2 servers. If you use an older version of OrientDB, you may be better off sticking with Pekko Connectors 1.x ([PR361](https://github.com/apache/pekko-connectors/pull/361)).

### Breaking Changes

* Some previously deprecated methods have been removed.
* Java APIs have been reworked for Couchbase3 and Slick. There is also a small change for Kinesis.

### Notable Changes
* OrientDB connector was rewritten to avoid a number of deprecated classes ([PR1553](https://github.com/apache/pekko-connectors/pull/1553)).

### Dependency Upgrades

Most dependencies have been upgraded to a recent version (April 2026). Pekko 2.x supports Java 17 as a minimum so we've been able to upgrade some dependencies that have dropped Java 8 and 11 support in more recent versions.
Exceptions include:

* HBase (see Known Issues above)
* Solr Client v9 and above are not supported
