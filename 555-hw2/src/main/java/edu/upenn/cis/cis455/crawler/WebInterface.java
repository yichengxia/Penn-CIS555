package edu.upenn.cis.cis455.crawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static spark.Spark.*;

import edu.upenn.cis.cis455.crawler.handlers.CreateChannelHandler;
import edu.upenn.cis.cis455.crawler.handlers.HomepageHandler;
import edu.upenn.cis.cis455.crawler.handlers.LoginFilter;
import edu.upenn.cis.cis455.storage.StorageFactory;
import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.Session;
import edu.upenn.cis.cis455.crawler.handlers.LoginHandler;
import edu.upenn.cis.cis455.crawler.handlers.LookupHandler;
import edu.upenn.cis.cis455.crawler.handlers.RegistrationHandler;
import edu.upenn.cis.cis455.crawler.handlers.ShowChannelHandler;

public class WebInterface {
    public static void main(String args[]) {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Syntax: WebInterface {path} {root}");
            System.exit(1);
        }

        if (!Files.exists(Paths.get(args[0]))) {
            try {
                Files.createDirectory(Paths.get(args[0]));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        port(45555);
        StorageInterface database = StorageFactory.getDatabaseInstance(args[0]);

        LoginFilter testIfLoggedIn = new LoginFilter(database);

        if (args.length == 2) {
            staticFiles.externalLocation(args[1]);
            staticFileLocation(args[1]);
        }


        before("/*", "*/*", testIfLoggedIn);
        // TODO:  add /register, /logout, /index.html, /, /lookup
        post("/login", new LoginHandler(database));
        post("/register", new RegistrationHandler(database));
        get("/logout", (request, response) -> {
            Session session = request.session();
            session.invalidate();
            System.err.println("Logged out!");
            response.redirect("/login-form.html");
            return "";
        });
        get("/", new HomepageHandler(database));
        get("/index", new HomepageHandler(database));
        get("/lookup", new LookupHandler(database));
        get("/create/:name", new CreateChannelHandler(database));
        get("/show", new ShowChannelHandler(database));

        awaitInitialization();
    }
}
