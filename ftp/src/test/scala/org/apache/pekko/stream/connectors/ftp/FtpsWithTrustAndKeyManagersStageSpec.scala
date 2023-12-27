/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.ftp

import org.apache.pekko
import pekko.stream.IOResult
import pekko.stream.scaladsl.{ Sink, Source }
import pekko.util.ByteString
import pekko.{ Done, NotUsed }
import org.mockito.ArgumentMatchers.{ any, anyString }
import org.mockito.Mockito.{ atLeastOnce, doNothing, verify }
import org.scalatestplus.mockito.MockitoSugar

import java.net.{ InetAddress, Socket }
import java.security.cert.X509Certificate
import javax.net.ssl.{ X509ExtendedKeyManager, X509ExtendedTrustManager }
import scala.concurrent.Future

class FtpsWithTrustAndKeyManagersStageSpec extends BaseFtpsSpec with CommonFtpStageSpec with MockitoSugar {

  // The implementation of X509ExtendedTrustManager and X509ExtendedKeyManager is final so
  // its not possible to put a Mockito spy on it, instead lets just mock the classes and the
  // checkServerTrusted method which is executed only when trustManager/keyManager is setup in FtpsSettings

  val keyManager: X509ExtendedKeyManager = mock[X509ExtendedKeyManager]
  val trustManager: X509ExtendedTrustManager = mock[X509ExtendedTrustManager]

  doNothing().when(trustManager).checkServerTrusted(any(classOf[Array[X509Certificate]]), anyString,
    any(classOf[Socket]))

  override val settings =
    FtpsSettings(
      InetAddress.getByName(HOSTNAME)).withPort(PORT)
      .withCredentials(CREDENTIALS)
      .withBinary(true)
      .withPassiveMode(true)
      .withTrustManager(trustManager)
      .withKeyManager(keyManager)

  private def verifyServerCheckCertificate(): Unit =
    verify(trustManager, atLeastOnce()).checkServerTrusted(any(classOf[Array[X509Certificate]]), anyString,
      any(classOf[Socket]))

  private def verifyAfterStream[O, Mat](source: Source[O, Mat]): Source[O, Mat] =
    source.map { result =>
      verifyServerCheckCertificate()
      result
    }

  private def verifyAfterStream[I, Mat](sink: Sink[I, Mat]): Sink[I, Mat] =
    sink.mapMaterializedValue { result =>
      verifyServerCheckCertificate()
      result
    }

  override protected def listFiles(basePath: String): Source[FtpFile, NotUsed] =
    verifyAfterStream(super.listFiles(basePath))

  override protected def listFilesWithFilter(basePath: String, branchSelector: FtpFile => Boolean,
      emitTraversedDirectories: Boolean): Source[FtpFile, NotUsed] =
    verifyAfterStream(super.listFilesWithFilter(basePath, branchSelector, emitTraversedDirectories))

  override protected def retrieveFromPath(path: String, fromRoot: Boolean): Source[ByteString, Future[IOResult]] =
    verifyAfterStream(super.retrieveFromPath(path, fromRoot))

  override protected def retrieveFromPathWithOffset(path: String, offset: Long): Source[ByteString, Future[IOResult]] =
    verifyAfterStream(super.retrieveFromPathWithOffset(path, offset))

  override protected def storeToPath(path: String, append: Boolean): Sink[ByteString, Future[IOResult]] =
    verifyAfterStream(super.storeToPath(path, append))

  override protected def remove(): Sink[FtpFile, Future[IOResult]] =
    verifyAfterStream(super.remove())

  override protected def move(destinationPath: FtpFile => String): Sink[FtpFile, Future[IOResult]] =
    verifyAfterStream(super.move(destinationPath))

  override protected def mkdir(basePath: String, name: String): Source[Done, NotUsed] =
    verifyAfterStream(super.mkdir(basePath, name))

}
