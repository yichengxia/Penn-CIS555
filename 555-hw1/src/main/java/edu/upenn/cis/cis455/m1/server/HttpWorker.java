package edu.upenn.cis.cis455.m1.server;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.handling.HttpIoHandler;

/**
 * Stub class for a thread worker that handles Web requests
 */
public class HttpWorker implements Runnable {
	
	static final Logger logger = LogManager.getLogger(HttpWorker.class);

    private HttpTask httpTask;
    private HttpTaskQueue httpTaskQueue;
    private boolean status;
    private String threadID;
    private HttpIoHandler httpIoHandler;

    public HttpWorker(HttpTaskQueue httpTaskQueue) {
        this.httpTaskQueue = httpTaskQueue;
    }

    @Override
    public void run() {
        while (true) {
            status = true;
            try {
                httpTask = httpTaskQueue.poll();
                logger.info("HTTP task got from queue.");
                httpIoHandler = new HttpIoHandler(httpTask);
                httpIoHandler.requestProcessor();
                httpTask.getSocket().close();
                status = false;
                httpIoHandler = null;
            } catch (InterruptedException e) {
                logger.error("Interrupted Exception in polling tasks from task queue.");
            } catch (HaltException e) {
                logger.error("Halt Exception happened.");
            } catch (IOException e) {
                logger.error("IO Exception happened.");
            }
        }
    }

    public HttpTask getHttpTask() {
        return httpTask;
    }

    public void setHttpTask(HttpTask httpTask) {
        this.httpTask = httpTask;
    }

    public HttpTaskQueue getHttpTaskQueue() {
        return httpTaskQueue;
    }

    public void setHttpTaskQueue(HttpTaskQueue httpTaskQueue) {
        this.httpTaskQueue = httpTaskQueue;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public String getThreadID() {
        return threadID;
    }

    public void setThreadID(String threadID) {
        this.threadID = threadID;
    }

    public HttpIoHandler getHttpIoHandler() {
        return httpIoHandler;
    }
}
