# Overview

The [Apache Pekko Connectors project](https://pekko.apache.org/docs/pekko-connectors/current/) is an open source initiative to implement stream-aware and reactive integration pipelines for Java and Scala. It is built on top of @extref:[Apache Pekko Streams](pekko:stream/index.html), and has been designed from the ground up to understand streaming natively and provide a DSL for reactive and stream-oriented programming, with built-in support for backpressure. Apache Pekko Streams is a [Reactive Streams](https://www.reactive-streams.org/) and JDK 9+ [java.util.concurrent.Flow](https://docs.oracle.com/javase/10/docs/api/java/util/concurrent/Flow.html)-compliant implementation and therefore @extref:[fully interoperable](pekko:general/stream/stream-design.html#interoperation-with-other-reactive-streams-implementations) with other implementations.

If you'd like to know what integrations with Apache Pekko Connectors look like, have a look at our 
@ref[self-contained examples](examples/index.md) section.

There are a few blog posts and presentations about Apache Pekko Connectors out there, we've @ref[collected some](other-docs/webinars-presentations-articles.md).


## Versions

The code in this documentation is compiled against:

* Apache Pekko Connectors $project.version$ ([Github](https://github.com/apache/incubator-pekko-connectors), [API docs](https://pekko.apache.org/api/pekko-connectors/current/org/apache/pekko/stream/connectors/index.html))
* Scala $scala.binary.version$ (all modules are available for Scala 2.13)
* Apache Pekko Streams $pekko.version$+ (@extref:[Reference](pekko:stream/index.html), [Github](https://github.com/apache/incubator-pekko))
* Apache Pekko HTTP $pekko-http.version$+ (@extref:[Reference](pekko-http:), [Github](https://github.com/apache/incubator-pekko-http))

See @ref:[Apache Pekko Connectors versioning](other-docs/versioning.md) for more details.

Release notes are found at @ref:[Release Notes](release-notes/index.md).

If you want to try out a connector that has not yet been released, give @ref[snapshots](other-docs/snapshots.md) a spin which are published after every merged PR.

## Contributing

Please feel free to contribute to Apache Pekko Connectors by reporting issues you identify, or by suggesting changes to the code. Please refer to our [contributing instructions](https://github.com/apache/incubator-pekko-connectors/blob/main/CONTRIBUTING.md) and our [contributor advice](https://github.com/apache/incubator-pekko-connectors/blob/main/contributor-advice.md) to learn how it can be done. The target structure for Apache Pekko Connectors connectors is illustrated by the @ref[Reference connector](reference.md).

We want Apache Pekko and Apache Pekko Connectors to strive in a welcoming and open atmosphere and expect all contributors to respect our [code of conduct](https://www.apache.org/foundation/policies/conduct.html).

Feel free to tag your project with *pekko-connectors* keyword in [Scaladex](https://index.scala-lang.org/search?topics=pekko-connnectors) for easier discoverability.

@@ toc { .main depth=2 }

@@@ index

* [External stream components](external-components.md) (hosted separately)
* [Self-contained examples](examples/index.md)
* [Other documentation resources](other-docs/index.md)
* [Integration Patterns](patterns.md)
* [Release notes](release-notes/index.md)
* [License Report](license-report.md)

@@@
