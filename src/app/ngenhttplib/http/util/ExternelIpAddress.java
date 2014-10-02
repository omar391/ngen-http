package app.ngenhttplib.http.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.ngenhttplib.http.HttpInvoker;
import app.ngenhttplib.store.ValuesStore;

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
            invoker.setUrl(ValuesStore.Http.extIpApiList[i]);
            ip = matchIp(invoker.getStringData());
            if (ip != null) {
                break;
            }
        }
        if (ip == null) {
            invoker.setUrl(ValuesStore.Http.extIpUrl);
            ip = matchIp(invoker.getStringData());
            if (ip == null) {
                invoker.close();
                throw new Exception("External IP parsing returns null");
            }
        }
        invoker.close();
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