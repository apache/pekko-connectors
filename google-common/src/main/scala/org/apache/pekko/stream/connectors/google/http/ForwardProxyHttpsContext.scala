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
import javax.net.ssl.{ SSLContext, SSLEngine, TrustManagerFactory }

@InternalApi
private[google] object ForwardProxyHttpsContext {

  private val TlsVersions = Map(
    "TLSv1.2" -> Array("TLSv1.2", "TLSv1.3"),
    "TLSv1.3" -> Array("TLSv1.3"))

  def apply(trustPemPath: String, minTlsVersion: String = "TLSv1.2"): HttpsConnectionContext = {
    val protocols = TlsVersions.getOrElse(minTlsVersion,
      throw new IllegalArgumentException(
        s"Unsupported TLS version: $minTlsVersion. Minimum supported is TLSv1.2. Valid values: ${TlsVersions.keys.mkString(", ")}"))

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

    // The enabled protocols have to be set on each SSLEngine. Setting them on the
    // SSLParameters returned by SSLContext.getDefaultSSLParameters has no effect,
    // because that method returns a fresh copy on every call.
    ConnectionContext.httpsClient(createSSLEngine(sslContext, protocols, _, _))
  }

  private[http] def createSSLEngine(sslContext: SSLContext,
      protocols: Array[String],
      host: String,
      port: Int): SSLEngine = {
    val engine = sslContext.createSSLEngine(host, port)
    engine.setUseClientMode(true)
    val params = engine.getSSLParameters
    params.setProtocols(protocols)
    // retain the hostname verification that ConnectionContext.httpsClient(SSLContext) sets up
    params.setEndpointIdentificationAlgorithm("https")
    engine.setSSLParameters(params)
    engine
  }

  private def x509Certificate(trustPemPath: String): X509Certificate = {
    val stream = new FileInputStream(trustPemPath)
    try CertificateFactory.getInstance("X509").generateCertificate(stream).asInstanceOf[X509Certificate]
    finally stream.close()
  }
}
