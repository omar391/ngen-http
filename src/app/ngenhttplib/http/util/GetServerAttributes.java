/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package app.ngenhttplib.http.util;

import java.util.HashMap;
import java.util.concurrent.Callable;

/**
 *
 * @author omar
 */
public class GetServerAttributes implements Callable<HashMap>{

    private String ip;
    private int port;
    private int timeOut = 3000;
    private HashMap result;
    public static String KEY_LATENCY = "latency";
    public static String KEY_HOSTNAME = "host";

    public GetServerAttributes(String ip, String port) {
        this.ip = ip;
        this.port = Integer.parseInt(port);
        this.result = new HashMap();
    }

    public HashMap getResult() {
        long latency = new PingTester(ip, port, timeOut).getLatency();
        String hostName = new ReverseHostLookUp(ip).getHostName();
        result.put(KEY_LATENCY, latency);
        result.put(KEY_HOSTNAME, hostName);
        return this.result;
    }

    @Override
    public HashMap call() throws Exception {
        return getResult();    
    }
}
