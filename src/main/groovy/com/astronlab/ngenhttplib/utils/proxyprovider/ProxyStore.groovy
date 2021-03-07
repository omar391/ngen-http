package com.astronlab.ngenhttplib.utils.proxyprovider

import com.astronlab.commonutils.AppContext
import com.astronlab.commonutils.concurrency.Tasker
import com.astronlab.commonutils.concurrency.ThreadWaitNotifier
import com.astronlab.commonutils.interfaces.DatabaseProvider
import com.astronlab.commonutils.interfaces.KeyValueDatabase
import com.astronlab.commonutils.utils.*
import com.astronlab.ngenhttplib.core.InvokerRequest
import com.astronlab.ngenhttplib.core.client.ProxyConfig
import com.astronlab.ngenhttplib.utils.IpProperties
import com.astronlab.ngenhttplib.utils.PingTester
import com.astronlab.ngenhttplib.utils.web.WebHttpHandler
import com.astronlab.ngenhttplib.utils.web.WebHttpOptions
import com.astronlab.ngenhttplib.utils.web.WebHttpResult
import com.sun.istack.internal.NotNull
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import org.apache.log4j.Level
import org.apache.log4j.Logger

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern
import java.util.stream.LongStream

@CompileStatic
class ProxyStore {
    //proxy store format: key=ip:port val=[ProxyResultIDX 0, ProxyResultIDX 1, ..]
    final static Logger logger = Logger.getLogger(ProxyStore.getName())
    @PackageScope
    final static ConcurrentHashMap<String, ArrayList<String>> finalProxyList = new ConcurrentHashMap(100, 0.9f, 2)
    @PackageScope
    final static ConcurrentHashMap<String, ArrayList<String>> tmpProxyList = new ConcurrentHashMap(100, 0.9f, 2)

    private final static CountDownLatch statusLatch = new CountDownLatch(2)
    private final static WebHttpHandler handler = new WebHttpHandler(new WebHttpOptions(ProxyFilter.SECURE_IP_SELECTOR(), 500))
    private final static ThreadWaitNotifier threadNotifier = new ThreadWaitNotifier()
    private final static AtomicLong activeTmpProxiesCount = new AtomicLong()
    private final static Tasker cachedLatencyTester = new Tasker(0, 60L, true, "Proxy latency testers")
    private static DatabaseProvider databaseProvider
    private static RegexFailureReporter regexFailureReporter
    private static volatile Tasker forkJoinBatchPingTester = AppContext.tasksPool


    static void testNAddStaticProxies(List<String> proxyList = ["127.0.0.1:9150", "127.0.0.1:9050"]) {
        ArrayList<String> ar = []
        ar[ProxyResultIDX.TYPE] = ProxyConfig.Type.SOCKS_V5.name()
        ar[ProxyResultIDX.COUNTRY_CODE] = "xx"

        //add tor proxies
        proxyList.each { k ->
            if (isPortOpen(k)) {
                finalProxyList.put(k.trim(), ar)
            } else {
                tmpProxyList.put(k.trim(), ar)
            }
        }
    }

    static boolean isPortOpen(String hostColonPort) {
        try {
            String[] str = hostColonPort.split(":")
            (new Socket(str[0], Integer.parseInt(str[1]))).close()
            return true
        }
        catch (SocketException ignored) {
        }

        return false
    }

    static void main(String[] args) {
        LogUtils.init()
        TimeUtils t = TimeUtils.newInstance()
        init(true)
        t.analyze()
    }


    static synchronized void setDbProvider(DatabaseProvider provider) {
        if (databaseProvider == null) {
            databaseProvider = provider
        }
    }


    static synchronized void setRegexFailureReporter(RegexFailureReporter reporter) {
        if (regexFailureReporter == null) {
            regexFailureReporter = reporter
        }
    }

    @CompileStatic
    static Map.Entry<String, ArrayList<String>> getNextProxy(@NotNull ProxyFilter filter, boolean waitTillFirstEntryAvailable = true) {
        if (statusLatch.count == 2 && finalProxyList.size() < 1) {
            synchronized (statusLatch) {
                if (statusLatch.count == 2) {
                    //keep running with an hour delay
                    AppContext.backgroundTasks.execute({ init() })
                    if (waitTillFirstEntryAvailable) {
                        //waiting twice: cache retrieve + page parsing
                        statusLatch.await()
                        if (finalProxyList.size() < 1) {
                            statusLatch.await()
                        }
                    }
                }
            }
        }

        while (true) {

            if (filter.currentItr && filter.currentItrIdx.get() < filter.currentItr.size()) {
                try {
                    Map.Entry<String, ArrayList<String>> proxy = filter.currentItr[filter.currentItrIdx.getAndIncrement()]

                    //check if its in bad proxies list or in recently returned keys
                    if (filter.badProxies.contains(proxy.key) || filter.curItrReturnedKeys.contains(proxy.key)) {
                        continue
                    }

                    //default filter; or already evaluated as good
                    if (filter.goodProxies.contains(proxy.key)) {
                        filter.curItrReturnedKeys.add(proxy.key)
                        return proxy
                    }

                    if (!filter.isAcceptable(proxy.key, proxy.value)) {
                        filter.badProxies.add(proxy.key)

                    } else {
                        filter.goodProxies.add(proxy.key)
                        filter.curItrReturnedKeys.add(proxy.key)
                        return proxy
                    }

                } catch (ignored) {
                    filter.currentItrIdx.set(0)
                    filter.currentItr = finalProxyList.entrySet().toArray(new Map.Entry<String, ArrayList<String>>[finalProxyList.entrySet().size()]) as Map.Entry<String, ArrayList<String>>[]
                }

            } else {
                if (finalProxyList.size() > filter.badProxies.size()) {
                    filter.curItrReturnedKeys.clear()
                    filter.currentItrIdx.set(0)
                    filter.currentItr = finalProxyList.entrySet().toArray(new Map.Entry<String, ArrayList<String>>[finalProxyList.entrySet().size()]) as Map.Entry<String, ArrayList<String>>[]

                } else {
                    if (activeTmpProxiesCount.get() > 0) {
                        while (finalProxyList.size() <= filter.badProxies.size() && activeTmpProxiesCount.get() > 0) {
                            threadNotifier.doWait(60000)
                        }
                    } else {
                        logger.warn("Returning null, seems there is no proxy available. \nBad proxies: " + filter.badProxies.size() + " Good proxies:" + filter.goodProxiesSize() + " Total proxies:" + finalProxyList.size())
                        return null
                    }
                }
            }
        }
    }


    private static void init(boolean waitTillAllProxiesTested = false) {
        //add static proxies
        testNAddStaticProxies()

        ExecutorService waiterFakePool = null
        if (waitTillAllProxiesTested) {
            waiterFakePool = Executors.newSingleThreadExecutor()
            waiterFakePool.submit({})
        }

        String badProxyKey = "bad_proxies"

        //todo: fix
        KeyValueDatabase fileDb = databaseProvider?.getDb("proxy_list")
        if (fileDb) {
            //revive old cache
            fileDb.setColdBackupPolicy(false).setDataLoadFinishListener({
                //retrieve old bad proxies if not expired
                DbUtils.retrieve_n_store_expire_able_data_ref(fileDb, badProxyKey, handler.defaultOptions.proxyFilter.badProxies, 1)
                activeTmpProxiesCount.lazySet(activeTmpProxiesCount.get() + fileDb.size())
                fileDb.consumeEntries({ String k, Object v ->
                    tmpProxyList.put(k, (ArrayList<String>) v)
                    fileDb.remove(k)
                }, false)

                fileDb.removeAll()
                threadNotifier.doNotify()
            }).init()

            //wait for cache loading into tmpProxies to be finished
            while (fileDb.size() > 0) {
                threadNotifier.doWait(60000)
            }
        } else {
            logger.debug("ProxySore is not using any file db to store proxies. Consider using 'setDbProvider' method to set the database.")
        }

        //start proxy latency & speed tester
        startProxyVerifier(fileDb)//TODO:fix

        //wait for a cached ip is checked
        if (activeTmpProxiesCount.get() > 0) {
            threadNotifier.doWait(60000)
        }

        while (statusLatch.count > 0) {
            //release next() call waiting
            statusLatch.countDown()
            if (finalProxyList.size() < 1) {
                //still not found in cache? then break and do another countdown later
                break
            }
        }

        //parse proxy sites
        intiProxySiteParser()

        if (waitTillAllProxiesTested) {
            while (activeTmpProxiesCount.get() > 0) {
                // we are doing it in while loop because activeTmpProxiesCount also count those are still not sent to the pool
                cachedLatencyTester.await()
            }

            // we need to call shut down explicitly  as we are using a scheduler pool
            waiterFakePool.shutdownNow()
        }

        logger.info("Total proxies: " + (activeTmpProxiesCount.get() + finalProxyList.size()) + " :: Total bad proxies: " + handler.defaultOptions.proxyFilter.badProxies.size())

        //schedule consecutive parsing
        AppContext.backgroundTasks.executeWithPeriodicDelay({ intiProxySiteParser() }, 1, 1, TimeUnit.HOURS, 0, null, null)
    }


    private static void intiProxySiteParser() {
        //add static proxies
        testNAddStaticProxies()

        //todo:remove
//        if (statusLatch.count > 0) {
//            statusLatch.countDown()
//        }
//        return

        Tasker worker = new Tasker(0, true, "Proxy parser", false)
        RegexUtils rgx = new RegexUtils(new RegexOptions(true, -1, null, regexFailureReporter))
        ProxyStoreContext proxyContext = new ProxyStoreContext()
        ThreadWaitNotifier taskNotifier = new ThreadWaitNotifier()

        for (final Map.Entry<String, String> siteObj in proxyContext.sites.entrySet()) {
            String lastUrl = ""
            List finishedPages = []
            List remPages = Collections.synchronizedList([proxyContext.attrs()[siteObj.value][ProxyStoreContext.K_INDEX_PATH]])
            AtomicLong pageCounter = new AtomicLong(0)
            RegexOptions paginationOptions = rgx.defaultOptions.clone().set_SANITIZE_TRANSFORMER(proxyContext.transformers()[siteObj.value][ProxyStoreContext.K_PAGINATION_SANITIZER])
            Map<Integer, Pattern> patterns = proxyContext.patterns()[siteObj.value]
            Closure<Boolean> isValidData = { String data, b -> patterns[ProxyStoreContext.K_PROXY_VALID_DATA_EXISTS_REGEX].matcher(data).find() }

            while (true) {
                if (remPages.size() > 0) {
                    final String urlPath = remPages.remove(0)
                    if (!finishedPages.contains(urlPath)) {
                        final String refPath = lastUrl
                        worker.execute({
                            remPages.addAll(parseProxyDetailsPage(handler.defaultOptions.invoker.init(siteObj.value + urlPath).setRequestHeader("referer", siteObj.value + refPath), siteObj.value, rgx, paginationOptions, patterns, isValidData))
                        }, 0, { a, b -> pageCounter.decrementAndGet(); taskNotifier.doNotifyAll() }, null)

                        if (statusLatch.count > 0) {
                            worker.await()
                            //release next() call waiting
                            statusLatch.countDown()
                        }

                        finishedPages.add(urlPath)
                        lastUrl = urlPath
                        pageCounter.incrementAndGet()
                    }

                } else if (pageCounter.get() > 0) {
                    taskNotifier.doWait(60000)//1min

                } else {
                    break
                }
            }

        }
        worker.runFailedTasksNShutDown()
    }


    private static List<String> parseProxyDetailsPage(InvokerRequest req, String siteRoot, RegexUtils rgx, RegexOptions paginationOptions, Map<Integer, Pattern> patterns, Closure<Boolean> isValidData) {
        //return list of next pages; you can infer if there are any page remains
        WebHttpResult htmlState = handler.invoke(req, isValidData)
        //direct mode yet not valid data
        if (htmlState.desiredDataMatchState != WebHttpResult.HTTP_VERIFICATION_STATE.PASSED) {
            return []
        }

        //push proxies into tmp map
        String key = "K_PROXY_REGEX_" + siteRoot
        RegexResult regexResult = rgx.parseSingle(htmlState.html, patterns[ProxyStoreContext.K_PROXY_REGEX], key, req.url)
        regexResult.resultMap[key].each { ArrayList<String> vl ->
            String tmpKey = vl.remove(0) + ":" + vl.remove(0)
            //println("New proxy: " + key + ":" + vl)1
            //for readability: if (!tmpProxies.containsKey(tmpKey) && !proxyList.containsKey(tmpKey)) {tmpProxies.put(tmpKey, vl)}
            if (!finalProxyList.containsKey(tmpKey)) {
                tmpProxyList.computeIfAbsent(tmpKey, {
                    activeTmpProxiesCount.incrementAndGet()
                    ArrayList<String> data = []
                    data[ProxyResultIDX.TYPE] = vl[0]
                    data[ProxyResultIDX.COUNTRY_CODE] = vl[1]
                    return data
                })
            }
        }

        //collect pagination links
        key = "K_PAGINATION_REGEX_" + siteRoot
        regexResult = rgx.parseSingle(regexResult.remHtml, patterns[ProxyStoreContext.K_PAGINATION_REGEX], key, req.url, paginationOptions)
        return regexResult.resultMap[key].collect {
            it[0]
        }
    }


    private static void startProxyVerifier(KeyValueDatabase fileDbObject) throws Exception {
        Tasker.TaskStatusListener listener = new Tasker.TaskStatusListener() {
            @Override
            void onFinished(Tasker.TASK_STATUS status, Object output) {
                activeTmpProxiesCount.decrementAndGet()
                //awake waiting threads; should notify in all cases
                threadNotifier.doNotifyAll()
            }
        }

        AppContext.backgroundTasks.executeWithPeriodicDelay({

            //each proxy use 10 concurrent fork-join threads to test; maximum pool size is 1000
            int len = Math.min(tmpProxyList.size() * 10, 1000)
            if (len > 0) {
                if (forkJoinBatchPingTester.processorUsed < len || len < 100 || forkJoinBatchPingTester.executorService.isShutdown()) {
                    synchronized (cachedLatencyTester) {
                        if (forkJoinBatchPingTester.processorUsed < len || len < 100 || forkJoinBatchPingTester.executorService.isShutdown()) {
                            if (forkJoinBatchPingTester != AppContext.tasksPool) {
                                forkJoinBatchPingTester.shutdown()
                            }
                            forkJoinBatchPingTester = new Tasker((len + 100), true, "ForkJoin Ping Tester", false)
                        }
                    }
                }

                Map.Entry<String, ArrayList<String>> v0
                while (true) {
                    Iterator<Map.Entry<String, ArrayList<String>>> itr = tmpProxyList.entrySet().iterator() as Iterator<Map.Entry<String, ArrayList<String>>>
                    if (itr.hasNext()) {
                        v0 = itr.next()
                        itr.remove()
                        if (finalProxyList.containsKey(v0.key)) {
                            continue
                        }
                    } else {
                        break
                    }

                    final Map.Entry<String, ArrayList<String>> v = v0

                    cachedLatencyTester.execute({
                        float avgPing = 0f
                        if (IpProperties.isPrivateIp(v.key)) {
                            if (!isPortOpen(v.key)) {
                                //no need to do avg counting; proceed with removing this local ip
                                return 0
                            }
                        } else {
                            avgPing = getAvgPingLatency(v.key)
                        }

                        if (avgPing >= 0f) {
                            v.value[ProxyResultIDX.LATENCY] = avgPing + ""
                            finalProxyList.put(v.key, v.value)

                            if (fileDbObject != null && !fileDbObject.isClosed()) {
                                fileDbObject.put(v.key, v.value)
                            }
                            logger.debug "Latency result: " + v.key + " -> " + v.value
                            //TODO: start download and upload speed test
                        }
                    }, listener)
                }
            }

        }, 0, 1, TimeUnit.SECONDS, 0, null, null)

    }


    static float getAvgPingLatency(String proxyIpPort, Tasker pool = forkJoinBatchPingTester) {
        String[] proxy = proxyIpPort.split(":")
        PingTester p = new PingTester(proxy[0], Integer.parseInt(proxy[1]), 10000)
        AtomicInteger i = new AtomicInteger()
        ThreadWaitNotifier notifier = new ThreadWaitNotifier()
        float avg = -1f

        synchronized (cachedLatencyTester) {
            if (pool.executorService.isShutdown()) {
                pool = AppContext.tasksPool
            }
            pool.execute({
                try {
                    avg = (float) (LongStream.rangeClosed(1L, 10L).parallel().map({
                        i.incrementAndGet()
                        p.latency
                    }).filter({ if (it != -1L) { return true } else { i.decrementAndGet(); return false } }).sum() / i.get())
                } catch (ignored) {
                }
                notifier.doNotifyAll()
            })
        }
        notifier.doWait(0)
        return avg
    }


    static void deactivateLog(boolean isDisableProxyHttpLog) {
        if (isDisableProxyHttpLog) {
            handler.defaultOptions.setIsHttpLogEnabled(false)
        }
        logger.setLevel(Level.OFF)
    }
}
