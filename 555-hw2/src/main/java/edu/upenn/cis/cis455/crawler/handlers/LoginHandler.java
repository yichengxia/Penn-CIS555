package edu.upenn.cis.cis455.crawler.handlers;

import spark.Request;
import spark.Route;
import spark.Response;
import spark.HaltException;
import spark.Session;
import edu.upenn.cis.cis455.storage.StorageInterface;

public class LoginHandler implements Route {

    StorageInterface db;

    public LoginHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public String handle(Request request, Response response) throws HaltException {
        String user = request.queryParams("username");
        String pass = request.queryParams("password");

        System.err.println("Login request for " + user + " and " + pass);
        if (db.getSessionForUser(user, pass)) {
            System.err.println("Logged in!");
            Session session = request.session();
            session.maxInactiveInterval(300);
            session.attribute("user", user);
            session.attribute("password", pass);
            response.body(user);
            response.redirect("/index");
        } else {
            System.err.println("Invalid credentials");
            response.redirect("/login-form.html");
        }

        return "";
    }
}
