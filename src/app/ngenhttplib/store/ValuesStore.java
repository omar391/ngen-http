package app.ngenhttplib.store;

public class ValuesStore {

    private ValuesStore() {
    }

    public static class Http {

        public static String externalIp = null;
        public static final String[] extIpApiList = {
            "http://api.exip.org/?call=ip",
            "http://checkip.dyndns.org/"
        };
        public static final String extIpUrl = "http://app.live-start.com/test-app/speedproxy/ip.php";
        public static final String judgeUrl = "http://app.live-start.com/test-app/speedproxy/judge.php";
        public static final String uploadUrl = "http://app.live-start.com/test-app/speedproxy/upload.php";
    }
}
