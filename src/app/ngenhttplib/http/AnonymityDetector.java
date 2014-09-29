package app.ngenhttplib.http;

import app.ngenhttplib.store.ValuesStore;
import java.net.Proxy;

public class AnonymityDetector {

    /* n = none/transparent
     * a = annonymous
     * h = high annonymous/elite
     */
    private String result = "u";
    private final String STATUS_NONE = "None";
    private final String STATUS_ANON = "Annonymous";
    private final String STATUS_ELITE = "Elite";
    private final String STATUS_UNKNOWN = "Unknown";
    private final String STATUS_SOCKS = "Socks";
    private Proxy.Type proxyType = Proxy.Type.HTTP;

    public AnonymityDetector(String ip, int port) throws Exception {
        HttpInvoker invoker = new HttpInvoker(ValuesStore.Http.judgeUrl + "?ip=" + new ExternelIpAddress().getAddress());
        invoker.setConnectionTimeOut(30000);
        invoker.setSocketTimeOut(30000);
        proxyType = port == 1080 ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        invoker.setProxy(ip, port, proxyType);
        invoker.addPresetHeaderSet();
        try {
            result = invoker.getStringData();
        } catch (Exception e) {
            if (proxyType.equals(Proxy.Type.SOCKS)) {
                proxyType = Proxy.Type.HTTP;
            } else {
                proxyType = Proxy.Type.SOCKS;
            }
            invoker.setProxy(ip, port, proxyType);
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
