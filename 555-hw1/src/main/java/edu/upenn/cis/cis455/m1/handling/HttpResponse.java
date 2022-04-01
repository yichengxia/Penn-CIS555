package edu.upenn.cis.cis455.m1.handling;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m2.handling.HttpCookie;
import edu.upenn.cis.cis455.m2.interfaces.Response;
import edu.upenn.cis.cis455.m2.server.WebService;

public class HttpResponse extends Response {

    static final Logger logger = LogManager.getLogger(HttpResponse.class);

    @Override
    public String getHeaders() {
        StringBuffer sb = new StringBuffer();
        headers.entrySet().forEach(e -> sb.append(String.format("%s: %s%s", e.getKey(), e.getValue(), "\r\n")));
        for (HttpCookie cookie : cookies) {
			sb.append(String.format("Set-Cookie: %s\r\n", cookie.toString()));
		}
        return sb.toString();
    }

    public Map<String, String> getHeaderMap() {
        return headers;
    }

    public String getStatus() {
        String statusString = statusMap().get(this.status());
        return String.format("%s %s%s", protocol, statusString, "\r\n");
    }

    private Map<Integer, String> statusMap(){
        Map<Integer, String> map = new HashMap<>();
        map.put(200, "200 OK");
        map.put(204, "204 No Content");
        map.put(304, "304 Not Modified");
        map.put(400, "400 Bad Request");
        map.put(404, "404 Not Found");
        map.put(405, "405 Method Not Allowed");
        map.put(412, "412 Precondition Failed");
        map.put(500, "500 Server Error");
        map.put(501, "501 Not Implemented");
        map.put(505, "505 HTTP Version Not Supported");
        return map;
    }

    @Override
    public byte[] response() {
        StringBuffer sb = new StringBuffer();
        String statusString = getStatus();
        String headers = getHeaders();
        sb.append(statusString);
        sb.append(headers.length() == 0 ? "\r\n" : headers + "\r\n");
        byte[] temp = sb.toString().getBytes();
        if (getRequestMethod().equals("HEAD")) {
            return temp;
        }
        byte[] body = this.bodyRaw() == null ? new byte[0] : this.bodyRaw();
        List<byte[]> vars = Arrays.asList(temp, body, "\r\n".getBytes());
        byte[] response = buildByteArray(vars);
        return response;
    }

    private byte[] buildByteArray(List<byte[]> vars) {
        int count = 0;
        for (byte[] var : vars) {
            count += var.length;
        }
        byte[] response = new byte[count];
        count = 0;
        for (byte[] var : vars) {
            System.arraycopy(var, 0, response, count, var.length);
            count += var.length;
        }
        return response;
    }

    @Override
    public byte[] responseException(HaltException exception) {
        String responseString = String.format("%s %s%s", protocol, statusMap().get(exception.statusCode()), "\r\n");
        this.addHeaders("Content-Length", String.valueOf(0));
        StringBuffer sb = new StringBuffer(responseString);
        sb.append(getHeaders() + "\r\n");
        return sb.toString().getBytes();
    }

    @Override
    public void header(String header, String value) {
        headers.put(header, value);
    }

    @Override
    public void redirect(String location) {
        redirect(location, 302);
    }

    @Override
    public void redirect(String location, int httpStatusCode) {
        redirectedPath = location;
        statusCode = httpStatusCode;
        header("location", location);
    }

    @Override
    public void cookie(String name, String value) {
        cookie(name, value, -1);
    }

    @Override
    public void cookie(String name, String value, int maxAge) {
        cookie(name, value, maxAge, false);
    }

    @Override
    public void cookie(String name, String value, int maxAge, boolean secured) {
        cookie(name, value, maxAge, secured, false);
    }

    @Override
    public void cookie(String name, String value, int maxAge, boolean secured, boolean httpOnly) {
        cookie(null, name, value, maxAge, secured, httpOnly);
    }

    @Override
    public void cookie(String path, String name, String value) {
        cookie(path, name, value, -1);
    }

    @Override
    public void cookie(String path, String name, String value, int maxAge) {
        cookie(path, name, value, maxAge, false);
    }

    @Override
    public void cookie(String path, String name, String value, int maxAge, boolean secured) {
        cookie(path, name, value, maxAge, secured, false);
    }

    @Override
    public void cookie(String path, String name, String value, int maxAge, boolean secured, boolean httpOnly) {
        cookies.add(new HttpCookie(path, name, value, maxAge, secured, httpOnly));
    }

    @Override
    public void removeCookie(String name) {
        removeCookie(null, name);
    }

    @Override
    public void removeCookie(String path, String name) {
        cookies.remove(new HttpCookie(path, name, "", -1, false, false));
    }

    public void setCookiesToHeader(){
        for (HttpCookie cookie : cookies){
            StringBuffer sb = new StringBuffer();
            sb.append(String.format("%s=%s;", cookie.getName(), cookie.getValue()));
            if (!cookie.getSecured()) {
                sb.append(String.format("%s=%s;", "HttpOnly", cookie.getHttpOnly()));
                sb.append(String.format("%s=%s;", "Path", cookie.getPath()));
                sb.append(String.format("%s=%s;", "Max-Age", cookie.getMaxAge()));
                sb.append(String.format("%s=localhost:%s;", "Domain", WebService.getInstance().getPort()));
            }
            headers.put("Set-Cookie", sb.toString());
        }
    }

    public Map<String, String> getCookieHeaders() {
        return headers;
    }

    public void setCookieHeaders(Map<String, String> cookieHeaders) {
        headers = cookieHeaders;
    }
}
