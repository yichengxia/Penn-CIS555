package edu.upenn.cis.cis455.m1.server;

import java.net.Socket;

public class HttpTask {
	
    private String directory;
    private Socket socket;

    public HttpTask(String directory, Socket socket) {
    	this.directory = directory;
        this.socket = socket;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }
}
