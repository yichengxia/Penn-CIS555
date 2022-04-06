package edu.upenn.cis.stormlite.spout;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Values;

public class CrawlerQueueSpout implements IRichSpout {

    Logger logger = LogManager.getLogger(CrawlerQueueSpout.class);

    private Crawler crawler;
    private BlockingQueue<String> queue;
    private String executorId;
    private SpoutOutputCollector spoutOutputCollector;

    public CrawlerQueueSpout() {
        crawler = Crawler.getCrawler();
        queue = crawler.getQueue();
        executorId = UUID.randomUUID().toString();
    }

    @Override
    public String getExecutorId() {
        return executorId;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("url"));
    }

    @Override
    public void open(Map<String, String> config, TopologyContext topo, SpoutOutputCollector collector) {
        spoutOutputCollector = collector;
        queue = crawler.getQueue();
    }

    @Override
    public void close() {
        return;
    }

    @Override
    public void nextTuple() {
        String url = null;
        while (url == null && !crawler.isDone()) {
            try {
                url = queue.poll(300, TimeUnit.MILLISECONDS);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error("Failed to fetch URL from queue.");
                continue;
            }
        }
        if (url == null || url.equals("")) {
            return;
        }
        crawler.setWorking(true);
        logger.info("Emitting URL: {}.", url);
        spoutOutputCollector.emit(new Values<>(url));
        crawler.setWorking(false);
    }

    @Override
    public void setRouter(IStreamRouter router) {
        spoutOutputCollector.setRouter(router);
    }
}
