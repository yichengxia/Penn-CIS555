package edu.upenn.cis.stormlite.bolt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.Crawler;
import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.crawler.utils.URLParsing;
import edu.upenn.cis.cis455.model.URL;
import edu.upenn.cis.cis455.storage.StorageInterface;
import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;
import spark.Spark;

public class DocumentFetcherBolt implements IRichBolt {

    Logger logger = LogManager.getLogger(DocumentFetcherBolt.class);

    private StorageInterface db;
    private Crawler crawler;
    private Fields schema;
    private String executorId;
    private boolean working;
    private OutputCollector outputCollector;

    public DocumentFetcherBolt(){
        crawler = Crawler.getCrawler();
        db = crawler.getDatabase();
        schema = new Fields( "url", "type", "content");
        executorId = UUID.randomUUID().toString();
        working = false;
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
        while (true) {
            setWorking(true);
            String nextUrl = input.getStringByField("url");
            if (nextUrl == null) {
                logger.error("URL is null.");
                break;
            }
            if (!nextUrl.toLowerCase().startsWith("http")) {
                logger.error("URL: {} is illegal.", nextUrl);
                continue;
            }
            URLInfo urlInfo = new URLInfo(nextUrl);
            String documentUrl = URLParsing.getURL(urlInfo, true);
            String hostUrl = URLParsing.getURL(urlInfo, false);
            logger.info("documentUrl: " + documentUrl);
            logger.info("hostUrl: " + hostUrl);
            if (!crawler.crawl(hostUrl)) {
                logger.info("We can crawl now!");
                continue;
            }
            while (crawler.waitCrawl(hostUrl)) {
                if (crawler.isDone()) {
                    logger.info("Crawler closed.");
                    break;
                }
            }
            if (crawler.isDone()) {
                logger.info("Crawler is done.");
                break;
            }
            if (!crawler.parse(urlInfo)) {
                logger.error("Crawler cannot parse {}", urlInfo.toString());
                return;
            }

            long timeStamp = 0;
            URL url = db.getUrl(urlInfo);
            if (url != null) {
                timeStamp = url.getTimestamp();
            }
            String urlString = null;
            boolean isHtml = false;
            try {
                HttpURLConnection httpURLConnection = connect(documentUrl, "HEAD");
                httpURLConnection.setIfModifiedSince(timeStamp);
                int responseCode = httpURLConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String type = httpURLConnection.getContentType().split(";")[0].toLowerCase().trim();
                    if (!crawler.valid(httpURLConnection.getContentLength(), type)) {
                        logger.error("Invalid document!");
                        httpURLConnection.disconnect();
                        return;
                    }
                    httpURLConnection.disconnect();
                    HttpURLConnection getConnection = connect(documentUrl, "GET");
                    logger.info("Downloading {}.", documentUrl);
                    if (getConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        logger.error("Failed to download from URL: {}", documentUrl);
                    }
                    urlString = parse(getConnection);
                    isHtml = type.equals("text/html");
                    if (!db.hasDocument(urlString)) {
                        logger.info("Find an uncrawled document!");
                        crawler.incCount();
                    }
                    String id = db.addDocument(urlString, type);
                    db.addUrl(new URL(id, removePort(documentUrl), httpURLConnection.getLastModified()));
                } else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    logger.info("{} is not modified.", documentUrl);
                    urlString = db.getDocument(documentUrl);
                    isHtml = db.html(documentUrl);
                } else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                    return;
                } else {
                    logger.error("Unexpected status code with URL: {}", documentUrl);
                    httpURLConnection.disconnect();
                    return;
                }
                httpURLConnection.disconnect();
            } catch (Exception e) {
                logger.error("{} happened.", e.toString());
                break;
            } finally {
                setWorking(false);
            }
            if (urlString != null){
                if (isHtml) {
                    logger.info("Output Collector emitting HTML value.");
                    outputCollector.emit(new Values<>(nextUrl, "html", urlString));
                } else {
                    logger.info("Output Collector emitting XML value.");
                    outputCollector.emit(new Values<>(nextUrl, "xml", urlString));
                }
            }
            break;
        }
    }

    @Override
    public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
        outputCollector = collector;
        crawler = Crawler.getCrawler();
        db = crawler.getDatabase();
        working = false;
    }

    @Override
    public void setRouter(IStreamRouter router) {
        outputCollector.setRouter(router);
    }

    @Override
    public Fields getSchema() {
        return schema;
    }

    private String removePort(String documentUrl) {
        int count = 0, j = 0, i;
        for (i = 0; i < documentUrl.length(); i++) {
            if (documentUrl.charAt(i) == ':') {
                count++;
                if (count == 2) {
                    j = i;
                }
            }
        }
        if (count <= 1) {
            return documentUrl;
        } else {
            String parsedUrl = documentUrl.substring(0, j);
            i = j + 1;
            while (i < documentUrl.length() && Character.isDigit(documentUrl.charAt(i))) {
                i++;
            }
            parsedUrl = parsedUrl + documentUrl.substring(i);
            return parsedUrl;
        }
    }

    private String parse(HttpURLConnection httpURLConnection) {
        BufferedReader bf = null;
        try {
            bf = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String lines = null;
            while ((lines = bf.readLine()) != null) {
                sb.append(lines + "\n");
            }
            bf.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HttpURLConnection connect(String urlString, String method) {
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = (HttpURLConnection) new java.net.URL(urlString).openConnection();
            httpURLConnection.setRequestMethod(method);
            httpURLConnection.setRequestProperty("User-Agent", "cis455crawler");
            return httpURLConnection;
        } catch (Exception e) {
            Spark.halt(500);
        }
        return httpURLConnection;
    }

    private synchronized void setWorking(boolean working) {
        if (this.working != working) {
            this.crawler.setWorking(working);
        }
        this.working = working;
    }
}
