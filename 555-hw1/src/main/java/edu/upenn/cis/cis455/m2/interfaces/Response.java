package edu.upenn.cis.cis455.m2.interfaces;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.upenn.cis.cis455.m2.handling.HttpCookie;

public abstract class Response extends edu.upenn.cis.cis455.m1.interfaces.Response {

    protected int statusCode = 200;
    protected byte[] body;
    protected String contentType = null;
    protected String requestMethod = "GET";
    protected String protocol = "HTTP/1.1";
    protected Map<String, String> headers = new HashMap<>();

    protected String headerString;
    protected String htmlString;
    public String redirectedPath;
    protected Set<HttpCookie> cookies = new HashSet<>();
    
    /**
     * Add a header key/value
     */
    public abstract void header(String header, String value);

    /**
     * Trigger an HTTP redirect to a new location
     */
    public abstract void redirect(String location);

    /**
     * Trigger a redirect with a specific HTTP 3xx status code
     */
    public abstract void redirect(String location, int httpStatusCode);

    public abstract void cookie(String name, String value);

    public abstract void cookie(String name, String value, int maxAge);

    public abstract void cookie(String name, String value, int maxAge, boolean secured);

    public abstract void cookie(String name, String value, int maxAge, boolean secured, boolean httpOnly);

    public abstract void cookie(String path, String name, String value);

    public abstract void cookie(String path, String name, String value, int maxAge);

    public abstract void cookie(String path, String name, String value, int maxAge, boolean secured);

    public abstract void cookie(String path, String name, String value, int maxAge, boolean secured, boolean httpOnly);

    public abstract void removeCookie(String name);

    public abstract void removeCookie(String path, String name);

    public abstract void setCookiesToHeader();
}
