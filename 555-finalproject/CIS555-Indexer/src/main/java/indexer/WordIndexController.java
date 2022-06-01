package indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import javax.sql.DataSource;

import com.amazonaws.services.cloudformation.model.Output;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;    

public class WordIndexController {
	private static final String DATABASE_NAME = "dev";
    private static final String JDBC_HOST = "cis555www.cwdvkb9sjfn3.us-east-1.rds.amazonaws.com:3306";
    private static final String JDBC_URL  = "jdbc:mysql://" + JDBC_HOST + "/" + DATABASE_NAME + "?autoReconnect=true";
    private static final String JDBC_USER  ="admin";
    private static final String JDBC_PASSWORD = "12345678";
	private static final DataSource ds = getDataSource();
	
	private static final DataSource getDataSource() {
        if (WordIndexController.ds != null) {
            return WordIndexController.ds;
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(JDBC_USER);
        config.setPassword(JDBC_PASSWORD);
        config.setConnectionTimeout(30000);
        config.setMaximumPoolSize(20);
        config.setLeakDetectionThreshold(5 * 60 * 1000);
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("useLocalSessionState", true);
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        config.addDataSourceProperty("cacheResultSetMetadata", true);
        config.addDataSourceProperty("cacheServerConfiguration", true);
        config.addDataSourceProperty("elideSetAutoCommits", true);
        config.addDataSourceProperty("maintainTimeStats", false);
        HikariDataSource ds = new HikariDataSource(config);
        return ds;
	}
	
	public WordIndexController() {
	}


	static public boolean addWordIndex(HashMap<String, Double> outputs) throws SQLException {
		boolean ret = true;
		
		try (Connection conn = ds.getConnection()) {
			conn.setAutoCommit(false);
			Statement stmt = conn.createStatement();
			for (String d : outputs.keySet()) {   
				System.out.println(d);
				
				String word = d.split("\t")[0];  
				String file = d.split("\t")[1];
//				if (word.length() > 45) {
//					word = word.substring(0, 45);
//				}
				word = word.length() > 45 ? word.substring(0, 45) : word;
				System.out.println("Ready to add word " + word + " " + String.valueOf(outputs.get(d)));
				//String addWordQuery = "INSERT INTO INDEX_TABLE (word, docId, tf) VALUES ('"+ word + "', '" + file + "', " + String.valueOf(outputs.get(d)) + " )";
				stmt.addBatch("INSERT INTO INDEX_TABLE (word, docId, tf) VALUES ('"+ word + "', '" + file + "', " + String.valueOf(outputs.get(d)) + " )");
				//stmt.executeQuery(addWordQuery);
			}
			stmt.executeBatch();
			conn.commit();
		}
		return ret;
	}
	
	static public boolean addBlock(String blockId) throws SQLException {
		boolean ret = true;
		java.sql.Date d = new java.sql.Date(new java.util.Date().getTime());
		try (Connection conn = ds.getConnection()) {
		    try (PreparedStatement ps = conn.prepareStatement(SQLQueries.ADD_BLOCK.getQuery())) {
		    	ps.setDate(2, d);;
		    	ps.setString(1, blockId);
		    	try {
		    		ps.executeUpdate();
		    	} catch (SQLIntegrityConstraintViolationException e) {
		    		ret = false;
		    	}
		    }
		}
		return ret;
	}
	
	static public int checkBlockIndexed(String blockName) throws SQLException {
		int ret = 0;
		try (Connection conn = ds.getConnection()) {
		    try (PreparedStatement ps = conn.prepareStatement(SQLQueries.CHECK_BLOCK_INDEX.getQuery())) {
		    	ps.setString(1, blockName);
		    	try (ResultSet rs = ps.executeQuery()) {
		    		rs.next();
		    		ret = rs.getInt(1);
		    	}
		    }
		}
		return ret;
	}
	
	public static void main(String[] args) throws SQLException {
//        int red = blockIndexed("Dali");
//        System.out.println(red);
//        addBlock("Dali");
//        red = blockIndexed("Dali");
//        System.out.println("red");
		
		HashMap<String, Double> testWord = new HashMap<>();
		testWord.put("Dali	docid", 0.5);
		addWordIndex(testWord);
		
    }
}
