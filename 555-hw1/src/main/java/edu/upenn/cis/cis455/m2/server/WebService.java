/**
 * CIS 455/555 route-based HTTP framework
 * 
 * Z. Ives, 8/2017
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
package edu.upenn.cis.cis455.m2.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m2.interfaces.Route;
import edu.upenn.cis.cis455.m2.interfaces.Session;
import edu.upenn.cis.cis455.m2.handling.HttpFilterMap;
import edu.upenn.cis.cis455.m2.handling.HttpHelper;
import edu.upenn.cis.cis455.m2.handling.HttpRouteMap;
import edu.upenn.cis.cis455.m2.interfaces.Filter;

public class WebService extends edu.upenn.cis.cis455.m1.server.WebService {

    final static Logger logger = LogManager.getLogger(WebService.class);

    private static WebService webService;

    private HttpRouteMap getMap;
    private HttpRouteMap postMap;
    private HttpRouteMap putMap;
    private HttpRouteMap deleteMap;
    private HttpRouteMap headMap;
    private HttpRouteMap optionsMap;

    private List<Filter> beforeFilters;
	private List<Filter> afterFilters;

    private HttpFilterMap beforeMap;
    private HttpFilterMap afterMap;

    public WebService() {
        super();
        initializeAll();
    }

    public static WebService getInstance(){
        if (webService == null) {
            webService = new WebService();
        }
        return webService;
    }

    private void initializeAll(){
        getMap = new HttpRouteMap();
        postMap = new HttpRouteMap();
        putMap = new HttpRouteMap();
        deleteMap = new HttpRouteMap();
        headMap = new HttpRouteMap();
        optionsMap = new HttpRouteMap();

        beforeFilters =  new ArrayList<>();
        afterFilters = new ArrayList<>();

        beforeMap = new HttpFilterMap();
        afterMap = new HttpFilterMap();
    }

    ///////////////////////////////////////////////////
    // For more advanced capabilities
    ///////////////////////////////////////////////////

    /**
     * Handle an HTTP GET request to the path
     */
    public void get(String path, Route route) {
        getMap.bindMap(path, route);
    }

    /**
     * Handle an HTTP POST request to the path
     */
    public void post(String path, Route route) {
        postMap.bindMap(path, route);
    }

    /**
     * Handle an HTTP PUT request to the path
     */
    public void put(String path, Route route) {
        putMap.bindMap(path, route);
    }

    /**
     * Handle an HTTP DELETE request to the path
     */
    public void delete(String path, Route route) {
        deleteMap.bindMap(path, route);
    }

    /**
     * Handle an HTTP HEAD request to the path
     */
    public void head(String path, Route route) {
        headMap.bindMap(path, route);
    }

    /**
     * Handle an HTTP OPTIONS request to the path
     */
    public void options(String path, Route route) {
        optionsMap.bindMap(path, route);
    }

    public HttpRouteMap getGetMap() {
        return getMap;
    }

    public HttpRouteMap getPostMap() {
        return postMap;
    }

    public HttpRouteMap getPutMap() {
        return putMap;
    }

    public HttpRouteMap getDeleteMap() {
        return deleteMap;
    }

    public HttpRouteMap getHeadMap() {
        return headMap;
    }

    public HttpRouteMap getOptionsMap() {
        return optionsMap;
    }

    ///////////////////////////////////////////////////
    // HTTP request filtering
    ///////////////////////////////////////////////////

    /**
     * Add filters that get called before a request
     */
    public void before(Filter filter) {
        beforeMap.bindMap(filter);
    }

    /**
     * Add filters that get called after a request
     */
    public void after(Filter filter) {
        afterMap.bindMap(filter);
    }

    /**
     * Add filters that get called before a request
     */
    public void before(String path, Filter filter) {
        beforeMap.bindMap(path, filter);
    }

    /**
     * Add filters that get called after a request
     */
    public void after(String path, Filter filter) {
        afterMap.bindMap(path, filter);
    }

    public List<Filter> getBeforeFilters() {
        return beforeFilters;
    }

    public List<Filter> getAfterFilters() {
        return afterFilters;
    }

    public HttpFilterMap getBeforeMap() {
        return beforeMap;
    }

    public HttpFilterMap getAfterMap() {
        return afterMap;
    }

    public String createSession() {
        return HttpHelper.createSession();
    }

    public Session getSession(String id) {
        return HttpHelper.getSession(id);
    }

    public int getPort() {
        return port;
    }
}
