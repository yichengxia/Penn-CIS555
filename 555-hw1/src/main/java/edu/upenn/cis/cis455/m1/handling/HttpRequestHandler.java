package edu.upenn.cis.cis455.m1.handling;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m1.interfaces.RequestHandler;
import edu.upenn.cis.cis455.m2.handling.HttpMatcher;
import edu.upenn.cis.cis455.m2.interfaces.Filter;
import edu.upenn.cis.cis455.m2.interfaces.Request;
import edu.upenn.cis.cis455.m2.interfaces.Response;
import edu.upenn.cis.cis455.m2.interfaces.Route;
import edu.upenn.cis.cis455.m2.server.WebService;

public class HttpRequestHandler implements RequestHandler {

    static final Logger logger = LogManager.getLogger(HttpRequestHandler.class);

    private Request request;
    private Response response;

    public HttpRequestHandler(Request request, Response response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public void handleRequest() throws Exception {
        boolean isHead = request.requestMethod().equals("HEAD");
        String uri = request.uri();
        if (uri.split("45555").length > 0) {
            try {
                uri = uri.split("45555")[uri.split("45555").length - 1];
            } catch (Exception e) {}
        }
        if (isHead || request.requestMethod().equals("GET")) {
            try {
                switch (uri) {
                    case "/control":
                        logger.debug("Prepare for controlling...");
                        byte[] controlPanel = ControlPanel.getControlPanel();
                        if (!isHead) {
                            response.bodyRaw(controlPanel);
                        }
                        response.addHeaders("Content-Length", String.valueOf(controlPanel.length));
                        response.type("text/html");
                        break;
                    case "/shutdown":
                        logger.debug("About to shutdown...");
                        WebService.getInstance().stop();
                        break;
                    default:
                        HttpMatcher httpMatcher = new HttpMatcher();
                        setFilters(httpMatcher, true);
                        Route route = httpMatcher.routeMatcher(request, response);
                        if (route != null) {
                            Object object = route.handle(request, response);
                            if (object != null) {
                                setFilters(httpMatcher, false);
                                response.body(object.toString());
                            }
                        } else {
                            getStaticFile(uri);
                            setFilters(httpMatcher, false);
                        }
                        response.setCookiesToHeader();
                        response.cookie("JSESSIONID", request.getSessionId());
                }
            } catch (HaltException e) {
                logger.error("Halt Exception in processing the request.");
            }
        } else {
            String method = request.requestMethod();
            if (method.equals("PUT")) {
                HttpIoHandler.putHandler(request, response);
                throw new HaltException(400, "No content for PUT request");
            } else if (method.equals("DELETE")) {
                HttpIoHandler.deleteHandler(request, response, uri);
            } else if (method.equals("POST")) {
                HttpIoHandler.postHandler(request, response);
                throw new HaltException(400, "No content for POST request");
            } else if (method.equals("OPTIONS")) {
                HttpIoHandler.optionsHandler(request, response);
            } else if (method.equals("PATCH")) {
                response.status(501);
            } else {
                response.status(405);
            }
        }
    }

    public void setFilters(HttpMatcher httpMatcher, boolean isbefore){
        List<Filter> filters = httpMatcher.filterMatcher(request, isbefore);
        for (Filter filter : filters) {
            try {
                filter.handle(request, response);
            } catch (Exception e) {
                logger.error("Exception in handling filter.");
            }
        }
    }

    public void getStaticFile(String uri){
        Path path = Paths.get("./www" + uri);
        logger.info("Getting uri from: {}", path.toString());
        String type ="text/html";
        try {
            type = Files.probeContentType(path);
            logger.info("type changed");
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info(type);
        if (!Files.exists(path)) {
            response.status(404);
        } else {
            logger.info("file found, read the file");
            StringBuilder tmp = new StringBuilder();
            byte[] body = null;
            try {
                body = Files.readAllBytes(path);
            } catch (IOException e) {
                logger.debug("THE FILE IS NOT FOUND");
            }
            tmp.append("200 OK\r\n");
            tmp.append("Content-Type: ");
            tmp.append(type).append("\r\n");
            tmp.append("Content-Length: ").append(body.length).append("\r\n");
            tmp.append("\r\n");
            byte[] header = tmp.toString().getBytes();
            body = addByteArray(header, body);
            response.bodyRaw(body);
        }
    }

    public static byte[] addByteArray(byte[] a, byte[] b){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        try {
            outputStream.write(a);
            outputStream.write(b);
        } catch (IOException e) {
            logger.warn("append byte array fail");
        }
        byte c[] = outputStream.toByteArray( );
        return c;
    }
}
