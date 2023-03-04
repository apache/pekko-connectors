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

import akka.util.ByteString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ArchiveHelper {

  public Map<String, ByteString> unzip(ByteString zipArchive) throws Exception {
    ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipArchive.toArray()));
    ZipEntry entry;
    Map<String, ByteString> result = new HashMap<>();
    try {
      while ((entry = zis.getNextEntry()) != null) {
        int count;
        byte[] data = new byte[1024];

        ByteArrayOutputStream dest = new ByteArrayOutputStream();
        while ((count = zis.read(data, 0, 1024)) != -1) {
          dest.write(data, 0, count);
        }
        dest.flush();
        dest.close();
        zis.closeEntry();
        result.putIfAbsent(entry.getName(), ByteString.fromArray(dest.toByteArray()));
      }
    } finally {
      zis.close();
    }
    return result;
  }
}
