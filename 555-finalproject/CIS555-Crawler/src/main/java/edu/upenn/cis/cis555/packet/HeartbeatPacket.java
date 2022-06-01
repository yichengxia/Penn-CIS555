package edu.upenn.cis.cis555.packet;

import java.io.Serializable;

/**
 * This is the packet to save heartbeat status.
 */
public class HeartbeatPacket implements Serializable {
    
    public String status;

    public HeartbeatPacket(String status) {
        this.status = status;
    }
}
