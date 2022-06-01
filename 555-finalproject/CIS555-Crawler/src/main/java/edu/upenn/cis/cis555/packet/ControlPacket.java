package edu.upenn.cis.cis555.packet;

import java.io.Serializable;

/**
 * This is the packet to save control signal.
 */
public class ControlPacket implements Serializable {
    
    public static transient int reporting = 0;
	
	public final int signal;
	
	public ControlPacket(int signal) {
		this.signal = signal;
	} 
}
