package com.astronlab.ngenhttplib.utils;

import com.astronlab.ngenhttplib.store.ValuesStore;
import com.astronlab.ngenhttplib.core.HttpInvoker;
import com.astronlab.ngenhttplib.core.InvokerRequest;
import com.astronlab.ngenhttplib.core.client.ProxyConfig;

import java.net.Proxy;

public class AnonymityDetector {

    /* n = none/transparent
     * a = annonymous
     * h = high annonymous/elite
     */
    private String result = "u";
    private final Proxy.Type proxyType;
    private final String ip;
    private final int port;
    private final InvokerRequest invokerRequest;

    public AnonymityDetector(String ip, int port) throws Exception {
        this.port = port;
        this.ip = ip;
        proxyType = port == 1080 ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        invokerRequest = new HttpInvoker().init(ValuesStore.Http.judgeUrl + "?ip=" + new ExternelIpAddress().getAddress()).setProxy(ip, port, proxyType).setConnectionTimeOut(30000).setSocketReadTimeOut(30000);
    }

    private void executeRequest() throws Exception {
        try {
            result = invokerRequest.getStringData();
        } catch (Exception e) {
            invokerRequest.setProxy(ip, port, ProxyConfig.Type.SOCKS_V5);
            result = invokerRequest.getStringData();
        }

        System.out.println(result);
    }

    public String getStatus() throws Exception {
        executeRequest();

        if (result.equalsIgnoreCase("u")) {
            String STATUS_UNKNOWN = "Unknown";
            return STATUS_UNKNOWN;
        } else if (result.equalsIgnoreCase("n")) {
            String STATUS_NONE = "None";
            return STATUS_NONE;
        } else if (result.equalsIgnoreCase("a")) {
            String STATUS_ANON = "Annonymous";
            return STATUS_ANON;
        } else {
            if (proxyType.equals(Proxy.Type.SOCKS)) {
                String STATUS_SOCKS = "Socks";
                return STATUS_SOCKS;
            } else {
                String STATUS_ELITE = "Elite";
                return STATUS_ELITE;
            }
        }
    }
}
