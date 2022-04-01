/**
 * CIS 455/555 route-based HTTP framework.
 * 
 * Basic service handler and route manager.
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

package edu.upenn.cis.cis455;

import java.util.Map;

import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m2.interfaces.Route;
import edu.upenn.cis.cis455.m2.interfaces.Filter;
import edu.upenn.cis.cis455.m2.interfaces.Session;

// change to to edu.upenn.cis.cis455.m2 for m2
import edu.upenn.cis.cis455.m2.server.WebService;

public class SparkController {

    private static WebService webService;

    // We don't want people to use the constructor
    protected SparkController() {}

    public static WebService getInstance() {
        if (webService == null) {
            webService = WebService.getInstance();
        }
        return webService;
    }

    public static void config(Map<String, String> propertyMap) {
        port(Integer.parseInt(propertyMap.get("port")));
        staticFileLocation(propertyMap.get("directory"));
    }

    /**
     * Milestone 2 only: Handle an HTTP GET request to the path
     */
    public static void get(String path, Route route) {
        webService.get(path, route);
    }

    /**
     * Milestone 2 only: Handle an HTTP POST request to the path
     */
    public static void post(String path, Route route) {
        webService.post(path, route);
    }

    /**
     * Milestone 2 only: Handle an HTTP PUT request to the path
     */
    public static void put(String path, Route route) {
        webService.put(path, route);
    }

    /**
     * Milestone 2 only: Handle an HTTP DELETE request to the path
     */
    public static void delete(String path, Route route) {
        webService.put(path, route);
    }

    /**
     * Milestone 2 only: Handle an HTTP HEAD request to the path
     */
    public static void head(String path, Route route) {
        webService.head(path, route);
    }

    /**
     * Milestone 2 only: Handle an HTTP OPTIONS request to the path
     */
    public static void options(String path, Route route) {
        webService.options(path, route);
    }

    ///////////////////////////////////////////////////
    // HTTP request filtering
    ///////////////////////////////////////////////////

    /**
     * Milestone 2 only: Add filters that get called before a request
     */
    public static void before(Filter... filters) {
        for (Filter filter : filters) {
            webService.before(filter);
        }
    }

    /**
     * Milestone 2 only: Add filters that get called after a request
     */
    public static void after(Filter... filters) {
        for (Filter filter : filters) {
            webService.after(filter);
        }
    }

    /**
     * Milestone 2 only: Add filters that get called before a request
     */
    public static void before(String path, Filter... filters) {
        for (Filter filter : filters){
            webService.before(path, filter);
        }
    }

    /**
     * Milestone 2 only: Add filters that get called after a request
     */
    public static void after(String path, Filter... filters) {
        for (Filter filter : filters){
            webService.after(path, filter);
        }
    }
    
    
    // The following 2 functions are OPTIONAL for Milestone 1.
    // They will be used in Milestone 2 so user code can fail
    // and we will gracefully return something.

    /**
     * Triggers a HaltException that terminates the request
     */
    public static HaltException halt() {
        return webService.halt();
    }

    /**
     * Triggers a HaltException that terminates the request
     */
    public static HaltException halt(int statusCode, String body) {
        return webService.halt(statusCode, body);
    }

    ////////////////////////////////////////////
    // Server configuration
    ////////////////////////////////////////////

    /**
     * Set the IP address to listen on (default 0.0.0.0)
     */
    public static void ipAddress(String ipAddress) {
        getInstance().ipAddress(ipAddress);
    }

    /**
     * Set the port to listen on (default 45555)
     */
    public static void port(int port) {
        getInstance().port(port);
    }

    /**
     * Set the size of the thread pool
     */
    public static void threadPool(int threads) {
        getInstance().threadPool(threads);
    }

    /**
     * Set the root directory of the "static web" files
     */
    public static void staticFileLocation(String directory) {
        getInstance().staticFileLocation(directory);
    }

    /**
     * Hold until the server is fully initialized
     */
    public static void awaitInitialization() {
        getInstance().awaitInitialization();
    }

    /**
     * Gracefully shut down the server
     */
    public static void stop() {
        getInstance().stop();
    }

    public static String createSession() {
        return webService.createSession();
    }

    public static Session getSession(String id) {
        return webService.getSession(id);
    }
}
