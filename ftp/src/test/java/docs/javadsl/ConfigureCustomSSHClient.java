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

// #configure-custom-ssh-client

import org.apache.pekko.stream.connectors.ftp.javadsl.Sftp;
import org.apache.pekko.stream.connectors.ftp.javadsl.SftpApi;
import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.SSHClient;

public class ConfigureCustomSSHClient {

  public ConfigureCustomSSHClient() {
    SSHClient sshClient = new SSHClient(new DefaultConfig());
    SftpApi sftp = Sftp.create(sshClient);
  }
}
// #configure-custom-ssh-client
