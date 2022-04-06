package edu.upenn.cis.stormlite.bolt;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;

public class LinkExtractorBolt implements IRichBolt {

    Logger logger = LogManager.getLogger(LinkExtractorBolt.class);

    private Crawler crawler;
    private Fields schema;
    private BlockingQueue<String> queue;
    private String executorId;
    private OutputCollector outputCollector;

    public LinkExtractorBolt() {
        crawler = Crawler.getCrawler();
        schema = new Fields();
        queue = crawler.getQueue();
        executorId = UUID.randomUUID().toString();
    }

    @Override
    public String getExecutorId() {
        return executorId;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(schema);
    }

    @Override
    public void cleanup() {
        return;
    }

    @Override
    public void execute(Tuple input) {
        crawler.setWorking(true);
        String url = input.getStringByField("url");
        String type = input.getStringByField("type");
        String content = input.getStringByField("content");
        if (type.equals("html")) {
            if (crawler.containsSign(content)) {
                return;
            }
            org.jsoup.nodes.Document tempDocument = Jsoup.parse(content);
            tempDocument.setBaseUri(url);
            tempDocument.getElementsByAttribute("href").forEach(e -> {
                String nextUrl = e.absUrl("href");
                logger.info("Adding URL: {}", nextUrl);
                queue.offer(nextUrl);
            });
        }
        crawler.setWorking(false);
    }

    @Override
    public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
        outputCollector = collector;
    }

    @Override
    public void setRouter(IStreamRouter router) {
        outputCollector.setRouter(router);
    }

    @Override
    public Fields getSchema() {
        return schema;
    }
}
