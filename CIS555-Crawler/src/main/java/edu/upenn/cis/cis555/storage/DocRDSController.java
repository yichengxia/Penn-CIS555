package edu.upenn.cis.cis555.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * This is the class for Amazon RDS controller.
 */
public class DocRDSController {
    
    private static String SQL_ADD_DOCUMENT = "INSERT INTO DOCUMENT_TABLE " + 
		"(url, urlId, docId, docBlockName, docBlockIndex, lastCrawledTs, lastModifiedTs) VALUES (?, ?, ?, ?, ?, ?, ?);";
	private static String SQL_QUERY_DOC_BY_DOCID = "SELECT * FROM DOCUMENT_TABLE USE INDEX (docIdIndex) WHERE docId=?;";
	private static String SQL_IS_URL_SEEN = "SELECT 1 FROM DOCUMENT_TABLE WHERE urlId=?;";

	private static final String DATABASE_NAME = "dev";
    private static final String JDBC_HOST = "cis555www.cwdvkb9sjfn3.us-east-1.rds.amazonaws.com:3306";
    private static final String JDBC_URL = "jdbc:mysql://" + JDBC_HOST + "/" + DATABASE_NAME;
    private static final String JDBC_USER  ="admin";
    private static final String JDBC_PASSWORD = "12345678";
	private static final DataSource ds = getDataSource();
	
	private static final DataSource getDataSource() {
		if (DocRDSController.ds != null) {
			return DocRDSController.ds;
		}
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(JDBC_URL);
		config.setUsername(JDBC_USER);
		config.setPassword(JDBC_PASSWORD);
		config.setConnectionTimeout(30000);
		config.setMaximumPoolSize(20);
		config.setLeakDetectionThreshold(5*60*1000);
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

	public boolean addDoc(final DocRDSEntity docRDSEntity) throws SQLException {
		boolean ret = true;
		try (Connection conn = ds.getConnection()) {
		    try (PreparedStatement ps = conn.prepareStatement(SQL_ADD_DOCUMENT)) {
		    	ps.setString(1, docRDSEntity.getUrl());
		    	ps.setString(2, docRDSEntity.getUrlId());
		    	ps.setString(3, docRDSEntity.getDocId());
		    	ps.setString(4, docRDSEntity.getDocBlockName());
		    	ps.setInt(5, docRDSEntity.getDocBlockIndex());
		    	ps.setLong(6, docRDSEntity.getLastCrawledTs());
		    	ps.setLong(7, docRDSEntity.getLastModifiedTs());
		    	try {
		    		ps.executeUpdate();
		    	} catch (SQLIntegrityConstraintViolationException e) {
		    		ret = false;
		    	}
		    }
		}
		return ret;
	}

	public List<DocRDSEntity> queryDocByDocId(String docId) throws SQLException {
		List<DocRDSEntity> result = new ArrayList<DocRDSEntity>();
		try (Connection conn = ds.getConnection()) {
		    try (PreparedStatement ps = conn.prepareStatement(SQL_QUERY_DOC_BY_DOCID)) {
		    	ps.setString(1, docId);
		        try (ResultSet rs = ps.executeQuery()) {
		            while (rs.next()) {
		            	final String url = rs.getString("url");
		            	final String urlId = rs.getString("urlId");
		            	final String docBlockName = rs.getString("docBlockName");
		            	final int docBlockIndex = rs.getInt("docBlockIndex");
		            	final long lastCrawledTs = rs.getLong("lastCrawledTs");
		            	final long lastModifiedTs = rs.getLong("lastModifiedTs");
		            	DocRDSEntity docRDSEntity = new DocRDSEntity(url, urlId, docId, docBlockName, docBlockIndex, lastCrawledTs, lastModifiedTs);
		            	result.add(docRDSEntity);
		            }
		        }
		    }
		}
		return result;
	}

	public boolean isUrlSeen(String urlId) throws SQLException {
		boolean ret = false;
		try (Connection conn = ds.getConnection()) {
			try (PreparedStatement ps = conn.prepareStatement(SQL_IS_URL_SEEN)) {
				ps.setString(1, urlId);
		        try (ResultSet rs = ps.executeQuery()) {
		            while (rs.next()) {
		            	ret = true;
		            }
		        }
	    	}
		}
		return ret;
	}
}
