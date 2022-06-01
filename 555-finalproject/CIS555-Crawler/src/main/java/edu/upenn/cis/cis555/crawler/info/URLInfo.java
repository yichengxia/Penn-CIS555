package edu.upenn.cis.cis555.crawler.info;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This is URLInfo class to parse URLs.
 */
public class URLInfo implements Serializable {

    private String protocol = null;
    private String host = null;
    private int portNo = -1;
    private String filePath = null;
    private boolean secure = false;

    public URLInfo(String protocol, String host, int portNo, String filePath) {
        this.protocol = protocol;
        this.host = host;
        this.portNo = portNo;
        this.filePath = filePath;
    }

    /**
     * Constructor method to parse the document URL
     * @param docURL
     */
    public URLInfo(String docURL) {
        System.err.println("Parsing document URL: " + docURL);
        if (docURL == null || docURL.equals("")) {
            return;
        }
        docURL = docURL.trim();
        if (docURL.startsWith("http://")) {
            protocol = "http";
            portNo = 80;
            if (docURL.length() <= 7) {
                return;
            }
            docURL = docURL.substring(7);
        } else if (docURL.startsWith("https://")) {
            protocol = "https";
            portNo = 443;
            secure = true;
            if (docURL.length() <= 8) {
                return;
            }
            docURL = docURL.substring(8);
        } else {
            return;
        }
        int i = 0;
        while (i < docURL.length()) {
            char c = docURL.charAt(i);
            if (c == '/' || c == '?') {
                break;
            }
            i++;
        }
        String address = docURL.substring(0, i);
        if (i == docURL.length()) {
            filePath = "/";
        } else if (docURL.charAt(i) == '/') {
            filePath = docURL.substring(i);
        } else {
            filePath = "/" + docURL.substring(i);
        }
        int pos = filePath.indexOf('#');
        if (pos >= 0) {
            filePath = filePath.substring(0, pos);
        }
        if (address.equals("/") || address.equals(""))
            return;
        if (address.indexOf(':') != -1) {
            String[] comp = address.split(":", 2);
            host = comp[0].trim();
            try {
                portNo = Integer.parseInt(comp[1].trim());
            } catch (NumberFormatException e) {
                System.err.println("Number Format Exception happened");
            }
        } else {
            host = address;
        }
    }

    public String getHost() {
        return host;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isSecure() {
        return secure;
    }

    /**
     * Convert URLInfo back to an http(s) URL without additional file path
     * @return root URL string
     */
    public String toRootURLString() {
        if ("http".equals(protocol) && portNo == 80 || "https".equals(protocol) && portNo == 443) {
            return protocol + "://" + host;
        }
        return protocol + "://" + host + ":" + portNo;
    }

    /**
     * Get a new URLInfo class with given filePath and default parameters
     * @param filePath
     * @return new URLInfo
     */
    public URLInfo getNewFilePath(String filePath) {
        return new URLInfo(protocol, host, portNo, filePath);
    }

    /**
     * Returns a connection
     * @return connection
     * @throws MalformedURLException
     * @throws IOException
     */
    public HttpURLConnection openConnection() throws MalformedURLException, IOException {
        if (!isValid()) {
            return null;
        }
        return (HttpURLConnection) (new URL(toString())).openConnection();
    }

    /**
     * Check if the URLInfo class is valid
     * @return boolean flag
     */
    public boolean isValid() {
        return portNo > 0 && portNo < 65536 && filePath != null && protocol != null && host != null && filePath != null;
    }

    /**
     * Generate URL ID with SHA-1 algorithm
     * @return URL ID
     */
    public String toUrlId() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(toString().getBytes());
            String res = String.format("%1$40s", (new BigInteger(1, md.digest())).toString(16)).replace(' ', '0');
            return res;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No Such Algorithm Exception happened");
        }
        return null;
    }

    /**
     * Get file exetension
     * @return file exetension
     */
    public final String getFileExtension() {
        String fileName = getFileName();
        int pos = fileName.lastIndexOf('.');
        return pos == -1 ? "" : fileName.substring(pos + 1);
    }

    /**
     * Helper method to get file name
     * @return file name
     */
    private String getFileName() {
        int pos = filePath.indexOf('?');
        return pos != -1 ? filePath = filePath.substring(0, pos) : filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    /**
     * Convert URLInfo back to an http(s) URL
     * @return URL string
     */
    @Override
    public String toString() {
        if ("http".equals(protocol) && portNo == 80 || "https".equals(protocol) && portNo == 443) {
            return protocol + "://" + host + filePath;
        }
        return protocol + "://" + host + ":" + portNo + filePath;
    }
}
