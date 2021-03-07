package com.astronlab.ngenhttplib.core.misc;

import com.astronlab.ngenhttplib.core.impl.IHttpProgressListener;
import okio.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

public abstract class BufferedProgressHandlerSink implements BufferedSink {

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

    private BufferedProgressHandlerSink incrementByteCount(long len) {
        amountComplete += len;

        return this;
    }

    @NotNull
    public Buffer getBuffer() {
        return delegateSink.buffer();
    }

    @NotNull
    @Override
    public BufferedSink write(@NotNull ByteString byteString) throws IOException {
        if (!closeUpload) {
            delegateSink.write(byteString);
            incrementByteCount(byteString.size());
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink write(@NotNull byte[] source) throws IOException {
        if (!closeUpload) {
            delegateSink.write(source);
            incrementByteCount(source.length);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink write(@NotNull byte[] source, int offset, int byteCount) throws IOException {
        if (!closeUpload) {
            delegateSink.write(source, offset, byteCount);
            incrementByteCount(byteCount);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    public BufferedSink write(@NotNull ByteString byteString, int offset, int byteCount) throws IOException {
        return write(byteString.toByteArray(), offset, byteCount);
    }

    @Override
    public long writeAll(@NotNull Source source) throws IOException {
        if (!closeUpload) {
            long len = delegateSink.writeAll(source);
            incrementByteCount(len);
            notifyListener();
            return len;
        } else {
            close();
        }

        return 0;
    }

    @NotNull
    @Override
    public BufferedSink write(@NotNull Source source, long byteCount) throws IOException {
        if (!closeUpload) {
            delegateSink.write(source, byteCount);
            incrementByteCount(byteCount);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeUtf8(@NotNull String string) throws IOException {
        if (!closeUpload) {
            delegateSink.writeUtf8(string);
            incrementByteCount(string.length());
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeUtf8(@NotNull String string, int beginIndex, int endIndex) throws IOException {
        if (!closeUpload) {
            delegateSink.writeUtf8(string, beginIndex, endIndex);
            incrementByteCount(endIndex - beginIndex);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeUtf8CodePoint(int codePoint) throws IOException {
        if (!closeUpload) {
            delegateSink.writeUtf8CodePoint(codePoint);
            incrementByteCount(1);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeString(@NotNull String string, @NotNull Charset charset) throws IOException {
        if (!closeUpload) {
            delegateSink.writeString(string, charset);
            incrementByteCount(string.length());
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeString(@NotNull String string, int beginIndex, int endIndex, @NotNull Charset charset) throws IOException {
        if (!closeUpload) {
            delegateSink.writeString(string, beginIndex, endIndex, charset);
            incrementByteCount(endIndex - beginIndex);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeByte(int b) throws IOException {
        if (!closeUpload) {
            delegateSink.writeByte(b);
            incrementByteCount(1);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeShort(int s) throws IOException {
        if (!closeUpload) {
            delegateSink.writeShort(s);
            incrementByteCount(2);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeShortLe(int s) throws IOException {
        if (!closeUpload) {
            delegateSink.writeShortLe(s);
            incrementByteCount(2);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeInt(int i) throws IOException {
        if (!closeUpload) {
            delegateSink.writeInt(i);
            incrementByteCount(4);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeIntLe(int i) throws IOException {
        if (!closeUpload) {
            delegateSink.writeIntLe(i);
            incrementByteCount(4);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeLong(long v) throws IOException {
        if (!closeUpload) {
            delegateSink.writeLong(v);
            incrementByteCount(8);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeLongLe(long v) throws IOException {
        if (!closeUpload) {
            delegateSink.writeLongLe(v);
            incrementByteCount(8);
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeDecimalLong(long v) throws IOException {
        if (!closeUpload) {
            delegateSink.writeDecimalLong(v);
            incrementByteCount(Long.toString(v, 10).length());
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink writeHexadecimalUnsignedLong(long v) throws IOException {
        if (!closeUpload) {
            delegateSink.writeHexadecimalUnsignedLong(v);
            incrementByteCount(Long.toString(v, 16).length());
            notifyListener();
        } else {
            close();
        }

        return delegateSink;
    }

    @NotNull
    @Override
    public BufferedSink emitCompleteSegments() throws IOException {
        return delegateSink.emitCompleteSegments();
    }

    @NotNull
    @Override
    public BufferedSink emit() throws IOException {
        return delegateSink.emit();
    }

    @NotNull
    @Override
    public OutputStream outputStream() {
        return delegateSink.outputStream();
    }

    @Override
    public void write(@NotNull Buffer source, long byteCount) throws IOException {
        if (!closeUpload) {
            delegateSink.write(source, byteCount);
            incrementByteCount(byteCount);
            notifyListener();
        } else {
            close();
        }
    }

    @Override
    public void flush() throws IOException {
        delegateSink.flush();
    }

    @NotNull
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

    public abstract boolean isOpen();

    public abstract int write(ByteBuffer src) throws IOException;
}