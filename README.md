Alpakka [![scaladex-badge][]][scaladex] [![maven-central-badge][]][maven-central] [![CI on GitHub actions](https://github.com/apache/incubator-pekko-connectors/actions/workflows/check-build-test.yml/badge.svg)](https://github.com/apache/incubator-pekko-connectors/actions/workflows/check-build-test.yml)
=======

[scaladex]:              https://index.scala-lang.org/apache/incubator-pekko-connectors
[scaladex-badge]:        https://index.scala-lang.org/apache/incubator-pekko-connectors/latest.svg
[maven-central]:         https://search.maven.org/#search%7Cga%7C1%7Cpekko-connectors
[maven-central-badge]:   https://maven-badges.herokuapp.com/maven-central/org.pekko/pekko-connectors-file_2.13/badge.svg

Systems don't come alone. In the modern world of microservices and cloud deployment, new components must interact with legacy systems, making integration an important key to success. Reactive Streams give us a technology-independent tool to let these heterogeneous systems communicate without overwhelming each other.

The Alpakka project is an open source initiative to implement stream-aware, reactive, integration pipelines for Java and Scala. It is built on top of [Akka Streams](https://doc.akka.io/docs/akka/current/stream/index.html), and has been designed from the ground up to understand streaming natively and provide a DSL for reactive and stream-oriented programming, with built-in support for backpressure. Akka Streams is a [Reactive Streams](http://www.reactive-streams.org/) and JDK 9+ [java.util.concurrent.Flow](https://docs.oracle.com/javase/10/docs/api/java/util/concurrent/Flow.html)-compliant implementation and therefore [fully interoperable](https://doc.akka.io/docs/akka/current/general/stream/stream-design.html#interoperation-with-other-reactive-streams-implementations) with other implementations.

Pekko connectors is a fork of [Alpakka](https://github.com/akka/alpakka) 2.6.x, prior to the Akka project's adoption of the Business Source License.

Documentation
-------------

**TODO add documentation links**

To keep up with the latest Alpakka releases check out [Pekko connectors releases](https://github.com/apache/incubator-pekko-connectors/releases) and [Pekko connectors Kafka releases](https://github.com/apache/incubator-pekko-connectors-kafka/releases).

Community
---------

You can join these forums and chats to discuss and ask Pekko and Pekko connector related questions:

- [GitHub discussions](https://github.com/apache/incubator-pekko/discussions): for questions and general discussion.
- [Pekko dev mailing list](https://lists.apache.org/list.html?dev@pekko.apache.org): for Pekko connectors development discussions.
- [GitHub issues](https://github.com/apache/incubator-pekko-connectors/issues): for bug reports and feature requests. Please search the existing issues before creating new ones. If you are unsure whether you have found a bug, consider asking in GitHub discussions or the mailing list first.

Contributing
------------

Contributions are very welcome. If you have an idea on how to improve Pekko, don't hesitate to create an issue or submit a pull request.

See [CONTRIBUTING.md](https://github.com/apache/incubator-pekko/blob/main/CONTRIBUTING.md) for details on the development workflow and how to create your pull request.

Code of Conduct
---------------

Apache Pekko is governed by the [Apache code of conduct](https://www.apache.org/foundation/policies/conduct.html). By participating in this project you agree to abide by its terms.

License
-------

Apache Pekko is available under the Apache License, version 2.0. See [LICENSE](https://github.com/apache/incubator-pekko-connectors/blob/main/LICENSE) file for details.

Caveat Emptor
-------------

Alpakka components are not always binary compatible between releases. API changes that are not backward compatible might be introduced as we refine and simplify based on your feedback. A module may be dropped in any release without prior deprecation. If not stated otherwise, the [Lightbend subscription](https://www.lightbend.com/subscription) does *not* cover support for Alpakka modules.

Our goal is to improve the stability and test coverage for Alpakka APIs over time.
