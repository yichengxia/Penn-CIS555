package edu.upenn.cis.cis455.crawler;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.utils.RobotParsing;
import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.crawler.utils.URLParsing;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.LocalCluster;
import edu.upenn.cis.stormlite.Topology;
import edu.upenn.cis.stormlite.TopologyBuilder;
import edu.upenn.cis.stormlite.bolt.DocumentFetcherBolt;
import edu.upenn.cis.stormlite.bolt.DomParserBolt;
import edu.upenn.cis.stormlite.bolt.LinkExtractorBolt;
import edu.upenn.cis.stormlite.bolt.PathMatcherBolt;
import edu.upenn.cis.stormlite.spout.CrawlerQueueSpout;

public class Crawler implements CrawlMaster {
    ///// TODO: you'll need to flesh all of this out. You'll need to build a thread
    // pool of CrawlerWorkers etc.

    Logger logger = LogManager.getLogger(Crawler.class);

    static final int NUM_WORKERS = 10;

    private StorageInterface db;
    private int size;
    private int count;

    private BlockingQueue<String> queue;
    // MS 1
    // private List<CrawlerWorker> workerList;
    // private List<Thread> threadPool;
    private Map<String, RobotParsing> robotMap;
    private Set<String> signSet;

    private AtomicInteger workingNum;
    private AtomicInteger exitedNum;
    private AtomicInteger documentNum;

    private static Crawler crawler;
    private LocalCluster localCluster;

    public Crawler(String startUrl, StorageInterface db, int size, int count) {
        // TODO: initialize
        logger.info("Initiating crawler with size={} and count={}.", size, count);
        this.db = db;
        this.size = size;
        this.count = count;

        queue = new LinkedBlockingDeque<>();
        try {
            queue.put(startUrl);
            logger.info("Start URL added to the blocking queue.");
        } catch (Exception e) {
            logger.error("Exception when adding start URL to the blocking queue.");
        }

        // MS 1
        // workerList = new ArrayList<>();
        // threadPool = new ArrayList<>();
        // for (int i = 0; i < NUM_WORKERS; i++) {
        //     CrawlerWorker crawlerWorker = new CrawlerWorker(this, queue, this.db);
        //     workerList.add(crawlerWorker);
        //     Thread thread = new Thread(crawlerWorker);
        //     thread.setName("Thread " + i);
        //     threadPool.add(thread);
        // }

        robotMap = new HashMap<>();
        signSet = new HashSet<>();

        workingNum = new AtomicInteger(0);
        exitedNum = new AtomicInteger(0);
        documentNum = new AtomicInteger(0);
    }

    /**
     * Main thread
     */
    public void start() {
        // MS 1
        // logger.info("Crawler starting!");
        // System.err.println("Crawler starting!");
        // for (Thread thread : threadPool) {
        //     thread.start();
        // }
        // while (exitedNum.get() != NUM_WORKERS) {
        //     try {
        //         Thread.sleep(1000);
        //     } catch (InterruptedException e) {
        //         logger.error("Interrupted Exception when executing thread to sleep.");
        //     }
        // }
        // for (Thread thread : threadPool) {
        //     try {
        //         thread.join();
        //     } catch (InterruptedException e) {
        //         logger.error("Interrupted Exception when executing thread to join.");
        //     }
        // }
        // logger.info("{} document(s) crawled!", documentNum.get());

        CrawlerQueueSpout crawlerQueueSpout = new CrawlerQueueSpout();
        DocumentFetcherBolt documentFetcherBolt = new DocumentFetcherBolt();
        LinkExtractorBolt linkExtractorBolt = new LinkExtractorBolt();
        DomParserBolt domParserBolt = new DomParserBolt();
        PathMatcherBolt pathMatcherBolt = new PathMatcherBolt();
        
        TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setSpout("crawlerQueueSpout", crawlerQueueSpout, 1);
        topologyBuilder.setBolt("documentFetcherBolt", documentFetcherBolt, 1).shuffleGrouping("crawlerQueueSpout");
        topologyBuilder.setBolt("linkExtractorBolt", linkExtractorBolt, 2).shuffleGrouping("documentFetcherBolt");
        topologyBuilder.setBolt("pathMatcherBolt", pathMatcherBolt, 3).shuffleGrouping("documentFetcherBolt");
        topologyBuilder.setBolt("domParserBolt", domParserBolt, 2).shuffleGrouping("pathMatcherBolt");
        localCluster = new LocalCluster();
        Topology topology = topologyBuilder.createTopology();
        try {
            String writeTopology = new ObjectMapper().writeValueAsString(topology);
            logger.info("Writing topology: {}.", writeTopology);
        } catch (JsonProcessingException e) {
            logger.error("Json Processing Exception when writing topology.");
        }
        Config config = new Config();
        localCluster.submitTopology("crawler", config, topology);
        while (!isDone()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        localCluster.killTopology("crawler");
        localCluster.shutdown();
        logger.debug("Cluster is shut down.");
    }

    /**
     * We've indexed another document
     */
    @Override
    public void incCount() {
        documentNum.getAndIncrement();
    }

    /**
     * Workers can poll this to see if they should exit, ie the crawl is done
     */
    @Override
    public boolean isDone() {
        return (queue.isEmpty() && workingNum.get() == 0) || count < documentNum.get();
    }

    /**
     * Workers should notify when they are processing an URL
     */
    @Override
    public void setWorking(boolean working) {
        if (working) {
            logger.info("Worker is working. Count incremented from {}.", workingNum.getAndIncrement());
        } else {
            logger.info("Worker is not working. Count decremented from {}.", workingNum.getAndDecrement());
        }
    }

    /**
     * Workers should call this when they exit, so the master knows when it can shut
     * down
     */
    @Override
    public void notifyThreadExited() {
        logger.info("Thread exiting.");
        exitedNum.getAndIncrement();
    }

    public boolean crawl(String url) {
        if (robotMap.get(url) == null) {
            synchronized (robotMap) {
                if (robotMap.get(url) == null) {
                    robotMap.put(url, new RobotParsing(url));
                }
            }
        }
        return robotMap.get(url).crawl();
    }

    public boolean waitCrawl(String url) {
        RobotParsing rp = robotMap.get(url);
        if (rp == null) {
            return false;
        }
        if (count < workingNum.get() + documentNum.get()) {
            return true;
        }
        return rp.waited();
    }

    public boolean parse(URLInfo urlInfo) {
        return robotMap.get(URLParsing.getURL(urlInfo, false)).parse(urlInfo.getFilePath());
    }

    public boolean index(String url) {
        String sign = URLParsing.getMD5(url);
        if (signSet.contains(sign)) {
            return false;
        } else {
            signSet.add(sign);
            return true;
        }
    }

    public boolean valid(int length, String type) {
        type = type.toLowerCase();
        if (type == null || size < length) {
            logger.error("Invalid document with length={} and type={}", length, type);
            return false;
        }
        return type.equals("application/xml") || type.equals("text/html") || type.equals("text/xml") || type.equals("+xml");
    }

    public StorageInterface getDatabase() {
        return db;
    }

    public synchronized BlockingQueue<String> getQueue() {
        return queue;
    }

    public static Crawler getCrawler() {
        return crawler;
    }

    public boolean containsSign(String url) {
        String sign = URLParsing.getMD5(url);
        if (signSet.contains(sign)) {
            return true;
        } else {
            signSet.add(sign);
            return false;
        }
    }

    private static void setCrawler(Crawler crawler) {
        Crawler.crawler = crawler;
    }

    /**
     * Main program: init database, start crawler, wait for it to notify that it is
     * done, then close.
     */
    public static void main(String args[]) {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
        if (args.length < 3 || args.length > 5) {
            System.out.println("Usage: Crawler {start URL} {database environment path} {max doc size in MB} {number of files to index}");
            System.exit(1);
        }

        System.out.println("Crawler starting");
        String startUrl = args[0];
        String envPath = args[1];
        Integer size = Integer.valueOf(args[2]) * 1024 * 1024;
        Integer count = args.length == 4 ? Integer.valueOf(args[3]) : 100;

        if (!Files.exists(Paths.get(args[1]))) {
            try {
                Files.createDirectory(Paths.get(args[1]));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        StorageInterface db = StorageFactory.getDatabaseInstance(envPath);

        Crawler crawler = new Crawler(startUrl, db, size, count);
        Crawler.setCrawler(crawler);

        System.out.println("Starting crawl of " + count + " documents, starting at " + startUrl);
        crawler.start();

        while (!crawler.isDone())
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        // TODO: final shutdown
        db.close();
        System.out.println("Done crawling!");
    }

}
