/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, which was derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.javadsl;

import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.japi.Pair;
import org.apache.pekko.stream.connectors.slick.javadsl.Slick;
import org.apache.pekko.stream.connectors.slick.javadsl.SlickSession;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// We're going to pretend we got messages from kafka.
// After we've written them to a db with Slick, we want
// to commit the offset to Kafka
public class DocSnippetFlowWithPassThrough {

  // mimics a Kafka 'Committable' type
  static class CommittableOffset {
    private Integer offset;

    public CommittableOffset(Integer offset) {
      this.offset = offset;
    }

    public CompletableFuture<Done> commit() {
      return CompletableFuture.completedFuture(Done.getInstance());
    }
  }

  static class KafkaMessage<A> {
    final A msg;
    final CommittableOffset offset;

    public KafkaMessage(A msg, CommittableOffset offset) {
      this.msg = msg;
      this.offset = offset;
    }

    public <B> KafkaMessage<B> map(Function<A, B> f) {
      return new KafkaMessage<>(f.apply(msg), offset);
    }
  }

  public static void main(String[] args) throws Exception {
    final ActorSystem system = ActorSystem.create();

    final SlickSession session = SlickSession.forConfig("slick-h2");
    system.registerOnTermination(session::close);

    final List<User> users =
        IntStream.range(0, 42)
            .boxed()
            .map((i) -> new User(i, "Name" + i))
            .collect(Collectors.toList());

    List<KafkaMessage<User>> messagesFromKafka =
        users.stream()
            .map(user -> new KafkaMessage<>(user, new CommittableOffset(users.indexOf(user))))
            .collect(Collectors.toList());

    // #flowWithPassThrough-example
    final CompletionStage<Done> done =
        Source.from(messagesFromKafka)
            .via(
                Slick.flowWithPassThrough(
                    session,
                    system.dispatcher(),
                    // add an optional second argument to specify the parallelism factor (int)
                    (kafkaMessage, connection) -> {
                      PreparedStatement statement =
                          connection.prepareStatement(
                              "INSERT INTO ALPAKKA_SLICK_JAVADSL_TEST_USERS VALUES (?, ?)");
                      statement.setInt(1, kafkaMessage.msg.id);
                      statement.setString(2, kafkaMessage.msg.name);
                      return statement;
                    },
                    (kafkaMessage, insertCount) ->
                        kafkaMessage.map(
                            user ->
                                Pair.create(
                                    user,
                                    insertCount)) // allows to keep the kafka message offset so it
                    // can be committed in a next stage
                    ))
            .log("nr-of-updated-rows")
            .mapAsync(
                1,
                kafkaMessage ->
                    kafkaMessage.offset.commit()) // in correct order, commit Kafka message
            .runWith(Sink.ignore(), system);
    // #flowWithPassThrough-example

    done.whenComplete(
        (value, exception) -> {
          system.terminate();
        });
  }
}
