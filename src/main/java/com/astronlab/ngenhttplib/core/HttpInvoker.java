package com.astronlab.ngenhttplib.core;

import com.astronlab.ngenhttplib.core.client.InvokerClientConfigObject;
import com.astronlab.ngenhttplib.core.misc.InvokerCookieJar;
import okhttp3.Cache;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * OkHttp v3 is stateless (doesn't store session cookies) but this Library is built to be stateful.
 * You could use a single instance of HttpInvoker for all of your tasks and threads, and its thread safe.
 * Use InvokerResponse's quit() method for preventing potential** memory leaks
 * ** = when "stream response types (string, byte[] responses are auto closed)" are consumed in different thread than InvokerResponse's thread.
 */
//Checked 4/1/2019: thread safe
public class HttpInvoker {
    private final OkHttpClient httpClient;
    static final Logger logger = Logger.getLogger(HttpInvoker.class);

    public HttpInvoker() {
        httpClient = new OkHttpClient().newBuilder().cookieJar(new InvokerCookieJar()).build();
    }

    private HttpInvoker(OkHttpClient httpClient) {
        //used for cloning
        this.httpClient = httpClient;
    }

    public boolean isCacheActive() {
        return httpClient.cache() != null;
    }

    //clone the client with its state
    public HttpInvoker fork() {
        return new HttpInvoker(httpClient);
    }

    public InvokerRequest init(String httpUrl) {
        return new InvokerRequest(httpUrl, httpClient);
    }

    public void close() {
        if (httpClient.cache() != null) {
            try {
                Objects.requireNonNull(httpClient.cache()).close();
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }
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
     * config.cloneCall("yahoo.com").etc
     */
    public InvokerClientConfig config() {
        return new InvokerClientConfig();
    }

    public class InvokerClientConfig extends InvokerClientConfigObject<InvokerClientConfig> {
        private InvokerClientConfig() {
            super(httpClient, InvokerClientConfig.class);
        }

        public InvokerRequest init(String httpUrl) {
            return new InvokerRequest(httpUrl, getUpdatedClient());
        }

        public InvokerClientConfig setCache(String cacheFile, long cacheSizeInMb) {
            getExtraClientConfigs().cache(new Cache(new File(cacheFile), cacheSizeInMb * 1024 * 1024));
            return this;
        }

        public InvokerClientConfig setConnectionsAttr(int maxRequests, int maxRequestsPerHost, ExecutorService executorService) {
            Dispatcher dispatcher = new Dispatcher(executorService);
            dispatcher.setMaxRequests(maxRequests < 1 ? dispatcher.getMaxRequests() : maxRequests);
            dispatcher.setMaxRequestsPerHost(maxRequestsPerHost < 1 ? dispatcher.getMaxRequestsPerHost() : maxRequestsPerHost);
            getExtraClientConfigs().dispatcher(dispatcher);
            return this;
        }

        public HttpInvoker build() {
            return new HttpInvoker(getUpdatedClient());
        }
    }

    //TODO:
    // - (check via thread id)check if instance call from multi thread for all the methods are thread safe + if every part (i.e. InvokerRequest ) can be used without data-race (check all class variables)
    // - set concurrent thread no for same host
    // - download progress listener in InvokerResponse
    // - add Lambda in Buffered sink methods
    // - remove deprecated method from acceptAllSSLCerts method
    // - fix proxy authentication
}