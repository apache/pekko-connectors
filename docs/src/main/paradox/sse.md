<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  license agreements; and to You under the Apache License, version 2.0:

    https://www.apache.org/licenses/LICENSE-2.0

  This file is part of the Apache Pekko project, which was derived from Akka.
-->

# Server-sent Events (SSE)

The SSE connector provides a continuous source of server-sent events recovering from connection failure.

@@project-info{ projectId="sse" }

## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=org.apache.pekko
  artifact=pekko-connectors-sse_$scala.binary.version$
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

@@dependencies { projectId="sse" }


## Usage

Define an `EventSource` by giving a URI, a function for sending HTTP requests, and an optional initial value for Last-Event-ID header:  

Scala
: @@snip [snip](/sse/src/test/scala/docs/scaladsl/EventSourceSpec.scala) { #event-source }

Java
: @@snip [snip](/sse/src/test/java/docs/javadsl/EventSourceTest.java) { #event-source }


Then happily consume `ServerSentEvent`s:

Scala
: @@snip [snip](/sse/src/test/scala/docs/scaladsl/EventSourceSpec.scala) { #consume-events }

Java
: @@snip [snip](/sse/src/test/java/docs/javadsl/EventSourceTest.java) { #consume-events }
