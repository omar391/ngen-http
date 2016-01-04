package com.astronlab.ngenhttplib.http;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

public class HttpInvoker {

    private String httpUrl;
    private OkHttpClient httpClient;
    private Response httpResponse;
    private Request.Builder httpRequestBuilder;

    public HttpInvoker() {
        httpClient = new OkHttpClient();
    }

    public HttpInvoker(String url) throws Exception {
        this();
        setUrl(url);
    }

    public Response getHttpResponse() throws Exception {
        System.out.println("Invoking: " + httpUrl);
        httpResponse = httpClient.newCall(getRequestBuilder().build()).execute();

        return httpResponse;
    }

    public String getUrl() throws Exception {
        if (httpUrl == null) {
            throw new Exception("Http Url is not initialized.");
        }

        return httpUrl;
    }

    public HttpInvoker setUrl(String httpUrl) {
        this.httpUrl = httpUrl;

        if (httpRequestBuilder != null) {
            httpRequestBuilder.url(httpUrl);
        } else {
            httpRequestBuilder = new Request.Builder().url(httpUrl);
        }

        return this;
    }

    private Request.Builder getRequestBuilder() throws Exception {
        if (httpRequestBuilder == null) {
            throw new Exception("Please set an URL for your request, first!");
        }

        return httpRequestBuilder;
    }

    public HttpInvoker removeProxy() {
        httpClient.setProxy(Proxy.NO_PROXY);

        return this;
    }

    public HttpInvoker setProxy(String proxyIp, int proxyPort, Proxy.Type proxyType) {
        removeProxy();
        InetSocketAddress socketAddress = new InetSocketAddress(proxyIp, proxyPort);
        Proxy proxy = new Proxy(proxyType, socketAddress);
        httpClient.setProxy(proxy);

        return this;
    }

    public HttpInvoker setConnectionTimeOut(int milliseconds) {
        httpClient.setConnectTimeout(milliseconds, TimeUnit.MILLISECONDS);

        return this;
    }

    public HttpInvoker setSocketTimeOut(int milliseconds) {
        httpClient.setReadTimeout(milliseconds, TimeUnit.SECONDS);

        return this;
    }

    public HttpInvoker post(MultipartEntityBuilder multipartEntityBuilder) throws Exception {
        getRequestBuilder().post(multipartEntityBuilder.build());

        return this;
    }

    public HttpInvoker addPresetRequestHeadersSet() throws Exception {
        getRequestBuilder().addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        getRequestBuilder().addHeader("Accept-Language", "en-US,en;q=0.8");
        getRequestBuilder().addHeader("Connection", "keep-alive");
        getRequestBuilder().addHeader("User-Agent", "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.70 Safari/537.36");

        return this;
    }

    public HttpInvoker addRequestHeader(String key, String value) throws Exception {
        getRequestBuilder().addHeader(key, value);

        return this;
    }

    public HttpInvoker enableRedirection() {
        httpClient.setFollowSslRedirects(true);

        return this;
    }

    public String getStringData() throws Exception {
        return getHttpResponse().body().string();
    }

    public InputStream getData() throws Exception {
        return getHttpResponse().body().byteStream();
    }

    public HttpInvoker downloadDataToFile(String fileName) throws Exception {
        File file = new File(fileName);
        FileOutputStream fop = new FileOutputStream(file);
        if (!file.exists()) {
            file.createNewFile();
        }
        fop.write(getHttpResponse().body().bytes());
        fop.flush();
        fop.close();

        return this;
    }

    public HttpInvoker setTag(String connectionName) throws Exception {
        getRequestBuilder().tag(connectionName);

        return this;
    }

    public HttpInvoker abortConnection(String connectionName) {
        httpClient.cancel(connectionName);

        return this;
    }

    public HttpInvoker closeNReleaseResource() throws IOException {
        if (httpResponse != null) {
            httpResponse.body().close();
        }

        return this;
    }

//    class MySchemeSocketFactory implements SocketFactory {
//
//        @Override
//        public Socket createSocket(final HttpParams params) throws IOException {
//            if (params == null) {
//                throw new IllegalArgumentException(
//                        "HTTP parameters may not be null");
//            }
//            String proxyHost = (String) params.getParameter("socks.host");
//            Integer proxyPort = (Integer) params.getParameter("socks.port");
//
//            InetSocketAddress socksaddr = new InetSocketAddress(proxyHost,
//                    proxyPort);
//            Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
//            return new Socket(proxy);
//        }
//
//        @Override
//        public Socket connectSocket(final Socket socket,
//                                    final InetSocketAddress remoteAddress,
//                                    final InetSocketAddress localAddress, final HttpParams params)
//                throws IOException, UnknownHostException,
//                ConnectTimeoutException {
//            if (remoteAddress == null) {
//                throw new IllegalArgumentException(
//                        "Remote address may not be null");
//            }
//            if (params == null) {
//                throw new IllegalArgumentException(
//                        "HTTP parameters may not be null");
//            }
//            Socket sock;
//            if (socket != null) {
//                sock = socket;
//            } else {
//                sock = createSocket(params);
//            }
//            if (localAddress != null) {
//                sock.setReuseAddress(HttpConnectionParams
//                        .getSoReuseaddr(params));
//                sock.bind(localAddress);
//            }
//            int timeout = HttpConnectionParams.getConnectionTimeout(params);
//            try {
//                sock.connect(remoteAddress, timeout);
//            } catch (SocketTimeoutException ex) {
//                throw new ConnectTimeoutException("Connect to "
//                        + remoteAddress.getHostName() + "/"
//                        + remoteAddress.getAddress() + " timed out");
//            }
//            return sock;
//        }
//
//        @Override
//        public boolean isSecure(final Socket sock)
//                throws IllegalArgumentException {
//            return false;
//        }
//
//        @Override
//        public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
//            return null;
//        }
//
//        @Override
//        public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
//            return null;
//        }
//
//        @Override
//        public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
//            return null;
//        }
//
//        @Override
//        public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
//            return null;
//        }
//    }
}
