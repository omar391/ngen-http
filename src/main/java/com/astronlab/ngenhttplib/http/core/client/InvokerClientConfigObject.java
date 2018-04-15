package com.astronlab.ngenhttplib.http.core.client;

import com.astronlab.ngenhttplib.http.core.misc.InvokerCookieJar;
import com.astronlab.ngenhttplib.http.core.misc.TrustAllCertsSSLSocket;
import okhttp3.OkHttpClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class InvokerClientConfigObject<R> {
    private volatile boolean accessFlag = false;
    private OkHttpClient updatedClient;
    private OkHttpClient.Builder clientConfigBuilder;
    private R returnType; //set returnType from sub class. ie. returnType = R_instance

    public InvokerClientConfigObject(OkHttpClient client) {
        this.updatedClient = client;
    }

    protected void setReturnType(R returnTypeInstance) {
        returnType = returnTypeInstance;
    }

    protected synchronized OkHttpClient getUpdatedClient() {
        if (accessFlag) {
            accessFlag = false;
            return updatedClient = clientConfigBuilder.build();
        } else {
            return updatedClient;
        }
    }

    private synchronized OkHttpClient.Builder getBuilder() {
        //we access the builder with this method to updated access status
        accessFlag = true;
        if (clientConfigBuilder == null) {
            clientConfigBuilder = updatedClient.newBuilder();
        }
        return clientConfigBuilder;
    }

    public R acceptAllSSLCerts()
            throws KeyManagementException, NoSuchAlgorithmException {
        TrustAllCertsSSLSocket socket = new TrustAllCertsSSLSocket();
        getBuilder().sslSocketFactory(
                socket.getSocketFactory()).hostnameVerifier(
                socket.getHostNameVerifier());

        return returnType;
    }

    public R setRedirection(boolean redirection) {
        getBuilder().followRedirects(redirection);
        getBuilder().followSslRedirects(redirection);

        return returnType;
    }

    public R removeProxy() {
        getBuilder().proxy(Proxy.NO_PROXY);

        return returnType;
    }

    public R setProxy(String proxyIp, int proxyPort,
                      Proxy.Type proxyType) {
        InetSocketAddress socketAddress = new InetSocketAddress(proxyIp, proxyPort);
        Proxy proxy = new Proxy(proxyType, socketAddress);
        getBuilder().proxy(proxy);

        return returnType;
    }

    public R setConnectionTimeOut(int milliseconds) {
        getBuilder().connectTimeout(milliseconds, TimeUnit.MILLISECONDS);

        return returnType;
    }

    public R setSocketTimeOut(int milliseconds) {
        getBuilder().readTimeout(milliseconds, TimeUnit.MILLISECONDS);

        return returnType;
    }

    public R setCookieJar(InvokerCookieJar cookieJar) {
        getBuilder().cookieJar(cookieJar);

        return returnType;
    }

    /***
     * This method is made public due the probable future necessity of accessing more settings in the invoker.
     * */
    public OkHttpClient.Builder getExtraClientConfigs() {
        return getBuilder();
    }
}
