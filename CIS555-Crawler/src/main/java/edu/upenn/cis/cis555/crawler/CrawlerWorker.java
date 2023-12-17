package edu.upenn.cis.cis555.crawler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.zip.GZIPInputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upenn.cis.cis555.crawler.info.RobotParser;
import edu.upenn.cis.cis555.crawler.info.URLInfo;
import edu.upenn.cis.cis555.storage.DocRDSController;
import edu.upenn.cis.cis555.storage.DocRDSEntity;
import edu.upenn.cis.cis555.storage.DocS3Block;
import edu.upenn.cis.cis555.storage.DocS3Controller;
import edu.upenn.cis.cis555.storage.DocS3Entity;
import edu.upenn.cis.cis555.util.LRUHashMap;

/**
 * This is a runnable crawler worker class.
 */
public class CrawlerWorker implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(CrawlerWorker.class);

    private final int workerId;
    private final Crawler crawler;
    private final TaskDispatcher dispatcher;
    private final DocRDSController docRDSController;
    private final DocS3Controller docS3Contoller;
    private final BlockingQueue<CrawlerTask> queue;
    private final LRUHashMap<String, Long> lastVisitedMap;
    private final LRUHashMap<String, RobotParser> robotParserMap;

    private boolean continued;
    
    public CrawlerWorker(int workerId, Crawler crawler) {
        this.workerId = workerId;
        this.crawler = crawler;
        dispatcher = crawler.getDispatcher();
        docRDSController = crawler.getDocRDSController();
        docS3Contoller = crawler.getDocS3Controller(workerId);
        queue = new DelayQueue<CrawlerTask>();
        lastVisitedMap = new LRUHashMap<String, Long>(256);
        robotParserMap = new LRUHashMap<String, RobotParser>(256);
        continued = true;
    }

    @Override
    public void run() {
        try {
            while (continued) {
                try {
                    CrawlerTask task = queue.take();
                    final URLInfo url = task.getUrl();
                    RobotParser robotParser = robotParserMap.get(url.getHost());
                    if (robotParser == null) {
                        URLInfo robotParserURL = url.getNewFilePath("/robots.txt");
                        CrawlerTask newTask = new CrawlerTask(url.getNewFilePath("/robots.txt"), 0L);
                        HttpURLConnection conn = send(newTask, "GET", false);
                        robotParser = processrobotParser(conn, robotParserURL);
                        robotParserMap.put(url.getHost(), robotParser);
                    }
                    int delay = robotParser.getDelay(Crawler.USER_AGENT, 1) * 500;
                    if (delay >= 60 * 1000) {
                        continue;
                    }
                    long curr = System.currentTimeMillis();
                    long lastTimestamp = lastVisitedMap.getOrDefault(url.getHost(), 0L);
                    if (curr - lastTimestamp < delay) {
                        task.setDelay(lastTimestamp + delay);
                        dispatcher.addTask(task);
                        continue;
                    }
                    if (!robotParser.allowedUrl(url, Crawler.USER_AGENT)) {
                        continue;
                    }
                    HttpURLConnection conn;

                    /** HEAD */
                    conn = send(task, "HEAD", true);
                    if (conn == null) {
                        continue;
                    }
                    int responseCode = conn.getResponseCode();
                    String lang = conn.getHeaderField("content-language");
                    int size = conn.getContentLength();
                    lastVisitedMap.put(url.getHost(), System.currentTimeMillis());
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        if (!interestedType(conn.getContentType()) || !interestedContentLang(lang) || !interestedContentSize(size)) {
                            conn.disconnect();
                            continue;
                        }
                    } else {
                        conn.disconnect();
                        continue;
                    }
                    closeConnection(conn);

                    /** GET */
                    conn = send(task, "GET", true);
                    if (conn != null && conn.getResponseCode() == 200) {
                        byte[] rawContent = getRawContent(conn);
                        if (rawContent == null) {
                            conn.disconnect();
                            continue;
                        }
                        String urlRaw = url.toString();
                        Document doc = Jsoup.parse(new String(rawContent), urlRaw);
                        short documentDBType = DocS3Block.DOCTYPE_HTML;
                        lang = doc.select("html").first().attr("lang");
                        if (!interestedDocLang(lang)) {
                            conn.disconnect();
                            continue;
                        }
                        Element metaRobotsElement = doc.select("meta[name=robots]").first();
                        if (!interestedMetaRobotsElement(metaRobotsElement)) {
                            conn.disconnect();
                            continue;
                        }
                        crawler.incHTMLCount(getWorkerId());
                        crawler.incDownloadedBytes(getWorkerId(), rawContent.length);
                        boolean isURLExistInDB = addDocument(url, conn.getLastModified(), documentDBType, rawContent);
                        if (isURLExistInDB) {
                            conn.disconnect();
                            continue;
                        }
                        Set<String> urlsInPage = new LinkedHashSet<>();
                        for (Element link : doc.select("a[href]")) {
                            if (!interestedLinkRef(link)) {
                                continue;
                            }
                            URLInfo newUrl = new URLInfo(link.absUrl("href"));
                            String newUrlStr = newUrl.toString();
                            if (urlRaw.equals(newUrlStr)) {
                                continue;
                            }
                            if (interestedUrl(newUrl)) {
                                if (!urlsInPage.add(newUrlStr) || docRDSController.isUrlSeen(newUrl.toUrlId())) {
                                    continue;
                                }
                                dispatcher.addTask(new CrawlerTask(newUrl));
                            }
                        }
                        conn.disconnect();
                    }
                } catch (IOException e) {
                    logger.error("Run loop", e);
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception e) {
                    logger.error("Run loop", e);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send CrawlerTask instance
     * @param task
     * @param method
     * @param toStore
     * @return connection
     */
    private HttpURLConnection send(CrawlerTask task, String method, boolean toStore) {
        HttpURLConnection conn = null;
        try {
            URLInfo url = task.getUrl();
            sendHeartbeatPacket(url, crawler.getMonitorInetAddr(), crawler.getSocket());
            conn = connect(url, method);
            // check responseCode
            int responseCode = conn.getResponseCode();
            if (responseCode < 0) {
                closeConnection(conn);
                return null;
            }
            while (responseCode / 100 == 3) {
                // get redirected URL
                String redirection = conn.getHeaderField("Location");
                if (redirection == null) {
                    closeConnection(conn);
                    return null;
                }
                task = task.getRedirectedTask(redirection);
                URLInfo redirectedURL = task.getUrl();
                if (task.getCount() > 5) {
                    closeConnection(conn);
                    return null;
                }
                if (!toStore) {
                    if (!redirection.endsWith("robots.txt")) {
                        closeConnection(conn);
                        return null;
                    }
                    conn = connect(task.getUrl(), method);
                    if (conn == null || conn.getResponseCode() < 0) {
                        closeConnection(conn);
                        return null;
                    }
                    continue;
                } else { // parse intersted URL
                    if (interestedUrl(redirectedURL)) {
                        try {
                            if (docRDSController.isUrlSeen(redirectedURL.toUrlId())) {
                                break;
                            }
                        } catch (SQLException e) {
                            logger.error("SQL Exception happened");
                        }
                        dispatcher.addTask(task);
                    }
                }
                closeConnection(conn);
                return null;
            }
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    /**
     * Helper method to check if we are intersted in some URL
     * @param url
     * @return boolean flag
     */
    private boolean interestedUrl(URLInfo url) {
        if (!url.isValid()) {
            return false;
        }
        String extension = url.getFileExtension();
        String rawUrl = url.toString();
        if (!(extension.equals("") || extension.equals("htm") || extension.equals("html")) || rawUrl.length() > 1024) {
            return false;
        }
        return true;
    }

    /**
     * Helper method to send HeartbeatPacket with UDP socket
     * @param url
     * @param monitorInetAddress
     * @param udpSocket
     */
    private void sendHeartbeatPacket(URLInfo url, InetAddress monitorInetAddress, DatagramSocket udpSocket){
        if (monitorInetAddress == null || udpSocket == null) {
            return;
        }
        byte[] data = ("yc;" + url).getBytes();
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, monitorInetAddress, 2333);
            udpSocket.send(packet);
        } catch (IOException e) {
            // logger.error("IO Exception happened when reporting to the web server");
        }
    }

    /**
     * Connect to some URL
     * @param url
     * @param method
     * @return connection
     * @throws IOException
     */
    private HttpURLConnection connect(URLInfo url, String method) throws IOException {
        try {
            HttpURLConnection conn = url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod(method);
            conn.setInstanceFollowRedirects(false);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("Accept-Encoding", "gzip");
            conn.addRequestProperty("User-Agent", Crawler.USER_AGENT);
            return conn;
        } catch (ProtocolException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Close some connection
     * @param conn
     */
    private void closeConnection(HttpURLConnection conn) {
        if (conn != null) {
            conn.disconnect();
            logger.info("Connection closed successfully");
        } else {
            logger.info("Connection closed before; no need to close again");
        }
    }

    /**
     * Process connection with robotParser
     * @param conn
     * @param robotParserURL
     * @return robotParser
     */
    private RobotParser processrobotParser(HttpURLConnection conn, URLInfo robotParserURL) {
        RobotParser robotParser = null;
        try {
            if (conn != null && conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                robotParser = new RobotParser(new String(getRawContent(conn)), Crawler.USER_AGENT);
            }
        } catch (IOException e) {
            logger.error("IO exception");
        }

        if (robotParser == null || !robotParser.initialized()) {
            robotParser = RobotParser.dummy;
        }
        closeConnection(conn);
        return robotParser;
    }

    /**
     * Get raw content of connection
     * @param conn
     * @return byte res
     * @throws IOException
     */
    private byte[] getRawContent(HttpURLConnection conn) throws IOException {
        InputStream is = conn.getInputStream();
        if ("gzip".equals(conn.getContentEncoding())) {
            is = new GZIPInputStream(is);
        }
        byte[] res = null;
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            try {
                byte[] buffer = new byte[4096];
                int read = 0;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                res = os.toByteArray();
            } catch (SocketTimeoutException e) {
                logger.error("Socket time out");
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                    logger.error("Failed to close input connection");
                }
            }
        }
        return res;
    }

    /**
     * Check if we are interested in some type
     * @param type
     * @return boolean flag
     */
    private boolean interestedType(String type) {
        if (type == null) {
            return false;
        }
        int pos = type.indexOf(';');
        if (pos >= 0) {
            type = type.substring(0, pos);
        }
        return type.startsWith("text/html");
    }

    /**
     * Check if we are interested in some content language
     * @param lang
     * @return boolean flag
     */
    private boolean interestedContentLang(String lang) {
        return lang == null || lang.contains("en");
    }

    /**
     * Check if we are interested in some content size
     * @param size
     * @return boolean flag
     */
    private boolean interestedContentSize(int size) {
        return size == -1 || size >= 0 && size <= 10 * 1024 * 1024;
    }

    /**
     * Check if we are interested in some document language
     * @param lang
     * @return boolean flag
     */
    private boolean interestedDocLang(String lang) {
        return lang.length() == 0 || lang.length() > 0 && lang.toLowerCase().contains("en");
    }

    /**
     * Check if we are interested in some meta robots element
     * @param metaRobotsElement
     * @return boolean flag
     */
    private boolean interestedMetaRobotsElement(Element metaRobotsElement) {
        if (metaRobotsElement == null) {
            return true;
        }
        String metaRobots = metaRobotsElement.attr("content").toLowerCase();
        return !metaRobots.contains("nofollow") && !metaRobots.contains("noindex") && !metaRobots.contains("none");
    }

    private int getWorkerId() {
        return workerId;
    }

    /**
     * Get new docRDSEntity and add it to docRDSController
     * @param url
     * @param lastModifiedTs
     * @param contentType
     * @param contentBytes
     * @return boolean flag of if the URL is seen
     */
    private boolean addDocument(URLInfo url, long lastModifiedTs, short contentType, byte[] contentBytes) {
        System.err.println("addDoc: " + url.toString());
        try {
            boolean isSeen = docRDSController.isUrlSeen(url.toUrlId());
            if (isSeen) {
                return true;
            }
            // check docId
            byte[] docId = byteToSHA1(contentBytes);
            String docIdStr = DocS3Entity.toHexString(docId);
            List<DocRDSEntity> documentIndexList = docRDSController.queryDocByDocId(docIdStr);
            System.err.println("docId found!");
            // check urlId
            String urlStr = url.toString();
            byte[] urlId = byteToSHA1(urlStr.getBytes());
            String urlIdStr = DocS3Entity.toHexString(urlId);
            System.err.println("urlId found!");
            // get document block index and name
            int docBlockIndex;
            String docBlockName;
            if (documentIndexList.size() == 0) {
                System.err.println("docS3Contoller.addDoc");
                List<Object> result = docS3Contoller.addDoc(urlStr, contentType, contentBytes, docId, urlId);
                System.err.println("docS3Contoller.addDoc - added");
                docBlockIndex = (int) result.get(0);
                docBlockName = (String) result.get(1);
            } else {
                DocRDSEntity docRDSEntity = documentIndexList.get(0);
                docBlockIndex = docRDSEntity.getDocBlockIndex();
                docBlockName = docRDSEntity.getDocBlockName();
            }
            DocRDSEntity docRDSEntity = new DocRDSEntity(urlStr, urlIdStr, docIdStr, docBlockName, docBlockIndex, System.currentTimeMillis(), lastModifiedTs);
            isSeen = !docRDSController.addDoc(docRDSEntity);
            return isSeen;
        } catch (SQLException e) {
            logger.error("Failed when adding document.", e);
        }
        return false;
    }

    /**
     * Hash bytes with SHA-1 algorithm
     * @param bytes
     * @return hashed bytes
     */
    private byte[] byteToSHA1(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(bytes);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            logger.error("No Such Algorithm Exception happened");
        }
        return null;
    }

    /**
     * Check if we are interested in the element link
     * @param link
     * @return boolean flag
     */
    private boolean interestedLinkRef(Element link) {
        String ref = link.attr("ref").toLowerCase();
        return !ref.contains("nofollow") && !ref.contains("ugc") && !ref.contains("sponsored");
    }

    public final int getQueueSize() {
        return queue.size();
    }

    public final void addToQueue(CrawlerTask task) throws InterruptedException {
        queue.put(task);
    }

    public final void stop() {
        continued = false;
    }

    public final Iterator<CrawlerTask> getTaskIterator() {
        return queue.iterator();
    }
}
