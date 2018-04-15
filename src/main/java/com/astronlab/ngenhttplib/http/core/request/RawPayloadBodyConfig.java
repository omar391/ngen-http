package com.astronlab.ngenhttplib.http.core.request;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class RawPayloadBodyConfig extends InvokerRequestBody {
    private final Object rawContent;

    RawPayloadBodyConfig(String rawContent) {
        this.rawContent = rawContent;
    }

    RawPayloadBodyConfig(File rawContent) {
        this.rawContent = rawContent;
    }

    RawPayloadBodyConfig(InputStream rawContent) {
        this.rawContent = rawContent;
    }

    RequestBody prepareBody() throws IOException {
        if (mediaType == null && rawContent instanceof File) {
            mediaType = getFileMimeType((File) rawContent);
        }

        if (rawContent instanceof String) {
            return RequestBody.create(mediaType, (String) rawContent);

        } else if (rawContent instanceof File) {
            return RequestBody.create(mediaType, (File) rawContent);

        } else {
            return new RequestBody() {
                @Override
                public MediaType contentType() {
                    return mediaType;
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    Source source = null;
                    try {
                        source = Okio.source((InputStream) rawContent);
                        sink.writeAll(source);
                    } finally {
                        Util.closeQuietly(source);
                    }
                }
            };
        }
    }
}
