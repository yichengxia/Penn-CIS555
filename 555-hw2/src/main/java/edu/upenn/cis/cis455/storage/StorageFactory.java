package edu.upenn.cis.cis455.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StorageFactory {

    Logger logger = LogManager.getLogger(StorageFactory.class);

    static volatile StorageInterface singletonDB = null;

    public static StorageInterface getDatabaseInstance(String directory) {
        if (singletonDB == null) {
            synchronized (StorageFactory.class) {
                if (singletonDB == null) {
                    singletonDB = new StorageDB(directory);
                }
            }
        }
        return singletonDB;
    }
}
