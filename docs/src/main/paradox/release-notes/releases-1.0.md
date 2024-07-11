# Release Notes (1.0.x)

The versioning strategy is described in @ref:[Apache Pekko Connectors' versioning scheme](../other-docs/versioning.md).

## 1.0.2

### Bug Fixes

* Accept any materializer type param for S3's chunkUploadSink ([#279](https://github.com/apache/pekko-connectors/pull/279))
* Change materializer type for chunkUploadSink in S3 DSLs ([#280](https://github.com/apache/pekko-connectors/pull/280))
* Kinesis: fix maxBytesPerSecond throttling ([#298](https://github.com/apache/pekko-connectors/pull/298)) 

### Dependency Upgrades

* sshj 0.38.0 - updated in FTP connector due to a CVE in sshj ([#305](https://github.com/apache/pekko-connectors/pull/305))
* netty 4.1.104 - updated in cassandra connector ([#309](https://github.com/apache/pekko-connectors/pull/309))

## 1.0.1

### Bug Fixes

* Fix `apiURL` so that projects depending on pekko-connectors have the correct
url in their scaladocs via sbt's [autoAPIMappings](https://www.scala-sbt.org/1.x/docs/Howto-Scaladoc.html#Define+the+location+of+API+documentation+for+a+library) feature ([PR252](https://github.com/apache/pekko-connectors/pull/252))

### Dependency Upgrades

Most dependency changes in this release relate to upgrading dependencies that are affected by CVEs.

* avro 1.11.3 ([#259](https://github.com/apache/pekko-connectors/issues/259))
* jackson 2.14.3 - use in more places ([#263](https://github.com/apache/pekko-connectors/pull/263))
* google-auth-library-oauth2-http 1.20.0 ([#256](https://github.com/apache/pekko-connectors/issues/256))
* netty 4.1.100 - updated in cassandra connector ([#262](https://github.com/apache/pekko-connectors/pull/262))

## 1.0.0

Apache Pekko Connectors 1.0.0 is based on Alpakka 4.0.0. Pekko came about as a result of Lightbend's decision to make future
Akka releases under a [Business Software License](https://www.lightbend.com/blog/why-we-are-changing-the-license-for-akka),
a license that is not compatible with Open Source usage.

Apache Pekko has changed the package names, among other changes. An example package name change is that the
Pekko Connectors equivalent of `akka.stream.alpakka.jms` is `org.apache.pekko.stream.connectors.jms`.
The `akka` part is replaced by `org.apache.pekko` and the `alpakka` part is replaced by `connectors`.

Config names that started with `akka` have changed to
use `pekko` instead. Config names that started with `alpakka` have changed to use `pekko.connectors`.

Users switching from Akka to Pekko should read our [Pekko Migration Guide](https://pekko.apache.org/docs/pekko/current/project/migration-guides.html).

Generally, we have tried to make it as easy as possible to switch existing Akka based projects over to using Pekko.

We have gone through the code base and have tried to properly acknowledge all third party source code in the
Apache Pekko code base. If anyone believes that there are any instances of third party source code that is not
properly acknowledged, please get in touch.

### Bug Fixes
We haven't had to fix many bugs that were in Alpakka 4.0.0.

* Fix some cases where functions were accidentally calling themselves, leading to infinite recursion
    * [PR142](https://github.com/apache/pekko-connectors/pull/142)
    * [PR164](https://github.com/apache/pekko-connectors/pull/164)
    * [PR186](https://github.com/apache/pekko-connectors/pull/186)
* S3 Connector: Force US_EAST_1 for listBuckets call ([PR66](https://github.com/apache/pekko-connectors/pull/66))
* S3 Connector: Only pass SSE headers for multipart upload requests ([PR81](https://github.com/apache/pekko-connectors/pull/81))

### Additions
* Add back Scala 2.12 support ([PR65](https://github.com/apache/pekko-connectors/pull/65))
* Scala 3 support ([#126](https://github.com/apache/pekko-connectors/issues/126))
    * The connectors that still only support Scala 2 are MongoDB and Slick.
* FTP Connector now supports UTF8 Autodetect mode ([PR221](https://github.com/apache/pekko-connectors/pull/221))
* FTP Connector now supports setting `TrustManager`/`KeyManager` ([PR205](https://github.com/apache/pekko-connectors/pull/205))
* IronMQ Connector: changed the Circe JSON integration to use [mdedetrich/pekko-streams-circe](https://github.com/mdedetrich/pekko-streams-circe) ([PR134](https://github.com/apache/pekko-connectors/pull/134)) 
* S3 Connector: Add Bucket With Versioning API support ([PR84](https://github.com/apache/pekko-connectors/pull/84))

### Dependency Upgrades
We have tried to limit the changes to third party dependencies that are used in Pekko HTTP 1.0.0. These are some exceptions:

* Cassandra Driver 4.15.0 ([PR100](https://github.com/apache/pekko-connectors/pull/100))
* protobuf 3.21.12 ([#222](https://github.com/apache/pekko-connectors/issues/222))
* jackson 2.14.3
* scalatest 3.2.14. Pekko users who have existing tests based on Akka Testkit may need to migrate their tests due to the scalatest upgrade. The [scalatest 3.2 release notes](https://www.scalatest.org/release_notes/3.2.0) have a detailed description of the changes needed.


## Extra Documentation

* [Alpakka Release Notes](https://doc.akka.io/docs/alpakka/current/release-notes/index.html)
