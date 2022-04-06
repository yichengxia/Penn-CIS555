package edu.upenn.cis.cis455.storage;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import edu.upenn.cis.cis455.model.URL;

public class StorageTest {
    
    private String path = "./test_storage";
    private StorageInterface db;

    @Before
    public void setup(){
        if (!Files.exists(Paths.get(path))) {
            try {
                Files.createDirectory(Paths.get(path));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        db = StorageFactory.getDatabaseInstance(path);
    }

    @Test
    public void storageTest1(){
        String key = db.addDocument("xyc", "xyc");
        db.addUrl(new URL(key, "url", 0));
        assertEquals(db.getDocument("url"), "xyc");
    }

    @Test
    public void storageTest2(){
        assertEquals(2, db.getCorpusSize());
    }
}
