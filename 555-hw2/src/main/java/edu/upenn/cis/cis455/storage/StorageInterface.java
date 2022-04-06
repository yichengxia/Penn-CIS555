package edu.upenn.cis.cis455.storage;

import java.util.List;
import java.util.Map;

import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.model.Channel;
import edu.upenn.cis.cis455.model.Document;
import edu.upenn.cis.cis455.model.URL;

public interface StorageInterface {

    /**
     * How many documents so far?
     */
    public int getCorpusSize();

    /**
     * Add a new document, getting its ID
     */
    public String addDocument(String url, String documentContents);

    /**
     * Retrieves a document's contents by URL
     */
    public String getDocument(String url);

    /**
     * Adds a user and returns an ID
     */
    public int addUser(String username, String password);

    /**
     * Tries to log in the user, or else throws a HaltException
     */
    public boolean getSessionForUser(String username, String password);

    /**
     * Shuts down / flushes / closes the storage system
     */
    public void close();

    public URL getUrl(URLInfo urlInfo);

    public URL removeUrl(URLInfo urlInfo);

    public boolean html(String url);

    public boolean hasDocument(String url);

    public void addUrl(URL url);

    public void addUrl(String channelName, String url);

    public String getCrawledTime(String url);

    public Document getDocumentModel(String url);

    public boolean addChannel(String name, String creator, String xpath);

    public Channel getChannel(int id);

    public int getChannelId(String name);

    public List<Channel> getChannelList();

    public Map<String, Channel> getChannelMap();

    public String[] getXPath();
}
