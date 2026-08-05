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

package org.apache.pekko.stream.connectors.google.http

import org.apache.pekko
import pekko.annotation.InternalApi
import pekko.http.scaladsl.{ ConnectionContext, HttpsConnectionContext }

import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.{ CertificateFactory, X509Certificate }
import javax.net.ssl.{ SSLContext, TrustManagerFactory }

@InternalApi
private[google] object ForwardProxyHttpsContext {

  private val TlsVersions = Map(
    "TLSv1.2" -> Array("TLSv1.2", "TLSv1.3"),
    "TLSv1.3" -> Array("TLSv1.3"))

  def apply(trustPemPath: String, minTlsVersion: String = "TLSv1.2"): HttpsConnectionContext = {
    val certificate = x509Certificate(trustPemPath: String)
    val sslContext = SSLContext.getInstance("TLS")

    val alias = certificate.getIssuerX500Principal.getName
    val trustStore = KeyStore.getInstance(KeyStore.getDefaultType)
    trustStore.load(null, null)
    trustStore.setCertificateEntry(alias, certificate)

    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(trustStore)
    val trustManagers = tmf.getTrustManagers
    sslContext.init(null, trustManagers, null)
    val protocols = TlsVersions.getOrElse(minTlsVersion,
      throw new IllegalArgumentException(
        s"Unsupported TLS version: $minTlsVersion. Minimum supported is TLSv1.2. Valid values: ${TlsVersions.keys.mkString(", ")}"))
    sslContext.getDefaultSSLParameters.setProtocols(protocols)
    ConnectionContext.httpsClient(sslContext)
  }

  private def x509Certificate(trustPemPath: String): X509Certificate = {
    val stream = new FileInputStream(trustPemPath)
    try CertificateFactory.getInstance("X509").generateCertificate(stream).asInstanceOf[X509Certificate]
    finally stream.close()
  }
}
