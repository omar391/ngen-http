package com.astronlab.ngenhttplib.utils;

import com.astronlab.ngenhttplib.core.InvokerRequest;
import com.astronlab.ngenhttplib.core.client.ProxyConfig;
import com.astronlab.ngenhttplib.core.impl.IHttpProgressListener;
import com.astronlab.ngenhttplib.extended.SimpleFileDownloadWithProgressListener;
import com.astronlab.ngenhttplib.extended.SimpleUploadFileWithProgressListener;
import com.astronlab.ngenhttplib.extended.SpeedManager;
import com.astronlab.ngenhttplib.core.HttpInvoker;

import java.io.IOException;
import java.util.regex.Pattern;

public class IpProperties implements IHttpProgressListener {

    private SpeedManager downloadSpeedManager;
    private SpeedManager uploadSpeedManager;
    private final String authUserName = "neoman";
    private final String authPassword = "madmap";
    private int uploadFileSize = 0;
    private String proxyIp;
    private int proxyPort;
    private ProxyConfig.Type proxyType;
    private InvokerRequest request;
    private static final Pattern privateIp = Pattern.compile("\\b(?i:localhost)|127.0.0.1\\b");

    //Constructor for given proxy ip
    public IpProperties(String proxyIp, int proxyPort, ProxyConfig.Type proxyType) {
        this.proxyIp = proxyIp;
        this.proxyPort = proxyPort;
        this.proxyType = proxyType;
    }

    //Constructor for client's own ip
    public IpProperties() {
    }

    public static boolean isPrivateIp(String ipColonPort) {
        return privateIp.matcher(ipColonPort).find();
    }

    private InvokerRequest getRequest(String url) {
        if (request != null) {
            return request = request.fork(url);

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
