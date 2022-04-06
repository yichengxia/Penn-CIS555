package edu.upenn.cis.stormlite.bolt;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.xml.sax.InputSource;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;

public class DomParserBolt implements IRichBolt {

    Logger logger = LogManager.getLogger(DomParserBolt.class);

    private Crawler crawler;
    private Fields schema;
    private String executorId;
    private OutputCollector outputCollector;
    private DocumentBuilder documentBuilder;;

    public DomParserBolt() {
        crawler = Crawler.getCrawler();
        schema = new Fields("url", "type", "content", "w3c", "extractedLinks");
        executorId = UUID.randomUUID().toString();
        try {
            documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.error("DocumentBuilder cannot be created.");
        }
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
        org.w3c.dom.Document document = null;
        List<String> urlList = new ArrayList<>();
        W3CDom w3cdom = new W3CDom();
        if (type.equals("html")) {
            org.jsoup.nodes.Document tempDocument = Jsoup.parse(content);
            tempDocument.setBaseUri(url);
            tempDocument.getElementsByAttribute("href").forEach(e -> {
                String nextUrl = e.absUrl("href");
                urlList.add(nextUrl);
            });
            document = w3cdom.fromJsoup(tempDocument);
        } else {
            try {
                document = documentBuilder.parse(new InputSource(new StringReader(content)));
            } catch (Exception e) {
                logger.error("Exception when processing XML file.");
            }
        }
        outputCollector.emit(new Values<>(url, type, content, document, urlList));
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
