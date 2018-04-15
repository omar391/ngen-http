package com.astronlab.ngenhttplib.http.core.impl;

public interface IHttpProgressListener {

    String CONNECTING_MSG = "Connecting..";
    String FAILS_MSG = "Failed";
    String SUCCESSFUL_MSG = "Working";
    String RETRY_MSG = "Retrying..";

    enum Status {
        FAILS, SUCCESS, RUNNING
    }

    enum UpdateType {
        DOWNLOAD, UPLOAD, STATUS, HEADER
    }

    void notifyListener(Status stateType, UpdateType updateType, Object value);
}