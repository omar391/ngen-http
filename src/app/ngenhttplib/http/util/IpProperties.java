package app.ngenhttplib.http.util;

import java.net.Proxy;

import app.ngenhttplib.http.HttpInvoker;
import app.ngenhttplib.http.extended.DownloadFileWithProgressListener;
import app.ngenhttplib.http.extended.SpeedManager;
import app.ngenhttplib.http.extended.UploadFileWithProgressListener;
import app.ngenhttplib.http.impl.IHttpProgressListener;

public class IpProperties implements IHttpProgressListener {

    private String host;
    private int port;
    private String ip = null;
    private SpeedManager downloadSpeedManager;
    private SpeedManager uploadSpeedManager;
    private String authUserName = "neoman";
    private String authPassword = "madmap";
    private int uploadFileSize = 0;
    private HttpInvoker httpInvoker;

    //Constructor for given proxy ip
    public IpProperties(String proxyIp, int proxyPort) {
        this.ip = proxyIp;
        this.port = proxyPort;
        httpInvoker = new HttpInvoker();
        httpInvoker.setProxy(proxyIp, proxyPort, Proxy.Type.HTTP);
    }

    //Constructor for client's own ip
    public IpProperties() {
        httpInvoker = new HttpInvoker();
    }

    public double calculateDownloadSpeed(String dlUrl) {
        httpInvoker.setUrl(dlUrl);
        DownloadFileWithProgressListener downloadTester = new DownloadFileWithProgressListener(httpInvoker, this);
        downloadSpeedManager = new SpeedManager(downloadTester);
        downloadTester.startDownload();
        return downloadSpeedManager.getSpeed();
    }

    public void setFileUploadSizeInKB(int size) {
        this.uploadFileSize = size;
    }

    public double calculateUploadSpeed(String uploadUrl) {
        httpInvoker.setUrl(uploadUrl);
        UploadFileWithProgressListener uploadTester = new UploadFileWithProgressListener(httpInvoker, this);
        uploadSpeedManager = new SpeedManager(uploadTester);
        if (uploadFileSize > 0) {
            uploadTester.setUploadSizeInKB(uploadFileSize);
        }
        uploadTester.startUploadSpeedTest();
        return uploadSpeedManager.getSpeed();
    }

    @Override
    public void HttpUpdateListener(Object stateType, Object updateType, Object value) {
        if (stateType == Status.FAILS) {
            //ConsoleService.info("Http connection fails for host: " + ip + " : " + port);
        } else if (stateType == Status.RUNNING) {
            if (updateType == UpdateType.DOWNLOAD) {
                downloadSpeedManager.manageSpeed((double) value);
            } else if (updateType == UpdateType.UPLOAD) {
                uploadSpeedManager.manageSpeed((double) value);
            } else if (updateType == UpdateType.HEADER) {
            } else {
                //ConsoleService.info(value.toString());+ ui status column
            }
        } else {
            //ConsoleService.info("Http task completed succesfully!");
        }
    }

//    public HashMap<String, String> siteTest(Set keySet) {
//        return new SiteTestTool().siteTest(keySet);
//    }
}
