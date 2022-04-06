package edu.upenn.cis.cis455.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.crawler.utils.URLParsing;
import edu.upenn.cis.cis455.model.URL;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Spark;

public class CrawlerWorker implements Runnable {

    Logger logger = LogManager.getLogger(CrawlerWorker.class);

    private Crawler crawler;
    private BlockingQueue<String> queue;
    private StorageInterface db;

    private boolean working;

    public CrawlerWorker(Crawler crawler, BlockingQueue<String> queue, StorageInterface db) {
        this.crawler = crawler;
        this.queue = queue;
        this.db = db;
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                setWorking(false);
                String nextUrl = null;
                while (true) {
                    try {
                        nextUrl = queue.poll(1, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted Exception when polling from the blocking queue.");
                    }
                    if (nextUrl != null || crawler.isDone()) {
                        break;
                    }
                }
                if (nextUrl == null) {
                    break;
                }
                logger.info("{} fetched {}!", Thread.currentThread().getName(), nextUrl);
                setWorking(true);

                URLInfo urlInfo = new URLInfo(nextUrl);
                String documentUrl = URLParsing.getURL(urlInfo, true);
                String hostUrl = URLParsing.getURL(urlInfo, false);
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
                    continue;
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
                            continue;
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
                        db.addUrl(new URL(id, documentUrl, httpURLConnection.getLastModified()));
                    } else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                        logger.info("{} is not modified.", documentUrl);
                        urlString = db.getDocument(documentUrl);
                        isHtml = db.html(documentUrl);
                    } else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                        logger.info("Redirecting with URL: {}", urlInfo.toString());
                        Document document = Jsoup.parse(String.format("<a href=\"%s\"></a>", httpURLConnection.getHeaderField("Location")));
                        document.absUrl(documentUrl);
                        queue.put(document.select("a").first().attr("abs:href"));
                        httpURLConnection.disconnect();
                        continue;
                    } else {
                        logger.error("Unexpected status code with URL: {}", documentUrl);
                        httpURLConnection.disconnect();
                        continue;
                    }
                    httpURLConnection.disconnect();
                } catch (Exception e) {
                    logger.error("{} happened.", e.toString());
                }
                if (!(isHtml && crawler.index(urlString))) {
                    continue;
                }
                Document document = Jsoup.parse(urlString);
                document.setBaseUri(documentUrl);
                document.getElementsByAttribute("href").forEach(e -> {
                    String next = e.absUrl("href");
                    try {
                        logger.info("Adding URL: {}", next);
                        queue.put(next);
                    } catch (InterruptedException ie) {
                        logger.error("Interrupted Exception when adding URL: {}", next);
                    }
                });
            }
        } finally {
            if (working) {
                setWorking(false);
            }
            this.crawler.notifyThreadExited();
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

    private synchronized void setWorking(boolean status) {
        if (working != status) {
            crawler.setWorking(status);
        }
        working = status;
    }
}
