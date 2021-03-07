package com.astronlab.ngenhttplib.core.request;

import okhttp3.FormBody;
import okhttp3.RequestBody;

import java.util.Map;

public class SingleFormBodyConfig extends InvokerRequestBody {
    private final FormBody.Builder formBuilder;

    SingleFormBodyConfig() {
        formBuilder = new FormBody.Builder();
    }

    public SingleFormBodyConfig addParams(Map<String, String> paramsMap) {
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
        }

        return this;
    }

    public SingleFormBodyConfig addParam(String name, String value) {
        formBuilder.add(name, value);

        return this;
    }

    RequestBody prepareBody() {
        return formBuilder.build();
    }
}