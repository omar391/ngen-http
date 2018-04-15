package com.astronlab.ngenhttplib.http.extended;

import com.astronlab.ngenhttplib.http.core.InvokerRequest;
import com.astronlab.ngenhttplib.http.core.InvokerResponse;
import com.astronlab.ngenhttplib.http.core.impl.IHttpConnectionManager;
import com.astronlab.ngenhttplib.http.core.impl.IHttpProgressListener;
import com.astronlab.ngenhttplib.http.core.impl.SyncResponseHandler;

import java.io.IOException;
import java.io.InputStream;

public class SimpleFileDownloadWithProgressListener implements IHttpConnectionManager {

    private InvokerRequest invokerRequest;
    private IHttpProgressListener updateListener;
    private boolean closeDownload = false;
    private int retriedNo = 0;

    public SimpleFileDownloadWithProgressListener(InvokerRequest invokerRequest, IHttpProgressListener listener) {
        this.invokerRequest = invokerRequest;
        this.updateListener = listener;
    }

    public void startDownload() {
        try {
            updateListener.notifyListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.CONNECTING_MSG);
            invokerRequest.execute((SyncResponseHandler) this::handleProgress);
            updateListener.notifyListener(IHttpProgressListener.Status.SUCCESS, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.SUCCESSFUL_MSG);

        } catch (Exception ex) {
            updateListener.notifyListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.RETRY_MSG);
            retryDownload();
        }
    }

    private void handleProgress(InvokerResponse response) throws IOException {
        InputStream inputStream = response.getData();
        updateListener.notifyListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.HEADER, response.getResponseObj().headers().toMultimap());

        byte[] buffer = new byte[1024 * 1024 * 10];
        double sizeOfChunk, amountComplete = 0, totalTime = 0;
        long segmentStartTime = System.currentTimeMillis(), timeDiff;
        while (inputStream != null && (sizeOfChunk = inputStream.read(buffer)) != -1) {
            if (closeDownload) {
                inputStream.close();
                break;
            }
            amountComplete += sizeOfChunk;
            timeDiff = System.currentTimeMillis() - segmentStartTime;

            long speedUpdateDelay = 1000;
            if (timeDiff >= speedUpdateDelay) {
                totalTime += timeDiff;
                updateListener.notifyListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.DOWNLOAD, (amountComplete / (totalTime * 1.024)));
                segmentStartTime = System.currentTimeMillis();
            }
        }
        response.close();
    }

    private void retryDownload() {
        int retryCount = 3;
        if (retriedNo < retryCount) {
            retriedNo++;
            this.startDownload();
        } else {
            updateListener.notifyListener(IHttpProgressListener.Status.FAILS, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.FAILS_MSG);
        }
    }

    @Override
    public void stopConnection() {
        this.closeDownload = true;
    }
}
