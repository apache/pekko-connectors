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

package org.apache.pekko.stream.connectors.google.http

import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.http.scaladsl.{ ConnectionContext, HttpsConnectionContext }

import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.{ CertificateFactory, X509Certificate }
import javax.net.ssl.{ SSLContext, TrustManagerFactory }

@InternalApi
private[google] object ForwardProxyHttpsContext {

  def apply(trustPemPath: String): HttpsConnectionContext = {
    val certificate = x509Certificate(trustPemPath: String)
    val sslContext = SSLContext.getInstance("SSL")

    val alias = certificate.getIssuerDN.getName
    val trustStore = KeyStore.getInstance(KeyStore.getDefaultType)
    trustStore.load(null, null)
    trustStore.setCertificateEntry(alias, certificate)

    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(trustStore)
    val trustManagers = tmf.getTrustManagers
    sslContext.init(null, trustManagers, null)
    ConnectionContext.httpsClient(sslContext)
  }

  private def x509Certificate(trustPemPath: String): X509Certificate = {
    val stream = new FileInputStream(trustPemPath)
    try CertificateFactory.getInstance("X509").generateCertificate(stream).asInstanceOf[X509Certificate]
    finally stream.close()
  }
}
