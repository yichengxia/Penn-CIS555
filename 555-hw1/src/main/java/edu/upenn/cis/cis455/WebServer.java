package edu.upenn.cis.cis455;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Initialization / skeleton class.
 * Note that this should set up a basic web server for Milestone 1.
 * For Milestone 2 you can use this to set up a basic server.
 * 
 * CAUTION - ASSUME WE WILL REPLACE THIS WHEN WE TEST MILESTONE 2,
 * SO ALL OF YOUR METHODS SHOULD USE THE STANDARD INTERFACES.
 * 
 * @author zives
 *
 */
public class WebServer {

    static final Logger logger = LogManager.getLogger(WebServer.class);

    public static void main(String[] args) {
        org.apache.logging.log4j.core.config.Configurator.setLevel("edu.upenn.cis.cis455", Level.DEBUG);
        logger.info("Starting Web Server...");

        // TODO: make sure you parse *BOTH* command line arguments properly
        
        // All user routes should go below here...
        int port = 45555;
        String directory = "./www";

        if (args.length < 0 || args.length > 2) {
            logger.error("Argument error. Please enter port and/or root directory only.");
        } else if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                directory = args[0];
            }
        } else if (args.length == 2) {
            try {
                port = Integer.parseInt(args[0]);
                directory = args[1];
            } catch (NumberFormatException e) {
                logger.error("Invalid input on start, with port: {}, directory: {}.", args[0], args[1]);
                System.exit(0);
            }
        }

        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("port", String.valueOf(port));
        propertyMap.put("directory", directory);

        SparkController.getInstance();
        SparkController.config(propertyMap);

        SparkController.get("/testRoute", (request, response) -> "testRoute content");

        SparkController.get("/:name/hello", (request, response) -> "Hello: " + request.params("name"));

        SparkController.get("/testCookie1", (request, response) -> {
            String body = "<HTML><BODY><h3>Cookie Test 1</h3>";
            response.cookie("TestCookie1", "1");
            body += "Added cookie (TestCookie,1) to response.";
            response.type("text/html");
            response.body(body);
            return response.body();
        });

        SparkController.get("/testSession1", (request, response) -> {
            String body = "<HTML><BODY><h3>Session Test 1</h3>";
            request.session(true).attribute("Attribute1", "Value1");
            body += "</BODY></HTML>";
            response.type("text/html");
            response.body(body);
            return response.body();
        });

        SparkController.before((request, response) -> request.attribute("attribute1", "everyone"));
  
        SparkController.get("/testFilter1", (request, response) -> {
            String body = "<HTML><BODY><h3>Filters Test</h3>";
            for (String attribute : request.attributes()) {
                body += "Attribute: " + attribute + " = " + request.attribute(attribute) + "\n";
            }
            body += "</BODY></HTML>";
            response.type("text/html");
            response.body(body);
            return response.body();
        });
  
        SparkController.after((request, response) -> {});

        SparkController.awaitInitialization();

        logger.info("Initialized with port: {}, directory: \"{}\"", port, directory);
        // ... and above here. Leave this comment for the Spark comparator tool

        System.out.println("Waiting to handle requests!");
    }
}
