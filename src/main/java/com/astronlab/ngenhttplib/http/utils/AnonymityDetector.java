package com.astronlab.ngenhttplib.http.utils;

import com.astronlab.ngenhttplib.http.core.HttpInvoker;
import com.astronlab.ngenhttplib.http.core.InvokerRequest;
import com.astronlab.ngenhttplib.store.ValuesStore;

import java.net.Proxy;

public class AnonymityDetector {

    /* n = none/transparent
     * a = annonymous
     * h = high annonymous/elite
     */
    private String result = "u";
    private Proxy.Type proxyType;
    private final String ip;
    private final int port;
    private final InvokerRequest invokerRequest;

    public AnonymityDetector(String ip, int port) throws Exception {
        this.port = port;
        this.ip = ip;
        proxyType = port == 1080 ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        invokerRequest = new HttpInvoker().init(ValuesStore.Http.judgeUrl + "?ip=" + new ExternelIpAddress().getAddress()).setProxy(ip, port, proxyType).setConnectionTimeOut(30000).setSocketTimeOut(30000);
    }

    private void executeRequest() throws Exception {
        try {
            result = invokerRequest.execute().getStringData();
        } catch (Exception e) {
            if (proxyType.equals(Proxy.Type.SOCKS)) {
                proxyType = Proxy.Type.HTTP;
            } else {
                proxyType = Proxy.Type.SOCKS;
            }

            invokerRequest.setProxy(ip, port, Proxy.Type.SOCKS);
            result = invokerRequest.execute().getStringData();
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
