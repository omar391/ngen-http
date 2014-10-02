package app.ngenhttplib.http.extended;

import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import app.ngenhttplib.http.HttpInvoker;
import app.ngenhttplib.http.impl.IHttpConnectionManager;
import app.ngenhttplib.http.impl.IHttpProgressListener;

public class DownloadFileWithProgressListener implements IHttpConnectionManager {

    private HttpInvoker httpInvoker;
    private IHttpProgressListener updateListener;
    private boolean closeDownload = false;
    private long speedUpdateDelay = 1000;
    private int retryCount = 3;
    private int retriedNo = 0;

    public DownloadFileWithProgressListener(HttpInvoker invoker, IHttpProgressListener listener) {
        this.httpInvoker = invoker;
        this.updateListener = listener;
    }

    public void startDownload() {
        try {
            updateListener.HttpUpdateListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.CONNECTING_MSG);
            HttpResponse response = httpInvoker.getHttpResponse();
            HttpEntity entity = response.getEntity();
            updateListener.HttpUpdateListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.HEADER, response.getAllHeaders());
            byte[] buffer = new byte[1024 * 1024 * 10];
            if (entity != null) {
                InputStream instream = entity.getContent();
                double sizeOfChunk, amountComplete = 0, totalTime = 0;
                long segmentStartTime = System.currentTimeMillis(), timeDiff;
                while ((sizeOfChunk = instream.read(buffer)) != -1) {
                    if (closeDownload) {
                        httpInvoker.close();
                        instream.close();
                        break;
                    }
                    amountComplete += sizeOfChunk;
                    timeDiff = System.currentTimeMillis() - segmentStartTime;

                    if (timeDiff >= speedUpdateDelay) { // timeout(default 1s) check
                        totalTime += timeDiff;
                        updateListener.HttpUpdateListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.DOWNLOAD, (amountComplete / (totalTime * 1.024)));
                        segmentStartTime = System.currentTimeMillis();
                    }
                }
                httpInvoker.close();
                updateListener.HttpUpdateListener(IHttpProgressListener.Status.SUCCESS, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.SUCCESSFUL_MSG);
            }
        } catch (Exception ex) {
            updateListener.HttpUpdateListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.RETRY_MSG);
            retryDownload();
        }
    }

    private void retryDownload() {
        if (retriedNo < retryCount) {
            retriedNo++;
            this.startDownload();
        } else {
            updateListener.HttpUpdateListener(IHttpProgressListener.Status.FAILS, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.FAILS_MSG);
        }
    }

    @Override
    public void StopConnection() {
        this.closeDownload = true;
    }
}
