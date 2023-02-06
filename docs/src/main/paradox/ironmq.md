# IronMQ

The IronMQ connector provides an Apache Pekko Stream source and sink to connect to the [IronMQ](https://www.iron.io/) queue.

IronMQ is a simple point-to-point queue, but it is possible to implement a fan-out semantic by configure the queue as push
queue and set other queue as subscribers. More information about that could be found on
[IronMQ documentation](https://old.iron.io/ironmq-fan-out-support/)

@@project-info{ projectId="ironmq" }

## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=org.apache.pekko
  artifact=pekko-connectors-ironmq_$scala.binary.version$
  version=$project.version$
  symbol2=PekkoVersion
  value2=$pekko.version$
  group2=org.apache.pekko
  artifact2=pekko-stream_$scala.binary.version$
  version2=PekkoVersion
  symbol3=PekkoHttpVersion
  value3=$pekko-http.version$
  group3=org.apache.pekko
  artifact3=pekko-http_$scala.binary.version$
  version3=PekkoHttpVersion
}

The table below shows direct dependencies of this module and the second tab shows all libraries it depends on transitively.

@@dependencies { projectId="ironmq" }

## Consumer

IronMQ can be used either in cloud or on-premise. Either way you need a authentication token and a project ID. These can be set in the `application.conf` file.

@@snip [snip](/ironmq/src/main/resources/reference.conf)

The consumer is poll-based. It will poll every `fetch-interval` milliseconds, waiting for `poll-timeout` milliseconds to consume new messages and will push those downstream.

It supports both at-most-once and at-least-once semantics. In the first case the messages are deleted straight away after
been fetched. In the latter case the messages piggy back a `Committable` object that should be used to commit the message.
Committing the message will cause the message to be deleted from the queue.

### At most once

The consumer source is instantiated using the @scala[@scaladoc[IronMqConsumer](org.apache.pekko.stream.connectors.ironmq.scaladsl.IronMqConsumer$)]@java[@scaladoc[IronMqConsumer](org.apache.pekko.stream.connectors.ironmq.javadsl.IronMqConsumer$)].

Scala
: @@snip [snip](/ironmq/src/test/scala/docs/scaladsl/IronMqDocsSpec.scala) { #atMostOnce }

Java
: @@snip [snip](/ironmq/src/test/java/docs/javadsl/IronMqDocsTest.java) { #imports #atMostOnce }

### At least once

To ensure at-least-once semantics, `CommittableMessage`s need to be committed after successful processing which will delete the message from IronMQ.

Scala
: @@snip [snip](/ironmq/src/test/scala/docs/scaladsl/IronMqDocsSpec.scala) { #atLeastOnce }

Java
: @@snip [snip](/ironmq/src/test/java/docs/javadsl/IronMqDocsTest.java) { #imports #atLeastOnce }



## Producer

The producer is very trivial at this time, it does not provide any batching mechanism, but sends messages to IronMq as
soon as they arrive to the stage.

The producer is instantiated using the @scala[@scaladoc[IronMqProducer](org.apache.pekko.stream.connectors.ironmq.scaladsl.IronMqProducer$)]@java[@scaladoc[IronMqProducer](org.apache.pekko.stream.connectors.ironmq.javadsl.IronMqProducer$)].
It provides methods to obtain either a @scala[`Flow[PushMessage, Messages.Id, NotUsed]`]@java[`Flow<PushMessage, Messages.Id, NotUsed>`] or a @scala[`Sink[PushMessage, NotUsed]`]@java[`Sink<PushMessage, NotUsed>`].


### Flow

The @scaladoc[PushMessage](org.apache.pekko.stream.connectors.ironmq.PushMessage) allows to specify the delay per individual message. The message expiration is set a queue level.

When using the `Flow` the returned @scala[@scaladoc[Messages.Ids](org.apache.pekko.stream.connectors.ironmq.Message$$Id)]@java[`String`] contains the ID of the pushed message, that can be used to manipulate the message. For each `PushMessage` from the upstream you will have exactly one @scala[`Message.Id`]@java[`String`] in downstream in the same order.

Scala
: @@snip [snip](/ironmq/src/test/scala/docs/scaladsl/IronMqDocsSpec.scala) { #flow }

Java
: @@snip [snip](/ironmq/src/test/java/docs/javadsl/IronMqDocsTest.java) { #imports #flow }

The producer also provides a committable aware Flow/Sink as @scala[`Flow[(PushMessage, Committable), Message.Id, NotUsed]`]@java[`Flow<CommittablePushMessage<Committable>, String, NotUsed>`].
It can be used to consume a Flow from an IronMQ consumer or any other source that provides a commit mechanism.

Scala
: @@snip [snip](/ironmq/src/test/scala/docs/scaladsl/IronMqDocsSpec.scala) { #atLeastOnceFlow }

Java
: @@snip [snip](/ironmq/src/test/java/docs/javadsl/IronMqDocsTest.java) { #imports #atLeastOnceFlow }


### Sink

Scala
: @@snip [snip](/ironmq/src/test/scala/docs/scaladsl/IronMqDocsSpec.scala) { #sink }

Java
: @@snip [snip](/ironmq/src/test/java/docs/javadsl/IronMqDocsTest.java) { #imports #sink }
