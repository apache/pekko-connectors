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

public class Animal {
  private final int id;
  private final String name;
  private final int owner;

  public Animal(int id, String name, int owner) {
    this.id = id;
    this.name = name;
    this.owner = owner;
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public int getOwner() {
    return owner;
  }

  @Override
  public String toString() {
    return getId() + ": " + getName() + " owner: " + getOwner();
  }
}
