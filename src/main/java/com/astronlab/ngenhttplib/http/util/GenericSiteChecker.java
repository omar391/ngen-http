package com.astronlab.ngenhttplib.http.util;

import com.astronlab.ngenhttplib.http.HttpInvoker;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GenericSiteChecker {

    private final String failureRemark = "Failed";
    protected HttpInvoker invoker;
    protected String resultHtml = "";
    protected String successRemark = "Working";
    private boolean isHttps = false;
    private String url;

    public GenericSiteChecker() {
        invoker = new HttpInvoker();
    }

    public HttpInvoker getInvoker() {
        return this.invoker;
    }

    public GenericSiteChecker setInvoker(HttpInvoker invoker) {
        this.invoker = invoker;
        return this;
    }

    public GenericSiteChecker setUrl(String url) {
        this.url = url;
        return this;
    }

    public GenericSiteChecker setSSl(boolean ssl) {
        isHttps = ssl;
        return this;
    }

    private void fixSsl() throws Exception {
        String sslStr = "https://";

        if (isHttps) {
            if (url.matches("^http.*?$")) {
                url = url.replaceAll("^https?://", sslStr);
            } else {
                url = sslStr + url;
            }
            invoker.setUrl(url);
        }
    }

    private String getStatus(String msg) {
        successRemark += msg;
        return getStatus();
    }

    public String getStatus() {
        try {
            if (isSupported()) {
                return successRemark;
            }
        } catch (Exception ex) {
            if (!isHttps) {
                isHttps = true;
                return getStatus("(Via HTTPS)");
            }
            Logger.getLogger(GenericSiteChecker.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(resultHtml);
        return failureRemark;
    }

    private void prepareInvoker() {
        invoker.setUrl(url);
        invoker.enableRedirection();
    }

    protected boolean isSupported() throws Exception {
        prepareInvoker();
        fixSsl();
        resultHtml = invoker.getStringData();
        invoker.closeNReleaseResource();
        return true;
    }
}
