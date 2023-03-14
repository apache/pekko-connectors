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

import org.apache.pekko.stream.connectors.geode.PekkoPdxSerializer;
import org.apache.geode.pdx.PdxReader;
import org.apache.geode.pdx.PdxWriter;

import java.util.Date;

// #person-pdx-serializer
public class PersonPdxSerializer implements PekkoPdxSerializer<Person> {

  @Override
  public Class<Person> clazz() {
    return Person.class;
  }

  @Override
  public boolean toData(Object o, PdxWriter out) {
    if (o instanceof Person) {
      Person p = (Person) o;
      out.writeInt("id", p.getId());
      out.writeString("name", p.getName());
      out.writeDate("birthDate", p.getBirthDate());
      return true;
    }
    return false;
  }

  @Override
  public Object fromData(Class<?> clazz, PdxReader in) {
    int id = in.readInt("id");
    String name = in.readString("name");
    Date birthDate = in.readDate("birthDate");
    return new Person(id, name, birthDate);
  }
}
// #person-pdx-serializer
