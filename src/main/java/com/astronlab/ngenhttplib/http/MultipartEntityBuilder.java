package com.astronlab.ngenhttplib.http;

import com.astronlab.ngenhttplib.http.extended.BufferedSinkProgressHandler;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;
import okio.BufferedSink;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by omar-mac on 1/2/16.
 */
public class MultipartEntityBuilder {

    private MultipartBuilder multipartBuilder = new MultipartBuilder();

    /**
     * This method is normally not required except the cases where we need to specifically set multipart "Sub-type"
     * Ref: https://en.wikipedia.org/wiki/MIME#Multipart_subtypes
     *
     * @param mimeType
     * @return MultipartEntityBuilder
     */
    public MultipartEntityBuilder setMultipartType(MultipartType mimeType) {
        multipartBuilder.type(MediaType.parse(mimeType.value));

        return this;
    }

    /**
     * Mime types, i.e. "application/json", "text/plain"
     *
     * @param content
     * @param mimeType
     * @return
     */
    public MultipartEntityBuilder addRawData(String content, String mimeType) {
        multipartBuilder.addPart(RequestBody.create(MediaType.parse(mimeType), content));

        return this;
    }

    public MultipartEntityBuilder addFormData(String name, String value) {
        multipartBuilder.addFormDataPart(name, value);

        return this;
    }

    public MultipartEntityBuilder addFormData(String name, File file) {
        return addFormData(name, file, null, null);
    }

    public MultipartEntityBuilder addFormData(String name, File file, String fileName, String fileMimeType) {
        RequestBody requestBody = RequestBody.create(MediaType.parse(fileMimeType), file);
        multipartBuilder.addFormDataPart(name, fileName, requestBody);

        return this;
    }

    public BufferedSinkProgressHandler addStreamingData(String uploadName, File file) throws IOException {
        return addStreamingData(uploadName, Files.readAllBytes(file.toPath()));
    }

    /**
     * Check a sample implementation in {@code UploadFileWithProgressListener}
     *
     * @param bufferedContent
     * @return
     */
    public BufferedSinkProgressHandler addStreamingData(String name, byte[] bufferedContent) {
        final BufferedSinkProgressHandler[] progressHandler = new BufferedSinkProgressHandler[0];
        RequestBody requestBody = new RequestBody() {

            @Override
            public MediaType contentType() {
                return null;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                progressHandler[0] = new BufferedSinkProgressHandler(sink);
                progressHandler[0].write(bufferedContent);
            }
        };
        multipartBuilder.addFormDataPart(name, null, requestBody);

        return progressHandler[0];
    }

    public RequestBody build() {
        return multipartBuilder.build();
    }

    public enum MultipartType {
        MIXED("multipart/mixed"), ALTERNATIVE("multipart/alternative"), DIGEST("multipart/digest"), PARALLEL("multipart/parallel"), FORM("multipart/form-data");

        private String value;

        private MultipartType(String typeStr) {
            this.value = typeStr;
        }
    }
}