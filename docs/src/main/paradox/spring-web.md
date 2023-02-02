# Spring Web

Spring 5.0 introduced compatibility with [Reactive Streams](https://www.reactive-streams.org), a library interoperability standardization effort co-lead by Lightbend (with Apache Pekko Streams) along with Kaazing, Netflix, 
Pivotal, Red Hat, Twitter and many others.

Thanks to adopting Reactive Streams, multiple libraries can now inter-op since the same interfaces are implemented by 
all these libraries. Apache Pekko Streams by-design, hides the raw reactive-streams types from end-users, since it allows for
detaching these types from RS and allows for a painless migration to @javadoc[java.util.concurrent.Flow](java.util.concurrent.Flow) which was introduced in Java 9.

This Apache Pekko Connectors module makes it possible to directly return a `Source` in your Spring Web endpoints.

@@project-info{ projectId="spring-web" }


## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group=org.apache.pekko
  artifact=pekko-connectors-spring-web_$scala.binary.version$
  version=$project.version$
  symbol2=PekkoVersion
  value2=$akka.version$
  group2=org.apache.pekko
  artifact2=akka-stream_$scala.binary.version$
  version2=PekkoVersion
}

The table below shows direct dependencies of this module and the second tab shows all libraries it depends on transitively.

@@dependencies { projectId="spring-web" }


## Usage

Using Apache Pekko Streams in Spring Web (or Boot for that matter) is very simple, as Apache Pekko Connectors provides autoconfiguration to the
framework, which means that Spring is made aware of Sources and Sinks etc. 

All you need to do is include the above dependency (`pekko-connectors-spring-web`), start your app as usual:

Java
: @@snip [snip](/spring-web/src/test/java/docs/javadsl/DemoApplication.java) { #use }


And you'll be able to return Apache Pekko Streams in HTTP endpoints directly:


Java
: @@snip [snip](/spring-web/src/test/java/docs/javadsl/SampleController.java) { #use }

Both `javadsl` and `scaladsl` Apache Pekko Stream types are supported.

In fact, since Apache Pekko supports Java 9 and the `java.util.concurrent.Flow.*` types already, before Spring, you could use it
to adapt those types in your applications as well.

### The provided configuration

The automatically enabled configuration is as follows:

Java
: @@snip [snip](/spring-web/src/main/java/akka/stream/alpakka/spring/web/SpringWebAkkaStreamsConfiguration.java) { #configure }

In case you'd like to manually configure it slightly differently.

## Shameless plug: Apache Pekko HTTP 

While the integration presented here works, it's not quite the optimal way of using Apache Pekko in conjunction with serving HTTP apps.
If you're new to reactive systems and picking technologies, you may want to have a look at @extref[Apache Pekko HTTP](akka-http:).

If, for some reason, you decided use Spring MVC this integration should help you achieve the basic streaming scenarios though.
