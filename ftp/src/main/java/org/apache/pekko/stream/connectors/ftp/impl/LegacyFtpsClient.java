/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.pekko.stream.connectors.ftp.impl;

import java.io.IOException;
import java.net.Socket;
import javax.net.ssl.SSLSocket;

import org.apache.commons.net.ftp.*;

/**
 * This class is a workaround for code introduced in commons-net 3.9.0 that breaks
 * FTPS support when HTTP proxies are used.
 * See https://issues.apache.org/jira/browse/NET-718
 * 
 * Derived from https://github.com/apache/commons-net/blob/master/src/main/java/org/apache/commons/net/ftp/FTPSClient.java
 */
final class LegacyFtpsClient extends FTPSClient {

    /** Default secure socket protocol name, i.e. TLS */
    private static final String DEFAULT_PROTOCOL = "TLS";

    /**
     * Constructor for LegacyFtpsClient calls.
     *
     * Sets protocol to {@link #DEFAULT_PROTOCOL} - i.e. TLS
     * @param isImplicit True - Implicit Mode / False - Explicit Mode
     */
    LegacyFtpsClient(final boolean isImplicit) {
        super(DEFAULT_PROTOCOL, isImplicit);
    }

    /**
     * Returns a socket of the data connection. Wrapped as an {@link SSLSocket}, which carries out handshake processing.
     *
     * @param command The textual representation of the FTP command to send.
     * @param arg     The arguments to the FTP command. If this parameter is set to null, then the command is sent with no arguments.
     * @return corresponding to the established data connection. Null is returned if an FTP protocol error is reported at any point during the establishment and
     *         initialization of the connection.
     * @throws IOException If there is any problem with the connection.
     * @see FTPClient#_openDataConnection_(int, String)
     * @since 3.2
     */
    @Override
    protected Socket _openDataConnection_(final String command, final String arg) throws IOException {
        final Socket socket = super._openDataConnection_(command, arg);
        _prepareDataSocket_(socket);
        if (socket instanceof SSLSocket) {
            final SSLSocket sslSocket = (SSLSocket) socket;

            sslSocket.setUseClientMode(isClientMode());
            sslSocket.setEnableSessionCreation(isCreation());

            // server mode
            if (!isClientMode()) {
                sslSocket.setNeedClientAuth(isNeedClientAuth());
                sslSocket.setWantClientAuth(isWantClientAuth());
            }
            if (getSuites() != null) {
                sslSocket.setEnabledCipherSuites(getSuites());
            }
            if (getProtocols() != null) {
                sslSocket.setEnabledProtocols(getProtocols());
            }
            sslSocket.startHandshake();
        }

        return socket;
    }
    
}
