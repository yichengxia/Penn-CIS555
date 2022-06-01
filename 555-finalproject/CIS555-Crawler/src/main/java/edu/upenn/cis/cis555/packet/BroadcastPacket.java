package edu.upenn.cis.cis555.packet;

import java.io.Serializable;
import java.util.List;

/**
 * This is the packet to save names and addresses.
 */
public class BroadcastPacket implements Serializable {
    
    final List<String> names;
    final List<String> addrs;

    public BroadcastPacket(List<String> names, List<String> addrs) {
        this.names = names;
        this.addrs = addrs;
    }

    public List<String> getNames() {
        return names;
    }

    public List<String> getAddrs() {
        return addrs;
    }
}
