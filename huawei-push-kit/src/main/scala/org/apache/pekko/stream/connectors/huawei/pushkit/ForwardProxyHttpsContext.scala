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

package org.apache.pekko.stream.connectors.huawei.pushkit

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.http.scaladsl.{ ConnectionContext, Http, HttpsConnectionContext }

import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.{ CertificateFactory, X509Certificate }
import javax.net.ssl.{ SSLContext, TrustManagerFactory }

/**
 * INTERNAL API
 */
@InternalApi
private[pushkit] object ForwardProxyHttpsContext {

  val SSL = "SSL"
  val X509 = "X509"

  implicit class ForwardProxyHttpsContext(forwardProxy: ForwardProxy) {

    def httpsContext(system: ActorSystem): HttpsConnectionContext = {
      forwardProxy.trustPem match {
        case Some(trustPem) => createHttpsContext(trustPem)
        case None           => Http()(system).defaultClientHttpsContext
      }
    }
  }

  private def createHttpsContext(trustPem: ForwardProxyTrustPem) = {
    val certificate = x509Certificate(trustPem)
    val sslContext = SSLContext.getInstance(SSL)

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

  private def x509Certificate(trustPem: ForwardProxyTrustPem) = {
    val stream = new FileInputStream(trustPem.pemPath)
    var result: X509Certificate = null
    try result = CertificateFactory.getInstance(X509).generateCertificate(stream).asInstanceOf[X509Certificate]
    finally if (stream != null) stream.close()
    result
  }

}
