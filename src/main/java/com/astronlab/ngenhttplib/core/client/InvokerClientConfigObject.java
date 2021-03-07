package com.astronlab.ngenhttplib.core.client;

import com.astronlab.ngenhttplib.core.misc.TrustAllCertsSSLSocket;
import com.astronlab.ngenhttplib.core.misc.InvokerCookieJar;
import okhttp3.Credentials;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import javax.net.SocketFactory;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

//Checked 4/1/2019: thread safe
//TODO: ThreadSafeBuilderByAccess wrapping is not actually needed; unwrap it
public abstract class InvokerClientConfigObject<R> {
    private final ThreadSafeBuilderByAccess<OkHttpClient.Builder, OkHttpClient> config;
    private final R returnType;

    public InvokerClientConfigObject(OkHttpClient client, Class<R> returnType) {
        config = new ThreadSafeBuilderByAccess<OkHttpClient.Builder, OkHttpClient>(client.newBuilder(), client) {
            @Override
            protected OkHttpClient build() {
                return builder.build();
            }
        };
        this.returnType = returnType.cast(this);
    }

    protected OkHttpClient getUpdatedClient() {
        return config.update();
    }

    public R reNewCookieState() {
        config.getBuilder().cookieJar(new InvokerCookieJar());

        return returnType;
    }

    public R acceptAllSSLCerts() throws KeyManagementException, NoSuchAlgorithmException {
        TrustAllCertsSSLSocket socket = new TrustAllCertsSSLSocket();
        config.getBuilder().sslSocketFactory(socket.getSocketFactory(), new TrustAllCertsSSLSocket.TrustAllCertsManager()).hostnameVerifier(socket.getHostNameVerifier());

        return returnType;
    }

    public R setRedirection(boolean redirection) {
        config.getBuilder().followRedirects(redirection);
        config.getBuilder().followSslRedirects(redirection);

        return returnType;
    }

    public R removeProxy() {
        config.getBuilder().proxy(Proxy.NO_PROXY);
        config.getBuilder().socketFactory(SocketFactory.getDefault());

        return returnType;
    }

    public R setProxyAuthentication(String userName, String pass) {
        config.getBuilder().proxyAuthenticator((route, response) -> {
            if (response.request().header("Authorization") != null) {
                System.out.println("Proxy auth failed!");
                return null; // Give up, we've already attempted to authenticate.
            }

            String credential = Credentials.basic(userName, pass);
            return response.request().newBuilder()
                    .header("Authorization", credential)
                    .build();
        });
        return returnType;
    }

    public R setProxy(String proxyIp, int proxyPort,
                      ProxyConfig.Type proxyType) {
        return setProxy(proxyIp, proxyPort, proxyType.value);
    }

    public R setProxy(String proxyIp, int proxyPort,
                      Proxy.Type proxyType) {
        InetSocketAddress socketAddress = new InetSocketAddress(proxyIp, proxyPort);
        Proxy proxy;

        //check for socks_v4
        if (proxyType == null) {
            proxy = new Proxy(Proxy.Type.SOCKS, socketAddress);
            SocketFactory sf = new SocketFactory() {
                public Socket createSocket() {
                    Socket socket = new Socket(proxy);

                    try {
                        Field sockImplField = socket.getClass().getDeclaredField("impl");
                        sockImplField.setAccessible(true);

                        SocketImpl socksImpl = (SocketImpl) sockImplField.get(socket);

                        Method setSockVersion = socksImpl.getClass().getDeclaredMethod("setV4");
                        setSockVersion.setAccessible(true);
                        setSockVersion.invoke(socksImpl);

                        sockImplField.set(socket, socksImpl);
                    } catch (Exception ignored) {
                        System.out.println("Error while creating SOCKS_V4 connection");
                    }

                    return socket;
                }

                @Override
                public Socket createSocket(String s, int i) throws IOException {
                    return null;
                }

                @Override
                public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException {
                    return null;
                }

                @Override
                public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
                    return null;
                }

                @Override
                public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
                    return null;
                }
            };
            config.getBuilder().socketFactory(sf);

        } else {
            proxy = new Proxy(proxyType, socketAddress);
            config.getBuilder().proxy(proxy);
        }

        return returnType;
    }

    public R setConnectionTimeOut(int milliseconds) {
        config.getBuilder().connectTimeout(milliseconds, TimeUnit.MILLISECONDS);

        return returnType;
    }

    public R setSocketReadTimeOut(int milliseconds) {
        config.getBuilder().readTimeout(milliseconds, TimeUnit.MILLISECONDS);

        return returnType;
    }

    public R setCookieJar(InvokerCookieJar cookieJar) {
        config.getBuilder().cookieJar(cookieJar);

        return returnType;
    }

    public R setMaxRequests(short maxRequests, short maxRequestsPerHost) {
        Dispatcher dispatcher = config.getLastOutcome().dispatcher();
        dispatcher.setMaxRequests(maxRequests);
        dispatcher.setMaxRequestsPerHost(maxRequestsPerHost);
        config.getBuilder().dispatcher(dispatcher);

        return returnType;
    }

    /***
     * This method is made public due the probable future necessity of accessing more settings in the invoker.
     * */
    public OkHttpClient.Builder getExtraClientConfigs() {
        return config.getBuilder();
    }
}
