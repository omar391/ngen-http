package com.astronlab.ngenhttplib.http.core.misc;

import com.astronlab.ngenhttplib.http.core.impl.IHttpProgressListener;
import okio.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public class BufferedProgressHandlerSink implements BufferedSink {

    private BufferedSink delegateSink;
    private long amountComplete = 0, totalTime = 0;
    private long segmentStartTime = System.currentTimeMillis();
    private boolean closeUpload = false;
    private long speedUpdateDelay = 1000;
    private final IHttpProgressListener httpProgressListener;

    public BufferedProgressHandlerSink(IHttpProgressListener progressListener) {
        this.httpProgressListener = progressListener;
    }

    public BufferedProgressHandlerSink setClosed(boolean isClosed) {
        closeUpload = isClosed;

        return this;
    }

    public BufferedProgressHandlerSink setSpeedUpdateDelay(long delay, TimeUnit timeUnit) {
        if (timeUnit != null && delay > 0) {
            speedUpdateDelay = timeUnit.toMillis(delay);
        }

        return this;
    }

    public BufferedProgressHandlerSink setRootSink(BufferedSink bufferedSink) {
        delegateSink = bufferedSink;

        return this;
    }

    private BufferedProgressHandlerSink incrementByetCount(long len) {
        amountComplete += len;

        return this;
    }

    @Override
    public Buffer buffer() {
        return delegateSink.buffer();
    }

    @Override
    public BufferedSink write(ByteString byteString) throws IOException {
        if (!closeUpload) {
            delegateSink.write(byteString);
            incrementByetCount(byteString.size());
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink write(byte[] source) throws IOException {
        if (!closeUpload) {
            delegateSink.write(source);
            incrementByetCount(source.length);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink write(byte[] source, int offset, int byteCount) throws IOException {
        if (!closeUpload) {
            delegateSink.write(source, offset, byteCount);
            incrementByetCount(byteCount);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public long writeAll(Source source) throws IOException {
        if (!closeUpload) {
            long len = delegateSink.writeAll(source);
            incrementByetCount(len);
            notifyListener();
            return len;
        } else {
            close();
        }

        return 0;
    }

    @Override
    public BufferedSink write(Source source, long byteCount) throws IOException {
        if (!closeUpload) {
            delegateSink.write(source, byteCount);
            incrementByetCount(byteCount);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeUtf8(String string) throws IOException {
        if (!closeUpload) {
            delegateSink.writeUtf8(string);
            incrementByetCount(string.length());
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeUtf8(String string, int beginIndex, int endIndex) throws IOException {
        if (!closeUpload) {
            delegateSink.writeUtf8(string, beginIndex, endIndex);
            incrementByetCount(endIndex - beginIndex);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeUtf8CodePoint(int codePoint) throws IOException {
        if (!closeUpload) {
            delegateSink.writeUtf8CodePoint(codePoint);
            incrementByetCount(1);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeString(String string, Charset charset) throws IOException {
        if (!closeUpload) {
            delegateSink.writeString(string, charset);
            incrementByetCount(string.length());
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeString(String string, int beginIndex, int endIndex, Charset charset) throws IOException {
        if (!closeUpload) {
            delegateSink.writeString(string, beginIndex, endIndex, charset);
            incrementByetCount(endIndex - beginIndex);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeByte(int b) throws IOException {
        if (!closeUpload) {
            delegateSink.writeByte(b);
            incrementByetCount(1);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeShort(int s) throws IOException {
        if (!closeUpload) {
            delegateSink.writeShort(s);
            incrementByetCount(2);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeShortLe(int s) throws IOException {
        if (!closeUpload) {
            delegateSink.writeShortLe(s);
            incrementByetCount(2);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeInt(int i) throws IOException {
        if (!closeUpload) {
            delegateSink.writeInt(i);
            incrementByetCount(4);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeIntLe(int i) throws IOException {
        if (!closeUpload) {
            delegateSink.writeIntLe(i);
            incrementByetCount(4);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeLong(long v) throws IOException {
        if (!closeUpload) {
            delegateSink.writeLong(v);
            incrementByetCount(8);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeLongLe(long v) throws IOException {
        if (!closeUpload) {
            delegateSink.writeLongLe(v);
            incrementByetCount(8);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeDecimalLong(long v) throws IOException {
        if (!closeUpload) {
            delegateSink.writeDecimalLong(v);
            incrementByetCount(Long.toString(v, 10).length());
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink writeHexadecimalUnsignedLong(long v) throws IOException {
        if (!closeUpload) {
            delegateSink.writeHexadecimalUnsignedLong(v);
            incrementByetCount(Long.toString(v, 16).length());
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @Override
    public BufferedSink emitCompleteSegments() throws IOException {
        return delegateSink.emitCompleteSegments();
    }

    @Override
    public BufferedSink emit() throws IOException {
        return delegateSink.emit();
    }

    @Override
    public OutputStream outputStream() {
        return delegateSink.outputStream();
    }

    @Override
    public void write(Buffer source, long byteCount) throws IOException {
        if (!closeUpload) {
            delegateSink.write(source, byteCount);
            incrementByetCount(byteCount);
            notifyListener();
        } else {
            close();
        }
    }

    @Override
    public void flush() throws IOException {
        delegateSink.flush();
    }

    @Override
    public Timeout timeout() {
        return delegateSink.timeout();
    }

    @Override
    public void close() throws IOException {
        setClosed(true);
        delegateSink.close();
    }

    private void notifyListener() {
        long timeDiff = System.currentTimeMillis() - segmentStartTime;

        if (timeDiff >= speedUpdateDelay && httpProgressListener != null) { // timeout(default 1s) check
            totalTime += timeDiff;
            httpProgressListener.notifyListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.UPLOAD, (amountComplete / (totalTime * 1.024)));
            segmentStartTime = System.currentTimeMillis();
        }
    }
}