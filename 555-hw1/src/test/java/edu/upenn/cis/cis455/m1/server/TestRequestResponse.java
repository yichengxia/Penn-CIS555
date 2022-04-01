package edu.upenn.cis.cis455.m1.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.junit.Assert.*;
import org.junit.Test;

import edu.upenn.cis.cis455.TestHelper;
import edu.upenn.cis.cis455.m1.handling.HttpIoHandler;

public class TestRequestResponse {
    
    @Test
    public void test200Response() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String response = "HTTP/1.1 200 OK\r\nContent-Length: 0";
        Socket socket = TestHelper.getMockSocket(response, baos);
        HttpIoHandler.sendResponse(socket, response.getBytes());
        String res = baos.toString().replace("\r", "");
        System.out.println(res);
        assertTrue(res.startsWith("HTTP/1.1 200 OK"));
    }

    @Test
    public void test404Response() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0";
        Socket socket = TestHelper.getMockSocket(response, baos);
        HttpIoHandler.sendResponse(socket, response.getBytes());
        String res = baos.toString().replace("\r", "");
        System.out.println(res);
        assertTrue(res.startsWith("HTTP/1.1 404"));
    }
}
