package edu.upenn.cis.cis455.crawler.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import spark.Spark;

public class URLParsing {
    
    static Logger logger = LogManager.getLogger(URLParsing.class);

    public static String getURL(URLInfo urlInfo, boolean fileExists) {
        
        boolean isSecure = urlInfo.isSecure();
        String hostName = urlInfo.getHostName();
        int portNo = urlInfo.getPortNo();
        String filePath = urlInfo.getFilePath();

        StringBuffer sb = new StringBuffer();
        if (isSecure) {
            sb.append("https://");
        } else {
            sb.append("http://");
        }
        sb.append(hostName);
        sb.append(":" + String.valueOf(portNo));
        if (fileExists) {
            sb.append("/" + filePath);
        }
        return sb.toString().replaceAll("//", "/").replaceFirst("/", "//");
    }
    
    public static String getMD5(String url) {
        String md5 = null;
        try {
            md5 = new String(MessageDigest.getInstance("MD5").digest(url.getBytes()));
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            logger.error("Exception when computing the MD5 hash.");
            Spark.halt(500);
        }
        return md5;
    }
}
