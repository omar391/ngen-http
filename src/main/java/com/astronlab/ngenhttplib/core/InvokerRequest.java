package com.astronlab.ngenhttplib.core;

import com.astronlab.ngenhttplib.core.client.InvokerClientConfigObject;
import com.astronlab.ngenhttplib.core.impl.AsyncResponseHandler;
import com.astronlab.ngenhttplib.core.impl.SyncResponseHandler;
import com.astronlab.ngenhttplib.core.request.InvokerRequestBody;
import com.astronlab.ngenhttplib.core.response.StringDataHandler;
import com.astronlab.ngenhttplib.utils.UserAgentSwitcher;
import org.jetbrains.annotations.NotNull;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class InvokerRequest extends InvokerClientConfigObject<InvokerRequest> implements StringDataHandler {
    private final String httpUrl;
    private final Request.Builder requestConfigBuilder;
    private Call requestCall;
    public Logger logger = Logger.getLogger(InvokerRequest.class.getName());

    InvokerRequest(String httpUrl, OkHttpClient updatedClient) {
        this(httpUrl, updatedClient, null);
    }

    private InvokerRequest(String httpUrl, OkHttpClient updatedClient, Request.Builder rConfigBuilder) {
        super(updatedClient, InvokerRequest.class);
        this.httpUrl = httpUrl;

        if (rConfigBuilder == null) {
            this.requestConfigBuilder = new Request.Builder().url(httpUrl);
            addPresetHeaders();
        } else {
            this.requestConfigBuilder = rConfigBuilder.tag(null).url(httpUrl).get();
        }
    }

    public String getUrl() {
        return httpUrl;
    }

    public InvokerRequest setLogger(@NotNull Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * In case, you want to reuse the request instance with same config but different url.
     * Keeps previous headers headers and all client configs
     * Only changes: method type to GET and tag into null.
     **/
    public InvokerRequest fork(String httpUrl, boolean reNewClientState) {
        OkHttpClient clientToUse;
        if (!reNewClientState) {
            clientToUse = getUpdatedClient();
        } else {
            clientToUse = reNewCookieState().getUpdatedClient();
        }

        return new InvokerRequest(httpUrl, clientToUse, requestConfigBuilder.build().newBuilder());
    }

    public InvokerRequest fork(String httpUrl) {
        return fork(httpUrl, false);
    }

    private Call prepareCaller() {
        Request request = requestConfigBuilder.build();
        return requestCall = getUpdatedClient().newCall(request);
    }

    /**
     * Recommended. Execute connections in threads.
     * Auto quit sources. Memory leak safe in the case of "same-thread" consumption!.
     */
    public void executeAsync(@NotNull AsyncResponseHandler callback) {
        callback.logger = logger;
        prepareCaller().enqueue(callback);
    }

    /**
     * Recommended.
     * Auto quit sources. Memory leak safe in the case of "same-thread" consumption!.
     * Use this method for sync "stream" calls instead of executeAsync(AsyncResponseHandler) call.
     */
    public InvokerResponse execute(SyncResponseHandler handler) throws Exception {
        InvokerResponse response = null;

        Call call = prepareCaller();
        try (InvokerResponse invokerResponse = response = new InvokerResponse(call)) {
            logger.debug((invokerResponse.getResponseObj().cacheResponse() != null ? (invokerResponse.getResponseObj().networkResponse() != null ? "Invoking(+cached): " : "From cache: ") : "Invoking: ") + response.getCallObj().request());
            handler.handleResponse(invokerResponse);
        } catch (Exception e) {
            logger.debug("Connection failure: " + call.request());
            handler.onFailure(response, e);
        }

        return response;
    }

    /**
     * Major use-case: execute the call with just header and/or related info and then quit the connection without consuming the data.
     * It just create appropriate session data where applicable.
     */
    public InvokerResponse executeWithoutConsuming() throws Exception {
        return execute(r -> {
        });
    }

    public void abortConnection() {
        if (requestCall != null) {
            requestCall.cancel();
        }
    }

    //TODO: return new object on the following method which uses requestConfigBuilder objects to safegurd against data-race
    //TODO: detct, check and store unique thread name and if thread names are diff then use clone
    public InvokerRequest post(InvokerRequestBody requestBodyConfig) throws IOException {
        requestConfigBuilder.post(requestBodyConfig.build());

        return this;
    }

    private void addPresetHeaders() {
        requestConfigBuilder.addHeader("Accept",
                "*/*");
        requestConfigBuilder.addHeader(
                "Accept-Language", "en-US,en;q=0.9,bn;q=0.8");
        requestConfigBuilder.addHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36");
    }

    public InvokerRequest randomizeUserAgentHeader() {
        requestConfigBuilder.header("User-Agent", UserAgentSwitcher.getNextUA());
        return this;
    }

    public InvokerRequest removeRequestHeader(String key) {
        requestConfigBuilder.removeHeader(key);

        return this;
    }

    /**
     * Overwrite the previous name-value pair if exists
     */
    public InvokerRequest setRequestHeader(String key, final String value) {
        requestConfigBuilder.header(key, value);

        return this;
    }

    public InvokerRequest setRequestHeaders(HashMap<String, String> headersMap) {
        for (Map.Entry<String, String> e : headersMap.entrySet()) {
            requestConfigBuilder.header(e.getKey(), e.getValue());
        }

        return this;
    }

    public InvokerRequest setCacheMaxStaleDays(int days) {
        if (days < 0) {
            days = 0;
        }
        requestConfigBuilder.cacheControl(new CacheControl.Builder().maxStale(days, TimeUnit.DAYS).build());

        return this;
    }

    public InvokerRequest forceNetwork() {
        requestConfigBuilder.cacheControl(CacheControl.FORCE_NETWORK);

        return this;
    }

    public InvokerRequest prioritizeCache() {
        requestConfigBuilder.cacheControl(new CacheControl.Builder().maxStale(Integer.MAX_VALUE, TimeUnit.SECONDS)
                .build());

        return this;
    }

    public InvokerRequest forceCache() {
        requestConfigBuilder.cacheControl(CacheControl.FORCE_CACHE);

        return this;
    }

    @Override
    public String getStringData() throws Exception {
        final String[] data = new String[1];
        execute(r -> data[0] = r.getStringData());

        return data[0];
    }

    /**
     * If this method is used then {@code update} method will not be available in the invoker callObj chain.
     * But you will have to do as such:
     * config = config().getExtraRequestConfigs().{other config methods}
     */
    public Request.Builder getExtraRequestConfigs() {
        return requestConfigBuilder;
    }
}

//TODO: - check fork method whether it works or not