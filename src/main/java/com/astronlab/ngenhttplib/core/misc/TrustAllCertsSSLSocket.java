package com.astronlab.ngenhttplib.core.misc;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class TrustAllCertsSSLSocket {
    private final SSLContext sslContext;

    public TrustAllCertsSSLSocket()
            throws NoSuchAlgorithmException, KeyManagementException {
        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new TrustAllCertsManager()},
                new SecureRandom());
    }

    public SSLSocketFactory getSocketFactory() {
        return sslContext.getSocketFactory();
    }

    public HostnameVerifier getHostNameVerifier() {
        return new HostnameVerifier() {

            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        };
    }

    public static class TrustAllCertsManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(
                X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public void checkServerTrusted(
                X509Certificate[] x509Certificates, String s) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}