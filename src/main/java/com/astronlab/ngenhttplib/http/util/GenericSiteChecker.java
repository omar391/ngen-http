package com.astronlab.ngenhttplib.http.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.astronlab.ngenhttplib.http.HttpInvoker;

public class GenericSiteChecker {

    private boolean isHttps = false;
    protected HttpInvoker invoker;
    private final String failureRemark = "Failed";
    protected String resultHtml = "";
    protected String successRemark = "Working";
    private String url;

    public GenericSiteChecker() {
        invoker = new HttpInvoker();
    }

    public GenericSiteChecker setInvoker(HttpInvoker invoker) {
        this.invoker = invoker;
        return this;
    }

    public HttpInvoker getInvoker() {
        return this.invoker;
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
        invoker.releaseConnection();
        return true;
    }
}
