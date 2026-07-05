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

import io.pravega.client.stream.Serializer;
import io.pravega.client.stream.impl.UTF8StringSerializer;
import io.pravega.client.tables.TableKey;
import java.nio.ByteBuffer;
import java.time.Duration;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.pravega.*;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PravegaSettingsTestCase {

  protected static ActorSystem system;

  @BeforeAll
  public static void setup() {
    system = ActorSystem.create();
  }

  @Test
  public void readerSettings() {

    // #reader-settings
    ReaderSettings<String> readerSettings =
        ReaderSettingsBuilder.create(system)
            .clientConfigBuilder(
                builder -> builder.enableTlsToController(true)) // ClientConfig customization
            .readerConfigBuilder(
                builder -> builder.disableTimeWindows(true)) // ReaderConfig customization
            .withTimeout(Duration.ofSeconds(3))
            .withSerializer(new UTF8StringSerializer());
    // #reader-settings

    Assertions.assertEquals(readerSettings.timeout(), 3000, "Timeout value doesn't match");
    Assertions.assertTrue(
        readerSettings.clientConfig().isEnableTlsToController(), "TLS does not match");
    Assertions.assertTrue(
        readerSettings.readerConfig().isDisableTimeWindows(), "Window should not be enabled");
  }

  @Test
  public void writerSettings() {

    // #writer-settings
    WriterSettings<String> writerSettings =
        WriterSettingsBuilder.<String>create(system)
            .withKeyExtractor((String str) -> str.substring(0, 1))
            .withSerializer(new UTF8StringSerializer());
    // #writer-settings

    Assertions.assertEquals(
        writerSettings.maximumInflightMessages(), 10, "Default value doesn't match");
  }

  @Test
  public void tableSettings() {

    Serializer<Integer> intSerializer =
        new Serializer<Integer>() {
          public ByteBuffer serialize(Integer value) {
            ByteBuffer buff = ByteBuffer.allocate(4).putInt(value);
            buff.position(0);
            return buff;
          }

          public Integer deserialize(ByteBuffer serializedValue) {

            return serializedValue.getInt();
          }
        };

    // #table-writer-settings
    TableWriterSettings<Integer, String> tableWriterSettings =
        TableWriterSettingsBuilder.<Integer, String>create(
                system, intSerializer, new UTF8StringSerializer())
            .withKeyExtractor(id -> new TableKey(intSerializer.serialize(id)))
            .build();

    // #table-writer-settings

    // #table-reader-settings
    TableReaderSettings<Integer, String> tableReaderSettings =
        TableReaderSettingsBuilder.<Integer, String>create(
                system, intSerializer, new UTF8StringSerializer())
            .withKeyExtractor(id -> new TableKey(intSerializer.serialize(id)))
            .build();

    // #table-reader-settings

    Assertions.assertEquals(
        tableWriterSettings.maximumInflightMessages(), 10, "Default value doesn't match");
  }

  @AfterAll
  public static void teardown() {
    TestKit.shutdownActorSystem(system);
  }
}
