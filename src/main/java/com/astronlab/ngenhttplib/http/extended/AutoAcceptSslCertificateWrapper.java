package com.astronlab.ngenhttplib.http.extended;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;

public class AutoAcceptSslCertificateWrapper {

	public HttpClient wrapClient(HttpClient base) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			ClientConnectionManager ccm = base.getConnectionManager();

			X509TrustManager tm = getForcedTrustManager();
			X509HostnameVerifier verifier = getForcedHostnameVerifier();

			ctx.init(null, new TrustManager[] { tm }, null);

			// SSLSocketFactory "will be" deprecated, use
			// SSLConnectionSocketFactory instead, Update it accordingly when
			// http-client version is updated
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(verifier); // Deprecation can be removed via
			// updating client lib
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, 443)); // Deprecation can be
			// removed via updating
			// client lib
			return new DefaultHttpClient(ccm, base.getParams());
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private X509TrustManager getForcedTrustManager() {
		X509TrustManager tm = new X509TrustManager() {

			public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
			}

			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};

		return tm;
	}

	private X509HostnameVerifier getForcedHostnameVerifier() {
		X509HostnameVerifier verifier = new X509HostnameVerifier() {

			@Override
			public void verify(String string, X509Certificate xc) throws SSLException {
			}

			@Override
			public void verify(String string, String[] strings, String[] strings1) throws SSLException {
			}

			@Override
			public boolean verify(String arg0, SSLSession arg1) {
				return true;
			}

			@Override
			public void verify(String arg0, SSLSocket arg1) throws IOException {
			}
		};

		return verifier;
	}
}