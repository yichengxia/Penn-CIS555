package edu.upenn.cis.cis455.m1.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Stub for your HTTP server, which listens on a ServerSocket and handles
 * requests
 */
public class HttpListener implements Runnable {
	
	static final Logger logger = LogManager.getLogger(HttpListener.class);
	
	private String directory;
    private ServerSocket serverSocket;
    private HttpTaskQueue httpTaskQueue;
    
    public HttpListener(int port, String directory, HttpTaskQueue httpTaskQueue) {
        this.directory = directory;
        this.httpTaskQueue = httpTaskQueue;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            logger.error("IO Exception in binding port to HTTP listener.");
            System.exit(0);
        }
        logger.info("HTTP listener set to port {}, with directory in \"{}\".", port, directory);
    }

    @Override
    public void run() {
    	while (true) {
            try {
                Thread.sleep(0);
                Socket socket = serverSocket.accept();
                logger.info("Detected new socket at: {}.", socket.toString());
                HttpTask httpTask = new HttpTask(directory, socket);
                httpTaskQueue.offer(httpTask);
                logger.info("New task offered to task queue: {}", httpTask.toString());
            } catch (IOException e) {
                logger.error("IO Exception in fetching information from socket.");
            } catch (InterruptedException e) {
                logger.error("Interrupted Exception in HTTP listener thread.");
            }
        }
    }
}
