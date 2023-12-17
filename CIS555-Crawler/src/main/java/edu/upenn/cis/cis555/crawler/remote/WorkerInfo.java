package edu.upenn.cis.cis555.crawler.remote;

import java.net.SocketAddress;

import edu.upenn.cis.cis555.packet.StatsPacket;

/**
 * This is the class to store worker infomation.
 */
public class WorkerInfo {
    
    public final String name;
    public final SocketAddress addr;

    public long timestamp;
    public String status;
    public StatsPacket stats;

    public WorkerInfo(String name, SocketAddress addr) {
        this.name = name;
        this.addr = addr;
    }
}
