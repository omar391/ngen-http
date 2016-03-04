package com.astronlab.ngenhttplib.http;

import okhttp3.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp v3 is stateless (doesn't store session cookies) but this Library is built to be stateful.
 * As for usage, please call "closeNReleaseResource" method after your task is done.
 */
public class HttpInvoker {
    private OkHttpClient httpClient;
    private Response currentHttpResponse;
    private Call currentRequestCall;

    public HttpInvoker() {
        httpClient = new OkHttpClient.Builder().cookieJar(getCustomCookieJar()).build();
    }

    public HttpInvoker(String url) throws Exception {
        this();
        config().setUrl(url);
    }

    public String getUrl() throws Exception {
        String httpUrl = getCurrentRequestCall().request().url().toString();

        return httpUrl;
    }

    /**
     * This method could be used in isolation where you might want to handle your
     * own request/response (+ with different url) based on already existent settings (proxy, timeout etc)
     *
     * @param httpUrl
     * @return
     */
    public Call getNewRequestCall(String httpUrl) {
        return createRequestCall(new Request.Builder().url(httpUrl).build());
    }

    private Call createRequestCall(Request request) {
        return httpClient.newCall(request);
    }

    private Call getCurrentRequestCall() throws Exception {
        if (currentRequestCall == null) {
            throw new Exception("Please set an URL for your request, first!");
        }

        return currentRequestCall;
    }

    public Response getHttpResponse() throws Exception {
        System.out.println("Invoking: " + getUrl());
        currentHttpResponse = getCurrentRequestCall().execute();

        return currentHttpResponse;
    }

    public Response getCurrentHttpResponse() {
        return currentHttpResponse;
    }

    /**
     * This method is used to set different invoker settings(proxy, connection timeout, etc).
     * But we must call {@code update} method at the end of call chain to update the client else we'll not get updated settings.
     * i.e invoker.config().setTimeOut(...).setProxy(...).update();
     *
     * @return
     */
    public InvokerConfig config() {
        return new InvokerConfig();
    }

    public String getStringData() throws Exception {
        return getHttpResponse().body().string();
    }

    /**
     * Call "closeNReleaseResource" method at the end of this method's usage
     *
     * @return InputStream of network source
     * @throws Exception
     */
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

    public HttpInvoker abortConnection() throws Exception {
        getCurrentRequestCall().cancel();

        return this;
    }

    private CookieJar getCustomCookieJar() {
        CookieJar cookieJar = new CookieJar() {
            //Store format: HashMap<Host, HashMap<Name_Path_Scheme, Cookie>>
            HashMap<String, HashMap<String, Cookie>> storedCookies = new HashMap<>();

            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                String hostKey = getHostKey(url);
                HashMap<String, Cookie> hostCookies = storedCookies.get(hostKey);

                if(hostCookies == null){
                    hostCookies = new HashMap<>();
                    storedCookies.put(hostKey, hostCookies);
                }

                for(Cookie cookie : cookies){
                    hostCookies.put(getCookieKey(cookie), cookie);
                }
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                HashMap<String, Cookie> hostCookies = storedCookies.get(getHostKey(url));
                List<Cookie> cookieList = new ArrayList<>();

                if(hostCookies != null){
                    Collection<Cookie> cookieCollection = hostCookies.values();
                    for(Cookie cookie : cookieCollection){
                        if(isPathMatch(url, cookie)){
                            cookieList.add(cookie);
                        }
                    }
                }

                return cookieList;
            }

            private String getHostKey(HttpUrl httpUrl) {
                return httpUrl.host().replaceAll(".*?([^\\s\\n:\\./]+\\.[^\\s\\n\\d:\\./]++|[\\d:\\.]++)$", "$1");
            }

            private String getCookieKey(Cookie cookie){
                return cookie.name()+"_"+cookie.path()+"_"+cookie.secure();
            }

            private boolean isPathMatch(HttpUrl url, Cookie cookie) {
                String urlPath = url.encodedPath();
                String cookiePath = cookie.path();

                if (urlPath.equals(cookiePath)) {
                    return true; // As in '/foo' matching '/foo'.
                }

                if (urlPath.startsWith(cookiePath)) {
                    if (cookiePath.endsWith("/")) return true; // As in '/' matching '/foo'.
                    if (urlPath.charAt(cookiePath.length()) == '/') return true; // As in '/foo' matching '/foo/bar'.
                }

                return false;
            }
        };

        return cookieJar;
    }

    public HttpInvoker closeNReleaseResource() throws IOException {
        if (currentHttpResponse != null) {
            currentHttpResponse.body().close();
        }

        //Remove all references for faster GC
        httpClient = null;
        currentHttpResponse = null;
        currentRequestCall = null;

        return this;
    }

    public class InvokerConfig {
        private OkHttpClient.Builder clientConfigBuilder;
        private Request.Builder requestConfigBuilder;

        private OkHttpClient.Builder getClientConfig() {
            if (clientConfigBuilder == null) {
                clientConfigBuilder = httpClient.newBuilder();
            }

            return clientConfigBuilder;
        }

        private Request.Builder getRequestConfig() throws Exception {
            if (requestConfigBuilder == null) {
                requestConfigBuilder = getCurrentRequestCall().request().newBuilder();
            }

            return requestConfigBuilder;
        }

        /**
         * If url is changed then headers/methods type/tags will be reset.
         *
         * @param httpUrl
         * @return
         */
        public InvokerConfig setUrl(String httpUrl) {
            if (currentRequestCall != null && currentRequestCall.request().url().host().equals(HttpUrl.parse(httpUrl).host())) {
                //preserve the last request's headers if the url's host is same
                requestConfigBuilder = currentRequestCall.request().newBuilder().url(httpUrl);
            } else {
                currentRequestCall = getNewRequestCall(httpUrl);
            }

            return this;
        }

        public InvokerConfig post(RequestEntityBuilder postEntityBuilder) throws Exception {
            getRequestConfig().post(postEntityBuilder.build());

            return this;
        }

        public InvokerConfig addPresetRequestHeadersSet() throws Exception {
            getRequestConfig().addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            getRequestConfig().addHeader("Accept-Language", "en-US,en;q=0.8");
            getRequestConfig().addHeader("User-Agent", "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.70 Safari/537.36");

            return this;
        }

        public InvokerConfig addRequestHeader(String key, String value) throws Exception {
            getRequestConfig().addHeader(key, value);

            return this;
        }

        public InvokerConfig setRedirection(boolean redirection) {
            getClientConfig().followRedirects(redirection);
            getClientConfig().followSslRedirects(redirection);

            return this;
        }

        public InvokerConfig removeProxy() {
            getClientConfig().proxy(Proxy.NO_PROXY);

            return this;
        }

        public InvokerConfig setProxy(String proxyIp, int proxyPort, Proxy.Type proxyType) {
            InetSocketAddress socketAddress = new InetSocketAddress(proxyIp, proxyPort);
            Proxy proxy = new Proxy(proxyType, socketAddress);
            getClientConfig().proxy(proxy);

            return this;
        }

        public InvokerConfig setConnectionTimeOut(int milliseconds) {
            getClientConfig().connectTimeout(milliseconds, TimeUnit.MILLISECONDS);

            return this;
        }

        public InvokerConfig setSocketTimeOut(int milliseconds) {
            getClientConfig().readTimeout(milliseconds, TimeUnit.MILLISECONDS);

            return this;
        }

        /**
         * If this method is used then {@code update} method will not be available in the invoker call chain.
         * But you will have to do as such:
         * config = config().getExtraClientConfigs().{other config methods}
         * invoker = config.update();
         *
         * @return an exit node of OkHttpClient.Builder class
         */
        public OkHttpClient.Builder getExtraClientConfigs() {
            return getClientConfig();
        }

        /**
         * If this method is used then {@code update} method will not be available in the invoker call chain.
         * But you will have to do as such:
         * config = config().getExtraRequestConfigs().{other config methods}
         * invoker = config.update();
         *
         * @return an exit node of Request.Builder class
         * @throws Exception
         */
        public Request.Builder getExtraRequestConfigs() throws Exception {
            return getRequestConfig();
        }

        public HttpInvoker update() {
            //Update client first
            if (clientConfigBuilder != null) {
                httpClient = clientConfigBuilder.build();
                clientConfigBuilder = null;
            }

            //Now update the request call
            if (requestConfigBuilder != null) {
                currentRequestCall = createRequestCall(requestConfigBuilder.build());
                requestConfigBuilder = null;
            }

            return HttpInvoker.this;
        }
    }
}