package com.astronlab.ngenhttplib.test;

import com.astronlab.ngenhttplib.core.HttpInvoker;

import static com.astronlab.ngenhttplib.core.request.InvokerRequestBody.RawMimeType.SINGLE_PART_FORM_URL_ENCODED;
import static com.astronlab.ngenhttplib.core.request.InvokerRequestBody.createViaRawStringPayload;

public class TestInvoker {

    public static void main(String[] args) throws Exception {
        HttpInvoker invoker = new HttpInvoker();

//        invoker.init("http://pmis.sebpo.net/index.php").executeAsync((SyncResponseHandler) r -> {
//        });
        final int[] res = new int[1];

        invoker.init("http://pmis.sebpo.net/index.php?ext=loginpage&controller=ext")
                .post(createViaRawStringPayload("action=login&username=md.omar&passhash=cc608d65570ba41d666720532dc6f88b&remain=true&area=loginpage")
                        .setBodyType(SINGLE_PART_FORM_URL_ENCODED))
                .setRequestHeader("X-Requested-With", "XMLHttpRequest")
                .execute(response -> {
                    System.out.println(response.getStringData());
                    res[0] = response.getResponseCode();

                });

        //new HttpInvoker().getStringData("http://google.com");
    }
}