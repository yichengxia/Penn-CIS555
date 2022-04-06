package edu.upenn.cis.cis455.crawler.handlers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.model.Channel;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Response;
import spark.Route;

public class HomepageHandler implements Route {

    Logger logger = LogManager.getLogger(HomepageHandler.class);

    StorageInterface db;

    public HomepageHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public String handle(Request request, Response response) throws Exception {
        logger.info("Presenting welcome message to user.");
        StringBuffer sb = new StringBuffer();
        sb.append("<!DOCTYPE html><body>");
        String username = request.session().attribute("user");
        sb.append("Welcome " + username + " <a href=\"/logout\"/>Log out</a>");
        List<Channel> channelList = db.getChannelList();
        channelList.forEach(e -> {
            sb.append("<div class=\"channelheader\">" + " Channel name: " + e.getName() + " created by: " + e.getCreator() + "</div>");
            sb.append("<a href='/show?channel=");
            sb.append(e.getName() + "'>" + e.getXpath() + "</a>");
        });
        sb.append("</body></html>");
        response.type("text/html");
        response.status(200);
        return sb.toString();
    }
}
