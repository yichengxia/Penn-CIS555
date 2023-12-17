package edu.upenn.cis.cis555.crawler;

import java.util.ArrayList;
import java.util.List;

/**
 * Main class to start a crawler.
 */
public class CrawlerMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Error input. Length < 2.");
            System.exit(1);
        }
        // max thread count
        int count = Integer.parseInt(args[0]);
        // node name
        String node = args[1];
        // master name
        String master = args.length > 2 ? args[2] : null;
        // remote ip to send heartbeat packet to
        String remoteHost = "18.118.218.95";
        // initiate URL seeds
        List<String> seeds = new ArrayList<>();
        seeds.add("https://en.wikipedia.org/");
        seeds.add("https://www.upenn.edu/");
        seeds.add("https://www.amcs.upenn.edu/");
        seeds.add("https://pics.upenn.edu/masters-science-engineering-scientific-computing/");
        Crawler crawler = new Crawler(count, remoteHost, seeds, node, master);
        crawler.start();
    }
}
