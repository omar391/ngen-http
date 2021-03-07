package com.astronlab.ngenhttplib.utils.web

import com.astronlab.commonutils.interfaces.KeyValueDatabase
import com.astronlab.ngenhttplib.core.HttpInvoker
import com.astronlab.ngenhttplib.utils.proxyprovider.ProxyFilter
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import java.util.regex.Pattern

@CompileStatic
class WebHttpOptions {
    @PackageScope
    static int longestCoolDownTime = 0
    HttpInvoker invoker
    int retryCount
    boolean verifySessionState
    boolean verifyIllegalDataState
    Pattern defaultIllegalDataPattern
    int maxyCycleCountOnProxyFailure
    ProxyFilter proxyFilter
    boolean randomizeUserAgent
    boolean isHttpLogEnabled
    int ipCoolDownTimeInSec
    KeyValueDatabase prioritizedCacheDb
    boolean useIllegalDataPatternIfDesiredDataPatternsNotProvided

    WebHttpOptions(ProxyFilter proxyFilter = null, int maxyCycleCountOnProxyFailure = 10, int ipCoolDownTimeInSec = 20,
                   boolean verifySessionState = false, boolean verifyIllegalDataState = true, int retryCount = 3, KeyValueDatabase prioritizedCacheDb = null,
                   HttpInvoker invoker = new HttpInvoker(), boolean randomizeUserAgent = true, boolean isHttpLogEnabled = true,
                   boolean useIllegalDataPatternIfDesiredDataPatternsNotProvided = true,
                   Pattern defaultIllegalDataPattern = Pattern.compile("(?s)(?>(?>unauthorized|restrict\\w*) access|access denied|i(?>nvalid request|llegal data))")) {

        this.invoker = invoker
        this.retryCount = retryCount
        this.verifySessionState = verifySessionState
        this.verifyIllegalDataState = verifyIllegalDataState
        this.defaultIllegalDataPattern = defaultIllegalDataPattern
        this.maxyCycleCountOnProxyFailure = maxyCycleCountOnProxyFailure
        this.proxyFilter = proxyFilter
        this.randomizeUserAgent = randomizeUserAgent
        setIpCoolDownTimeInSec(ipCoolDownTimeInSec)
        this.isHttpLogEnabled = isHttpLogEnabled
        this.prioritizedCacheDb = prioritizedCacheDb
        this.useIllegalDataPatternIfDesiredDataPatternsNotProvided = useIllegalDataPatternIfDesiredDataPatternsNotProvided
    }

    WebHttpOptions useIllegalDataPatternIfDesiredDataPatternsNotProvided(boolean useIllegalDataPatternIfDesiredDataPatternsNotProvided) {
        this.useIllegalDataPatternIfDesiredDataPatternsNotProvided = useIllegalDataPatternIfDesiredDataPatternsNotProvided
        return this
    }

    WebHttpOptions setPriorityCache(KeyValueDatabase prioritizedCacheDb) {
        this.prioritizedCacheDb = prioritizedCacheDb
        return this
    }

    WebHttpOptions setIsHttpLogEnabled(boolean isHttpLogEnabled) {
        this.isHttpLogEnabled = isHttpLogEnabled
        return this
    }

    WebHttpOptions setInvoker(HttpInvoker invoker) {
        this.invoker = invoker
        return this
    }

    WebHttpOptions setIpCoolDownTimeInSec(int ipCoolDownTimeInSec) {
        this.ipCoolDownTimeInSec = ipCoolDownTimeInSec
        if (longestCoolDownTime < ipCoolDownTimeInSec) {
            longestCoolDownTime = ipCoolDownTimeInSec
        }
        return this
    }

    WebHttpOptions setRetryCount(int retryCount) {
        this.retryCount = retryCount
        return this
    }

    WebHttpOptions setVerifySessionState(boolean verifySessionState) {
        this.verifySessionState = verifySessionState
        return this
    }

    WebHttpOptions setVerifyIllegalDataState(boolean verifyIllegalDataState) {
        this.verifyIllegalDataState = verifyIllegalDataState
        return this
    }

    WebHttpOptions setDefaultIllegalDataPattern(Pattern defaultIllegalDataPattern) {
        this.defaultIllegalDataPattern = defaultIllegalDataPattern
        return this
    }

    WebHttpOptions setMaxyCycleCountOnProxyFailure(int maxyCycleCountOnProxyFailure) {
        this.maxyCycleCountOnProxyFailure = maxyCycleCountOnProxyFailure
        return this
    }

    WebHttpOptions setProxyFilter(ProxyFilter proxyFilter) {
        this.proxyFilter = proxyFilter
        return this
    }

    WebHttpOptions setProxyFilter(boolean randomizeUserAgent) {
        this.randomizeUserAgent = randomizeUserAgent
        return this
    }
}