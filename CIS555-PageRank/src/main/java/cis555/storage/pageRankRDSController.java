package cis555.storage;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import cis555.utils.daoUtils;
import cis555.utils.pageRankConfig;

import javax.sql.DataSource;
import java.sql.*;
import java.util.UUID;
import java.util.*;


public class pageRankRDSController {

	private static Logger logger = LogManager.getLogger(pageRankRDSController.class);
    private static final String DATABASE_NAME = "dev";
    private static final String JDBC_HOST = "cis555www.cwdvkb9sjfn3.us-east-1.rds.amazonaws.com:3306";
    private static final String JDBC_URL  = "jdbc:mysql://" + JDBC_HOST + "/" + DATABASE_NAME + "?autoReconnect=true";
    private static final String CREATE_URL  = "jdbc:mysql://" + JDBC_HOST + "/";
    private static final String JDBC_USER  ="admin";
    private static final String JDBC_PASSWORD = "12345678";

    private static String SQL_ADD_PAGERANK = "REPLACE INTO "+ pageRankConfig.PAGERANK_TABLE
            + " (urlID, url, pageRank) "
            + "VALUES (?, ?, ?);";

    private static String SQL_ADD_PAGERANKS = "REPLACE INTO "+ pageRankConfig.PAGERANK_TABLE
            + " (urlID, url, pageRank) "
            + "VALUES ";

    private static String SQL_QUERY_PR_BY_URL = "SELECT pageRank FROM "+ pageRankConfig.PAGERANK_TABLE +
            " WHERE url=?;";


    private static String SQL_QUERY_PRS_BY_URLS = "SELECT * FROM "+ pageRankConfig.PAGERANK_TABLE +
            " WHERE url IN ";


    private static final DataSource ds = getDataSource();
    
    public pageRankRDSController() {}
    
    private static final DataSource getDataSource() {
    	if (pageRankRDSController.ds != null) {
    		return pageRankRDSController.ds;
    	}
    	
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(JDBC_USER);
        config.setPassword(JDBC_PASSWORD);
        config.setConnectionTimeout(100000);
        config.setMaximumPoolSize(20);
        
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
        
        DataSource ds = (DataSource) new HikariDataSource(config);
        return ds;
    }
    
    public boolean addPageRank(String url, double pageRank) throws SQLException {
    	try {
    		Connection conn = ds.getConnection();
			PreparedStatement ps = conn.prepareStatement(SQL_ADD_PAGERANK);
            ps.setString(1, daoUtils.toUrlId(url)); //TODO
            ps.setString(2, url);
            ps.setDouble(3, pageRank);

            ps.executeUpdate();
    	} catch (SQLIntegrityConstraintViolationException e) {
    		e.printStackTrace();
    		return false;
    	}
    	return true;
    }
    
    public boolean addPageRanks(List<String[]> res) throws SQLException {

        StringBuilder sb = new StringBuilder();

        for (String[] pr : res) {
            String url = pr[0];
            String urlId = daoUtils.toUrlId(url);
            String pagerank = pr[1];

            sb.append('(').append('"').append(urlId).append('"');
            sb.append(',').append('"').append(url).append('"');
            sb.append(',').append('"').append(pagerank).append('"');
            sb.append(')').append(",");
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append(';');

        try (Connection conn = ds.getConnection()) {
            //  String values = urlIds.stream().map((s) -> {return "\""+s+"\"";}).collect(Collectors.joining(",", "(", ")"));
            try (PreparedStatement ps = conn.prepareStatement(SQL_ADD_PAGERANKS + sb.toString())) {
//                ps.setString(1, sb.toString());
                System.out.println(ps.toString());
                try {
                    ps.executeUpdate(); // ret = 1
                } catch (SQLIntegrityConstraintViolationException e) {
                    return false;
                }
            }
        }
        return true;    	
    }
    
    
    public double getPageRankByUrl(String url) {
    	try {
    		Connection conn = ds.getConnection();
    		PreparedStatement ps = conn.prepareStatement(SQL_QUERY_PR_BY_URL);
    		ps.setString(1, url);
    		ResultSet res = ps.executeQuery();
    		if (res.next()) {
    			return res.getDouble("pageRank");
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return -1;
    }
    
    public static void main(String[] args) {
  	
    	pageRankRDSController controller = new pageRankRDSController();
    	boolean res = false;
		try {
			res = controller.addPageRank("google.com", 2.33);
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	System.out.println(res);
    }
    
}
