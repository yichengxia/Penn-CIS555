package edu.upenn.cis.cis455.m2.server;

import edu.upenn.cis.cis455.m1.handling.HttpRequest;
import edu.upenn.cis.cis455.m1.handling.HttpResponse;
import edu.upenn.cis.cis455.m2.handling.HttpMatcher;
import edu.upenn.cis.cis455.m2.handling.HttpRouteMap;
import edu.upenn.cis.cis455.m2.interfaces.Request;
import edu.upenn.cis.cis455.m2.interfaces.Response;
import edu.upenn.cis.cis455.m2.interfaces.Route;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestMatcher {

    Route route1;
    Route route2;
    Route route3;
    Request request;
    Response response;
    String rawPath1;
    String rawPath2;
    String rawPath3;
    String requestPath;
    WebService webService;

    @Before
    public void before(){
        request = new HttpRequest();
        response = new HttpResponse();
        route1 = (Request request, Response response) -> {
            return "1234";
        };
        route2 = (Request request, Response response) -> {
            return "abcd";
        };
        route3 = (Request request, Response response) -> {
            return "!@#$";
        };
        request.setRequestMethod("GET");
        webService = WebService.getInstance();
    }

    @Test
    public void test0(){
        rawPath1 = "/index/1/7";
        rawPath2 = "/index/winnie/a";
        rawPath3 = "/index/symbol/pooh";
        requestPath = "/index/1/7";
        request.setUri(requestPath);
        webService.get(rawPath1, route1);
        webService.get(rawPath2, route2);
        webService.get(rawPath3, route3);
        HttpMatcher pathMapper = new HttpMatcher();
        Route res = pathMapper.routeMatcher(request, response);
        assertTrue(res == route1);
    }

    @Test
    public void test1(){
        rawPath1 = "/index/1/7";
        rawPath2 = "/index/winnie/a";
        rawPath3 = "/index/symbol/pooh";
        requestPath = "/index/1/7";
        request.setUri(requestPath);
        webService.get(rawPath1, route1);
        webService.get(rawPath2, route2);
        webService.get(rawPath3, route3);
        HttpMatcher pathMapper = new HttpMatcher();
        Route res = pathMapper.routeMatcher(request, response);
        assertTrue(res == route1);
    }

    @Test
    public void test2(){
        rawPath1 = "/index/num/1";
        rawPath2 = "/index/b/:a";
        rawPath3 = "/index/symbol/pooh";
        requestPath = "/index/b/1";
        request.setUri(requestPath);
        webService.get(rawPath1, route1);
        webService.get(rawPath2, route2);
        webService.get(rawPath3, route3);
        HttpMatcher pathMapper = new HttpMatcher();
        Route res = pathMapper.routeMatcher(request, response);
        assertTrue(res == route2);
    }

    @Test
    public void test3(){
        rawPath1 = "/index/symbol/pooh";
        rawPath2 = "/index/:c/aa";
        rawPath3 = "/index/winnie/pooh";
        requestPath = "/index/:c/aa";
        request.setUri(requestPath);
        webService.get(rawPath1, route1);
        webService.get(rawPath2, route2);
        webService.get(rawPath3, route3);
        HttpMatcher pathMapper = new HttpMatcher();
        Route res = pathMapper.routeMatcher(request, response);
        assertTrue(res == route2);
    }

    @Test
    public void test4(){
        rawPath1 = "/index/m/pooh";
        rawPath2 = "/index/symbol/pooh";
        rawPath3 = "/index/winnie/pooh";
        requestPath = "/index/winnie/pooh";
        request.setUri(requestPath);
        webService.get(rawPath1, route1);
        webService.get(rawPath2, route2);
        webService.get(rawPath3, route3);
        HttpMatcher pathMapper = new HttpMatcher();
        Route res = pathMapper.routeMatcher(request, response);
        assertTrue(res == route3);
    }
}
