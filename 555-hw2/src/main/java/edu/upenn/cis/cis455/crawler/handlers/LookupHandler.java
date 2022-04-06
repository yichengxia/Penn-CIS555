package edu.upenn.cis.cis455.crawler.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.crawler.utils.URLInfo;
import edu.upenn.cis.cis455.crawler.utils.URLParsing;
import edu.upenn.cis.cis455.model.Document;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class LookupHandler implements Route {

    Logger logger = LogManager.getLogger(LookupHandler.class);

    StorageInterface db;

    public LookupHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public String handle(Request request, Response response) throws Exception {
        String url = request.queryParams("url");
        if (url == null) {
            logger.error("Null URL.");
            Spark.halt(404);
            response.status(404);
        }
        Document document = db.getDocumentModel(URLParsing.getURL(new URLInfo(url), true));
        if (document == null) {
            logger.error("Null document in database.");
            Spark.halt(404);
            response.status(404);
            return null;
        }
        response.type(document.getType());
        return document.getContent();
    }
}
