package com.astronlab.ngenhttplib.http.core;

import com.astronlab.ngenhttplib.http.core.client.InvokerClientConfigObject;
import com.astronlab.ngenhttplib.http.core.impl.AsyncResponseHandler;
import com.astronlab.ngenhttplib.http.core.impl.SyncResponseHandler;
import com.astronlab.ngenhttplib.http.core.request.InvokerRequestBody;
import com.astronlab.ngenhttplib.http.core.response.InvokerResponseObject;
import com.astronlab.ngenhttplib.http.utils.UserAgentSwitcher;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;

public class InvokerRequest extends InvokerClientConfigObject<InvokerRequest> {
    private final String httpUrl;
    private final Request.Builder requestConfigBuilder;
    private Call requestCall;

    InvokerRequest(String httpUrl, OkHttpClient updatedClient) {
        this(httpUrl, updatedClient, null);
    }

    private InvokerRequest(String httpUrl, OkHttpClient updatedClient, Request.Builder requestConfigBuilder) {
        super(updatedClient);
        super.setReturnType(this);
        this.httpUrl = httpUrl;

        if (requestConfigBuilder == null) {
            this.requestConfigBuilder = new Request.Builder().url(httpUrl);
            addPresetHeaders();

        } else {
            this.requestConfigBuilder = requestConfigBuilder.tag(null).url(httpUrl).get();
        }
    }

    public String getUrl() {
        return httpUrl;
    }

    /**
     * In case, you want to reuse the request instance with same config but different url.
     * Keeps previous headers headers and all client configs
     * Only changes: method type to GET and tag into null.
     **/
    public InvokerRequest cloneInit(String httpUrl, boolean reNewClientState) {
        OkHttpClient clientToUse;
        if (!reNewClientState) {
            clientToUse = getUpdatedClient();
        } else {
            clientToUse = HttpInvoker.reNewCookieState(getUpdatedClient());
        }

        return new InvokerRequest(httpUrl, clientToUse, requestConfigBuilder.build().newBuilder());
    }

    public InvokerRequest cloneInit(String httpUrl) {
        return cloneInit(httpUrl, false);
    }

    private Call prepareCaller() {
        Request request = requestConfigBuilder.build();
        System.out.println("Invoking: " + request);

        return requestCall = getUpdatedClient().newCall(request);
    }

    /**
     * Recommended. Execute connections in threads.
     * Auto close sources. Memory leak safe in the case of "same-thread" consumption!.
     */
    public void execute(AsyncResponseHandler callback) throws Exception {
        if (callback != null) {
            prepareCaller().enqueue(callback);
        } else {
            throw new Exception("Callback must not be null!");
        }
    }

    /**
     * Recommended.
     * Auto close sources. Memory leak safe in the case of "same-thread" consumption!.
     * Use this method for sync "stream" calls instead of raw execute() call.
     */
    public void execute(SyncResponseHandler handler) {
        try (InvokerResponse invokerResponse = new InvokerResponse(prepareCaller())) {
            handler.handleResponse(invokerResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Warning: InvokerResponse object must be closed after use, from the same thread
     */
    public InvokerResponseObject execute() throws Exception {
        return new InvokerResponseObject(prepareCaller());
    }

    public void abortConnection() {
        if (requestCall != null) {
            requestCall.cancel();
        }
    }

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
    public InvokerRequest setRequestHeader(String key, String value) {
        requestConfigBuilder.header(key, value);

        return this;
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