package com.astronlab.ngenhttplib.http.core.misc;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class InvokerCookieJar implements CookieJar, Serializable {
    private static final long serialVersionUID = 40L;

    //Store format: HashMap<Host, HashMap<Name_Path_Scheme, Cookie>>
    private final HashMap<String, HashMap<String, Cookie>> storedCookies = new HashMap<>();

    @Override
    public void saveFromResponse(HttpUrl url,
                                 List<Cookie> cookies) {
        String hostKey = getHostKey(url);
        HashMap<String, Cookie> hostCookies = storedCookies.get(hostKey);

        if (hostCookies == null) {
            hostCookies = new HashMap<>();
            storedCookies.put(hostKey, hostCookies);
        }

        for (Cookie cookie : cookies) {
            hostCookies.put(getCookieKey(cookie), cookie);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        HashMap<String, Cookie> hostCookies = storedCookies.get(
                getHostKey(url));
        List<Cookie> cookieList = new ArrayList<>();

        if (hostCookies != null) {
            Collection<Cookie> cookieCollection = hostCookies.values();
            for (Cookie cookie : cookieCollection) {
                if (isPathMatch(url, cookie)) {
                    cookieList.add(cookie);
                }
            }
        }

        return cookieList;
    }

    private String getHostKey(HttpUrl httpUrl) {
        return httpUrl.host().replaceAll(
                ".*?([^\\s\\n:\\./]+\\.[^\\s\\n\\d:\\./]++|[\\d:\\.]++)$", "$1");
    }

    private String getCookieKey(Cookie cookie) {
        return cookie.name() + "_" + cookie.path() + "_" + cookie.secure();
    }

    private boolean isPathMatch(HttpUrl url, Cookie cookie) {
        String urlPath = url.encodedPath();
        String cookiePath = cookie.path();

        if (urlPath.equals(cookiePath)) {
            return true; // As in '/foo' matching '/foo'.
        }

        if (urlPath.startsWith(cookiePath)) {
            if (cookiePath.endsWith("/")) {
                return true; // As in '/' matching '/foo'.
            }
            return urlPath.charAt(cookiePath.length()) == '/';
        }

        return false;
    }

    public void removeAllCookies(){
        storedCookies.clear();
    }
}
