package com.astronlab.ngenhttplib.core;

import com.sun.javafx.tk.Toolkit;
import com.sun.webkit.dom.HTMLDocumentImpl;
import groovy.lang.Closure;
import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class FxWebClient extends Application {
    private static final CountDownLatch latch_ = new CountDownLatch(1);
    private static Browser browser;
    public final OPTIONS options = new OPTIONS();

    public final class OPTIONS {
        public final Config URL = Config.URL;
        public final Config DATA_MATCH_CLOSURE = Config.DATA_MATCH_CLOSURE;
        public final Config MAX_REFRESH_COUNT_TILL_MATCH = Config.MAX_REFRESH_COUNT_TILL_MATCH;
    }

    private enum Config {
        URL(null), DATA_MATCH_CLOSURE(null), MAX_REFRESH_COUNT_TILL_MATCH(5);
        Object value;

        Config(Object val) {
            value = val;
        }

        public Object getValue() {
            return value;
        }
    }

    private static void prepare() {
        if (browser == null) {
            Thread thread = new Thread(() -> Application.launch(FxWebClient.class, (String[]) null));
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Override
    public void start(Stage stage) {
        browser = new Browser();
        Scene scene = new Scene(browser, 1024, 768, Color.WHITE);
        stage.setScene(scene);
        stage.show();
        latch_.countDown();
    }

    class Browser extends Region {
        void addView(Node webView) {
            getChildren().add(webView);
        }

        void remove(Node view) {
            getChildren().remove(view);
        }
    }

    public static String getData(String url) throws Exception {
        Map<Config, Object> optionsObjectMap = new HashMap<Config, Object>() {
            {
                put(Config.URL, url);
            }
        };

        return getData(optionsObjectMap);
    }

    public static String getData(Map<Config, Object> options) throws Exception {
        if (options.get(Config.URL) == null) {
            throw new Exception("Please provide url for the FxWebClient");
        }

        prepare();
        latch_.await();
        CountDownLatch latch = new CountDownLatch(1);
        final String[] data = {""};

        Toolkit.getToolkit().defer(() -> {
            WebView webView = new WebView();
            browser.addView(webView);
            WebEngine webEngine = webView.getEngine();
            webEngine.setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.140 Safari/537.36");
            webEngine.load((String) options.get(Config.URL));
            webEngine.getLoadWorker().stateProperty().addListener(
                    (ov, oldState, newState) -> {
                        if (newState == Worker.State.SUCCEEDED) {
                            try {
                                data[0] = docToString((HTMLDocumentImpl) webEngine.getDocument());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (data[0].contains("html")/*!isReSchedule(options, data[0])*/) {
                                latch.countDown();
                                browser.remove(webView);

                            } else {
                                webEngine.setOnVisibilityChanged(event -> {
                                    try {
                                        data[0] = docToString((HTMLDocumentImpl) webEngine.getDocument());

                                        System.out.println(data[0]);
                                    } catch (TransformerException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        }
                    });
        });

        latch.await();
        return data[0];
    }

    private static boolean isReSchedule(Map<Config, Object> options, String data) {
        Closure matchClosure = (Closure) options.get(Config.DATA_MATCH_CLOSURE);
        if (matchClosure != null) {
            Object reTryCountObject = options.get(Config.MAX_REFRESH_COUNT_TILL_MATCH);
            int retryCount = (int) (reTryCountObject == null ? Config.MAX_REFRESH_COUNT_TILL_MATCH.value : reTryCountObject);

            if (retryCount > 0) {
                if (!(boolean) matchClosure.call(data)) {
                    retryCount--;
                    options.put(Config.MAX_REFRESH_COUNT_TILL_MATCH, retryCount);
                    return true;

                } else {
                    return false;
                }
            }
        }

        return false;
    }

    private static String docToString(HTMLDocumentImpl doc) throws TransformerException {
        DOMSource domSource = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.transform(domSource, result);

        return writer.toString();
    }

    //TODO: Activate these
    //- 1. Add method/closure to return response cookies
    //- 2. Add custom content filtering for lighter loading (ie. do not load img/ico/non-js contents)
    //- 3. Convert the invoker and other methods into instance based rather than static-method based
    //- 4. Use quit method to destroy the hidden window

    //ie. https://stackoverflow.com/questions/12524580/disable-automatic-image-loading-in-webview
}