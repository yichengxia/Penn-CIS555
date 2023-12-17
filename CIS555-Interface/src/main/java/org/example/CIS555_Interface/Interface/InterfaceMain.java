package org.example.CIS555_Interface.Interface;
import static spark.Spark.*;

import org.example.CIS555_Interface.Handler.*;
import org.example.CIS555_Interface.Storage.*;


public class InterfaceMain {
	private final static String PAGEPATH = "./webPages/";
	public static int NUM_OF_DISTINCT_DOC = 0;
	public static DocRDSController docRDSController;
    public static DocS3Controller docS3Controller;
    public static IndexerDB indexerDB;
    public static PageRankController pageRankController;

	public static String getPagePath() {
		return PAGEPATH;
	}
	
	private static void init() {
		docRDSController = new DocRDSController();
	    docS3Controller = new DocS3Controller();
	    indexerDB = new IndexerDB();
	    pageRankController = new PageRankController();
	}
	
	public static void main(String[] args) {

		port(8080);
		init();

		get("/", new HomePageHandler());

        
        get("/css/:name", new CSSHandler());


        
        get("/result", new ResultPageHandler());

	}
}