package com.astronlab.ngenhttplib.test;

import com.astronlab.ngenhttplib.http.HttpInvoker;

/**
 * Created by omar on 15/03/16.
 */
public class TestClient {
	static String url = "https://inventory.sebpo.net/seinventory/api/login?username=emp1&password=se2121&secret=1076-576-SEinventory";

	public static void main(String args[]) throws Exception {
		HttpInvoker httpInvoker = new HttpInvoker(url);
		httpInvoker.config().acceptAllSSLCerts().update();
		System.out.println(httpInvoker.getStringData());
	}
}
