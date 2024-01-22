# Apache Pekko Connectors [![scaladex-badge][]][scaladex] [![maven-central-badge][]][maven-central] [![CI on GitHub actions](https://github.com/apache/incubator-pekko-connectors/actions/workflows/check-build-test.yml/badge.svg)](https://github.com/apache/incubator-pekko-connectors/actions/workflows/check-build-test.yml)[![Nightly Builds](https://github.com/apache/incubator-pekko-connectors/actions/workflows/nightly-builds.yaml/badge.svg)](https://github.com/apache/incubator-pekko-connectors/actions/workflows/nightly-builds.yaml)

[scaladex]:              https://index.scala-lang.org/apache/incubator-pekko-connectors
[scaladex-badge]:        https://index.scala-lang.org/apache/incubator-pekko-connectors/latest.svg
[maven-central]:         https://search.maven.org/#search%7Cga%7C1%7Cpekko-connectors
[maven-central-badge]:   https://maven-badges.herokuapp.com/maven-central/org.pekko/pekko-connectors-file_2.13/badge.svg

Systems don't come alone. In the modern world of microservices and cloud deployment, new components must interact with legacy systems, making integration an important key to success. Reactive Streams give us a technology-independent tool to let these heterogeneous systems communicate without overwhelming each other.

The Apache Pekko Connectors project is an open source initiative to implement stream-aware, reactive, integration pipelines for Java and Scala. It is built on top of Pekko Streams, and has been designed from the ground up to understand streaming natively and provide a DSL for reactive and stream-oriented programming, with built-in support for backpressure. Pekko Streams is a [Reactive Streams](http://www.reactive-streams.org/) and JDK 9+ [java.util.concurrent.Flow](https://docs.oracle.com/javase/10/docs/api/java/util/concurrent/Flow.html)-compliant implementation and therefore [fully interoperable](https://pekko.apache.org/docs/pekko/current/general/stream/stream-design.html#interoperation-with-other-reactive-streams-implementations) with other implementations.

Pekko Connectors is a fork of [Alpakka](https://github.com/akka/alpakka) 4.0.0, prior to the Akka project's adoption of the Business Source License.

## Documentation

Apache Pekko Connectors are documented at https://pekko.apache.org/docs/pekko-connectors/current/.

To keep up with the latest releases check out [Pekko Connectors releases](https://github.com/apache/incubator-pekko-connectors/releases) and [Pekko Connectors Kafka releases](https://github.com/apache/incubator-pekko-connectors-kafka/releases).

## Building From Source

The build commands in the [incubator-pekko](https://github.com/apache/incubator-pekko?tab=readme-ov-file#building-from-source) repo are also useful here. Java 8 should work well for this building the source in this repo. Building the Paradox docs is significatntly harder if you use Java 17 or above. You will need to specify a large number of `--add-opens` settings.

This repo contains shell scripts. These scripts are designed to help with the testing of pekko Please avoid running the scripts without checking if you need to and try to understand what the script does first. There is also a `nested-sample.tar` file that is used in tests. This tar file does not contain compiled artifacts.

## Running Tests

There are details in the [Contributing page](https://github.com/apache/incubator-pekko-connectors/blob/main/CONTRIBUTING.md). That page also has guidelines about how to prepare Pull Requests.

## Community

You can join these forums and chats to discuss and ask Pekko and Pekko connector related questions:

- [GitHub discussions](https://github.com/apache/incubator-pekko/discussions): for questions and general discussion.
- [Pekko users mailing list](https://lists.apache.org/list.html?users@pekko.apache.org): for Pekko Connectors usage discussions.
- [Pekko dev mailing list](https://lists.apache.org/list.html?dev@pekko.apache.org): for Pekko Connectors development discussions.
- [GitHub issues](https://github.com/apache/incubator-pekko-connectors/issues): for bug reports and feature requests. Please search the existing issues before creating new ones. If you are unsure whether you have found a bug, consider asking in GitHub discussions or the mailing list first.

## Contributing

Contributions are very welcome. If you have an idea on how to improve Pekko, don't hesitate to create an issue or submit a pull request.

See [CONTRIBUTING.md](https://github.com/apache/incubator-pekko-connectors/blob/main/CONTRIBUTING.md) for details on the development workflow and how to create your pull request.

## Code of Conduct

Apache Pekko is governed by the [Apache code of conduct](https://www.apache.org/foundation/policies/conduct.html). By participating in this project you agree to abide by its terms.

## License

Apache Pekko is available under the Apache License, version 2.0. See [LICENSE](https://github.com/apache/incubator-pekko-connectors/blob/main/LICENSE) file for details.

## Caveat Emptor

Pekko Connectors components are not always binary compatible between releases. API changes that are not backward compatible might be introduced as we refine and simplify based on your feedback. A module may be dropped in any release without prior deprecation. 

Our goal is to improve the stability and test coverage for Pekko Connectors APIs over time.
