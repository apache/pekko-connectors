# Some advice for Apache Pekko Connectors contributors

## Reference connector

To inspect how all of the below listed guidelines are to be implemented in practice, take a look at
[the reference](reference/) connector. Feel free to use the reference connector as a starting point for new connectors.

## API design

### Public factory methods

Depending on the technology you integrate with Pekko Streams and Pekko Connectors you'll create Sources, Flows and Sinks.
Regardless on how they are implemented make sure that you create the relevant Sources, Sinks and Flows APIs so they are
simple and easy to use.

### Flows

> Reference connector [model classes](reference/src/main/scala/org/apache/pekko/stream/connectors/reference/model.scala)

When designing Flows, consider adding an extra field to the in- and out-messages which is passed through. A common
use case we see, is committing a Kafka offset after passing data to another system.

### Implementing the Java API in Scala

> Reference connector [Java API factory methods](reference/src/main/scala/org/apache/pekko/stream/connectors/reference/javadsl/Reference.scala)

Apache Pekko Connectors, same as Apache Pekko, aims to keep 100% feature parity between the various language DSLs. Implementing even the API for Java in Scala has proven the most viable way to do it, as long as you keep the following in mind:


1. Keep entry points separated in `javadsl` and `scaladsl`

1. Provide factory methods for Sources, Flows and Sinks in the `javadsl` package wrapping all the methods in the Scala API. The Pekko Stream Scala instances have a `.asJava` method to convert to the `org.apache.pekko.stream.javadsl` counterparts.

1. When using Scala `object` instances, offer a `getInstance()` method.

1. When the Scala API contains an `apply` method, use `create` for Java users.

1. Do not nest Scala `object`s more than two levels (as access from Java becomes weird)

1. Be careful to convert values within data structures (eg. for `scala.Long` vs. `java.lang.Long`, use `scala.Long.box(value)`)

1. Complement any methods with Scala collections with a Java collection version

1. Use the `org.apache.pekko.japi.Pair` class to return tuples

1. If the underlying Scala code requires an `ExecutionContext`, make the Java API take an `Executor` and use `ExecutionContext.fromExecutor(executor)` for conversion.

1. Make use of Scala JDK conversions (eg. `scala.jdk.javaapi.FutureConverters` to translate Futures to `CompletionStage`s).


### Overview of Scala types and their Java counterparts

| Scala | Java |
|-------|------|
| `scala.Option[T]` | `java.util.Optional<T>` (`OptionalDouble`, ...) |
| `scala.collection.immutable.Seq[T]` | `java.util.List<T>` |
| `scala.concurrent.Future[T]` | `java.util.concurrent.CompletionStage<T>` |
| `scala.concurrent.Promise[T]` | `java.util.concurrent.CompletableFuture<T>` |
| `scala.concurrent.duration.FiniteDuration` | `java.time.Duration` |

Functions, if you want to allow the function to throw checked exceptions (like
Scala functions - the infrastructure should recover from those gracefully) or
leverage variance in the parameter or return types:

| Scala | Java |
|-------|------|
| `T => Unit` | `org.apache.pekko.japi.function.Procedure<T>` |
| `() => R` (`scala.Function0[R]`) | `org.apache.pekko.japi.function.Creator<R>` |
| `T => R` (`scala.Function1[T, R]`) | `org.apache.pekko.japi.function.Function<T, R>` |

Functions, if you don't want to allow the function to throw checked exceptions
(unlike Scala functions):

| Scala | Java |
|-------|------|
| `T => Unit` | `java.util.function.Consumer<T>` |
| `() => R` (`scala.Function0[R]`) | `java.util.function.Supplier<R>` |
| `T => R` (`scala.Function1[T, R]`) | `java.util.function.Function<T, R>` |


### Settings

> Reference connector [settings classes](reference/src/main/scala/org/apache/pekko/stream/connectors/reference/settings.scala)

Most technologies will have a couple of configuration settings that will be needed for several Sinks, Flows, or Sinks. 
Create case classes collecting these settings instead of passing them in every method.

Create a default instance in the companion object with good defaults which can be updated via `withXxxx` methods.

Add `withXxxx` methods to specify certain fields in the settings instance.

In case you see the need to support reading the settings from `Config`, offer a method taking the `Config` instance so
that the user can apply a proper namespace.
Refrain from using `pekko.stream.connectors` as Config prefix, prefer `pekko` as root namespace.


## Implementation details

### Evolving APIs with binary compatibility

All Apache Pekko APIs aim to evolve in a binary compatible way within minor versions.

1. Do not use any default arguments

1. Do not use case classes (as the public copy method relies on default arguments)

1. To generate a case class replacement, consider using [Kaze Class](https://github.com/ktoso/kaze-class)

See [Binary Compatibilty Rules](https://pekko.apache.org/docs/pekko/current/common/binary-compatibility-rules.html) in the Apache Pekko documentation.

See [Binary Compatibility for library authors](https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html)


### External Dependencies

All the external runtime dependencies for the project, including transitive dependencies, must have an open source license that is equal to, or compatible with, [Apache 2](http://www.apache.org/licenses/LICENSE-2.0).

Which licenses are compatible with Apache 2 are defined in [this doc](http://www.apache.org/legal/3party.html#category-a), where you can see that the licenses that are listed under ``Category A`` automatically compatible with Apache 2, while the ones listed under ``Category B`` needs additional action:

> Each license in this category requires some degree of [reciprocity](http://www.apache.org/legal/3party.html#define-reciprocal); therefore, additional action must be taken in order to minimize the chance that a user of an Apache product will create a derivative work of a reciprocally-licensed portion of an Apache product without being aware of the applicable requirements.

Dependency licenses will be checked automatically by [FOSSA](https://fossa.com/). 

### Packages & Scoping

Use `private`, `private[connector]` and `final` extensively to limit the API surface.

| Package                                  | Purpose
| -----------------------------------------|------------------------
| `org.apache.pekko.stream.connectors.connector.javadsl`  | Java-only part of the API, normally factories for Sources, Flows and Sinks
| `org.apache.pekko.stream.connectors.connector.scaladsl` | Scala-only part of the API, normally factories for Sources, Flows and Sinks
| `org.apache.pekko.stream.connectors.connector`          | Shared API, eg. settings classes
| `org.apache.pekko.stream.connectors.connector.impl`     | Internal implementation in separate package

### Graph stage checklist

> Reference connector [operator implementations](reference/src/main/scala/org/apache/pekko/stream/connectors/reference/impl/)

* Keep mutable state within the `GraphStageLogic` only 
* Open connections in `preStart`
* Release resources in `postStop`
* Fail early on configuration errors
* Make sure the code is thread-safe; if in doubt, please ask!
* No Blocking At Any Time -- in other words, avoid blocking whenever possible and replace it with asynchronous 
programming (async callbacks, stage actors)

### Use of blocking APIs

Many technologies come with client libraries that only support blocking calls. Pekko Stream stages that use blocking APIs should preferably be run on Pekko's `IODispatcher`. (In rare cases you might want to allow the users to configure another dispatcher to run the blocking operations on.)

To select Pekko's `IODispatcher` for a stage use
```$scala
override protected def initialAttributes: Attributes = Attributes(ActorAttributes.IODispatcher)
```

When the `IODispatcher` is selected, you do NOT need to wrap the blocking calls in `Future`s or `blocking`.

(Issue [akka/akka#25540](https://github.com/akka/akka/issues/25540) requests better support for selecting the correct execution context.)


### Keep the code DRY

Avoid duplication of code between different Sources, Sinks and Flows. Extract the common logic to a common abstract
base `GraphStageLogic` that is inherited by the `GraphStage`s.

Sometimes it may be useful to provide a Sink or Source for a connector, even if the main concept is implemented
as a Flow. This can be easily done by reusing the Flow implementation:
* Source: `Source.maybe.viaMat(MyFlow)(Keep.right)`
* Sink: `MyFlow.toMat(Sink.ignore)(Keep.right)`

You do not need to expose every configuration a Flow offers this way -- Focus on the most important ones.

## Test

> Reference connector [Scala](reference/src/test/scala/docs/scaladsl/ReferenceSpec.scala) and
> [Java](reference/src/test/java/docs/javadsl/ReferenceTest.java) tests

Write tests for all public methods and possible settings.

Use Docker containers for any testing needed by your application. The container's configuration can be added to the
`docker-compose.yml` file.
Please ensure that you limit the amount of resources used by the containers.

## Documentation

> Reference connector [paradox documentation](docs/src/main/paradox/reference.md)

Using [Paradox](https://github.com/lightbend/paradox) syntax (which is very close to markdown), create or complement
the documentation in the `docs` module.
Prepare code snippets to be integrated by Paradox in the tests. Such example should be part of real tests and not in
unused methods.

Use ScalaDoc if you see the need to describe the API usage better than the naming does.
The `@apidoc` Paradox directive automatically creates links to the corresponding Scaladoc page for both `scaladsl` and `javadsl`. Be sure to add a `$` at the end of the name if you point to an `object`.
`@apidoc[AmqpSink$]` will link to `org/apache/pekko/stream/connectors/amqp/scaladsl/AmqpSink$.html` when viewing "Scala" and `org/apache/pekko/stream/connectors/amqp/javadsl/AmqpSink$.html` for "Java".

```

Run `sbt docs/previewSite` to generate reference and API docs, start an embedded web-server, and open a tab to the generated documentation while developing.
