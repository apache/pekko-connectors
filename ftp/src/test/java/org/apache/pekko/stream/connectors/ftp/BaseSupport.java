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

package org.apache.pekko.stream.connectors.ftp;

interface BaseSupport {

  void cleanFiles();

  void generateFiles(int numFiles, int pageSize, String basePath);

  void putFileOnFtp(String filePath);

  void putFileOnFtpWithContents(String filePath, byte[] fileContents);

  byte[] getFtpFileContents(String filePath);

  boolean fileExists(String filePath);

  String getDefaultContent();
}
