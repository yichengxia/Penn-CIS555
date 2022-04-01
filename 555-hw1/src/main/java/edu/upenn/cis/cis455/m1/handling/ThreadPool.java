package edu.upenn.cis.cis455.m1.handling;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m1.server.HttpTaskQueue;
import edu.upenn.cis.cis455.m1.server.HttpWorker;

public class ThreadPool {
    
    static final Logger logger = LogManager.getLogger(ThreadPool.class);

    private int maxSize = 15;
    private List<Thread> threadPool;
    private List<HttpWorker> httpWorkers;
    private HttpTaskQueue httpTaskQueue;

    public ThreadPool() {
        threadPool = new ArrayList<>();
        httpWorkers = new ArrayList<>();
        httpTaskQueue = new HttpTaskQueue();
        while (threadPool.size() < maxSize) {
            HttpWorker httpWorker = new HttpWorker(httpTaskQueue);
            Thread thread = new Thread(httpWorker);
            thread.setName("Worker No." + threadPool.size());
            logger.info("{} starts.", thread.getName());
            httpWorker.setThreadID(thread.getName());
            httpWorkers.add(httpWorker);
            threadPool.add(thread);
            thread.start();
        }
    }

    public List<Thread> getThreadPool() {
        return threadPool;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getSize() {
        return threadPool.size();
    }

    public List<HttpWorker> getHttpWorkers() {
        return httpWorkers;
    }

    public HttpTaskQueue getHttpTaskQueue() {
        return httpTaskQueue;
    }

    public void setHttpTaskQueue(HttpTaskQueue httpTaskQueue) {
        this.httpTaskQueue = httpTaskQueue;
    }
}
