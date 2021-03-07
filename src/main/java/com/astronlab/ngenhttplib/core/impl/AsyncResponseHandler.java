package com.astronlab.ngenhttplib.core.impl;

import com.astronlab.ngenhttplib.core.InvokerResponse;
import org.jetbrains.annotations.NotNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import org.apache.log4j.Logger;

import java.io.IOException;

public abstract class AsyncResponseHandler implements Callback {
    public Logger logger;

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        logResponse(call, null);
        //override this method if you need to handle exceptions in your own ways.
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) {
        logResponse(call, response);
        try (InvokerResponse invokerResponse = new InvokerResponse(call, response)) {
            onResponseHandler(invokerResponse);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    abstract void onResponseHandler(InvokerResponse response) throws Exception;

    void logResponse(@NotNull Call call, Response response) {
        if (response != null) {
            logger.debug((response.cacheResponse() != null ? (response.networkResponse() != null ? "Invoking(+cached): " : "From cache: ") : "Invoking: ") + call.request());
        } else {
            logger.debug("Connection failure: " + call.request());
        }
    }
}