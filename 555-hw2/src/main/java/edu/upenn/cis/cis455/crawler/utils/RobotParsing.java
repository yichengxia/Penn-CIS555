package edu.upenn.cis.cis455.crawler.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RobotParsing {
    
    Logger logger = LogManager.getLogger(RobotParsing.class);

    private Set<String> allowList;
    private Set<String> blockList;
    private Set<String> visitedPath;

    private String url;
    private boolean ready;
    private int delay;
    private long timeStamp;

    public RobotParsing(String url) {
        allowList = new HashSet<>();
        blockList = new HashSet<>();
        visitedPath = new HashSet<>();

        this.url = url;
        logger.info("Processing robot.txt for site: {}", url);
        boolean status = robot(url);
        ready = true;
        if (!status) {
            return;
        }
        timeStamp = System.currentTimeMillis();
    }

    private boolean robot(String url) {
        System.err.println("Executing robot file!");
        InputStream inputStream = null;
        String roboUrl = url + "/robots.txt";
        try {
            inputStream = parseInputStream(roboUrl);
        } catch (Exception e) {
            logger.error("Exception when processing input stream from: {}.", url);
        }
        if (inputStream == null) {
            logger.error("Input stream is null.");
            return false;
        }
        String line;
        StringBuffer sb = new StringBuffer();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = br.readLine()) != null) {
                sb.append(line + "\r\n");
            }
        } catch (IOException e) {
            logger.error("IO Exception when reading from the input stream.");
        }
        String response = sb.toString();
        parseUserAgent(response, "cis455crawler");
        parseUserAgent(response, "*");
        return true;
    }

    private void parseUserAgent(String response, String userAgent) {
        response = response.toLowerCase();
        String[] responseArr = response.split("user-agent:");
        for (int i = 0; i < responseArr.length; i++) {
            responseArr[i] = responseArr[i].trim();
        }
        List<String> inputList = Arrays.asList(responseArr);
        inputList.forEach(input -> {
            if (input.startsWith(userAgent + "\n") || input.startsWith(userAgent + "\r\n")) {
                String[] rowsArr = input.split("(\r\n|\n)");
                for (String rows : rowsArr) {
                    String[] rowArr = rows.split(":");
                    if (rowArr == null || rowArr.length != 2) {
                        continue;
                    }
                    String key = rowArr[0].toLowerCase().trim();
                    String value = rowArr[1].toLowerCase().trim();
                    if (key.equals("crawl-delay")) {
                        try {
                            delay = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            logger.error("Number Format Exception when parsing.");

                        }
                    } else {
                        String path = Paths.get(value).normalize().toString();
                        if (key.equals("allow")) {
                            allowList.add(path.endsWith("*") ? path.substring(0, path.length() - 1) : path);
                        } else if (key.equals("disallow")) {
                            blockList.add(path.endsWith("*") ? path.substring(0, path.length() - 1) : path);
                        }
                    }
                }
            }
        });
    }

    private InputStream parseInputStream(String roboUrl) throws IOException {
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = (HttpURLConnection) new URL(roboUrl).openConnection();
        } catch (MalformedURLException e) {
            logger.error("Exception when setting up URL connection.");
        }
        httpURLConnection.setRequestProperty("User-Agent", "cis455crawler");
        httpURLConnection.connect();
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            logger.info("URL: {} found!", roboUrl);
            return httpURLConnection.getInputStream();
        } else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
            logger.info("Redirecting!");
            String redirectUrl = httpURLConnection.getHeaderField("Location");
            return parseInputStream(redirectUrl);
        } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            logger.error("Not found!");
            ready = true;
        }
        logger.error("{} error when trying to find robots.txt.", responseCode);
        return null;
    }

    public String url() {
        return url;
    }

    public boolean waited() {
        if (delay * 1000 > System.currentTimeMillis() - timeStamp || !ready) {
            return true;
        } else {
            timeStamp = System.currentTimeMillis();
            return false;
        }
    }

    public boolean crawl() {
        return ready;
    }

    public boolean parse(String filePath) {
        if (!ready) {
            return false;
        }
        filePath = Paths.get(filePath).normalize().toString();
        if (visitedPath.contains(filePath)) {
            return false;
        }
        visitedPath.add(filePath);
        return search(allowList, blockList, filePath);
    }

    private boolean search(Set<String> allowList, Set<String> blockList, String filePath) {
        int allowIndex = match(allowList, filePath);
        int blockIndex = match(blockList, filePath);
        return allowIndex >= blockIndex;
    }

    private int match(Set<String> list, String filePath) {
        int index = -1;
        for (String listPath : list) {
            int temp = compare(0, 0, listPath, filePath) ? listPath.length() : -1;
            if (temp > index) {
                index = temp;
            }
        }
        return index;
    }

    private boolean compare(int x, int y, String listPath, String filePath) {
        if (x == listPath.length()) {
            return true;
        } else if (y == filePath.length()) {
            return x == listPath.length() - 1 && listPath.charAt(x) == '$';
        } else if (listPath.charAt(x) == '*') {
            return compare(x + 1, y, listPath, filePath) || compare(x, y + 1, listPath, filePath);
        } else if (listPath.charAt(x) != filePath.charAt(y)) {
            return false;
        } else {
            return compare(x + 1, y + 1, listPath, filePath);
        }
    }
}
