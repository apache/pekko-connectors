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

import org.apache.pekko.stream.connectors.geode.PekkoPdxSerializer;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxWriter;

// #animal-pdx-serializer
public class AnimalPdxSerializer implements PekkoPdxSerializer<Animal> {
  @Override
  public Class<Animal> clazz() {
    return Animal.class;
  }

  @Override
  public boolean toData(Object o, PdxWriter out) {
    if (o instanceof Animal p) {
      out.writeInt("id", p.getId());
      out.writeString("name", p.getName());
      out.writeInt("owner", p.getOwner());
      return true;
    }
    return false;
  }

  @Override
  public Object fromData(Class<?> clazz, PdxReader in) {
    int id = in.readInt("id");
    String name = in.readString("name");
    int owner = in.readInt("owner");
    return new Animal(id, name, owner);
  }
}
// #animal-pdx-serializer
