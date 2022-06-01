package edu.upenn.cis.cis555.crawler.info;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the RobotParser class to parse web documents.
 */
public class RobotParser {

    private static Logger logger = LoggerFactory.getLogger(RobotParser.class);
    
    private Map<String, List<Object>> allowlist;
    private Map<String, List<Object>> blocklist;
    private Map<String, Integer> delays;
    private List<String> userAgents;

    public static final RobotParser dummy = new RobotParser();

    private RobotParser() {
        allowlist = new HashMap<>();
        blocklist = new HashMap<>();
        delays = new HashMap<>();
        userAgents = new ArrayList<>();
    }

    /**
     * Constructor to parse robotsTxt
     * @param robotsTxt
     * @param userAgentInterested
     */
    public RobotParser(String robotsTxt, String userAgentInterested) {
        this();
        System.err.println("Parsing interested: " + userAgentInterested);
        String currentUserAgent = null;
        String[] lines = robotsTxt.split("\\r?\\n|\\r");
        for (String line : lines) {
            // remove # comments
            int pos = line.indexOf("#");
            if (pos != -1) {
                line = line.substring(0, pos);
            }
            // skip empty lines and convert to lowercase
            line = line.trim().toLowerCase();
            if (line.length() == 0 || line.charAt(0) == '\uFEFF') {
                continue;
            }
            // parse each line by the prefixes of different lines
            if (line.startsWith("user-agent")) {
                line = line.substring(10).trim();
                line = line.startsWith(":") ? line.substring(1) : line;
                if (userAgentInterested.startsWith(line) || line.equals("*")) {
                    userAgents.add(line);
                    currentUserAgent = line;
                } else {
                    currentUserAgent = null;
                }
            } else if (currentUserAgent != null && line.startsWith("allow")) {
                line = line.substring(5).trim();
                line = line.startsWith(":") ? line.substring(1) : line;
                addToAllowlist(currentUserAgent, line);
            } else if (currentUserAgent != null && line.startsWith("disallow")) {
                line = line.substring(8).trim();
                line = line.startsWith(":") ? line.substring(1) : line;
                addToBlocklist(currentUserAgent, line);
            } else if (currentUserAgent != null && line.startsWith("crawl-delay")) {
                line = line.substring(11).trim();
                line = line.startsWith(":") ? line.substring(1) : line;
                try {
                    delays.put(currentUserAgent, Integer.parseInt(line));
                } catch (NumberFormatException e) {
                    logger.error("Number Format Exception happened when parsing robots.txt");
                }
            }
        }
    }

    /**
     * Add key and URL line to allowlist
     * @param key
     * @param line
     */
    private void addToAllowlist(String key, String line) {
        List<Object> list = allowlist.getOrDefault(key, new ArrayList<>());
        Object value;
        if (line.contains("*") || line.endsWith("$")) {
            value = Pattern.compile(wildcardToRegex(line));
        } else {
            value = line;
        }
        list.add(value);
        allowlist.putIfAbsent(key, list);
    }

    /**
     * Add key and URL line to blocklist
     * @param key
     * @param line
     */
    private void addToBlocklist(String key, String line) {
        List<Object> list = blocklist.getOrDefault(key, new ArrayList<>());
        Object value;
        if (line.contains("*") || line.endsWith("$")) {
            value = Pattern.compile(wildcardToRegex(line));
        } else {
            value = line;
        }
        list.add(value);
        allowlist.putIfAbsent(key, list);
    }

    /**
     * Convert wildcard in line to regex expression
     * @param line
     * @return regex expression
     */
    private static final String wildcardToRegex(String line) {
        // getCharSet
        Set<Character> set = new HashSet<>();
        String pattern = ".\\/?+&:{}[]()^";
        for (Character c : pattern.toCharArray()) {
            set.add(c);
        }
        // regexEscape
        StringBuilder sb = new StringBuilder();
        StringCharacterIterator iterator = new StringCharacterIterator(line);
        char ch = iterator.current();
        while (ch != CharacterIterator.DONE) {
            if (set.contains(ch)) {
                sb.append("\\");
            }
            sb.append(ch);
            ch = iterator.next();
        }
        return "^" + sb.toString().replace("*", ".*");
    }

    /**
     * Check default parameters to see if the class is initialized
     * @return boolean flag
     */
    public boolean initialized() {
        return !allowlist.isEmpty() || !blocklist.isEmpty() || !delays.isEmpty() || !userAgents.isEmpty();
    }

    /**
     * Get the delay time
     * @param key
     * @param time
     * @return delay
     */
    public int getDelay(String key, int time) {
        Integer delay = delays.get(key);
        if (delay == null) {
            delay = delays.get("*");
            if (delay == null) {
                return time;
            }
        }
        return delay;
    }

    /**
     * Check URL to see if it is allowed
     * @param url
     * @param userAgent
     * @return boolean flag
     */
    public boolean allowedUrl(URLInfo url, String userAgent) {
        // check existed userAgents
        if (userAgents.isEmpty()) {
            return true;
        }
        String matchedAgent = null;
        for (String temp : userAgents) {
            if (userAgent.startsWith(temp)) {
                matchedAgent = temp;
                break;
            }
        }
        if (matchedAgent == null) {
            if (userAgents.contains("*")) {
                matchedAgent = "*";
            } else {
                return true;
            }
        }
        // matched value in allowlist
        String path = url.getFilePath();
        List<Object> allowed = allowlist.get(matchedAgent);
        if (allowed != null) {
            for (Object condition : allowed) {
                if (condition instanceof String) {
                    String prefix = (String) condition;
                    if (path.startsWith(prefix)) {
                        return true;
                    }
                } else {
                    Pattern pattern = (Pattern) condition;
                    Matcher matcher = pattern.matcher(path);
                    if (matcher.find()) {
                        return true;
                    }
                }
            }
        }
        // matched value in blocklist
        List<Object> disallowedLinks = blocklist.get(matchedAgent);
        if (disallowedLinks != null) {
            for (Object condition : disallowedLinks) {
                if (condition instanceof String) {
                    String prefix = (String) condition;
                    if (prefix.length() == 0) {
                        return true;
                    }
                    if (path.startsWith(prefix)) {
                        return false;
                    }
                } else {
                    Pattern pattern = (Pattern) condition;
                    Matcher matcher = pattern.matcher(path);
                    if (matcher.find()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
