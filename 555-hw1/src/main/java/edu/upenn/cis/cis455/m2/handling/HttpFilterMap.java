package edu.upenn.cis.cis455.m2.handling;

import edu.upenn.cis.cis455.m2.interfaces.Filter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HttpFilterMap {

    final static Logger logger = LogManager.getLogger(HttpFilterMap.class);

    private Map<Path, Filter> map = new HashMap<>();;

    public Map<Path, Filter> getMap() {
        return map;
    }

    public void bindMap(Filter filter) {
        bindMap("*", filter);
    }

    public void bindMap(String pathString, Filter filter) {
        if (pathString == null) {
            logger.error("Path is null!");
            return;
        }
        if (filter == null) {
            logger.error("Filter is null!");
            return;
        }
        Path path = Paths.get(pathString);
        if (map.containsKey(path)) {
            logger.info("Filter: {} already bound!", pathString);
        } else {
            map.put(path, filter);
        }
    }
}
