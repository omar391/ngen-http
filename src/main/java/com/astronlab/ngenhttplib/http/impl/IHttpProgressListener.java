package com.astronlab.ngenhttplib.http.impl;

public interface IHttpProgressListener {

    public String CONNECTING_MSG = "Connecting..";
    public String FAILS_MSG = "Failed";
    public String SUCCESSFUL_MSG = "Working";
    public String RETRY_MSG = "Retrying..";

    public void notifyListener(Object stateType, Object updateType, Object value);
    enum Status {
        FAILS, SUCCESS, RUNNING
    }

    enum UpdateType {
        DOWNLOAD, UPLOAD, STATUS, HEADER
    }
}