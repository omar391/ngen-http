package com.astronlab.ngenhttplib.http.core.response;

import okhttp3.Call;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class InvokerResponseObject {
    protected final Call callObj;
    protected final Response responseObj;

    public InvokerResponseObject(Call callObj) throws IOException {
        this.callObj = callObj;
        responseObj = callObj.execute();
    }

    public InvokerResponseObject(Call callObj, Response responseObj) {
        this.callObj = callObj;
        this.responseObj = responseObj;
    }

    public String getStringData() throws Exception {
        return responseObj.body().string();
    }

    public InvokerResponseObject downloadDataToFile(String fileName) throws Exception {
        File file = new File(fileName);
        FileOutputStream fop = new FileOutputStream(file);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        fop.write(responseObj.body().bytes());
        fop.flush();
        fop.close();

        return this;
    }

    public int getResponseCode() {
        return responseObj.code();
    }

    public String getResponseMessage() {
        return responseObj.message();
    }
}