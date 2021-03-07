package com.astronlab.ngenhttplib.utils;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author omar
 */
public class PingTester {

    private final String Ip;
    private final int Port;
    private final int timeOut;

    public PingTester(String ip, int port, int timeOut) {
        this.Ip = ip;
        this.Port = port;
        this.timeOut = timeOut;
    }

    public long getLatency() {
        Socket socket = new Socket();
        InetSocketAddress endPoint = new InetSocketAddress(Ip, Port);
        long startTime, endTime;

        if (endPoint.isUnresolved()) { // Checks wether the address has been resolved or not.
            //ConsoleService.info(" Not able to resolve host : " + Ip);
            return -1;
        } else {
            try {
                startTime = System.currentTimeMillis();
                socket.connect(endPoint, timeOut);
            } catch (Exception e) {
                //ConsoleService.info("Exception : "+e.getMessage()+", Host: "+Ip+":"+Port);
                //e.printStackTrace();
                return -1;
            }
        }
        endTime = System.currentTimeMillis();
        //ConsoleService.info(" Connected : " + Ip+":"+Port);
        return (endTime - startTime);
    }
}
