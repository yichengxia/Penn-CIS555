package edu.upenn.cis.cis555.util;

import java.io.File;
import java.math.BigInteger;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;

/**
 * This is the local database.
 */
public class StorageDB {

    private Environment environment;
    private Database database;
    private String name;
    private int size;
    private int count;

    public StorageDB(final String directory, final String name, final int size) {
        new File(directory).mkdirs();
        environmentConfig(directory);
        database = environment.openDatabase(null, name, databaseConfig());
        this.name = name;
        this.size = size;
        count = 0;
    }

    public StorageDB(final String name) {
        this("./StorageDB/", name, 2000);
    }

    /**
     * Configure database environment with directory
     * @param directory
     */
    private void environmentConfig(String directory) {
        final EnvironmentConfig environmentConfig = new EnvironmentConfig();
        environmentConfig.setTransactional(false);
        environmentConfig.setAllowCreate(true);
        environment = new Environment(new File(directory), environmentConfig);
    }

    /**
     * Configure database and return it
     * @return databaseConfig
     */
    private DatabaseConfig databaseConfig() {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setTransactional(false);
        databaseConfig.setAllowCreate(true);
        databaseConfig.setDeferredWrite(true);
        databaseConfig.setBtreeComparator(new KeyComparator());
        return databaseConfig;
    }

    /**
     * Offer a new element into the database
     * @param bytes
     */
    public synchronized void offer(final byte[] bytes) {
        Cursor cursor = database.openCursor(null, null);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        cursor.getLast(key, value, LockMode.RMW);
        BigInteger prevKeyValue;
        if (key.getData() == null) {
            prevKeyValue = BigInteger.valueOf(-1);
        } else {
            prevKeyValue = new BigInteger(key.getData());
        }
        BigInteger newKeyValue = prevKeyValue.add(BigInteger.ONE);
        final DatabaseEntry newKey = new DatabaseEntry(newKeyValue.toByteArray());
        final DatabaseEntry newValue = new DatabaseEntry(bytes);
        database.put(null, newKey, newValue);
        if (++count >= size) {
            database.sync();
            count = 0;
        }
        cursor.close();
    }

    /**
     * Poll an element from the database
     * @return res
     */
    public byte[] poll() {
        Cursor cursor = database.openCursor(null, null);
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        cursor.getFirst(key, value, LockMode.RMW);
        if (value.getData() == null) {
            return null;
        }
        byte[] res = value.getData();
        cursor.delete();
        if (++count >= size) {
            database.sync();
            count = 0;
        }
        cursor.close();
        return res;
    }

    public String getName() {
        return name;
    }

    public void sync() {
        database.sync();
        count = 0;
    }

    public long size() {
        return database.count();
    }

    /**
     * Close the database and environment
     */
    public void close() {
        database.sync();
        database.close();
        environment.close();
    }
}
