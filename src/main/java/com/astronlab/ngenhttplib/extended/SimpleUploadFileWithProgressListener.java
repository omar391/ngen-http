package com.astronlab.ngenhttplib.extended;

import com.astronlab.ngenhttplib.core.impl.IHttpConnectionManager;
import com.astronlab.ngenhttplib.core.impl.IHttpProgressListener;
import com.astronlab.ngenhttplib.core.InvokerRequest;
import com.astronlab.ngenhttplib.core.InvokerResponse;
import com.astronlab.ngenhttplib.core.misc.BufferedProgressHandlerSink;
import com.astronlab.ngenhttplib.core.request.InvokerRequestBody;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class SimpleUploadFileWithProgressListener implements IHttpConnectionManager {

    private final InvokerRequest invokerRequest;
    private final IHttpProgressListener updateListener;
    private int retriedNo = 0;
    private final int defaultFileUploadSize = 204800;
    private int fileUploadSizeInKB = defaultFileUploadSize;
    private final String uploadFileName = "TEST_UPLOAD";
    private BufferedProgressHandlerSink progressHandler;

    public SimpleUploadFileWithProgressListener(InvokerRequest request, IHttpProgressListener listener) {
        this.invokerRequest = request;
        this.updateListener = listener;
    }

    public void setUploadSizeInKB(int size) {
        this.fileUploadSizeInKB = size;
    }

    public void startUploadSpeedTest() throws Exception {
        try {
            updateListener.notifyListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.CONNECTING_MSG);
            if (retriedNo < 1) {
                InvokerRequestBody requestBody = InvokerRequestBody.createViaMultiPartBody().addParam("uploadfile", getUploadFile());
                progressHandler = requestBody.setProgressListener(updateListener);
                invokerRequest.post(requestBody);
            }

            invokerRequest.execute(InvokerResponse::close);
        } catch (Exception ex) {
            updateListener.notifyListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.RETRY_MSG);
            retryUpload();
        }
    }

    private void retryUpload() throws Exception {
        int retryCount = 3;
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
        invokerRequest.abortConnection();
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
            e.printStackTrace();
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