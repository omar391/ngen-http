package com.astronlab.ngenhttplib.core.client;

import java.net.Proxy;
import java.util.regex.Pattern;

public class ProxyConfig {

    public enum Type {
        DIRECT(Proxy.Type.DIRECT),
        HTTP(Proxy.Type.HTTP),
        HTTPS(Proxy.Type.HTTP),
        ANONYMOUS(Proxy.Type.HTTP),
        CODEEN(Proxy.Type.HTTP),
        DISTORTING(Proxy.Type.HTTP),
        TRANSPARENT(Proxy.Type.HTTP),
        ELITE(Proxy.Type.HTTP),
        SOCKS_V4(null),
        SOCKS_V5(Proxy.Type.SOCKS);

        public final Proxy.Type value;

        Type(Proxy.Type value) {
            this.value = value;
        }
    }


    public static Type detectProxyType(String type) {
        if (Pattern.compile("(?i)Socks.{0,3}?4").matcher(type).find()) {
            return Type.SOCKS_V4;

        } else if (Pattern.compile("(?i)Socks.{0,3}?5").matcher(type).find()) {
            return Type.SOCKS_V5;

        } else if (Pattern.compile("(?i)\\bHTTP\\b").matcher(type).find()) {
            return Type.HTTP;

        } else if (Pattern.compile("(?i)\\bHTTPS\\b").matcher(type).find()) {
            return Type.HTTPS;

        } else if (Pattern.compile("(?i)\\bANONYMOUS\\b").matcher(type).find()) {
            return Type.ANONYMOUS;

        } else if (Pattern.compile("(?i)\\bCODEEN\\b").matcher(type).find()) {
            return Type.CODEEN;

        } else if (Pattern.compile("(?i)\\bDISTORTING\\b").matcher(type).find()) {
            return Type.DISTORTING;

        } else if (Pattern.compile("(?i)\\bTRANSPARENT\\b").matcher(type).find()) {
            return Type.TRANSPARENT;

        } else if (Pattern.compile("(?i)\\bELITE\\b").matcher(type).find()) {
            return Type.ELITE;

        }

        return Type.DIRECT;
    }
}
