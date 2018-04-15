package com.astronlab.ngenhttplib.http.core.impl;

import com.astronlab.ngenhttplib.http.core.InvokerResponse;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

public interface AsyncResponseHandler extends Callback {

    @Override
    default void onFailure(Call call, IOException e) {
        e.printStackTrace();
        //override this method if you need to handle exceptions in your own ways.
    }

    @Override
    default void onResponse(Call call, Response response) {
        try (InvokerResponse invokerResponse = new InvokerResponse(call, response)) {
            onResponseHandler(invokerResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void onResponseHandler(InvokerResponse response) throws Exception;
}