package edu.upenn.cis.cis555.packet;

import java.io.Serializable;

/**
 * This is the packet to save crawled URLs.
 */
public class URLPacket implements Serializable {
    
    public final String[] urls;

    public URLPacket(String[] urls) {
        this.urls = urls;
    }
}
