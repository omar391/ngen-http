package com.astronlab.ngenhttplib.http.utils;

import com.astronlab.ngenhttplib.http.core.HttpInvoker;
import com.astronlab.ngenhttplib.http.core.InvokerRequest;
import com.astronlab.ngenhttplib.http.core.impl.IHttpProgressListener;
import com.astronlab.ngenhttplib.http.extended.SimpleFileDownloadWithProgressListener;
import com.astronlab.ngenhttplib.http.extended.SimpleUploadFileWithProgressListener;
import com.astronlab.ngenhttplib.http.extended.SpeedManager;

import java.io.IOException;
import java.net.Proxy;

public class IpProperties implements IHttpProgressListener {

    private SpeedManager downloadSpeedManager;
    private SpeedManager uploadSpeedManager;
    private String authUserName = "neoman";
    private String authPassword = "madmap";
    private int uploadFileSize = 0;
    private String proxyIp;
    private int proxyPort;
    private Proxy.Type proxyType;
    private InvokerRequest request;

    //Constructor for given proxy ip
    public IpProperties(String proxyIp, int proxyPort, Proxy.Type proxyType) {
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
        this.proxyType = proxyType;
    }

    //Constructor for client's own ip
    public IpProperties() {
    }

    private InvokerRequest getRequest(String url) {
        if (request != null) {
            return request = request.cloneInit(url);

        } else {
            request = new HttpInvoker().init(url);
            if (proxyIp != null) {
                request.setProxy(proxyIp, proxyPort, proxyType);
            }
            return request;
        }
    }

    public double calculateDownloadSpeed(String dlUrl) {
        SimpleFileDownloadWithProgressListener downloadTester = new SimpleFileDownloadWithProgressListener(getRequest(dlUrl), this);
        downloadSpeedManager = new SpeedManager(downloadTester);
        downloadTester.startDownload();
        return downloadSpeedManager.getSpeed();
    }

    public void setFileUploadSizeInKB(int size) {
        this.uploadFileSize = size;
    }

    public double calculateUploadSpeed(String uploadUrl) throws Exception {
        SimpleUploadFileWithProgressListener uploadTester = new SimpleUploadFileWithProgressListener(getRequest(uploadUrl), this);
        uploadSpeedManager = new SpeedManager(uploadTester);
        if (uploadFileSize > 0) {
            uploadTester.setUploadSizeInKB(uploadFileSize);
        }
        uploadTester.startUploadSpeedTest();
        return uploadSpeedManager.getSpeed();
    }

    @Override
    public void notifyListener(Status stateType, UpdateType updateType, Object value) {
        if (stateType == Status.FAILS) {
            //ConsoleService.info("Http connection fails for host: " + ip + " : " + port);
        } else if (stateType == Status.RUNNING) {
            if (updateType == UpdateType.DOWNLOAD) {
                try {
                    downloadSpeedManager.manageSpeed((Double) value);
                } catch (IOException ignored) {
                }
            } else if (updateType == UpdateType.UPLOAD) {
                try {
                    uploadSpeedManager.manageSpeed((Double) value);
                } catch (IOException ignored) {
                }
            } else if (updateType == UpdateType.HEADER) {
            } else {
                //ConsoleService.info(value.toString());+ ui status column
            }
        } else {
            //ConsoleService.info("Http task completed succesfully!");
        }
    }
}
