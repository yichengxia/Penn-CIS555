package edu.upenn.cis.cis455.crawler.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class CreateChannelHandler implements Route {

    Logger logger = LogManager.getLogger(CreateChannelHandler.class);

    StorageInterface db;

    public CreateChannelHandler(StorageInterface db){
        this.db = db;
    }

    @Override
    public String handle(Request request, Response response) throws Exception {
        String name = request.params("name");
        String xpath = request.queryParams("xpath");
        if (name == null || xpath == null){
            logger.error("Invalid request format.");
            Spark.halt(400);
        }
        logger.info("Creating channel. Name: {}. XPath: {}", name, xpath);
        response.header("content-type", "text/html");
        String username = request.session().attribute("user");
        boolean added = db.addChannel(name, username, xpath);
        if (added) {
            logger.info("New channel added.");
            return "Channel " + name + " is added.";
        }
        Spark.halt(400);
        return "Failed to add new channel!";
    }
}
