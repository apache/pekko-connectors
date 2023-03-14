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

package org.apache.pekko.stream.connectors.googlecloud.storage

final class CustomerEncryption private (encryptionAlgorithm: String, keySha256: String) {
  def withEncryptionAlgorithm(encryptionAlgorithm: String): CustomerEncryption =
    copy(encryptionAlgorithm = encryptionAlgorithm)
  def withKeySha256(keySha256: String): CustomerEncryption = copy(keySha256 = keySha256)

  private def copy(encryptionAlgorithm: String = encryptionAlgorithm,
      keySha256: String = keySha256): CustomerEncryption =
    new CustomerEncryption(encryptionAlgorithm, keySha256)

  override def toString: String =
    s"CustomerEncryption(encryptionAlgorithm=$encryptionAlgorithm, keySha256=$keySha256)"
}

object CustomerEncryption {
  def apply(encryptionAlgorithm: String, keySha256: String): CustomerEncryption =
    new CustomerEncryption(encryptionAlgorithm, keySha256)

  def create(encryptionAlgorithm: String, keySha256: String): CustomerEncryption =
    new CustomerEncryption(encryptionAlgorithm, keySha256)
}
