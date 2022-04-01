package edu.upenn.cis.cis455.m2.handling;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import edu.upenn.cis.cis455.m2.interfaces.Session;

public class HttpSession extends Session {

    private String id;
    private long creationTime;
    private long lastAccessedTime;
    private boolean valid;
    private int maxInactiveInterval;
    private Map<String, Object> attributes;

    public HttpSession() {
		id = UUID.randomUUID().toString();
		creationTime = System.currentTimeMillis();
		lastAccessedTime = creationTime;
		attributes = new HashMap<>();
		maxInactiveInterval = 600;
		valid = true;
	}

    @Override
    public String id() {
        access();
        return id;
    }

    @Override
    public long creationTime() {
        access();
        return creationTime;
    }

    @Override
    public long lastAccessedTime() {
        access();
        return lastAccessedTime;
    }

    @Override
    public boolean valid() {
        access();
        return valid;
    }

    @Override
    public void invalidate() {
        access();
        valid = false;
    }

    @Override
    public int maxInactiveInterval() {
        access();
        return maxInactiveInterval;
    }

    @Override
    public void maxInactiveInterval(int interval) {
        access();
        maxInactiveInterval = interval; 
    }

    @Override
    public void access() {
        long currentTime = System.currentTimeMillis();
        if (currentTime > lastAccessedTime + maxInactiveInterval) {
            invalidate();
        }
        lastAccessedTime = currentTime;
    }

    @Override
    public void attribute(String name, Object value) {
        access();
        attributes.put(name, value);
    }

    @Override
    public Object attribute(String name) {
        access();
        return attributes.get(name);
    }

    @Override
    public Set<String> attributes() {
        access();
        return attributes.keySet();
    }

    @Override
    public void removeAttribute(String name) {
        access();
        attributes.remove(name);
    }
}
