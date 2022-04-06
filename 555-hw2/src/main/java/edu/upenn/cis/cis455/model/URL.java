package edu.upenn.cis.cis455.model;

import java.io.Serializable;

public class URL implements Serializable {
    
    private String id;
    private String url;
    private long timestamp;

    public URL(String id, String url, long timestamp) {
        this.id = id;
        this.url = url;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
