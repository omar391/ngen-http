package com.astronlab.ngenhttplib.http.extended;

import com.astronlab.ngenhttplib.http.HttpInvoker;
import com.astronlab.ngenhttplib.http.impl.IHttpConnectionManager;
import com.astronlab.ngenhttplib.http.impl.IHttpProgressListener;
import com.squareup.okhttp.Response;

import java.io.InputStream;
import java.util.Calendar;

public class DownloadFileWithProgressListener implements IHttpConnectionManager {

  private HttpInvoker httpInvoker;
  private IHttpProgressListener updateListener;
  private boolean closeDownload = false;
  private long speedUpdateDelay = 1000;
  private int retryCount = 3;
  private int retriedNo = 0;
  private final String connectionTag;

  public DownloadFileWithProgressListener(HttpInvoker invoker, IHttpProgressListener listener) {
	this.httpInvoker = invoker;
	this.updateListener = listener;
	connectionTag = ""+Calendar.getInstance().getTimeInMillis();
	httpInvoker.setTag(connectionTag);
  }

  public void startDownload() {
	try {
	  updateListener.HttpUpdateListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.CONNECTING_MSG);
	  Response response = httpInvoker.getHttpResponse();
	  updateListener.HttpUpdateListener(IHttpProgressListener.Status.RUNNING, IHttpProgressListener.UpdateType.HEADER, response.headers().toMultimap());

	  byte[] buffer = new byte[1024 * 1024 * 10];
	  InputStream inputStream = response.body().byteStream();
	  double sizeOfChunk, amountComplete = 0, totalTime = 0;
	  long segmentStartTime = System.currentTimeMillis(), timeDiff;
	  while ((sizeOfChunk = inputStream.read(buffer)) != -1) {
		if (closeDownload) {
		  inputStream.close();
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
	  httpInvoker.abortConnection(connectionTag);
	  updateListener.HttpUpdateListener(IHttpProgressListener.Status.SUCCESS, IHttpProgressListener.UpdateType.STATUS, IHttpProgressListener.SUCCESSFUL_MSG);

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

  @Override public void StopConnection() {
	this.closeDownload = true;
  }
}
