package com.astronlab.ngenhttplib.http;

import com.squareup.okhttp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class HttpInvoker {

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

    public void setUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public Response getHttpResponse() throws Exception {
        System.out.println("Invoking: " + httpUrl);
        httpRequestBuilder = new Request.Builder().url(getUrl());
        httpResponse = httpClient.newCall(httpRequestBuilder.build()).execute();
        return httpResponse;
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

    public RequestBody createMultipartRequestBody(List<RequestBody> partsList, MediaType type) {
        MultipartBuilder multipartBuilder = new MultipartBuilder().type(type);
        for (RequestBody requestBody : partsList) {
            multipartBuilder.addPart(requestBody);
        }
        return multipartBuilder.build();
    }

    public void post(Map<String, String> valueList) {
        MultipartBuilder multipartBuilder = new MultipartBuilder();
        for (Map.Entry<String, String> entry : valueList.entrySet()) {
            multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
        }
        post(multipartBuilder.build());
    }

    public void post(RequestBody requestBody) {
        httpRequestBuilder.post(requestBody);
    }

    public void addPresetRequestHeadersSet() {
        httpRequestBuilder.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        httpRequestBuilder.addHeader("Accept-Language", "en-US,en;q=0.8");
        httpRequestBuilder.addHeader("Connection", "keep-alive");
        httpRequestBuilder.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.70 Safari/537.36");
    }

    public void addReqestHeader(String key, String value) {
        httpRequestBuilder.addHeader(key, value);
    }

    public void enableRedirection() {
        httpClient.setFollowSslRedirects(true);
    }

    public String getStringData() throws Exception {
        return getHttpResponse().body().string();
    }

    public InputStream getData() throws Exception {
        return getHttpResponse().body().byteStream();
    }

    public void downloadDataToFile(String fileName) throws Exception {
        File file = new File(fileName);
        FileOutputStream fop = new FileOutputStream(file);
        if (!file.exists()) {
            file.createNewFile();
        }
        fop.write(getHttpResponse().body().bytes());
        fop.flush();
        fop.close();
    }

    public void setTag(String connectionName){
        httpRequestBuilder.tag(connectionName);
    }

    public void abortConnection(String connectionName) {
        httpClient.cancel(connectionName);
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
