/**
 * CIS 455/555 route-based HTTP framework
 * 
 * V. Liu, Z. Ives
 * 
 * Portions excerpted from or inspired by Spark Framework, 
 * 
 *                 http://sparkjava.com,
 * 
 * with license notice included below.
 */

/*
 * Copyright 2011- Per Wendel
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.upenn.cis.cis455.m1.server;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.handling.ThreadPool;

public class WebService {

    static final Logger logger = LogManager.getLogger(WebService.class);
    
    public String directory = "./www";
    public String ipAddress = "0.0.0.0";
    public int port = 45555;
    public int threads = 10;

    private static WebService webService;

    private HttpListener httpListener;
    private HttpTaskQueue httpTaskQueue;
    private Thread listenerThread;
    private ThreadPool threadPool;

    public static WebService getInstance() {
        if (webService == null) {
            webService = new WebService();
        }
        return webService;
    }

    /**
     * Launches the Web server thread pool and the listener
     */
    public void start() {
        threadPool = new ThreadPool();
        httpTaskQueue = threadPool.getHttpTaskQueue();
        httpListener = new HttpListener(port, directory, httpTaskQueue);
        listenerThread = new Thread(httpListener);
        listenerThread.start();
    }

    /**
     * Gracefully shut down the server
     */
    public void stop() {
        logger.debug("Stopping HTTP listener...");
        listenerThread.interrupt();
        logger.info("Successfully stopped HTTP listener.");
        logger.info("Stopping HTTP worker...");
        try {
            while (httpTaskQueue.getSize() > 0) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted Exception in shutting down web service.");
            System.exit(0);
        }
        logger.info("Successfully stopped HTTP worker.");
        System.exit(0);
    }

    /**
     * Hold until the server is fully initialized.
     * Should be called after everything else.
     */
    public void awaitInitialization() {
        logger.info("Initializing server...");
        start();
    }

    /**
     * Triggers a HaltException that terminates the request
     */
    public HaltException halt() {
        throw new HaltException();
    }

    /**
     * Triggers a HaltException that terminates the request
     */
    public HaltException halt(int statusCode) {
        throw new HaltException(statusCode);
    }

    /**
     * Triggers a HaltException that terminates the request
     */
    public HaltException halt(String body) {
        throw new HaltException(body);
    }

    /**
     * Triggers a HaltException that terminates the request
     */
    public HaltException halt(int statusCode, String body) {
        throw new HaltException(statusCode, body);
    }

    ////////////////////////////////////////////
    // Server configuration
    //////////////////////////f//////////////////

    /**
     * Set the root directory of the "static web" files
     */
    public void staticFileLocation(String directory) {
    	this.directory = directory;
    }

    public String getStaticFileLocation() {
    	return directory;
    }

    /**
     * Set the IP address to listen on (default 0.0.0.0)
     */
    public void ipAddress(String ipAddress) {
    	this.ipAddress = ipAddress;
    }

    /**
     * Set the TCP port to listen on (default 45555)
     */
    public void port(int port) {
    	this.port = port;
    }

    /**
     * Set the size of the thread pool
     */
    public void threadPool(int threads) {
    	this.threads = threads;
    }

    public List<HttpWorker> getThreadWorkers() {
        return threadPool.getHttpWorkers();
    }
}
