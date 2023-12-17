package edu.upenn.cis.cis555.packet;

import java.io.Serializable;

/**
 * This is the packet to save BroadcastPacket with timestamp.
 */
public class RemotePacket implements Serializable {
    
    public String source;
    public Object payload;
    public String etag;

    public RemotePacket(String source, Serializable payload, String etag) {
        this.source = source;
        this.payload = payload;
        this.etag = etag;
    }
}
