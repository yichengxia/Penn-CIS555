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
package edu.upenn.cis.cis455.m1.interfaces;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Initial (simplified) request API, for Milestone 1
 */
public abstract class Request {
    /**
     * Indicates we have a persistent HTTP 1.1 connection
     */
    boolean persistent = false;

    protected String requestMethod;
    protected String host;
    protected String userAgent;
    protected int port;
    protected String pathInfo;
    protected String url;
    protected String uri;
    protected String protocol = "HTTP/1.1";
    protected String contentType;
    protected String ip;
    protected String body;
    protected int contentLength;
    protected Map<String, String> headers;
    protected Map<String, List<String>> queryParams;

    /**
     * The request method (GET, POST, ...)
     */
    public abstract String requestMethod();

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    /**
     * @return The host
     */
    public abstract String host();

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return The user-agent
     */
    public abstract String userAgent();

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * @return The server port
     */
    public abstract int port();

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return The path
     */
    public abstract String pathInfo();

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    /**
     * @return The URL
     */
    public abstract String url();

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return The URI up to the query string
     */
    public abstract String uri();

    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return The protocol name and version from the request
     */
    public abstract String protocol();

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return The MIME type of the body
     */
    public abstract String contentType();

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return The client's IP address
     */
    public abstract String ip();

    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * @return The request body sent by the client
     */
    public abstract String body();

    public void setBody(String body) {
        this.body = body;
    }

    /**
     * @return The length of the body
     */
    public abstract int contentLength();

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    /**
     * @return Get the item from the header
     */
    public abstract String headers(String name);

    public abstract Set<String> headers();

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Indicates we have a persistent HTTP 1.1 connection
     */
    public boolean persistentConnection() {
        return persistent;
    }

    /**
     * Sets whether we have a persistent HTTP 1.1 connection
     */
    public void persistentConnection(boolean persistent) {
        this.persistent = persistent;
    }
}
