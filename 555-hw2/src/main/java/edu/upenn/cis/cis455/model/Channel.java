package edu.upenn.cis.cis455.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Channel implements Serializable {
    
    private int id;
    private String name;
    private String creator;
    private String xpath;
    private Set<String> urls;

    public Channel(int id, String name, String creator, String xpath) {
        this.id = id;
        this.name = name;
        this.creator = creator;
        this.xpath = xpath;
        urls = new HashSet<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    } 

    public Set<String> getUrls() {
        return urls;
    }

    public void setUrls(Set<String> urls) {
        this.urls = urls;
    }
}
