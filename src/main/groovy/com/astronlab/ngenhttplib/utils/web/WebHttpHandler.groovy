package com.astronlab.ngenhttplib.utils.web

import com.astronlab.commonutils.AppContext
import com.astronlab.commonutils.utils.FileUtils
import com.astronlab.ngenhttplib.core.InvokerRequest
import com.astronlab.ngenhttplib.core.InvokerResponse
import com.astronlab.ngenhttplib.core.client.ProxyConfig
import com.astronlab.ngenhttplib.utils.IpProperties
import com.astronlab.ngenhttplib.utils.proxyprovider.ProxyResultIDX
import com.astronlab.ngenhttplib.utils.proxyprovider.ProxyStore
import groovy.transform.CompileStatic
import org.apache.log4j.Level
import org.apache.log4j.Logger

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Collectors

@CompileStatic
class WebHttpHandler {
    final Logger logger = Logger.getLogger(WebHttpHandler.class.name)
    final WebHttpOptions defaultOptions
    private static final Map<String, Map<String, Long>> ipLastUseTime = [:]
    private static final ConcurrentHashMap<String, ReentrantLock> hostIpLockMap = new ConcurrentHashMap(100, 0.9f, 2)


    static {
        AppContext.backgroundTasks.executeWithPeriodicDelay({
            ipLastUseTime.entrySet().parallelStream().forEach({ ipPortEntry ->
                ipPortEntry.value.keySet().parallelStream()
                        .filter({ ipPort ->
                            ReentrantLock lock = hostIpLockMap[ipPortEntry.key + ipPort]
                            if (lock == null) {
                                return ipPortEntry.value[ipPort] + WebHttpOptions.longestCoolDownTime * 1000 > new Date().time
                            }

                            //remove the reentrant lock from the map if no other thread holds it
                            if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                                synchronized (ipPortEntry.value) {
                                    if (!lock.isLocked() && !lock.hasQueuedThreads()) {
                                        hostIpLockMap.remove(ipPortEntry.key + ipPort)
                                    }
                                }
                                return ipPortEntry.value[ipPort] + WebHttpOptions.longestCoolDownTime * 1000 > new Date().time
                            } else {
                                return false
                            }
                        })
                        .collect(Collectors.toList()).parallelStream().forEach({ ipPortEntry.value.remove(it) })
            })
        }, 30L, 60L, TimeUnit.MINUTES, 0, null, null)
    }

    @CompileStatic
    WebHttpHandler(WebHttpOptions defaultOptions = new WebHttpOptions()) {
        this.defaultOptions = defaultOptions

        if (!defaultOptions.invoker.isCacheActive()) {
            defaultOptions.invoker = defaultOptions.invoker.config().setCache(FileUtils.getFileFromCWD("./cache/http_cache").canonicalPath, 1024).build()
        }
    }

    @CompileStatic
    WebHttpResult.HTTP_VERIFICATION_STATE isWebSessionAvailable(String html, InvokerResponse response) {
        return WebHttpResult.HTTP_VERIFICATION_STATE.NOT_VERIFIED
    }


    /**
     * Illegal data contains these strings: i.e.: unauthorized access, invalid request, illegal data, access denied string etc
     *
     * @param html data
     * @return return true if illegal data is present
     */
    @CompileStatic
    WebHttpResult.HTTP_VERIFICATION_STATE isDataContainsNoIllegalState(String data, InvokerResponse response) {
        return !defaultOptions.defaultIllegalDataPattern.matcher(data).find() ? WebHttpResult.HTTP_VERIFICATION_STATE.PASSED : WebHttpResult.HTTP_VERIFICATION_STATE.FAILED
    }

    /**
     * Directly call this method or call via creating an invoke method as necessary
     * @param key
     * @param invokerRequest
     * @param desiredDataMatcher
     * @param stateVerifier
     * @return
     */
    @CompileStatic
    WebHttpResult invoke(String url, Closure<Boolean> desiredDataMatcher = null, WebHttpOptions options = defaultOptions) {
        return invoke(defaultOptions.invoker.init(url), desiredDataMatcher, options)
    }


    @CompileStatic
    WebHttpResult invoke(InvokerRequest invokerRequest, Closure<Boolean> desiredDataMatcher = null, WebHttpOptions options = defaultOptions) {

        WebHttpResult.HTTP_VERIFICATION_STATE patternState, illegalDataState
        InvokerResponse[] response = new InvokerResponse[1]
        String html = ""

        if (options.prioritizedCacheDb) {
            //see if we can get the stored html in the key-value db
            html = options.prioritizedCacheDb.get(invokerRequest.url)
        }

        if (!html) {
            if (!options.isHttpLogEnabled) {
                invokerRequest.logger.setLevel(Level.OFF)
                logger.setLevel(Level.OFF)
            }

            //set time out to 5 min
            invokerRequest.setConnectionTimeOut(300000).setSocketReadTimeOut(300000)

            if (options.randomizeUserAgent) {
                invokerRequest.randomizeUserAgentHeader()
            }

            String host = new URI(invokerRequest.getUrl()).host

            if (options.proxyFilter) {
                WebHttpResult.HTTP_VERIFICATION_STATE[] states = new WebHttpResult.HTTP_VERIFICATION_STATE[2]
                html = downloadViaProxy(invokerRequest, desiredDataMatcher, options, states, response, host)
                patternState = states[0]
                illegalDataState = states[1]

            } else {
                html = download(invokerRequest, options.retryCount, response, options, host)
            }
        }

        if (patternState == null) {
            patternState = (desiredDataMatcher != null) ? (desiredDataMatcher(html, response[0]) ? WebHttpResult.HTTP_VERIFICATION_STATE.PASSED : WebHttpResult.HTTP_VERIFICATION_STATE.FAILED) : options.useIllegalDataPatternIfDesiredDataPatternsNotProvided ? isDataContainsNoIllegalState(html, response[0]) : WebHttpResult.HTTP_VERIFICATION_STATE.NOT_VERIFIED
        }

        if (illegalDataState == null) {
            illegalDataState = options.verifyIllegalDataState ? isDataContainsNoIllegalState(html, response[0]) : WebHttpResult.HTTP_VERIFICATION_STATE.NOT_VERIFIED
        }

        WebHttpResult.HTTP_VERIFICATION_STATE sessionState = options.verifySessionState ? isWebSessionAvailable(html, response[0]) : WebHttpResult.HTTP_VERIFICATION_STATE.NOT_VERIFIED

        if (options.prioritizedCacheDb && patternState != WebHttpResult.HTTP_VERIFICATION_STATE.FAILED && sessionState != WebHttpResult.HTTP_VERIFICATION_STATE.FAILED && illegalDataState != WebHttpResult.HTTP_VERIFICATION_STATE.FAILED) {
            //store data in the provided db if all are ok, for 30 days
            options.prioritizedCacheDb.put(invokerRequest.url, html, false, 30, TimeUnit.DAYS, false)
        }

        return new WebHttpResult(html, patternState, sessionState, illegalDataState)
    }


    private static long currentIpRemainingCoolDownTime(int ipCoolDownTimeInSec, String host, String ipPort) {
        Long waitTime = ipLastUseTime.get(host)?.get(ipPort)
        waitTime = waitTime == null ? 0 : waitTime + ipCoolDownTimeInSec * 1000 - new Date().time

        return waitTime < 1 ? 0 : waitTime
    }

    private static boolean tryIpLock(String host, String ipPort) {
        ReentrantLock lock = hostIpLockMap.get(host + ipPort)

        //try to create a lock instance if null
        if (lock == null) {
            //check if host related map is null
            Map<String, Long> ipWaitMap = ipLastUseTime[host]
            if (ipWaitMap == null) {
                synchronized (ipLastUseTime) {
                    ipWaitMap = ipLastUseTime[host]
                    if (ipWaitMap == null) {
                        ipWaitMap = ipLastUseTime[host] = new HashMap<String, Long>()
                    }
                }
            }

            //create lock for the host-ip-port
            synchronized (ipWaitMap) {
                String key = host + ipPort
                lock = (ReentrantLock) hostIpLockMap.get(key)
                if (lock == null) {
                    lock = new ReentrantLock()
                    hostIpLockMap.put(key, lock)
                    return lock.tryLock()
                }
            }
        }

        return lock.tryLock()
    }

    private static boolean isIpLocked(String hostIpPort) {
        ReentrantLock lock = hostIpLockMap.get(hostIpPort)
        return lock != null && lock.isLocked()
    }

    private static void unlockIp(String host, String ipPort) {
        ReentrantLock lock = hostIpLockMap.get(host + ipPort)
        if (lock != null) {
            //unlock current threads
            while (lock.holdCount > 0) {
                lock.unlock()
            }
        }
    }


    @CompileStatic
    private String downloadViaProxy(InvokerRequest invokerRequest, Closure<Boolean> desiredDataMatcher, WebHttpOptions options, WebHttpResult.HTTP_VERIFICATION_STATE[] states, InvokerResponse[] response, String host) {
        //return list of next pages; you can infer if there are any page remains

        ProxyConfig.Type[] typeArr = new ProxyConfig.Type[3]
        Map.Entry<String, ArrayList<String>> proxyEntry, lowestWaitTimeProxyEntry
        String[] proxyPart
        String data
        REQUEST_STATUS[] req_status = new REQUEST_STATUS[1]
        int cycleCount = 0, waitPassCount = 0
        long lowestWaitTime = Long.MAX_VALUE, waitTime

        while ((proxyEntry = ProxyStore.getNextProxy(options.proxyFilter)) != null) {
            //check ip lock
            if (isIpLocked(host + proxyEntry.key)) {
                continue
            }

            //check wait time for this proxy
            if ((waitTime = currentIpRemainingCoolDownTime(options.ipCoolDownTimeInSec, host, proxyEntry.key)) != 0L) {
                //check and store lowest wait time
                if (waitPassCount < options.proxyFilter.goodProxiesSize()) {

                    if (lowestWaitTime > waitTime) {
                        lowestWaitTime = waitTime
                        lowestWaitTimeProxyEntry = proxyEntry
                    }

                    waitPassCount++
                    continue
                } else if (lowestWaitTimeProxyEntry) {
                    proxyEntry = lowestWaitTimeProxyEntry
                }
            }

            //now try to lock this ip for our use
            if (!tryIpLock(host, proxyEntry.key)) {
                //cant lock; keep trying
                waitPassCount--
                continue
            }

            proxyPart = proxyEntry.key.split(":")
            typeArr[0] = ProxyConfig.detectProxyType(proxyEntry.value[ProxyResultIDX.TYPE])

            if (typeArr[0] == ProxyConfig.Type.SOCKS_V4) {
                typeArr[1] = ProxyConfig.Type.SOCKS_V5
                typeArr[2] = ProxyConfig.Type.HTTP

            } else if (typeArr[0] == ProxyConfig.Type.SOCKS_V5) {
                typeArr[1] = ProxyConfig.Type.SOCKS_V4
                typeArr[2] = ProxyConfig.Type.HTTP
            } else {
                typeArr[1] = ProxyConfig.Type.SOCKS_V4
                typeArr[2] = ProxyConfig.Type.SOCKS_V5
            }

            String ip = proxyPart[0]
            Integer port = Integer.parseInt(proxyPart[1])
            boolean isTypeVerified = proxyEntry.value[ProxyResultIDX.TYPE_VERIFIED] == "1"

            int retryCount = options.retryCount
            String tag = "Cycle count:" + cycleCount + ", " + proxyEntry.key + ":" + proxyEntry.value[ProxyResultIDX.COUNTRY_CODE] + ":"
            for (type in typeArr) {
                invokerRequest.setProxy(ip, port, type)
                invokerRequest.extraRequestConfigs.tag(tag + type.name())
                data = download(invokerRequest, retryCount, response, options, host, proxyEntry.key, req_status)

                if (options.useIllegalDataPatternIfDesiredDataPatternsNotProvided) {
                    states[1] = isDataContainsNoIllegalState(data, response[0])
                }

                if ((desiredDataMatcher != null && desiredDataMatcher(data, response[0])) || states[1] == WebHttpResult.HTTP_VERIFICATION_STATE.PASSED) {
                    //update proper type
                    proxyEntry.value[ProxyResultIDX.TYPE] = type.name()

                    //type_verified_bit set on network data
                    if (response[0].responseObj.networkResponse() != null) {
                        proxyEntry.value[ProxyResultIDX.TYPE_VERIFIED] = "1"
                        defaultOptions.proxyFilter.addToWorkingVerifiedProxy(proxyEntry.key)
                    }
                    states[0] = WebHttpResult.HTTP_VERIFICATION_STATE.PASSED
                    return data

                } else if (data) {
                    if (!options.verifyIllegalDataState) {
                        if (states[1] == null) {
                            states[1] = WebHttpResult.HTTP_VERIFICATION_STATE.NOT_VERIFIED
                        }
                        return data
                    }
                    if (states[1] == WebHttpResult.HTTP_VERIFICATION_STATE.FAILED) {
                        logger.warn("Illegal data state found in => " + tag + type.name())
                    } else {
                        //no illegal pattern in the data; we can return this data
                        return data
                    }
                    //there is data - good or bad; so no need to go with loop
                    break

                } else if (isTypeVerified || IpProperties.isPrivateIp(proxyEntry.key) || data == null) {
                    break
                }
                invokerRequest.removeProxy()
                retryCount = 1
            }

            //add into bad proxy list if there is illegal state or desired data is not found while ip is still not type verified
            if ((desiredDataMatcher != null && !isTypeVerified)
                    || states[1] == WebHttpResult.HTTP_VERIFICATION_STATE.FAILED
                    || req_status[0] == REQUEST_STATUS.REQ_REJECTED) {
                    options.proxyFilter.ignoreProxy(proxyEntry.key)
            }

            if (++cycleCount == options.maxyCycleCountOnProxyFailure) {
                break
            }

            //reset wait pass vars
            lowestWaitTime = Long.MAX_VALUE
            waitPassCount = 0
        }

        if (proxyPart != null) {
            invokerRequest.removeProxy()
        }
        return download(invokerRequest, options.retryCount, response, options, host)
    }

    private enum REQUEST_STATUS {
        REQ_REJECTED
    }

    @CompileStatic
    private String download(InvokerRequest invokerRequest, int retryCount, InvokerResponse[] response, WebHttpOptions options, String host, String ipPort = "DIRECT", REQUEST_STATUS[] status=null ) {
        //acquire ip lock first
        tryIpLock(host, ipPort)

        //wait till ip cool down
        long waitTime = currentIpRemainingCoolDownTime(options.ipCoolDownTimeInSec, host, ipPort)
        if (waitTime > 0) {
            Thread.sleep(waitTime)
        }

        int itr = 0
        int timeOutTry = 0

        String data = ""
        while (itr < retryCount) {
            try {
                invokerRequest.execute({
                    data = it.stringData
                    response[0] = it
                })

                //update ip use time
                if (response[0].getResponseObj().networkResponse() != null) {
                    ipLastUseTime[host][ipPort] = new Date().getTime()
                }
                break

            } catch (Exception e) {
                if ((e instanceof UnknownHostException)) {
                    //don't blacklist: either network is disconnected or website doesn't exists
                    return null
                }

                if((e.message =~ /(?i)\b(?>Failed to connect|Connection refused|authentication failed|valid certification)\b/).find()){
                    //blacklist: bad errs = proxy port is down, proxy needs auth
                    status?[0] = REQUEST_STATUS.REQ_REJECTED
                    return null
                }

                if (options.isHttpLogEnabled) {
                    logger.debug("Error: " + e.message)
                }
                itr++

            }
        }

        //unlock ip
        unlockIp(host, ipPort)

        return data
    }
}


