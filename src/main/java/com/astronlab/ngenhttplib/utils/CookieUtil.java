package com.astronlab.ngenhttplib.utils;

import com.astronlab.ngenhttplib.core.misc.InvokerCookieJar;
import okhttp3.HttpUrl;
import org.openqa.selenium.Cookie;

import java.util.Date;
import java.util.Set;

public class CookieUtil {
    public static InvokerCookieJar seleniumCookiesToInvokerCookieJar(Set<Cookie> cookieSet) {
        final InvokerCookieJar cookieJar = new InvokerCookieJar();
        for (Cookie selCookie : cookieSet) {
            okhttp3.Cookie okCookie = seleniumToOkHttpCookie(selCookie);
            cookieJar.saveFromResponse(selCookie.getDomain(), okCookie);
        }

        return cookieJar;
    }

    //left for reference only
    public static HttpUrl getUrl(Cookie cookie) {
        return new HttpUrl.Builder().host(cookie.getDomain()).encodedPath(cookie.getPath()).scheme(cookie.isSecure() ? "https" : "http").build();
    }

    public static okhttp3.Cookie seleniumToOkHttpCookie(Cookie seleniumCookie) {
        final Date expiry = seleniumCookie.getExpiry();
        final long time = (expiry == null ? 0 : expiry.getTime());
        okhttp3.Cookie.Builder okHttpCookieBuilder = new okhttp3.Cookie.Builder()
                .name(seleniumCookie.getName())
                .value(seleniumCookie.getValue())
                .expiresAt(time)
                .domain(seleniumCookie.getDomain())
                .path(seleniumCookie.getPath());

        if (seleniumCookie.isHttpOnly()) {
            okHttpCookieBuilder.httpOnly();
        }

        if (seleniumCookie.isSecure()) {
            okHttpCookieBuilder.secure();
        }

        return okHttpCookieBuilder.build();
    }

}
