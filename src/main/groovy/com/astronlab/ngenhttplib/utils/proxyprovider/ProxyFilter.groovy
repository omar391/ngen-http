package com.astronlab.ngenhttplib.utils.proxyprovider

import com.astronlab.commonutils.AppContext
import com.astronlab.commonutils.utils.CollectionUtils
import com.astronlab.ngenhttplib.core.client.ProxyConfig
import com.astronlab.ngenhttplib.utils.IpProperties
import groovy.transform.CompileStatic

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@CompileStatic
abstract class ProxyFilter {
    protected final AtomicInteger currentItrIdx = new AtomicInteger()
    protected volatile Map.Entry<String, ArrayList<String>>[] currentItr
    protected final List<String> curItrReturnedKeys = CollectionUtils.getSynchronizedUniqueElemList(String.class)
    protected final List<String> goodProxies = CollectionUtils.getSynchronizedUniqueElemList(String.class)
    final ArrayList<String> badProxies = (ArrayList<String>) CollectionUtils.getSynchronizedUniqueElemList(String.class)
    protected final List<String> workedOnceInThisSessionProxies = CollectionUtils.getSynchronizedUniqueElemList(String.class)


    static ProxyFilter SECURE_IP_SELECTOR() {
        return new ProxyFilter() {
            @Override
            boolean isAcceptable(String ipColonPort, ArrayList<String> proxyAttributes) {
                ProxyConfig.Type type = ProxyConfig.detectProxyType(proxyAttributes.get(ProxyResultIDX.TYPE))
                return type == ProxyConfig.Type.SOCKS_V5 || type == ProxyConfig.Type.SOCKS_V4 || type == ProxyConfig.Type.ANONYMOUS || type == ProxyConfig.Type.DISTORTING || type == ProxyConfig.Type.ELITE

            }
        }
    }

    //Note: un-calculated default speed values are -1

    abstract boolean isAcceptable(String ipColonPort, ArrayList<String> proxyAttributes);


    void ignoreProxy(String ipColonPort) {
        boolean isLocalIp = IpProperties.isPrivateIp(ipColonPort)

        if (!isLocalIp && !workedOnceInThisSessionProxies.contains(ipColonPort) && ProxyStore.getAvgPingLatency(ipColonPort) >= 0f) {
            //still have active latency means its open but not working for our case; so put it into the bad list
            badProxies.add(ipColonPort)
            goodProxies.remove(ipColonPort)

        } else {
            if (isLocalIp && ProxyStore.isPortOpen(ipColonPort)) {
                //no need to remove local ips (as they are mostly tor or other routing proxy)
                return
            }

            ArrayList<String> val = (ArrayList<String>) ProxyStore.finalProxyList.remove(ipColonPort)
            if (val != null) {

                //see if it was working previously in this session
                if (workedOnceInThisSessionProxies.contains(ipColonPort)) {
                    //schedule a recheck: no need to add into tmp list if it worked once or local ip
                    scheduleForRecheckForPreviouslyWorkingIp(ipColonPort, val)

                } else {
                    //add for re-testing later
                    ProxyStore.tmpProxyList.put(ipColonPort, val)
                }

                goodProxies.remove(ipColonPort)
            }
        }
    }

    private void scheduleForRecheckForPreviouslyWorkingIp(String ipColonPort, ArrayList<String> val) {
        AppContext.backgroundTasks.executeAfterInitialDelay({
            if (ProxyStore.isPortOpen(ipColonPort)) {
                ProxyStore.finalProxyList.put(ipColonPort, val)
            } else {
                //re schedule
                scheduleForRecheckForPreviouslyWorkingIp(ipColonPort, val)
            }
        }, 10, TimeUnit.MINUTES, 0, null, null)
    }


    void addToWorkingVerifiedProxy(String ipColonPort) {
        workedOnceInThisSessionProxies.add(ipColonPort)
    }


    int goodProxiesSize() {
        return goodProxies.size()
    }
}