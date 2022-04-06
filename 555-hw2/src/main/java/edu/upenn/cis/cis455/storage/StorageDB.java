package edu.upenn.cis.cis455.storage;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentFailureException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.crawler.utils.URLParsing;
import edu.upenn.cis.cis455.model.Channel;
import edu.upenn.cis.cis455.model.Document;
import edu.upenn.cis.cis455.model.URL;
import edu.upenn.cis.cis455.model.User;

public class StorageDB implements StorageInterface {
    
    Logger logger = LogManager.getLogger(StorageDB.class);

    private Environment environment;
    private EnvironmentConfig environmentConfig;

    private DatabaseConfig databaseConfig;
    
    private Database catalogDB;
    private StoredClassCatalog storedClassCatalog;

    private Database documentDB;
    private StoredSortedMap<String, Document> documentMap;

    private Database userDB;
    private StoredSortedMap<String, User> userMap;

    private Database urlDB;
    private StoredSortedMap<String, URL> urlMap;

    private Database channelDB;
    private StoredSortedMap<String, Channel> channelMap;

    public StorageDB(String directory) {
        environmentConfig(directory);
        databaseConfig();
    }

    private void environmentConfig(String directory) {
        environmentConfig = new EnvironmentConfig();
        environmentConfig.setTransactional(true);
        environmentConfig.setAllowCreate(true);
        environment = new Environment(new File(directory), environmentConfig);
    }

    private void databaseConfig() {
        databaseConfig = new DatabaseConfig();
        databaseConfig.setTransactional(true);
        databaseConfig.setAllowCreate(true);

        // catalogDBConfig
        catalogDB = environment.openDatabase(null, "class-table", databaseConfig);
        storedClassCatalog = new StoredClassCatalog(catalogDB);

        // userDBConfig
        TupleBinding<String> userKey = TupleBinding.getPrimitiveBinding(String.class);
        EntryBinding<User> userValue = new SerialBinding<>(storedClassCatalog, User.class);
        userDB = environment.openDatabase(null, "user-table", databaseConfig);
        userMap = new StoredSortedMap<>(userDB, userKey, userValue, true);

        // documentDBConfig
        TupleBinding<String> documentKey = TupleBinding.getPrimitiveBinding(String.class);
        EntryBinding<Document> documentValue = new SerialBinding<>(storedClassCatalog, Document.class);
        documentDB = environment.openDatabase(null, "doc-table", databaseConfig);
        documentMap = new StoredSortedMap<>(documentDB, documentKey, documentValue, true);

        // urlDBConfig
        TupleBinding<String> urlKey = TupleBinding.getPrimitiveBinding(String.class);
        EntryBinding<URL> urlValue = new SerialBinding<>(storedClassCatalog, URL.class);
        urlDB = environment.openDatabase(null, "url-table", databaseConfig);
        urlMap = new StoredSortedMap<>(urlDB, urlKey, urlValue, true);

        // channelDBConfig
        TupleBinding<String> channelKey = TupleBinding.getPrimitiveBinding(String.class);
        EntryBinding<Channel> channelValue = new SerialBinding<>(storedClassCatalog, Channel.class);
        channelDB = environment.openDatabase(null, "channel-table", databaseConfig);
        channelMap = new StoredSortedMap<>(channelDB, channelKey, channelValue, true);
    }

    @Override
    public int getCorpusSize() {
        synchronized (documentDB) {
            return documentMap.size();
        }
    }

    @Override
    public String addDocument(String url, String type) {
        synchronized (documentDB) {
            String id = URLParsing.getMD5(url);
            Document document = new Document(id, url, type);
            documentMap.put(id, document);
            return id;
        }
    }

    @Override
    public String getDocument(String url) {
        synchronized (urlDB) {
            synchronized (documentDB) {
                if (urlMap.get(url) == null) {
                    return "";
                }
                URL url2 = urlMap.get(url);
                Document document = documentMap.get(url2.getId());
                String res = null;
                if (document != null) {
                    res = document.getContent();
                }
                return res;
            }
        }
    }

    @Override
    public int addUser(String username, String password) {
        synchronized (userDB) {
            if (userMap.get(username) != null) {
                logger.error("User: {} already exists.", username);
                return 0;
            } else {
                logger.info("Adding new user: {}.", username);
                userMap.put(username, new User(username, password));
                return 1;
            }
        }
    }

    @Override
    public boolean getSessionForUser(String username, String password) {
        synchronized (userDB) {
            if (userMap.get(username) == null) {
                logger.error("User: {} does not exist.", username);
                return false;
            }
            User user = userMap.get(username);
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                password = new String(messageDigest.digest(password.getBytes()));
            } catch (NoSuchAlgorithmException | NullPointerException e) {
                logger.error("Exception when encrypting the password.");
            }
            return password.equals(user.getPassword());
        }
    }

    @Override
    public void close() {
        logger.info("Closing the storage databases.");

        try {
            catalogDB.close();
            logger.info("Closed catalogDB.");
        } catch (EnvironmentFailureException | IllegalStateException e) {
            logger.error("Exception when closing catalogDB.");
        }

        try {
            documentDB.close();
            logger.info("Closed documentDB.");
        } catch (EnvironmentFailureException | IllegalStateException e) {
            logger.error("Exception when closing documentDB.");
        }

        try {
            userDB.close();
            logger.info("Closed userDB.");
        } catch (EnvironmentFailureException | IllegalStateException e) {
            logger.error("Exception when closing userDB.");
        }

        try {
            urlDB.close();
            logger.info("Closed urlDB.");
        } catch (EnvironmentFailureException | IllegalStateException e) {
            logger.error("Exception when closing urlDB.");
        }
        try {
            channelDB.close();
            logger.info("Closed channelDB.");
        } catch (EnvironmentFailureException | IllegalStateException e) {
            logger.error("Exception when closing channelDB.");
        }
    }

    @Override
    public URL getUrl(URLInfo urlInfo) {
        synchronized (urlDB) {
            String url = URLParsing.getURL(urlInfo, true);
            logger.info("Getting URL: {}", url);
            return urlMap.getOrDefault(url, null);
        }
    }

    @Override
    public URL removeUrl(URLInfo urlInfo) {
        synchronized (urlDB) {
            String url = URLParsing.getURL(urlInfo, true);
            logger.info("Removing URL: {}", url);
            return urlMap.remove(url);
        }
    }

    @Override
    public boolean html(String url) {
        synchronized (documentDB) {
            synchronized (urlDB) {
                URL url2 = urlMap.get(url);
                if (url2 == null) {
                    return false;
                } else {
                    Document document = documentMap.get(url2.getId());
                    return document == null ? false : document.getType().equals("text/html");
                }
            }
        }
    }

    @Override
    public boolean hasDocument(String url) {
        synchronized (documentDB) {
            return documentMap.get(URLParsing.getMD5(url)) == null;
        }
    }

    @Override
    public void addUrl(URL url) {
        synchronized (urlDB) {
            logger.info("Adding URL: {}", url.getUrl());
            urlMap.put(url.getUrl(), url);
        }
    }

    @Override
    public void addUrl(String channelName, String url) {
        if (channelMap.get(channelName) == null) {
            logger.error("Channel: {} not exists.", channelName);
            return;
        }
        Channel channel = channelMap.get(channelName);
        channel.getUrls().add(url);
        channelMap.put(channelName, channel);
    }

    @Override
    public String getCrawledTime(String url) {
        URL url2 = urlMap.get(url);
        if (url2 == null) {
            url2 = new URL(url, url, 0);
        }
        String epochSecond = Instant.ofEpochMilli(url2.getTimestamp()).toString();
        return epochSecond.substring(0, epochSecond.length() - 1);
    }

    @Override
    public Document getDocumentModel(String url) {
        synchronized (urlDB) {
            synchronized (documentDB) {
                URL url2 = urlMap.get(url);
                if (url2 == null) {
                    return null;
                }
                return documentMap.get(url2.getId());
            }
        }
    }

    @Override
    public boolean addChannel(String name, String creator, String xpath) {
        synchronized (channelDB) {
            if (name == null || creator == null || xpath == null) {
                return false;
            }
            for (Channel channel : channelMap.values()) {
                if (channel.getName().equals(name)) {
                    logger.info("Channel {} already exists.", name);
                    return false;
                }
            }
            logger.info("Adding channel {}. Creator: {}. XPath: {}.", name, creator, xpath);
            Channel channel = new Channel(channelMap.size(), name, creator, xpath);
            channelMap.put(name, channel);
            return true;
        }
    }

    @Override
    public Channel getChannel(int id) {
        synchronized (channelMap) {
            for (Channel channel : channelMap.values()) {
                if (channel.getId() == id) {
                    return channel;
                }
            }
        }
        logger.info("Channel not found. ID: {}.", id);
        return null;
    }

    @Override
    public int getChannelId(String name) {
        for (String key : channelMap.keySet()) {
            if (key.equals(name)) {
                return channelMap.get(key).getId();
            }
        }
        return -1;
    }

    @Override
    public List<Channel> getChannelList() {
        List<Channel> channelList = new ArrayList<>();
        synchronized (channelMap) {
            channelList.addAll(channelMap.values());
        }
        return channelList;
    }

    @Override
    public Map<String, Channel> getChannelMap() {
        return channelMap;
    }

    @Override
    public String[] getXPath() {
        synchronized (channelMap) {
            String[] xpathArr = new String[channelMap.size()];
            for (Channel channel : channelMap.values()) {
                xpathArr[channel.getId()] = channel.getXpath();
            }
            return xpathArr.length == 0 ? null : xpathArr;
        }
    }
}
