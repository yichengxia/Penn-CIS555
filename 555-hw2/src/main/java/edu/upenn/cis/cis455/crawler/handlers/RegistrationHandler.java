package edu.upenn.cis.cis455.crawler.handlers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.upenn.cis.cis455.storage.StorageInterface;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class RegistrationHandler implements Route {

    StorageInterface db;

    public RegistrationHandler(StorageInterface db) {
        this.db = db;
    }

    @Override
    public String handle(Request request, Response response) throws Exception {
        System.err.println("Ready for registration.");
        QueryParamsMap queryParamsMap = request.queryMap();
        String username = queryParamsMap.value("username");
        String password = queryParamsMap.value("password");
        if (username == null || username.length() == 0 || password == null || password.length() == 0) {
            response.redirect("register.html");
        }
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException | NullPointerException e) {
            System.err.println("Exception when encrypting the password.");
        }
        password = new String(messageDigest.digest(password.getBytes()));
        System.err.printf("Registering for user: %s with password (encrypted): %s.\n", username, password);
        int status = db.addUser(username, password);
        if (status == 1) {
            response.status(200);
            return "Successful registration! <a href=\"login-form.html\"/>Login</a>";
        }
        Spark.halt(409);
        return "Conflict registration!";
    }
}
