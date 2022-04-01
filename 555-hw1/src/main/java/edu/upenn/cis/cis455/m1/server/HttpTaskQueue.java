package edu.upenn.cis.cis455.m1.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Stub class for implementing the queue of HttpTasks
 */
public class HttpTaskQueue {

    static final Logger logger = LogManager.getLogger(HttpTaskQueue.class);

    private Queue<HttpTask> queue;

    public HttpTaskQueue() {
        queue = new ArrayDeque<>();
        Thread heartbeatThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    logger.info("HTTP task queue has {} task(s) now.", queue.size());
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        // finish heartbeat execution
                        logger.error("Interrupted Exception happened.");
                    }
                }
            }
        });
        heartbeatThread.start();
    }

    public void offer(HttpTask httpTask) throws InterruptedException {
        while (true) {
            synchronized (queue) {
                if (queue.size() >= 100) {
                    queue.wait();
                } else {
                    queue.offer(httpTask);
                    queue.notifyAll();
                    break;
                }
            }
        }
    }

    public HttpTask poll() throws InterruptedException {
        while (true) {
            synchronized (queue) {
                if (queue.isEmpty()) {
                    queue.wait();
                } else {
                    HttpTask tempTask = queue.poll();
                    queue.notifyAll();
                    return tempTask;
                }
            }
        }
    }

    public int getSize() {
        return queue.size();
    }
}
