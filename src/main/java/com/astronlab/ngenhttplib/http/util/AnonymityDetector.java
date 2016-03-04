package com.astronlab.ngenhttplib.http.util;

import com.astronlab.ngenhttplib.http.HttpInvoker;
import com.astronlab.ngenhttplib.store.ValuesStore;

import java.net.Proxy;

public class AnonymityDetector {

    private final String STATUS_NONE = "None";
    private final String STATUS_ANON = "Annonymous";
    private final String STATUS_ELITE = "Elite";
    private final String STATUS_UNKNOWN = "Unknown";
    private final String STATUS_SOCKS = "Socks";
    /* n = none/transparent
     * a = annonymous
     * h = high annonymous/elite
     */
    private String result = "u";
    private Proxy.Type proxyType = Proxy.Type.HTTP;

    public AnonymityDetector(String ip, int port) throws Exception {
        HttpInvoker invoker = new HttpInvoker(ValuesStore.Http.judgeUrl + "?ip=" + new ExternelIpAddress().getAddress());
        invoker.config().setConnectionTimeOut(30000).setSocketTimeOut(30000).update();
        proxyType = port == 1080 ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        invoker.config().setProxy(ip, port, proxyType).addPresetRequestHeadersSet().update();
        try {
            result = invoker.getStringData();
        } catch (Exception e) {
            if (proxyType.equals(Proxy.Type.SOCKS)) {
                proxyType = Proxy.Type.HTTP;
            } else {
                proxyType = Proxy.Type.SOCKS;
            }
            invoker.config().setProxy(ip, port, proxyType).update();
            result = invoker.getStringData();
        }
        System.out.println(result);
    }

    public String getStatus() {
        if (result.equalsIgnoreCase("u")) {
            return STATUS_UNKNOWN;
        } else if (result.equalsIgnoreCase("n")) {
            return STATUS_NONE;
        } else if (result.equalsIgnoreCase("a")) {
            return STATUS_ANON;
        } else {
            if (proxyType.equals(Proxy.Type.SOCKS)) {
                return STATUS_SOCKS;
            } else {
                return STATUS_ELITE;
            }
        }
    }
}
