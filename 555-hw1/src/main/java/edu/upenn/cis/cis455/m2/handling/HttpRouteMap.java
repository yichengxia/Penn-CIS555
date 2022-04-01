package edu.upenn.cis.cis455.m2.handling;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.m2.interfaces.Route;

public class HttpRouteMap {
    
    final static Logger logger = LogManager.getLogger(HttpRouteMap.class);

    private Map<Path, Route> map = new HashMap<>();;

    public Map<Path, Route> getMap() {
        return map;
    }

    public void bindMap(String pathString, Route route) {
        if (pathString == null) {
            logger.error("Path is null!");
            return;
        }
        if (route == null) {
            logger.error("Route is null!");
            return;
        }
        Path path = Paths.get(pathString);
        if (map.containsKey(path)) {
            logger.info("Path: {} already bound!", pathString);
        } else {
            map.put(path, route);
        }
    }
}
