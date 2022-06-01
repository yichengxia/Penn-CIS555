package org.example.CIS555_Interface.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.example.CIS555_Interface.Interface.InterfaceMain;
import org.example.CIS555_Interface.Tool.QueryHandler;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import opennlp.tools.stemmer.PorterStemmer;


public class IndexerDB {
	private static final String DATABASE_NAME = "dev";
    private static final String JDBC_HOST = "cis555www.cwdvkb9sjfn3.us-east-1.rds.amazonaws.com:3306";
    private static final String JDBC_URL  = "jdbc:mysql://" + JDBC_HOST + "/" + DATABASE_NAME + "?autoReconnect=true";
    private static final String CREATE_URL  = "jdbc:mysql://" + JDBC_HOST + "/";
    private static final String JDBC_USER  ="admin";
    private static final String JDBC_PASSWORD = "12345678";
	private static final DataSource ds = getDataSource();
	private static PorterStemmer stemmer = new PorterStemmer();
	
	/**
	 * connect to the RDS database
	 * 
	 * @return DataSource, corresponding database
	 */
	private final static DataSource getDataSource() {
		if (IndexerDB.ds != null) {
			return IndexerDB.ds;
		}
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(JDBC_URL);
		config.setUsername(JDBC_USER);
		config.setPassword(JDBC_PASSWORD);
		config.setConnectionTimeout(10000);
		config.setMaximumPoolSize(10);
		
		config.addDataSourceProperty("cachePrepStmts", true);
		config.addDataSourceProperty("prepStmtCacheSize", 500);
		config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
		config.addDataSourceProperty("useServerPrepStmts", true);
		config.addDataSourceProperty("useLocalSessionState", true);
		config.addDataSourceProperty("rewriteBatchedStatements", true);
		config.addDataSourceProperty("cacheResultSetMetadata", true);
		config.addDataSourceProperty("cacheServerConfiguration", true);
		config.addDataSourceProperty("elideSetAutoCommits", true);
		config.addDataSourceProperty("maintainTimeStats", false);
		
		DataSource ds = (DataSource) new HikariDataSource(config);
		return ds;
	}
	
	public static IndexerData getIndex(String word) {
		System.out.println(word);
		String query = "SELECT * FROM INDEX_TABLE WHERE word = \"" + word + "\";";
		System.out.println(query);
		IndexerData res = new IndexerData();
		res.setWord(word);
		try (Connection conn = ds.getConnection()) {
		    PreparedStatement ps = conn.prepareStatement(query);
		    ResultSet rs = ps.executeQuery();
		    
		    while (rs.next()) {
		    	String docId = rs.getString("docId");
		    	double tf = rs.getDouble("tf");
		    	res.addDoc(docId, tf);
		    }
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return res;
		
	}


	public List<IndexerData> getIndexFromQuery(String query) {
		List<IndexerData> res = new ArrayList<>();

		String[] wordsArray = QueryHandler.normalizeInputStr(query);
		
		List<String> stemmedWords = QueryHandler.stemTheWords(wordsArray, stemmer);
		
		for (String stemmedWord: stemmedWords) {
			System.out.println("stemmedWord : " + stemmedWord);
			IndexerData wordIndex = getIndex(stemmedWord);
			long numOfDocWithWord = wordIndex.getTfs().size();
			double idf = Math.log(InterfaceMain.NUM_OF_DISTINCT_DOC/(1 + numOfDocWithWord));
			wordIndex.setIdf(idf);
			res.add(wordIndex);
		}
		
		return res;
	}
}