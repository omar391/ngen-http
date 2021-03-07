package com.astronlab.ngenhttplib.utils.proxyprovider

import groovy.transform.CompileStatic

import java.util.regex.Pattern

@CompileStatic
class ProxyStoreContext {
    Map<String, String> sites =
        [
                xroxy: "https://www.xroxy.com",
        ]


    //proxy store format: key=ip:port val=[type, country_code, speed_index]
    LinkedHashMap<String, LinkedHashMap<Integer, String>> attrs() {
        [
                (sites.xroxy): [
                        (K_INDEX_PATH): "/proxylist.php?port=&type=&ssl=&country=&latency=&reliability=&sort=reliability&desc=true&pnum=0#table"/*"/proxylist.php?port=&type=All_socks&ssl=&country=&latency=&reliability=&sort=reliability&desc=true&pnum=0#table"*/,
                        (K_REQ_TYPE)  : "GET",
                ]
        ]
    }

    //key is the site root (K_ROOT)
    LinkedHashMap<String, LinkedHashMap<Integer, Pattern>> patterns() {
        [
                (sites.xroxy): [
                        (K_PROXY_REGEX)                  : Pattern.compile("(?s)Proxy details.>((?:\\d+\\.){3}+\\d+).*?port number.(\\d+).*?type.>([^<]++).*?ext_(\\w+)"),
                        (K_PAGINATION_REGEX)             : Pattern.compile("\\[<a\\b[^>]+?href=.([^'\"]++)[^>]++>\\d+<"),
                        (K_PROXY_VALID_DATA_EXISTS_REGEX): Pattern.compile("<form[^>]+?action=./proxylist.php#table.[^>]*>"),
                ]
        ]
    }

    //key is the site root (K_ROOT)
    LinkedHashMap<String, LinkedHashMap<Integer, Closure<String>>> transformers() {
        [
                (sites.xroxy): [
                        (K_PAGINATION_SANITIZER): { String v, i, j -> v.replaceAll(/&amp;/, "&") },
                ]
        ]
    }

    //-------------- SiteKeys ---
    public static int K_INDEX_PATH = 1
    public static int K_PROXY_REGEX = 2
    public static int K_PAGINATION_REGEX = 3
    public static int K_REQ_TYPE = 4
    public static int K_PAGINATION_SANITIZER = 5
    public static int K_PROXY_VALID_DATA_EXISTS_REGEX = 6
}
