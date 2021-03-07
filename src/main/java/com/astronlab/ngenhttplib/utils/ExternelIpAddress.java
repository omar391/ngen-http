package com.astronlab.ngenhttplib.utils;

import com.astronlab.ngenhttplib.store.ValuesStore;
import com.astronlab.ngenhttplib.core.HttpInvoker;

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
            ip = matchIp(invoker.init(ValuesStore.Http.extIpApiList[i]).getStringData());
            if (ip != null) {
                break;
            }
        }
        if (ip == null) {
            ip = matchIp(invoker.init(ValuesStore.Http.extIpUrl).getStringData());
            if (ip == null) {
                throw new Exception("External IP parsing returns null");
            }
        }
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