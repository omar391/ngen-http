package com.astronlab.ngenhttplib.http.core.impl;

import com.astronlab.ngenhttplib.http.core.InvokerResponse;

import java.io.IOException;

public interface SyncResponseHandler {
    void handleResponse(InvokerResponse response) throws Exception;
}
