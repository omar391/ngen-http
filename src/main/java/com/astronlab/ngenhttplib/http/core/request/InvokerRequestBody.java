package com.astronlab.ngenhttplib.http.core.request;

import com.astronlab.ngenhttplib.http.core.impl.IHttpProgressListener;
import com.astronlab.ngenhttplib.http.core.misc.BufferedProgressHandlerSink;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public abstract class InvokerRequestBody {
    private BufferedProgressHandlerSink progressHandlerSink;
    MediaType mediaType;

    public enum RawMimeType {
        BINARY(MediaType.parse("application/octet-stream")),
        TEXT_PLAIN(MediaType.parse("text/plain")),
        JSON(MediaType.parse("application/json")),
        XML(MediaType.parse("application/xml"));

        MediaType value;

        RawMimeType(MediaType mediaType) {
            this.value = mediaType;
        }

        public MediaType getValue() {
            return value;
        }
    }

    public BufferedProgressHandlerSink setProgressListener(IHttpProgressListener httpProgressListener) {
        return setProgressListener(httpProgressListener, 0, null);
    }

    public BufferedProgressHandlerSink setProgressListener(IHttpProgressListener iHttpProgressListener, long updateDelayTime, TimeUnit timeUnit) {
        return progressHandlerSink = new BufferedProgressHandlerSink(iHttpProgressListener).setSpeedUpdateDelay(updateDelayTime, timeUnit);
    }

    public InvokerRequestBody setBodyType(RawMimeType mediaType) {
        this.mediaType = mediaType.value;
        return this;
    }

    public InvokerRequestBody setBodyType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public InvokerRequestBody setBodyType(String mediaType) {
        this.mediaType = MediaType.parse(mediaType);
        return this;
    }

    public static RawPayloadBodyConfig createViaRawStringPayload(String rawContent) {
        return new RawPayloadBodyConfig(rawContent);
    }

    public static RawPayloadBodyConfig createViaRawFilePayload(File rawContent) {
        return new RawPayloadBodyConfig(rawContent);
    }

    public static RawPayloadBodyConfig createViaRawBinaryPayload(InputStream rawContent) {
        return new RawPayloadBodyConfig(rawContent);
    }

    public static SingleFormBodyConfig createViaSingleFormBody() {
        return new SingleFormBodyConfig();
    }

    public static MultipartBodyConfig createViaMultiPartBody() {
        return new MultipartBodyConfig();
    }

    static MediaType getFileMimeType(File file) throws IOException {
        String type = Files.probeContentType(file.toPath());
        MediaType mediaType;

        if (type == null) {
            mediaType = RawMimeType.BINARY.value;
        } else {
            mediaType = MediaType.parse(type);
        }

        return mediaType;
    }

    abstract RequestBody prepareBody() throws IOException;

    public RequestBody build() throws IOException {
        final RequestBody body = prepareBody();

        if (progressHandlerSink != null) {
            return new RequestBody() {

                public long contentLength() throws IOException {
                    return body.contentLength();
                }

                @Override
                public MediaType contentType() {
                    return body.contentType();
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    body.writeTo(progressHandlerSink.setRootSink(sink));
                }
            };
        }

        return body;
    }
}