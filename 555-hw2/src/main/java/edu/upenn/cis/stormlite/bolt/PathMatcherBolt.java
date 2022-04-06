package edu.upenn.cis.stormlite.bolt;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.model.Channel;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.cis455.xpathengine.OccurrenceEvent;
import edu.upenn.cis.cis455.xpathengine.XPathEngine;
import edu.upenn.cis.cis455.xpathengine.XPathEngineFactory;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;

public class PathMatcherBolt implements IRichBolt {

    Logger logger = LogManager.getLogger(PathMatcherBolt.class);

    private Crawler crawler;
    private StorageInterface db;
    private Fields schema;
    private String executorId;
    private OutputCollector outputCollector;

    public PathMatcherBolt() {
        crawler = Crawler.getCrawler();
        db = crawler.getDatabase();
        schema = new Fields();
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
        List<Channel> channelList = db.getChannelList();
        String[] xpathArr = db.getXPath();
        if (channelList.size() == 0 || xpathArr == null) {
            crawler.setWorking(false);
            return;
        }
        OccurrenceEvent occurrenceEvent = new OccurrenceEvent(content, type);
        XPathEngine xPathEngine = XPathEngineFactory.getXPathEngine();
        xPathEngine.setXPaths(xpathArr);
        boolean[] xpathMatching = xPathEngine.evaluateEvent(occurrenceEvent);
        for (int i = 0; i < xpathMatching.length; i++) {
            if (xpathMatching[i]) {
                db.addUrl(channelList.get(i).getName(), url);
            }
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
