# AWS SNS

The AWS SNS connector provides an Apache Pekko Stream Flow and Sink for push notifications through AWS SNS.

For more information about AWS SNS please visit the [official documentation](https://docs.aws.amazon.com/sns/index.html).

@@project-info{ projectId="sns" }

## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=org.apache.pekko
  artifact=pekko-connectors-sns_$scala.binary.version$
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

@@dependencies { projectId="sns" }


## Setup

This connector requires an @scala[implicit] @javadoc[SnsAsyncClient](software.amazon.awssdk.services.sns.SnsAsyncClient) instance to communicate with AWS SNS.

It is your code's responsibility to call `close` to free any resources held by the client. In this example it will be called when the actor system is terminated.

Scala
: @@snip [snip](/sns/src/test/scala/org/apache/pekko/stream/connectors/sns/IntegrationTestContext.scala) { #init-client }

Java
: @@snip [snip](/sns/src/test/java/docs/javadsl/SnsPublisherTest.java) { #init-client }

The example above uses @extref:[Apache Pekko HTTP](pekko-http:) as the default HTTP client implementation. For more details about the HTTP client, configuring request retrying and best practices for credentials, see @ref[AWS client configuration](aws-shared-configuration.md) for more details.

We will also need an @apidoc[org.apache.pekko.actor.ActorSystem].

Scala
: @@snip [snip](/sns/src/test/scala/org/apache/pekko/stream/connectors/sns/IntegrationTestContext.scala) { #init-system }

Java
: @@snip [snip](/sns/src/test/java/docs/javadsl/SnsPublisherTest.java) { #init-system }

This is all preparation that we are going to need.

## Publish messages to an SNS topic

Now we can publish a message to any SNS topic where we have access to by providing the topic ARN to the
@apidoc[SnsPublisher$] Flow or Sink factory method.

### Using a Flow

Scala
: @@snip [snip](/sns/src/test/scala/docs/scaladsl/SnsPublisherSpec.scala) { #use-flow }

Java
: @@snip [snip](/sns/src/test/java/docs/javadsl/SnsPublisherTest.java) { #use-flow }

As you can see, this would publish the messages from the source to the specified AWS SNS topic.
After a message has been successfully published, a
@javadoc[PublishResult](software.amazon.awssdk.services.sns.model.PublishRequest)
will be pushed downstream.

### Using a Sink

Scala
: @@snip [snip](/sns/src/test/scala/docs/scaladsl/SnsPublisherSpec.scala) { #use-sink }

Java
: @@snip [snip](/sns/src/test/java/docs/javadsl/SnsPublisherTest.java) { #use-sink }

As you can see, this would publish the messages from the source to the specified AWS SNS topic.

@@@ index

* [retry conf](aws-shared-configuration.md)

@@@
