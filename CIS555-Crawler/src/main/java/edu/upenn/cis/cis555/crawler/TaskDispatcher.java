package edu.upenn.cis.cis555.crawler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upenn.cis.cis555.crawler.info.URLInfo;
import edu.upenn.cis.cis555.crawler.remote.Worker;
import edu.upenn.cis.cis555.util.Serializer;
import edu.upenn.cis.cis555.util.StorageDB;

/**
 * This is the dispatcher to assign threads for crawling.
 */
public class TaskDispatcher {

    private static Logger logger = LoggerFactory.getLogger(TaskDispatcher.class);

    private static final int QUEUE_SIZE = 20000;
    
    private List<CrawlerWorker> threads;
    private int threadCount;
    private Worker controller;
    private List<String> nodeList;
    private String nodeId;
    private List<StorageDB> storageDBs;
    private Thread dispatcherThread;

    private boolean continued = true;
    
    /**
     * Constructor of TaskDispatcher, periodically checking the tasks
     * @param crawler
     */
    public TaskDispatcher(Crawler crawler) {
        logger.info("Initializing dispatcher");
        initialize(crawler);
        logger.info("Starting dispatcher thread");
        dispatcherThread = new Thread(() -> {
            try {
                while (continued) {
                    for (int i = 0; i < threadCount; i++) {
                        CrawlerWorker worker = threads.get(i);
                        int capacity = QUEUE_SIZE - worker.getQueueSize();
                        if (capacity > 200) {
                            int count;
                            for (count = 0; count < capacity - 200; count++) {
                                byte[] bytes = storageDBs.get(i).poll();
                                if (bytes == null) {
                                    break;
                                }
                                worker.addToQueue((CrawlerTask) Serializer.toObject(bytes));
                            }
                            storageDBs.get(i).sync();
                            if (count > 0) {
                                logger.info(count + " tasks are moved from persistent queue to in-memory queue for thread " + i);
                            }
                        }
                        if (!continued) {
                            break;
                        }
                    }
                    Thread.sleep(5 * 1000);
                }
            } catch (InterruptedException e) {
                System.err.println("Failed to interrupt the thread");
            }
            logger.info("End dispatcher");

        }, "Dispatcher");
    }

    /**
     * Initialize the crawler parameters
     * @param crawler
     */
    private void initialize(Crawler crawler) {
        threads = crawler.getThreads();
        threadCount = crawler.getThreadCount();
        controller = crawler.getController();
        nodeList = controller.getNames();
        nodeId = crawler.getCrawlerIdentifier();
        storageDBs = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            storageDBs.add(new StorageDB("PS" + i));
        }
    }

    public synchronized void start() {
        dispatcherThread.start();
    }

    public void stop() {
        continued = false;
    }

    /**
     * Dispatch task to node and add it to local database
     * @param task
     */
    public void addTask(CrawlerTask task) {
        if (nodeList.size() > 0) {
            int assigned = getNodeId(task);
            if (!nodeId.equals(nodeList.get(assigned))) {
                addToRemote(assigned, task);
                return;
            }
        }
        int threadId = getThreadId(task);
        addToLocal(threadId, task);
    }

    /**
     * Helper method to get thread ID with hashed task
     * @param task
     * @return thread ID
     */
    private int getThreadId(CrawlerTask task) {
        int hash = task.getUrl().getHost().hashCode();
        return (hash % threadCount + threadCount) % threadCount;
    }

    /**
     * Helper method to get node ID with hashed task
     * @param task
     * @return node ID
     */
    private int getNodeId(CrawlerTask task) {
        int hash = task.getUrl().getHost().hashCode();
        int size = nodeList.size() == 0 ? 1 : nodeList.size();
        return hash % size;
    }

    /**
     * Get task from URL and add it to local database
     * @param url
     */
    public void getFromRemote(String url) {
        CrawlerTask task = new CrawlerTask(new URLInfo(url));
        addToLocal(getThreadId(task), task);
    }

    /**
     * Add task to remote worker
     * @param nodeId
     * @param task
     */
    private void addToRemote(int nodeId, CrawlerTask task) {
        List<String> arr = new ArrayList<String>();
        arr.add(task.getUrl().toString());
        controller.sendUrls(nodeId, arr);
    }

    /**
     * Add task to local database
     * @param threadId
     * @param task
     */
    private void addToLocal(int threadId, CrawlerTask task) {
        try {
            CrawlerWorker worker = threads.get(threadId);
            if (continued && worker.getQueueSize() < QUEUE_SIZE) {
                worker.addToQueue(task);
            } else {
                storageDBs.get(threadId).offer(Serializer.toBinary(task));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save the task progress in local database
     */
    public void saveProgress() {
        for (int i = 0; i < threadCount; i++) {
            int count = 0;
            CrawlerWorker worker = threads.get(i);
            StorageDB db = storageDBs.get(i);
            Iterator<CrawlerTask> iterator = worker.getTaskIterator();
            while (iterator.hasNext()) {
                CrawlerTask task = iterator.next();
                db.offer(Serializer.toBinary(task));
                count++;
                iterator.remove();
            }
            db.sync();
            logger.info(count + " tasks have been saved to persistent queue for thread " + i);
        }
        storageDBs.forEach(e -> e.close());
    }
}
