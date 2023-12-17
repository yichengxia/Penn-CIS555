package edu.upenn.cis.cis555.crawler.remote;

import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.upenn.cis.cis555.crawler.Crawler;
import edu.upenn.cis.cis555.crawler.TaskDispatcher;
import edu.upenn.cis.cis555.packet.BroadcastPacket;
import edu.upenn.cis.cis555.packet.ControlPacket;
import edu.upenn.cis.cis555.packet.HeartbeatPacket;
import edu.upenn.cis.cis555.packet.RemotePacket;
import edu.upenn.cis.cis555.packet.StatsPacket;
import edu.upenn.cis.cis555.packet.URLPacket;
import edu.upenn.cis.cis555.util.Serializer;

/**
 * This is the remote worker class to handle sender and receiver threads.
 */
public class Worker {
    
    private static Logger logger = LoggerFactory.getLogger(Worker.class);

    private static final int PORT = 20556;

    private final InetSocketAddress masterSocketAddr;
    private final Crawler crawler;

    private List<String> names;
    private List<InetSocketAddress> addrs;
    private boolean running;
    private DatagramSocket socket;

    private TaskDispatcher dispatcher;
    private Thread sender;
    private Thread receiver;
    

    public Worker(InetSocketAddress masterSocketAddr, Crawler crawler) {
        this.masterSocketAddr = masterSocketAddr;
        this.crawler = crawler;
        names = new ArrayList<>();
        addrs = new ArrayList<>();
        running = true;
        try {
            if (this.masterSocketAddr != null) {
                socket = new DatagramSocket(PORT);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start sender and receiver threads
     */
    public void start() {
        if (masterSocketAddr == null) {
            return;
        }
        dispatcher = crawler.getDispatcher();
        sender = new Thread(this::send, "Sender");
        receiver = new Thread(this::receive, "Receiver");
        sender.start();
        receiver.start();
    }

    /**
     * Helper method for thread to send HeartbeatPacket within RemotePacket
     */
    private void send() {
        try {
            while (!Thread.interrupted()) {
                try {
                    HeartbeatPacket heartbeat = new HeartbeatPacket(running ? "Running" : "Stopping");
                    RemotePacket message = new RemotePacket(crawler.getCrawlerIdentifier(), heartbeat, String.valueOf(System.nanoTime()));
                    byte[] outputBinary = Serializer.toBinary(message);
                    socket.send(new DatagramPacket(outputBinary, outputBinary.length, masterSocketAddr));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted Exception when sending message");
        }
    }

    /**
     * Helper method for thread to receive inbound RemotePacket
     */
    private void receive() {
        byte[] buffer = new byte[65535];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                SocketAddress senderAddr = packet.getSocketAddress();
                RemotePacket inMessage = (RemotePacket) Serializer.toObject(buffer);
                RemotePacket outMessage = handle(inMessage);
                if (outMessage == null) {
                    continue;
                }
                byte[] outputBinary = Serializer.toBinary(outMessage);
                socket.send(new DatagramPacket(outputBinary, outputBinary.length, senderAddr));
            } catch (IOException e) {
                logger.error("Error when receiving message.");
            }
        }
    }

    /**
     * Helper method for thread to handle inbound messages
     * @param inMessage
     * @return outbound payload
     */
    private RemotePacket handle(RemotePacket inMessage) {
        Serializable payloadOut = null;
        Object payload = inMessage.payload;
        if (payload instanceof ControlPacket) {
            ControlPacket packet = (ControlPacket) payload;
            if (packet.signal == ControlPacket.reporting) {
                payloadOut = new StatsPacket(crawler.getHTMLCount());
            }
        } else if (payload instanceof URLPacket) {
            URLPacket packet = (URLPacket) payload;
            String[] urls = packet.urls;
            for (String url : urls) {
                dispatcher.getFromRemote(url);
            }
            String source = inMessage.source;
            if (source.equals("master")) {
                logger.info("Master URL Dispatching: {}", packet.urls.toString());
            }
        } else if (payload instanceof BroadcastPacket) {
            BroadcastPacket packet = (BroadcastPacket) payload;
            names.clear();
            names.addAll(packet.getNames());
            addrs = packet.getAddrs().stream().map(ip -> new InetSocketAddress(ip, PORT)).collect(Collectors.toList());
        }
        return payloadOut == null ? null : new RemotePacket(crawler.getCrawlerIdentifier(), payloadOut, String.valueOf(System.nanoTime()));
    }

    /**
     * Stop sender and receiver threads
     */
    public void stop() {
        if (masterSocketAddr == null) {
            return;
        }
        running = false;
        try {
            Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.interrupt();
        receiver.interrupt();
    }

    /**
     * Get destination and send URLs to it within DatagramPacket
     * @param nodeId
     * @param urls
     */
    public void sendUrls(int nodeId, List<String> urls) {
        try {
            InetSocketAddress dest = addrs.get(nodeId);
            String[] input = new String[urls.size()];
            urls.toArray(input);
            URLPacket urlListPacket = new URLPacket(input);
            RemotePacket message = new RemotePacket(crawler.getCrawlerIdentifier(), urlListPacket, String.valueOf(System.nanoTime()));
            byte[] outputBinary = Serializer.toBinary(message);
            DatagramPacket responsePacket = new DatagramPacket(outputBinary, outputBinary.length, dest);
            socket.send(responsePacket);
        } catch (IOException e) {
            logger.error("IO Exception when sending URLs");
        }
    }

    /**
     * Get the names list
     * @return names
     */
    public final List<String> getNames() {
        return names;
    }
}
