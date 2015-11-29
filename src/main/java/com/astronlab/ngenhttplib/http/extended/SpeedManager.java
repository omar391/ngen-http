package com.astronlab.ngenhttplib.http.extended;

import com.astronlab.ngenhttplib.http.impl.IHttpConnectionManager;

public class SpeedManager {

    private double speed = 0;
    private int counter = 0;
    private int countLimit = 3;
    private IHttpConnectionManager httpController;

    public SpeedManager(IHttpConnectionManager connectionController) {
        httpController = connectionController;
    }

    public void manageSpeed(double speed) {
        if ((int) speed > (int) this.speed) {
            counter = 0;
        } else {
            if (counter < countLimit) {
                counter++;
            } else {
                httpController.StopConnection();
            }
        }
        this.speed = speed;
        System.out.println(speed + " KB/s");
    }

    public double getSpeed() {
        return this.speed;
    }
}