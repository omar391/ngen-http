package com.astronlab.ngenhttplib.http;

import com.astronlab.ngenhttplib.http.extended.BufferedSinkProgressHandler;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Created by omar-mac on 1/2/16.
 */
public class PostEntityBuilder {

    private MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();

    /**
     * This method is normally not required except the cases where we need to specifically set multipart "Sub-type"
     * Ref: https://en.wikipedia.org/wiki/MIME#Multipart_subtypes
     *
     * @param mimeType
     * @return MultipartEntityBuilder
     */
    public PostEntityBuilder setMultipartType(MultipartType mimeType) {
        multipartBuilder.setType(mimeType.value);

        return this;
    }

    /**
     * Mime types, i.e. "application/json", "text/plain"
     *
     * @param content
     * @param mimeType
     * @return
     */
    public PostEntityBuilder addRawData(String content, String mimeType) {
        multipartBuilder.addPart(RequestBody.create(MediaType.parse(mimeType), content));

        return this;
    }

    public PostEntityBuilder addFormData(String name, String value) {
        multipartBuilder.addFormDataPart(name, value);

        return this;
    }

    public PostEntityBuilder addFormData(String name, File file) {
        return addFormData(name, file, null, null);
    }

    public PostEntityBuilder addFormData(String name, File file, String fileName, String fileMimeType) {
        setMultipartType(MultipartType.FORM);
        RequestBody requestBody = RequestBody.create(MediaType.parse(fileMimeType), file);
        multipartBuilder.addFormDataPart(name, fileName, requestBody);

        return this;
    }

    public BufferedSinkProgressHandler addStreamingData(String uploadName, File file) throws IOException {
        setMultipartType(MultipartType.FORM);
        return addStreamingData(uploadName, Files.readAllBytes(file.toPath()));
    }

    /**
     * Check a sample implementation in {@code UploadFileWithProgressListener}
     *
     * @param bufferedContent
     * @return
     */
    public BufferedSinkProgressHandler addStreamingData(String name, final byte[] bufferedContent) {
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
        MIXED(MultipartBody.MIXED), ALTERNATIVE(MultipartBody.ALTERNATIVE), DIGEST(MultipartBody.DIGEST), PARALLEL(MultipartBody.PARALLEL), FORM(MultipartBody.FORM);

        private MediaType value;

        private MultipartType(MediaType typeStr) {
            this.value = typeStr;
        }
    }
}