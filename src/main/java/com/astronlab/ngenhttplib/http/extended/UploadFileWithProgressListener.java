package com.astronlab.ngenhttplib.http.extended;

import com.astronlab.ngenhttplib.http.HttpInvoker;
import com.astronlab.ngenhttplib.http.RequestEntityBuilder;
import com.astronlab.ngenhttplib.http.impl.IHttpConnectionManager;
import com.astronlab.ngenhttplib.http.impl.IHttpProgressListener;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class UploadFileWithProgressListener implements IHttpConnectionManager {

    private HttpInvoker httpInvoker;
    private IHttpProgressListener updateListener;
    private int retryCount = 3;
    private int retriedNo = 0;
    private int defaultFileUploadSize = 204800;
    private int fileUploadSizeInKB = defaultFileUploadSize;
    private String uploadFileName = "TEST_UPLOAD";
    private BufferedSinkProgressHandler progressHandler;

    public UploadFileWithProgressListener(HttpInvoker invoker, IHttpProgressListener listener) {
        this.httpInvoker = invoker;
        this.updateListener = listener;
    }

    public void setUploadSizeInKB(int size) {
        this.fileUploadSizeInKB = size;
    }

    public void startUploadSpeedTest() {
        try {
            updateListener.notifyListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.CONNECTING_MSG);
            RequestEntityBuilder mEntity = new RequestEntityBuilder();
            progressHandler = mEntity.addStreamingData("uploadfile", getUploadFile()).setListener(updateListener);
            httpInvoker.config().post(mEntity).update().getHttpResponse();
        } catch (Exception ex) {
            updateListener.notifyListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.RETRY_MSG);
            retryUpload();
        }
    }

    private void retryUpload() {
        if (retriedNo < retryCount) {
            retriedNo++;
            this.startUploadSpeedTest();
        } else {
            updateListener.notifyListener(IHttpProgressListener.Status.FAILS, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.FAILS_MSG);
        }
    }

    @Override
    public void stopConnection() {
        progressHandler.setClosed(true);
        try {
            httpInvoker.closeNReleaseResource();
        } catch (IOException e) {
        }
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
}