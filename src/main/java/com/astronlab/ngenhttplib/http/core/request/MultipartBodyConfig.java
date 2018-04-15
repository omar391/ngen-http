package com.astronlab.ngenhttplib.http.core.request;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MultipartBodyConfig extends InvokerRequestBody {
    private final MultipartBody.Builder multiPartBuilder;

    MultipartBodyConfig() {
        mediaType = MultipartType.FORM.value;
        multiPartBuilder = new MultipartBody.Builder();
    }

    public enum MultipartType {
        FORM(MultipartBody.FORM), MIXED(MultipartBody.MIXED), ALTERNATIVE(MultipartBody.ALTERNATIVE),
        DIGEST(MultipartBody.DIGEST), PARALLEL(MultipartBody.PARALLEL);

        private MediaType value;

        MultipartType(MediaType mediaType) {
            this.value = mediaType;
        }

        public MediaType getValue() {
            return value;
        }
    }

    public MultipartBodyConfig setBodyType(MultipartType bodyType) {
        this.mediaType = bodyType.value;
        return this;
    }

    public MultipartBodyConfig addParams(HashMap<String, String> paramsMap) {
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            multiPartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
        }

        return this;
    }

    public MultipartBodyConfig addParam(String keyName, String value) {
        multiPartBuilder.addFormDataPart(keyName, value);

        return this;
    }

    public MultipartBodyConfig addParam(String keyName, File file)
            throws IOException {
        return addParam(keyName, file, file.getName(), getFileMimeType(file));
    }

    public MultipartBodyConfig addParam(String keyName, File file, String fileName,
                                        String fileMimeType) {
        return addParam(keyName, file, fileName, MediaType.parse(fileMimeType));
    }

    private MultipartBodyConfig addParam(String keyName, File file, String fileName,
                                         MediaType fileMimeType) {
        multiPartBuilder.addFormDataPart(keyName, fileName, RequestBody.create(fileMimeType,
                file));

        return this;
    }

    RequestBody prepareBody() {
        if (mediaType != null) {
            multiPartBuilder.setType(mediaType);
        }

        return multiPartBuilder.build();
    }
}