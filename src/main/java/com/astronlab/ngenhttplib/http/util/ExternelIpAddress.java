package com.astronlab.ngenhttplib.http.util;

import com.astronlab.ngenhttplib.http.HttpInvoker;
import com.astronlab.ngenhttplib.store.ValuesStore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExternelIpAddress {

    public String getAddress() throws Exception {
        if (ValuesStore.Http.externalIp == null) {
            ValuesStore.Http.externalIp = fetchIpAddress();
        }
        return ValuesStore.Http.externalIp;
    }

    private String fetchIpAddress() throws Exception {
        int i = 0, urlListLen = ValuesStore.Http.extIpApiList.length;
        String ip = null;
        HttpInvoker invoker = new HttpInvoker();
        for (; i < urlListLen; i++) {
            invoker.config().setUrl(ValuesStore.Http.extIpApiList[i]).update();
            ip = matchIp(invoker.getStringData());
            if (ip != null) {
                break;
            }
        }
        if (ip == null) {
            invoker.config().setUrl(ValuesStore.Http.extIpUrl).update();
            ip = matchIp(invoker.getStringData());
            if (ip == null) {
                throw new Exception("External IP parsing returns null");
            }
        }
        invoker.closeNReleaseResource();
        return ip;
    }

    private String matchIp(String html) {
        Matcher matcher = Pattern.compile("(?s)(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})").matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}