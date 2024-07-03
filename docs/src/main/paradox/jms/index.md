# JMS

@@@ note { title="Java Message Service (JMS)" }

The Java Message Service (JMS) API is a Java message-oriented middleware API for sending messages between two or more clients. It is an implementation to handle the producer–consumer problem. JMS is a part of the [Java Platform, Enterprise Edition (Java EE)](https://docs.oracle.com/javaee/7/tutorial/jms-concepts001.htm#BNCDR), and was defined by a specification developed at Sun Microsystems, but which has since been guided by the Java Community Process. It is a messaging standard that allows application components based on Java EE to create, send, receive, and read messages. It allows the communication between different components of a distributed application to be loosely coupled, reliable, and asynchronous.

-- [Wikipedia](https://en.wikipedia.org/wiki/Java_Message_Service)

@@@

The Apache Pekko Connectors JMS connector provides Apache Pekko Stream sources and sinks to connect to JMS providers.

In v1.1.0, there is also a Jakarta MS connector that provides the equivalent support for [Jakarta Messaging](https://jakarta.ee/learn/docs/jakartaee-tutorial/current/messaging/jms-concepts/jms-concepts.html).

@@project-info{ projectId="jms" }

## Artifacts

@@dependency [sbt,Maven,Gradle] {
  group1=org.pekko
  artifact1=pekko-connectors-jms_$scala.binary.version$
  version1=$project.version$
  group2=javax.jms
  artifact2=jms
  version2=1.1
}

@@toc { depth=2 }

@@@ index

* [p](producer.md)
* [c](consumer.md)
* [c](browse.md)
* [ibm](ibm-mq.md)

@@@
