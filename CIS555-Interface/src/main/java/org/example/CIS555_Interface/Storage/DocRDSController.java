package org.example.CIS555_Interface.Storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import org.example.CIS555_Interface.Tool.SQLQuery;

import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * This class is used as a controller for our java backend to talk with Amazon RDS.
 * The RDS table contains 7 columns.
 * url: url itself
 * urlId: hash value of the url
 * docId: hash value of the content of url(hash(raw bytes))
 * docBlockName: name of the block stored in S3
 * docBlockIndex: index within the S3 block
 * lastCrawledTs: timestamp at crawled time
 * lastModifiedTs: timestamp at updated time, 0 if never updated
 */
public class DocRDSController {
    // RDS Credentials
	private static final String DATABASE_NAME = "dev";
    private static final String JDBC_HOST = "cis555www.cwdvkb9sjfn3.us-east-1.rds.amazonaws.com:3306";
    private static final String JDBC_URL  = "jdbc:mysql://" + JDBC_HOST + "/" + DATABASE_NAME + "?autoReconnect=true";
    private static final String CREATE_URL  = "jdbc:mysql://" + JDBC_HOST + "/";
    private static final String JDBC_USER  ="admin";
    private static final String JDBC_PASSWORD = "12345678";
    private static final DataSource ds = getDataSource();
    
    /**
     * https://github.com/brettwooldridge/HikariCP
     * https://github.com/spring-framework-guru/sfg-blog-posts/issues/86
     * https://www.baeldung.com/hikaricp
     */
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

    public DocRDSController() {
    }

    public boolean addDoc(final DocRDSEntity docRDSEntity) throws SQLException {
        boolean ret = true;
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(SQLQuery.ADD_DOC.value())) {
                ps.setString(1, docRDSEntity.getUrl());
                ps.setString(2, docRDSEntity.getUrlId());
                ps.setString(3, docRDSEntity.getDocId());
                ps.setString(4, docRDSEntity.getDocBlockName());
                ps.setInt(5, docRDSEntity.getDocBlockIndex());
                ps.setLong(6, docRDSEntity.getLastCrawledTs());
                ps.setLong(7, docRDSEntity.getLastModifiedTs());
                try {
                    ps.executeUpdate(); // ret = 1
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
            try (PreparedStatement ps = conn.prepareStatement(SQLQuery.SELECT_DOC_BY_DOCID.value())) {
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

    /**
     * Use urlId for query. urlId is the PK so there should be only one query result.
     * Subject to change in future iteration.
     */
    public List<DocRDSEntity> queryDocByUrlId(String urlId) throws SQLException {
        List<DocRDSEntity> result = new ArrayList<DocRDSEntity>();
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(SQLQuery.SELECT_DOC_BY_URLID.value())) {
                ps.setString(1, urlId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final String url = rs.getString("url");
                        final String docId = rs.getString("docId");
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

    /**
     * Query the url directly.
     */
    public String queryUrlByUrlId(String urlId) throws SQLException {
        String url = null;
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(SQLQuery.SELECT_URL_BY_ONE_URLID.value() + "\"" + urlId + "\"")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        url = rs.getString("url");
                    }
                }
            }
        }
        return url;
    }

    /**
     * 1-to-1 mapping from urlId to actual url
     */
    public List<String> queryUrlsByUrlIds(List<String> urlIds) throws SQLException {
        HashMap<String, String> result = new HashMap<String, String>();
        int STEP = 1600; // avoid reaching MySQL maximum query length
        for (int fromIndex = 0; fromIndex < urlIds.size(); fromIndex += STEP) {
            List<String> vars = urlIds.subList(fromIndex, Math.min(fromIndex + STEP, urlIds.size()));
            String queryInput = concat(vars);
            try (Connection conn = ds.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(SQLQuery.SELECT_URL_BY_URLID.value() + queryInput)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            final String url = rs.getString("url");
                            final String urlId = rs.getString("urlId");
                            result.put(urlId, url);
                        }
                    }
                }
            }
        }
        return urlIds.stream().map(urlId -> result.getOrDefault(urlId, "")).collect(Collectors.toList());
    }

    public List<List<String>> queryUrlsByDocIds(List<String> docIds) throws SQLException {
    	// docId, list of urls
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        // create a value which is list for every docId
        for (String docId : docIds) {
            result.put(docId, new ArrayList<String>());
        }
        int STEP = 1600; // avoid reaching MySQL maximum query length
        for (int fromIndex = 0; fromIndex < docIds.size(); fromIndex += STEP) {
            List<String> vars = docIds.subList(fromIndex, Math.min(fromIndex + STEP, docIds.size()));
            String queryInput = concat(vars);
            try (Connection conn = ds.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(SQLQuery.SELECT_URL_BY_DOCID.value() + queryInput + ";")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String url = rs.getString("url");
                            String docId = rs.getString("docId");
                            result.get(docId).add(url);
                        }
                    }
                }
            }
        }
        // docId is gone, directly urls
        return docIds.stream().map(docId -> result.getOrDefault(docId, new ArrayList<>())).collect(Collectors.toList());
    }

    /**
     * Detect duplicate url
     */
    public boolean isUrlSeen(String urlId) throws SQLException {
        boolean ret = false;
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(SQLQuery.URL_IS_SEEN.value())) {
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

    private static String concat(List<String> input){
        List<String> modified = new ArrayList<>();
        input.forEach(e -> {
            modified.add(String.format("\"%s\"", e));
        });
        String concat = "(" + String.join(",", modified) + ")";
        return concat;
    }
    
	public List<String> queryUrlsUnderDomain(String domain, int limit) throws SQLException {
		List<String> result = new ArrayList<String>();
		try (Connection conn = ds.getConnection()) {
			try (PreparedStatement ps = conn.prepareStatement(SQLQuery.SQL_URL_UNDER_DOMAIN.value())) {
				ps.setString(1, domain);
				ps.setInt(2, limit);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final String url = rs.getString("url");
						result.add(url);
					}
				}
			}
		}
		return result;
	}

	public List<Entry<String, Integer>> topKFrequentDomain(int limit) throws SQLException {
		limit = Math.min(limit, 1000);
		List<Entry<String, Integer>> result = new ArrayList<Entry<String, Integer>>();
		try (Connection conn = ds.getConnection()) {
			try (PreparedStatement ps = conn.prepareStatement(SQLQuery.SQL_TOP_K_FREQ.value())) {
				ps.setInt(1, limit);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						final String domain = rs.getString("DOMAIN");
						final int freq = rs.getInt("FREQ");
						result.add(new AbstractMap.SimpleEntry<String, Integer>(domain, freq));
					}
				}
			}
		}
		return result;
	}

	public int getDistictDocNum() throws SQLException {
		try (Connection conn = ds.getConnection()) {
			try (PreparedStatement ps = conn.prepareStatement(SQLQuery.SQL_QUERY_COUNT_DISTINCT_DOC.value())) {
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						return  Integer.valueOf(rs.getString("TOTAL"));
					}
				}
			}
		}
		return 0;
	}
}