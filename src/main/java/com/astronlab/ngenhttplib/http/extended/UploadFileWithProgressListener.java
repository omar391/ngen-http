package com.astronlab.ngenhttplib.http.extended;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import com.astronlab.ngenhttplib.http.HttpInvoker;
import com.astronlab.ngenhttplib.http.impl.IHttpConnectionManager;
import com.astronlab.ngenhttplib.http.impl.IHttpProgressListener;
import org.apache.http.entity.mime.content.FileBody;

public class UploadFileWithProgressListener implements IHttpConnectionManager {

    private HttpInvoker httpInvoker;
    private IHttpProgressListener updateListener;
    private boolean closeUpload = false;
    private long speedUpdateDelay = 1000;
    private int retryCount = 3;
    private int retriedNo = 0;
    private int defaultFileUploadSize = 204800;
    private int fileUploadSizeInKB = defaultFileUploadSize;
    private String uploadFileName = "TEST_UPLOAD";

    public UploadFileWithProgressListener(HttpInvoker invoker, IHttpProgressListener listener) {
        this.httpInvoker = invoker;
        this.updateListener = listener;
    }

    public void setUploadSizeInKB(int size) {
        this.fileUploadSizeInKB = size;
    }

    public void startUploadSpeedTest() {
        try {
            updateListener.HttpUpdateListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.CONNECTING_MSG);
            CustomMultipartEntity mEntity = new CustomMultipartEntity();
            mEntity.addPart("uploadfile", new FileBody(getUploadFile()));
            httpInvoker.setPostParams(mEntity);
            httpInvoker.getHttpResponse();
            updateListener.HttpUpdateListener(IHttpProgressListener.Status.SUCCESS, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.SUCCESSFUL_MSG);
            httpInvoker.close();
        } catch (Exception ex) {
            updateListener.HttpUpdateListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.RETRY_MSG);
            retryUpload();
        }
    }

    private void retryUpload() {
        if (retriedNo < retryCount) {
            retriedNo++;
            this.startUploadSpeedTest();
        } else {
            updateListener.HttpUpdateListener(IHttpProgressListener.Status.FAILS, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.FAILS_MSG);
        }
    }

    @Override
    public void StopConnection() {
        this.closeUpload = true;
    }

    private void createNewDummyFile() {
        RandomAccessFile dummyFile;
        int size = 1024 * fileUploadSizeInKB;
        try {
            dummyFile = new RandomAccessFile("build/" + uploadFileName, "rw");
            dummyFile.setLength(size);
            dummyFile.writeUTF(uploadFileName);
            dummyFile.close();
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private File getUploadFile() throws IOException {
        File uploadFile = new File("build/" + uploadFileName);

        if (!uploadFile.exists() || fileUploadSizeInKB != defaultFileUploadSize) {
            new File("build/").mkdirs();
            uploadFile.createNewFile();
            uploadFile.setReadable(true);
            uploadFile.setWritable(true);
            createNewDummyFile();
        }
        return uploadFile;
    }

    private class OutputStreamProgress extends OutputStream {

        private final OutputStream outstream;
        private double amountComplete = 0, totalTime = 0;
        private long segmentStartTime = System.currentTimeMillis(), timeDiff;

        public OutputStreamProgress(OutputStream outstream) {
            this.outstream = outstream;
        }

        @Override
        public void write(int b) throws IOException {
            if (!closeUpload) {
                outstream.write(b);
                amountComplete++;
                notifyListener();
            } else {
                close();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (!closeUpload) {
                outstream.write(b, off, len);
                amountComplete += len;
                notifyListener();
            } else {
                close();
            }
        }

        @Override
        public void flush() throws IOException {
            outstream.flush();
        }

        @Override
        public void close() throws IOException {
            outstream.close();
            httpInvoker.close();
        }

        private void notifyListener() {
            timeDiff = System.currentTimeMillis() - segmentStartTime;

            if (timeDiff >= speedUpdateDelay) { // timeout(default 1s) check
                totalTime += timeDiff;
                updateListener.HttpUpdateListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.UPLOAD, (amountComplete / (totalTime * 1.024)));
                segmentStartTime = System.currentTimeMillis();
            }
        }
    }

    private class CustomMultipartEntity extends org.apache.http.entity.mime.MultipartEntity {

        private OutputStreamProgress outstream;

        public CustomMultipartEntity() {
            super();
        }

        @Override
        public void writeTo(OutputStream outstream) throws IOException {
            this.outstream = new OutputStreamProgress(outstream);
            super.writeTo(this.outstream);
        }
    }
}