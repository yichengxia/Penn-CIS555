package edu.upenn.cis.cis555.packet;

import java.io.Serializable;
import java.util.List;

/**
 * The packet to save the count of crawled HTML pages.
 */
public class StatsPacket implements Serializable {
    
    public final List<Long> counts;

    public StatsPacket(List<Long> counts) {
        this.counts = counts;
    }
}
