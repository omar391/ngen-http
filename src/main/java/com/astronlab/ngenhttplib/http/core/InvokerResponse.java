package com.astronlab.ngenhttplib.http.core;

import com.astronlab.ngenhttplib.http.core.response.InvokerResponseObject;
import okhttp3.Call;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;

public class InvokerResponse extends InvokerResponseObject implements AutoCloseable {

    InvokerResponse(Call callObj) throws IOException {
        super(callObj);
    }

    public InvokerResponse(Call callObj, Response responseObj) {
        super(callObj, responseObj);
    }

    /**
     * Warning: Be sure to close the InvokerResponse instance after using "getData" method (ar any stream methods), from the same thread
     */
    public InputStream getData() {
        return responseObj.body().byteStream();
    }

    public Response getResponseObj() {
        return responseObj;
    }

    public Call getCallObj() {
        return callObj;
    }

    /**
     * Response must be consumed and closed from the same thread for "stream" type responses.
     */
    @Override
    public void close() {
        responseObj.close();
    }

    //TODO: create a download progress handler similar to upload handler
}