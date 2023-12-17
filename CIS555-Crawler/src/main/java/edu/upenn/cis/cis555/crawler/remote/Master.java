package edu.upenn.cis.cis555.crawler.remote;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import edu.upenn.cis.cis555.packet.BroadcastPacket;
import edu.upenn.cis.cis555.packet.ControlPacket;
import edu.upenn.cis.cis555.packet.HeartbeatPacket;
import edu.upenn.cis.cis555.packet.RemotePacket;
import edu.upenn.cis.cis555.packet.StatsPacket;
import edu.upenn.cis.cis555.util.Serializer;

/**
 * This remote master class is to monitor all crawlers.
 */
public class Master {

    private static final int PORT = 21555;

    private final ConcurrentHashMap<String, WorkerInfo> workers = new ConcurrentHashMap<>();

    private DatagramSocket socket;
    private Thread sender;
	private Thread receiver;
    
    public Master() {
        try {
			socket = new DatagramSocket(PORT);
			socket.setSoTimeout(0);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		sender = new Thread(this::send, "Master-Sender");
		receiver = new Thread(this::receive, "Master-Receiver");
		sender.start();
		receiver.start();
    }

	/**
	 * Send BroadcastPacket within RemotePacket
	 */
    private void send() {
		try {
			while (true) {
				try {
					List<String> workerNames = workers.keySet().stream().sorted().collect(Collectors.toCollection(ArrayList::new));
					List<String> workerAddrs =  workerNames.stream().map(name -> ((InetSocketAddress) workers.get(name).addr).getHostString()).collect(Collectors.toCollection(ArrayList::new));
					BroadcastPacket payloadOut = new BroadcastPacket(workerNames, workerAddrs);
					RemotePacket outMessage = new RemotePacket("master", payloadOut, String.valueOf(System.nanoTime()));
		            byte[] outBytes = Serializer.toBinary(outMessage);
		            for (WorkerInfo worker: workers.values()) {
		            	DatagramPacket responsePacket = new DatagramPacket(outBytes, outBytes.length, worker.addr);
						socket.send(responsePacket);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
            e.printStackTrace();
		}
	}

	/**
	 * Receive RemotePacket
	 */
    private void receive() {
		byte[] buf = new byte[65535];
		try {
			while (true) {
				try {
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					socket.receive(packet);
		            SocketAddress sender = packet.getSocketAddress();
		            RemotePacket inMessage = (RemotePacket) Serializer.toObject(buf);
		            RemotePacket outMessage = handle(inMessage, sender);
		            if (outMessage == null) {
		            	continue;
		            }
		            byte[] outBytes = Serializer.toBinary(outMessage);
		            DatagramPacket responsePacket = new DatagramPacket(outBytes, outBytes.length, sender);
		            socket.send(responsePacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handle the inbound message in RemotePacket
	 * @param inMessage
	 * @param sender
	 * @return null
	 */
    private final RemotePacket handle(RemotePacket inMessage, SocketAddress sender) {
        Object payload = inMessage.payload;
		if (payload instanceof HeartbeatPacket) {
			HeartbeatPacket packet = (HeartbeatPacket) payload;
			WorkerInfo workerInfo = workers.get(inMessage.source);
			if (workerInfo == null) {
				workerInfo = new WorkerInfo(inMessage.source, sender);
			}
			workerInfo.status = packet.status;
			workerInfo.timestamp = System.nanoTime();
			workers.putIfAbsent(inMessage.source, workerInfo);
		} else if (payload instanceof StatsPacket) {
			StatsPacket packet = (StatsPacket) payload;
			WorkerInfo workerInfo = workers.get(inMessage.source);
			if (workerInfo != null) {
				workerInfo.stats = packet;
			}
			workerInfo.timestamp = System.nanoTime();
		}
        return null;
    }

    public final List<WorkerInfo> getWorkerInfo() {
		List<String> workerNames = workers.keySet().stream().sorted().collect(Collectors.toCollection(ArrayList::new));
		return workerNames.stream().map(name -> workers.get(name)).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Send ControlPacket to the worker with given workerName
	 * @param workerName
	 * @param messageType
	 * @return boolean flag show if sending is successful
	 */
    public boolean sendControlPacket(String workerName, int messageType) {
		WorkerInfo workerInfo = workers.get(workerName);
		if (workerInfo == null) {
			return false;
		}
		try {
			ControlPacket outPayload = new ControlPacket(messageType);
			RemotePacket outMessage = new RemotePacket("master", outPayload, String.valueOf(System.nanoTime()));
			byte[] outBytes = Serializer.toBinary(outMessage);
			DatagramPacket responsePacket = new DatagramPacket(outBytes, outBytes.length, workerInfo.addr);
			socket.send(responsePacket);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
