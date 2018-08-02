/*
 * Licensed to Sematext Group, Inc
 *
 * See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Sematext Group, Inc licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sematext.spm.client.sender.bootstrap;

import org.apache.http.conn.ssl.SSLSocketFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class AcceptAllSslCertificatesSocketFactory extends SSLSocketFactory {
  private SSLContext sslContext = SSLContext.getInstance("TLS");

  public AcceptAllSslCertificatesSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
      KeyManagementException, KeyStoreException, UnrecoverableKeyException {
    super(truststore);

    TrustManager tm = new X509TrustManager() {
      public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      }

      public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
      }

      public X509Certificate[] getAcceptedIssuers() {
        return null;
      }
    };

    sslContext.init(null, new TrustManager[] { tm }, null);
  }

  @Override
  public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
    return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
  }

  @Override
  public Socket createSocket() throws IOException {
    return sslContext.getSocketFactory().createSocket();
  }
}
