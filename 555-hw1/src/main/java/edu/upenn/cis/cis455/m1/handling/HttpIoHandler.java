package edu.upenn.cis.cis455.m1.handling;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.SparkController;
import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.interfaces.RequestHandler;
import edu.upenn.cis.cis455.m1.server.HttpTask;
import edu.upenn.cis.cis455.m2.interfaces.Filter;
import edu.upenn.cis.cis455.m2.interfaces.Request;
import edu.upenn.cis.cis455.m2.interfaces.Response;

/**
 * Handles marshaling between HTTP Requests and Responses
 */
public class HttpIoHandler {

    final static Logger logger = LogManager.getLogger(HttpIoHandler.class);

    private HttpTask httpTask;
    private Socket socket;
    private Response response;
    private InetAddress inetAddress;
    private String remoteIp;
    private String uri;

    private Map<String, String> headers = new HashMap<>();
    private Map<String, List<String>> parms = new HashMap<>();

    public HttpIoHandler(HttpTask httpTask) {
        this.httpTask = httpTask;
        socket = this.httpTask.getSocket();
        response = new HttpResponse();
    }

    public void requestProcessor() throws IOException {
        try {
            processSocketInput();
        } catch (HaltException e) {
            logger.error("Halt Exception happened.");
            HaltException he = new HaltException(400);
            sendException(socket, new HttpRequest(), he);
            return;
        }
        Request request = HttpRequest.parseRequest(httpTask, headers, parms, uri);
        if (!request.protocol().substring(0, 4).equals("HTTP")) {
            HaltException he = new HaltException(400);
            sendException(socket, new HttpRequest(), he);
            return;
        }
        if (request.protocol().compareTo("HTTP/1.1") > 0) {
            HaltException he = new HaltException(505);
            sendException(socket, new HttpRequest(), he);
            return;
        }
        RequestHandler requestHandler = new HttpRequestHandler(request, response);
        try {
            requestHandler.handleRequest();
            sendResponse(socket, response);
        } catch (Exception e) {
            logger.error("Exception in processing requests.");
            HaltException he = new HaltException(404);
            sendException(socket, new HttpRequest(), he);
        }
    }

    private void processSocketInput() {
        Map<String, String> headersMap = new HashMap<>();
        Map<String, List<String>> parmsMap = new HashMap<>();
        inetAddress = socket.getInetAddress();
        remoteIp = inetAddress.toString() == null ? "" : inetAddress.toString();
        try {
            uri = HttpParsing.parseRequest(remoteIp, socket.getInputStream(), headersMap, parmsMap);
        } catch (IOException | HaltException e) {
            logger.error("Exception when parsing Http request with socket: {}.", socket.toString());
            HaltException tr = new HaltException(400, "400 Bad Request");
            throw tr;
        }
        headers.putAll(headersMap);
        parms.putAll(parmsMap);
    }

    /**
     * Sends an exception back, in the form of an HTTP response code and message.
     * Returns true if we are supposed to keep the connection open (for persistent
     * connections).
     * @throws IOException
     */
    public static boolean sendException(Socket socket, Request request, HaltException except) throws IOException {
        Response response = new HttpResponse();
        logger.info("Sending exception to Socket:{}, Exception:{}", socket.toString(), except.toString());
        socket.getOutputStream().write(response.responseException(except));
        return true;
    }

    /**
     * Sends data back. Returns true if we are supposed to keep the connection open
     * (for persistent connections).
     */
    public static boolean sendResponse(Socket socket, Request request, Response response) throws IOException {
        return sendResponse(socket, response);
    }

    public static boolean sendResponse(Socket socket, Response response) {
        try {
            sendResponse(socket, response.response());
        } catch (IOException e) {
            logger.error("IO Exception in sending response.");
            return false;
        }
        return true;
    }

    public static boolean sendResponse(Socket socket, byte[] bytes) throws IOException {
        socket.getOutputStream().write(bytes);
        return true;
    }

    public String getUri() {
        return uri;
    }

    public static void putHandler(Request request, Response response) throws Exception {}

    public static void deleteHandler(Request request, Response response, String uri) throws Exception {
        for (Filter filter : SparkController.getInstance().getBeforeFilters()) {
            filter.handle(request, response);
        }
        Path path = Paths.get("./www", uri);
        File file = path.toFile();
        if (!file.exists() || !file.isFile()) {
            logger.error("Halt Exception in finding file from path: {}", uri);
            byte[] controlPanel204 = ControlPanel.get204();
            response.bodyRaw(controlPanel204);
            response.addHeaders("Content-Length", String.valueOf(controlPanel204.length));
            response.type("text/html");
            response.status(204);
            throw new HaltException(204, "204 No Content");
        } else {
            file.delete();
            byte[] controlPanel = "<html><body><h1>File Deleted</h1></body></html>".getBytes();
            response.bodyRaw(controlPanel);
            response.addHeaders("Content-Length", String.valueOf(controlPanel.length));
            response.type("text/html");
            response.status(200);
        }
        for (Filter filter : SparkController.getInstance().getAfterFilters()) {
            filter.handle(request, response);
        }
    }

    public static void postHandler(Request request, Response response) throws Exception {}

    public static void optionsHandler(Request request, Response response) throws Exception {
        for (Filter filter : SparkController.getInstance().getBeforeFilters()) {
            filter.handle(request, response);
        }
        response.header("Allow","OPTIONS, GET, HEAD, POST");
        for (Filter filter : SparkController.getInstance().getAfterFilters()) {
            filter.handle(request, response);
        }
    }
}
