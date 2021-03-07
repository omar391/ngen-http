package com.astronlab.ngenhttplib.core;

import com.astronlab.ngenhttplib.core.response.StringDataHandler;
import okhttp3.Call;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class InvokerResponse implements AutoCloseable, StringDataHandler {
    private final Call callObj;
    private final Response responseObj;

    InvokerResponse(Call callObj) throws IOException {
        this.callObj = callObj;
        responseObj = callObj.execute();
    }

    public InvokerResponse(Call callObj, Response responseObj) {
        this.callObj = callObj;
        this.responseObj = responseObj;
    }

    public String getStringData() throws Exception {
        ResponseBody body = responseObj.body();
        return body != null ? body.string() : null;
    }

    /**
     * Warning: Be sure to quit the InvokerResponse instance after using "getData" method (ar any stream methods), from the same thread
     */
    public InputStream getData() {
        ResponseBody body = responseObj.body();
        return body != null ? body.byteStream() : null;
    }

    public Response getResponseObj() {
        return responseObj;
    }

    public Call getCallObj() {
        return callObj;
    }

    public void downloadDataToFile(String fileName) throws Exception {
        File file = new File(fileName);
        FileOutputStream fop = new FileOutputStream(file);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        ResponseBody body = responseObj.body();
        fop.write(body != null ? body.bytes() : new byte[0]);
        fop.flush();
        fop.close();
    }

    public int getResponseCode() {
        return responseObj.code();
    }

    public String getResponseMessage() {
        return responseObj.message();
    }

    /**
     * Close the data stream whether used or not used.
     * Response must be consumed and closed from the same thread for "stream" type responses.
     */
    public void close() {
        responseObj.close();
    }


    //TODO: create a download progress handler similar to upload handler
}