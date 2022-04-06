package edu.upenn.cis.cis455.crawler.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.model.Channel;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class ShowChannelHandler implements Route {

    Logger logger = LogManager.getLogger(ShowChannelHandler.class);

    StorageInterface db;

    public ShowChannelHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public String handle(Request request, Response response) throws Exception {
        String name = request.queryParams("channel");
        int id = db.getChannelId(name);
        if (id == -1) {
            logger.error("Channel {} does not exist.", name);
            response.status(404);
            Spark.halt(404);
            return "Channel does not exist!";
        }
        Channel channel = db.getChannel(id);
        if (channel == null) {
            logger.error("Channel {} does not exist.", name);
            response.status(404);
            Spark.halt(404, "Channel does not exist!");
            return "Channel does not exist!";
        }
        response.status(200);
        response.type("text/html");
        StringBuffer sb = new StringBuffer();
        sb.append("<!DOCTYPE html>\n<html><body>");
        sb.append("<div class=\"channelheader\">\n");
        sb.append("Channel name: " + channel.getName() + ", created by: " + channel.getCreator() + "</div>\n");
        sb.append("<div class=\"docs\"><ul>\n");
        for (String url : channel.getUrls()) {
            logger.info("Getting url: {}.", url);
            String crawledTime = db.getCrawledTime(url);
            String document = db.getDocument(url).trim();
            sb.append("<li><p>Crawled on: " + crawledTime + "</p><p>Location: " + url + "</p>");
            sb.append("<div class=\"document\">\n" + document + "</div>\n</li>\n");
        }
        sb.append("</ul>\n</div>\n</body>\n</html>\n");
        logger.info("Sending html back.");
        return sb.toString();
    }
}
