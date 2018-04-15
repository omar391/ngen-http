package com.astronlab.ngenhttplib.test;

import com.astronlab.ngenhttplib.http.core.HttpInvoker;
import com.astronlab.ngenhttplib.http.core.impl.SyncResponseHandler;
import com.astronlab.ngenhttplib.http.core.request.InvokerRequestBody;

public class TestInvoker {

    public static void main(String[] args) throws Exception {
        new HttpInvoker().config().setSocketTimeOut(1000)
                .init("http://httpbin.org/post")
                .post(InvokerRequestBody.createViaMultiPartBody().addParam("d", "dd"))
                .execute((SyncResponseHandler) response -> {
            System.out.println(response.getStringData());
        });

        //new HttpInvoker().getStringData("http://google.com");
    }
}