/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) since 2016 Lightbend Inc. <https://www.lightbend.com>
 */

package docs.javadsl;

import org.apache.pekko.NotUsed;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.connectors.avroparquet.javadsl.AvroParquetFlow;
import org.apache.pekko.stream.connectors.avroparquet.javadsl.AvroParquetSource;
import org.apache.pekko.stream.javadsl.Flow;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import java.io.IOException;
// #init-reader
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.Path;
import org.apache.avro.Schema;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.parquet.avro.AvroParquetReader;
// #init-reader

public class Examples {

  private final Schema schema =
      new Schema.Parser()
          .parse(
              "{\"type\":\"record\",\"name\":\"Document\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"body\",\"type\":\"string\"}]}");
  // #init-system
  ActorSystem system = ActorSystem.create();
  // #init-system

  // #init-reader

  Configuration conf = new Configuration();

  ParquetReader<GenericRecord> reader =
      AvroParquetReader.<GenericRecord>builder(
              HadoopInputFile.fromPath(new Path("./test.parquet"), conf))
          .disableCompatibility()
          .build();
  // #init-reader

  // #init-source
  Source<GenericRecord, NotUsed> source = AvroParquetSource.create(reader);
  // #init-source

  public Examples() throws IOException {

    // #init-flow
    ParquetWriter<GenericRecord> writer =
        AvroParquetWriter.<GenericRecord>builder(new Path("./test.parquet"))
            .withConf(conf)
            .withSchema(schema)
            .build();

    Flow<GenericRecord, GenericRecord, NotUsed> flow = AvroParquetFlow.create(writer);

    source.via(flow).runWith(Sink.ignore(), system);
    // #init-flow

  }
}
