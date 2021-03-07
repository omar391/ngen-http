package com.astronlab.ngenhttplib.core;

import com.astronlab.ngenhttplib.core.misc.InvokerCookieJar;
import com.astronlab.ngenhttplib.utils.CookieUtil;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.ProxyConfig;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.UserAgent;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class HttpInvokerUI {
    private JBrowserDriver driver;
    private Settings.Builder settings;
    private final AtomicBoolean isUpdate = new AtomicBoolean(false);
    private Callable<Object> onWindowsClosedAbruptly;
    private final Logger logger = Logger.getLogger(HttpInvokerUI.class.getName());

    //common initializer
    {
        Runtime.getRuntime().addShutdownHook(new Thread(this::quit));
    }

    private void init() {
        if (settings != null) return;
        settings = Settings.builder()
                .cacheDir(new File(System.getProperty("java.io.tmpdir")))
                .headless(false)
                .javascript(true)
                .ignoreDialogs(true)
                .screen(new Dimension(1024, 768))
                .cache(true)
                .csrf()
                .blockAds(true)
                .saveAttachments(false)
                .userAgent(UserAgent.CHROME);

        //Every instance creates a new session
        //TODO: use a static single instance of driver for state sharing and add option to create new session by another method
    }

    public HttpInvokerUI setLogLevel(Level level) {
//TODO:first enamurate the loggers here then set level
//                 Logger.getLogger(Augmenter.class.getName()).setLevel(Level.OFF);
//                settings.loggerLevel(Level.OFF).logWarnings(false)
        return this;
    }

    public JBrowserDriver getDriver() {
        init();
        return getUpdatedDriver();
    }

    private synchronized JBrowserDriver getUpdatedDriver() {
        if (isUpdate.get() || driver == null) {
            try {
                if (driver != null) {
                    driver.reset(settings.build());
                } else {
                    driver = new JBrowserDriver(settings.build());
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
                try {
                    driver.close();
                } catch (Exception ex) {
                    logger.info(ex.getMessage());
                }
                driver = new JBrowserDriver(settings.build());
            }
        }
        isUpdate.set(false);
        return driver;
    }

    public HttpInvokerUI hideElement(By elm) {
        init();
        getUpdatedDriver();
        executeJs("arguments[0].style.visibility='hidden'", driver.findElement(elm));
        return this;
    }

    public void executeJs(String js, Object... arguments) {
        ((JavascriptExecutor) driver).executeScript(js, arguments);
    }

    public HttpInvokerUI get(String url) {
        init();
        //sometime ui window gets closed for bad request; check if open on exception and reopen ui if needed
        try {
            getUpdatedDriver().get(url);
        } catch (Exception e) {
            logger.error(e.getMessage());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                System.out.println(e1.getMessage());
            }
            Set<String> handles = driver.getWindowHandles();
            if (handles == null || handles.isEmpty()) {
                //TODO: also check if connection is reset
                //window is closed
                try {
                    onWindowsClosedAbruptly.call();
                } catch (Exception e2) {
                    logger.error(e2.getMessage());
                }
                return get(url);
            }
        }
        return this;
    }

    public HttpInvokerUI saveScreenShot(File saveAs) throws IOException {
        return saveScreenShot(saveAs, null);
    }

    public HttpInvokerUI saveScreenShot(File saveAs, By selectedArea) throws IOException {
        synchronized (this) {
            Screenshot image;
            AShot capture = new AShot().shootingStrategy(ShootingStrategies.viewportPasting(100));
            if (selectedArea == null) {
                image = capture.takeScreenshot(driver);
            } else {
                image = capture.takeScreenshot(driver, driver.findElement(selectedArea));
            }
            ImageIO.write(image.getImage(), "png", saveAs);
        }

        return this;
    }

    public HttpInvokerUI submitForm(String pageUrl, HashMap<By, String> formData, By submitButtonSelector) {
        init();
        get(pageUrl);
        return submitForm(formData, submitButtonSelector);
    }

    public HttpInvokerUI submitForm(HashMap<By, String> formData, By submitButtonSelector) {
        init();
        for (Map.Entry<By, String> selector : formData.entrySet()) {
            sendKeys(selector.getKey(), selector.getValue());
        }
        sendKeys(submitButtonSelector, Keys.ENTER);

        return this;
    }

    public void sendKeys(By key, CharSequence... value) {
        driver.findElement(key).sendKeys(value);
    }

    public InvokerCookieJar getInvokerCompatibleCookieJar() {
        init();
        return CookieUtil.seleniumCookiesToInvokerCookieJar(driver.manage().getCookies());
    }

    public Settings.Builder getSettings() {
        init();
        synchronized (this) {
            isUpdate.set(true);
        }
        return settings;
    }

    public HttpInvokerUI setProxy(ProxyConfig.Type type, String host, int port, String user, String pass) {
        init();
        settings.proxy(new ProxyConfig(type, host, port, user, pass));
        return this;
    }

    public void quit() {
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
    }

    public void setOnWindowsClosedAbruptly(Callable<Object> onWindowsClosedAbruptly) {
        this.onWindowsClosedAbruptly = onWindowsClosedAbruptly;
    }

    //TODO: remove all try catch with a single trycatch func and check if the exception is for connection refusal
    // if so then reset the connection with all cookies else throw initial exception
    // + create random user agent
}
