package com.astronlab.ngenhttplib.utils.web

import groovy.transform.CompileStatic

@CompileStatic
class WebHttpResult {
    //----data states---
    enum HTTP_VERIFICATION_STATE {
        NOT_VERIFIED, FAILED, PASSED
    }

    final String html
    final HTTP_VERIFICATION_STATE desiredDataMatchState
    final HTTP_VERIFICATION_STATE sessionVerificationState
    final HTTP_VERIFICATION_STATE illegalDataVerificationState

    WebHttpResult(String html, HTTP_VERIFICATION_STATE patternFound, HTTP_VERIFICATION_STATE sessionVerificationState, HTTP_VERIFICATION_STATE illegalDataVerificationState) {
        this.html = html
        this.desiredDataMatchState = patternFound
        this.sessionVerificationState = sessionVerificationState
        this.illegalDataVerificationState = illegalDataVerificationState
    }
}