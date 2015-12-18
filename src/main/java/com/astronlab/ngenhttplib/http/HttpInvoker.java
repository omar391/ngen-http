package com.astronlab.ngenhttplib.http;

import com.astronlab.ngenhttplib.http.extended.AutoAcceptSslCertificateWrapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public final class HttpInvoker {

    private String httpUrl;
    private OkHttpClient httpClient;
    private Request httpRequest;

    public HttpInvoker() {
        httpClient = new OkHttpClient();
    }

    public HttpInvoker(String url) throws Exception {
        this();
        setUrl(url);
    }

    public void setUrl(String httpUrl) {
        this.httpUrl = httpUrl;
        httpRequest = new Request.Builder()
                .url(httpUrl)
                .build();
    }

    public Response getHttpResponse() throws IOException {
        System.out.println("Invoking: " + httpUrl);
        return httpClient.newCall(httpRequest).execute();
    }

    public void forceAcceptSslCertificate() {
        AutoAcceptSslCertificateWrapper cert = new AutoAcceptSslCertificateWrapper();
        httpClient = (OkHttpClient) cert.wrapClient(httpClient);
    }

    public String getUrl() throws Exception {
        if (httpUrl == null) {
            throw new Exception("Http Url is not initialized.");
        }
        return httpUrl;
    }

    public void removeProxy() {
        httpClient.setProxy(Proxy.NO_PROXY);
    }

    public void setProxy(String proxyIp, int proxyPort, Proxy.Type proxyType) {
        removeProxy();
        InetSocketAddress socketAddress = new InetSocketAddress(proxyIp, proxyPort);
        Proxy proxy = new Proxy(proxyType, socketAddress);
        httpClient.setProxy(proxy);
    }

    public void setConnectionTimeOut(int milisec) {
        httpClient.setConnectTimeout(milisec, TimeUnit.MILLISECONDS);
    }

    public void setSocketTimeOut(int milisec) {
        httpClient.setReadTimeout(milisec, TimeUnit.SECONDS);
    }

    public void setPostParams(MultipartEntity mEntity) throws Exception {
        HttpPost httpPost = new HttpPost(getUrl());
        httpPost.setEntity(mEntity);
        httpRequest = httpPost;
    }

    public void addPresetHeaderSet() {
        httpRequest
                .setHeader("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        httpRequest.setHeader("Accept-Language", "en-US,en;q=0.8");
        httpRequest.setHeader("Connection", "keep-alive");
        httpRequest
                .setHeader(
                        "User-Agent",
                        "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.70 Safari/537.36");
    }

    public void addReqestHeader(String key, String value) {
        httpRequest.setHeader(key, value);
    }

    public DefaultHttpClient getClient() {
        return httpClient;
    }

    public void enableRedirection() {
        httpClient.setRedirectStrategy(new LaxRedirectStrategy());
    }

    public String getStringData() throws IOException, Exception {
        HttpResponse hResponse = getHttpResponse();
        return EntityUtils.toString(hResponse.getEntity(), "UTF-8");
    }

    public InputStream getData() throws IOException, Exception {
        return getHttpResponse().getEntity().getContent();
    }

    public void downloadDataToFile(String fileName) throws FileNotFoundException, Exception {
        File file = new File(fileName);
        FileOutputStream fop = new FileOutputStream(file);
        if (!file.exists()) {
            file.createNewFile();
        }
        getHttpResponse().getEntity().writeTo(fop);
        fop.flush();
        fop.close();
    }

    public HashMap<String, String> getHeaders() throws Exception {
        HashMap<String, String> hList = new HashMap<String, String>();
        Header[] header = getHttpResponse().getAllHeaders();
        for (Header h : header) {
            hList.put(h.getName(), h.getValue());
        }
        return hList;
    }

    public int getStatusCode() throws Exception {
        return this.getHttpResponse().getStatusLine().getStatusCode();
    }

    public void releaseConnection() {
        httpRequest.abort();
    }

    public void close() {
        try {
            if (!httpRequest.isAborted()) {
                httpRequest.abort();
            }
            httpClient.getConnectionManager().shutdown();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    class MySchemeSocketFactory implements SchemeSocketFactory {

        @Override
        public Socket createSocket(final HttpParams params) throws IOException {
            if (params == null) {
                throw new IllegalArgumentException(
                        "HTTP parameters may not be null");
            }
            String proxyHost = (String) params.getParameter("socks.host");
            Integer proxyPort = (Integer) params.getParameter("socks.port");

            InetSocketAddress socksaddr = new InetSocketAddress(proxyHost,
                    proxyPort);
            Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
            return new Socket(proxy);
        }

        @Override
        public Socket connectSocket(final Socket socket,
                                    final InetSocketAddress remoteAddress,
                                    final InetSocketAddress localAddress, final HttpParams params)
                throws IOException, UnknownHostException,
                ConnectTimeoutException {
            if (remoteAddress == null) {
                throw new IllegalArgumentException(
                        "Remote address may not be null");
            }
            if (params == null) {
                throw new IllegalArgumentException(
                        "HTTP parameters may not be null");
            }
            Socket sock;
            if (socket != null) {
                sock = socket;
            } else {
                sock = createSocket(params);
            }
            if (localAddress != null) {
                sock.setReuseAddress(HttpConnectionParams
                        .getSoReuseaddr(params));
                sock.bind(localAddress);
            }
            int timeout = HttpConnectionParams.getConnectionTimeout(params);
            try {
                sock.connect(remoteAddress, timeout);
            } catch (SocketTimeoutException ex) {
                throw new ConnectTimeoutException("Connect to "
                        + remoteAddress.getHostName() + "/"
                        + remoteAddress.getAddress() + " timed out");
            }
            return sock;
        }

        @Override
        public boolean isSecure(final Socket sock)
                throws IllegalArgumentException {
            return false;
        }
    }
}
