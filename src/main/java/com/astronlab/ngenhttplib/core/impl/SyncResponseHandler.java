package com.astronlab.ngenhttplib.core.impl;

import com.astronlab.ngenhttplib.core.InvokerResponse;

public interface SyncResponseHandler {
    default void onFailure(InvokerResponse response, Exception e) throws Exception {
        throw e;
        //override this method if you need to handle exceptions in your own ways.
    }

    void handleResponse(InvokerResponse response) throws Exception;
}