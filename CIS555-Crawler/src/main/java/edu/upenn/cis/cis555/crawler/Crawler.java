package edu.upenn.cis.cis555.crawler;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upenn.cis.cis555.crawler.info.URLInfo;
import edu.upenn.cis.cis555.crawler.remote.Worker;
import edu.upenn.cis.cis555.storage.DocRDSController;
import edu.upenn.cis.cis555.storage.DocS3Controller;

/**
 * This is the Crawler class.
 */
public class Crawler {

    private static Logger logger = LoggerFactory.getLogger(Crawler.class);

    public static final String USER_AGENT = "cis555crawler";

    private final int threadCount;
    private DatagramSocket socket;
    private final InetAddress monitorInetAddr;
    private final String crawlerIdentifier;
    private InetSocketAddress masterAddr;
    private final Worker controller;

    private final DocRDSController docRDSController;
    private final List<DocS3Controller> docS3Controllers;

    private final List<Long> HTMLCount;
    private final List<Long> downloadedBytes;
    private final TaskDispatcher dispatcher;
    private final List<CrawlerWorker> threads = new ArrayList<>();
    
    public Crawler(int count, String remoteHost, List<String> seeds, String node, String master) {
        logger.info("Initializing rawler!");
        threadCount = count;

        InetAddress host = null;
        try (DatagramSocket s = new DatagramSocket()) {
            socket = s;
            host = InetAddress.getByName(remoteHost);
        } catch (SocketException e) {
            logger.error("Socket Exception when resolving host");
        } catch (UnknownHostException e) {
            logger.error("Unknown Host Exception when resolving host");
        }
        monitorInetAddr = host;

        crawlerIdentifier = node;

        masterAddr = null;
        try {
            InetAddress inetAddr = InetAddress.getByName(master);
            masterAddr = new InetSocketAddress(inetAddr, 21555);
            logger.info("Master address: {}; remote controller enabled.", inetAddr);

        } catch (UnknownHostException e) {
            logger.warn("Unknown host: {}; remote controller disabled.", master);
        }
        controller = new Worker(masterAddr, this);

        docRDSController = new DocRDSController();
        docS3Controllers = new ArrayList<>();
        for (int i = 0; i < (int) Math.ceil(threadCount / 10.0); i++) {
            String tId = "" + "0123456789ABCDEF".charAt((i / 16) % 16) + "0123456789ABCDEF".charAt(i % 16);
            String temp = String.format("%s-T%s", crawlerIdentifier, tId);
            docS3Controllers.add(new DocS3Controller(temp));
        }

        HTMLCount = new ArrayList<>(Collections.nCopies(threadCount, 0L));
        downloadedBytes = new ArrayList<>(Collections.nCopies(threadCount, 0L));
        dispatcher = new TaskDispatcher(this);
        for (int i = 0; i < threadCount; i++) {
            threads.add(new CrawlerWorker(i, this));
        }
        seeds.forEach(e -> dispatcher.addTask(new CrawlerTask(new URLInfo(e))));
        logger.info("Crawler initialized successfully!");
    }

    /**
     * Method to start crawler
     */
    public synchronized final void start() {
        List<Thread> allThreads = new ArrayList<>();
        dispatcher.start();
        for (CrawlerWorker worker : threads) {
            Thread rawWorker = new Thread(worker);
            allThreads.add(rawWorker);
            rawWorker.start();
        }
        controller.start();
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // stop network emit
        logger.info("Stopping current crawler.");
        dispatcher.stop();
        threads.forEach(e -> e.stop());
        logger.info("Waiting for threads to be interrupted");
        try {
            Thread.sleep(15 * 1000);
        } catch (InterruptedException e) {
            logger.info("Wait 15 seconds for all the threads to end.");
        }
        controller.stop();

        // stop storage
        logger.info("Handling storage closing...");
        docS3Controllers.forEach(e -> e.close());
        dispatcher.saveProgress();
        logger.info("Storage closed successfully!");

        allThreads.forEach(e -> e.interrupt());
        logger.info("Crawler quits");
    }

    public final int getThreadCount() {
        return threadCount;
    }

    public final DatagramSocket getSocket() {
        return socket;
    }

    public final InetAddress getMonitorInetAddr() {
        return monitorInetAddr;
    }

    public final String getCrawlerIdentifier() {
        return crawlerIdentifier;
    }

    public final Worker getController() {
        return controller;
    }

    public final DocRDSController getDocRDSController() {
        return docRDSController;
    }

    public final DocS3Controller getDocS3Controller(int threadId) {
        return docS3Controllers.get(threadId / 10);
    }

    public final List<Long> getHTMLCount() {
        return HTMLCount;
    }

    public final TaskDispatcher getDispatcher() {
        return dispatcher;
    }

    public final List<CrawlerWorker> getThreads() {
        return threads;
    }

    /**
     * Get HTML count with threadId and increment it
     * @param threadId
     */
    public final void incHTMLCount(int threadId) {
        HTMLCount.set(threadId, HTMLCount.get(threadId) + 1);
    }

    /**
     * Get downloaded bytes with threadId and increment it with given bytes
     * @param threadId
     * @param bytes
     */
    public final void incDownloadedBytes(int threadId, long bytes) {
        downloadedBytes.set(threadId, downloadedBytes.get(threadId) + bytes);
    }
}
