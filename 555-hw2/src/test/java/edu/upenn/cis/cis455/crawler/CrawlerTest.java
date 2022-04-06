package edu.upenn.cis.cis455.crawler;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class CrawlerTest {
    
    private String path = "./test";
    private String url = "https://crawltest.cis.upenn.edu/bbc/frontpage.xml";
    private StorageInterface db = null;
    private Crawler crawler = null;

    @Before
    public void setup(){
        if (!Files.exists(Paths.get(path))) {
            try {
                Files.createDirectory(Paths.get(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void crawlerTest1() {
        db = StorageFactory.getDatabaseInstance(path);
        crawler = new Crawler(url, db, 1048576, 100);
        crawler.start();
        assertEquals(1, db.getCorpusSize());
    }

    @Test
    public void crawlerTest2() {
        path = "https://crawltest.cis.upenn.edu/cnn/cnn_us.rss.xml";
        db = StorageFactory.getDatabaseInstance(path);
        crawler = new Crawler(url, db, 0, 100);
        crawler.start();
        assertEquals(1, db.getCorpusSize());
    }
}
