package com.astronlab.ngenhttplib.http.core;

import com.astronlab.ngenhttplib.http.core.client.InvokerClientConfigObject;
import com.astronlab.ngenhttplib.http.core.misc.InvokerCookieJar;
import okhttp3.OkHttpClient;

/**
 * OkHttp v3 is stateless (doesn't store session cookies) but this Library is built to be stateful.
 * You could use a single instance of HttpInvoker for all of your tasks and threads, and its thread safe.
 * Use InvokerResponse's close() method for preventing potential** memory leaks
 * ** = when "stream response types (string, byte[] responses are auto closed)" are consumed in different thread than InvokerResponse's thread.
 */
public class HttpInvoker {
    private final static OkHttpClient SHARED_PARENT_CLIENT = new OkHttpClient();
    private final OkHttpClient httpClient;

    public HttpInvoker() {
        httpClient = reNewCookieState(SHARED_PARENT_CLIENT);
    }

    private HttpInvoker(OkHttpClient httpClient) {
        //used for cloning
        this.httpClient = httpClient.newBuilder().build();
    }

    static OkHttpClient reNewCookieState(OkHttpClient clientToUse) {
        return clientToUse.newBuilder().cookieJar(new InvokerCookieJar()).build();
    }

    public HttpInvoker reNewClientCookieState() {
        ((InvokerCookieJar) httpClient.cookieJar()).removeAllCookies();

        return this;
    }

    public HttpInvoker getInvokerClone() {
        return new HttpInvoker(httpClient);
    }

    /**
     * Execute a simple sync-GET call to fetch string data.
     * Efficient for simple direct use-cases.
     */
    public String getStringData(String httpUrl) throws Exception {
        return init(httpUrl).execute().getStringData();
    }

    public InvokerRequest init(String httpUrl) {
        return new InvokerRequest(httpUrl, httpClient);
    }

    /**
     * This method could be used if you want to configure the client before setting url and use the same client config for other requests.
     * However, you could also configure the client after init call but those configuration will only be applied for that specific call.
     * IE.
     * InvokerClientConfig config = new HttpInvoker().config();
     * config.init("google.com").etc
     * config.init("yahoo.com").etc
     * <p>
     * Alternatively, if you want to use both of same "client" and "request" config for other requests then use:
     * InvokerRequest config = new HttpInvoker().config().etc.init("google.com").etc;
     * config.cloneInit("yahoo.com").etc
     */
    public InvokerClientConfig config() {
        return new InvokerClientConfig();
    }

    public class InvokerClientConfig extends InvokerClientConfigObject<InvokerClientConfig> {
        InvokerClientConfig() {
            super(httpClient);
            super.setReturnType(this);
        }

        public InvokerRequest init(String httpUrl) {
            return new InvokerRequest(httpUrl, getUpdatedClient());
        }

        public HttpInvoker getUpdatedInvoker() {
            return new HttpInvoker(getUpdatedClient());
        }
    }

    //TODO:
    // - add cache support/location
    // - concurrent thread no for same host
    // - download progress listener in InvokerResponse
    // - add Lambda in Buffered sink methods
    // - remove deprecated method from acceptAllSSLCerts method
}