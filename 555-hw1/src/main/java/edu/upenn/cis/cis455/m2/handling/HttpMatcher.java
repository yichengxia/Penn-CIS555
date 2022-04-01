package edu.upenn.cis.cis455.m2.handling;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.SparkController;
import edu.upenn.cis.cis455.exceptions.HaltException;
import edu.upenn.cis.cis455.m2.interfaces.Filter;
import edu.upenn.cis.cis455.m2.interfaces.Request;
import edu.upenn.cis.cis455.m2.interfaces.Response;
import edu.upenn.cis.cis455.m2.interfaces.Route;
import edu.upenn.cis.cis455.m2.server.WebService;

public class HttpMatcher {

    final static Logger logger = LogManager.getLogger(HttpMatcher.class);
    
    public Route routeMatcher(Request request, Response response) throws HaltException {
        Path path = Paths.get(request.uri());
        String requestMethod = request.requestMethod();
        HttpRouteMap httpRouteMap = null;
        switch (requestMethod) {
            case "GET":
                httpRouteMap = WebService.getInstance().getGetMap();
                break;
            case "POST":
                httpRouteMap = WebService.getInstance().getPostMap();
                break;
            case "PUT":
                httpRouteMap = WebService.getInstance().getPutMap();
                break;
            case "DELETE":
                httpRouteMap = WebService.getInstance().getDeleteMap();
                break;
            case "HEAD":
                httpRouteMap = WebService.getInstance().getHeadMap();
                break;
            case "OPTIONS":
                httpRouteMap = WebService.getInstance().getOptionsMap();
                break;
            default:
                logger.error("Failed to map path: {}", path.toString());
                WebService.getInstance().halt(500);
        }

        Map<Path, Route> map = httpRouteMap.getMap();

        for (Path mapPath : map.keySet()) {
            if (mapPath.toString().equals("*")) {
                return map.get(mapPath);
            }
            if ((path.getNameCount() != mapPath.getNameCount()) && !path.endsWith("*")) {
                continue;
            }
            Map<String, String> paramMap = new HashMap<>();
            for (int i = 0; i < mapPath.getNameCount(); i++) {
                boolean a = path.getName(i).equals(mapPath.getName(i));
                boolean b = mapPath.getName(i).toString().charAt(0) == ':';
                boolean c = mapPath.getName(i).toString().equals("*") && mapPath.getNameCount() == i + 1;
                if (a || b || c) {
                    if (mapPath.getName(i).toString().equals("*") || mapPath.getNameCount() == i + 1) {
                        request.addParam(paramMap);
                        return map.get(mapPath);
                    }
                    if (b) {
                        paramMap.put(mapPath.getName(i).toString(), path.getName(i).toString());
                    }
                } else {
                    break;
                }
            }
        }
        logger.error("Failed to process route parsing.");
        return null;
    }

    public List<Filter> filterMatcher(Request request, boolean before) {
        Map<Path, Filter> map;
        if (before) {
            map = SparkController.getInstance().getBeforeMap().getMap();
        } else {
            map = SparkController.getInstance().getAfterMap().getMap();
        }

        Map<String, String> paramMap = new HashMap<>();
        List<Filter> list = new ArrayList<>();
        String path = request.uri();
        String[] pathArr = path.split("/");

        logger.info("Pairing the path...");
        for (Path keyPath : map.keySet()) {
            String key = keyPath.toString();
            if (key.equals("/*") || key.equals(path)) {
                list.add(map.get(keyPath));
                continue;
            }
            logger.info("Got path: {}.", key);
            String[] uriArr = key.split("/", 0);
            if (uriArr.length > pathArr.length) {
                continue;
            }
            for (int i = 0; i < uriArr.length; i++) {
                if (uriArr[i].length() == 0) {
                    continue;
                }
                if (uriArr[i].charAt(0) == ':') {
                    paramMap.put(uriArr[i], pathArr[i]);
                } else if (!uriArr[i].equals(pathArr[i]) && !uriArr[i].equals("*")) {
                    break;
                }
                if (i >= uriArr.length - 1) {
                    if (uriArr[i].equals("*") || uriArr[i].length() == pathArr.length) {
                        request.setparams(paramMap);
                        list.add(map.get(keyPath));
                        request.setparams(paramMap);
                    }
                }
            }
        }
        return list;
    }
}
