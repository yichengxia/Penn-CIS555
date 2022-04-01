package edu.upenn.cis.cis455.m1.interfaces;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.upenn.cis.cis455.exceptions.HaltException;

public interface RequestHandler {

    static final Logger logger = LogManager.getLogger(RequestHandler.class);

    public void handleRequest() throws HaltException, IOException, Exception;
}
