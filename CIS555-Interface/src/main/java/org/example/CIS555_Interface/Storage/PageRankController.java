package org.example.CIS555_Interface.Storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import org.example.CIS555_Interface.Tool.SQLQuery;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class PageRankController {
    DataSource ds;

    private static final String DATABASE_NAME = "dev";
    private static final String JDBC_HOST = "cis555www.cwdvkb9sjfn3.us-east-1.rds.amazonaws.com:3306";
    private static final String JDBC_URL  = "jdbc:mysql://" + JDBC_HOST + "/" + DATABASE_NAME + "?autoReconnect=true";
    private static final String JDBC_USER  ="admin";
    private static final String JDBC_PASSWORD = "12345678";

    public PageRankController() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(JDBC_URL);
        config.setUsername(JDBC_USER);
        config.setPassword(JDBC_PASSWORD);
        config.setConnectionTimeout(10000);
        config.setMaximumPoolSize(10);

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

        this.ds = (DataSource) new HikariDataSource(config);
    }

    public List<Double> queryPRbyURLs(List<String> urls) throws SQLException {
        HashMap<String, Double> result = new HashMap<>();
        
        StringBuilder sb = new StringBuilder();
        for (int i =  0; i < urls.size(); i++ ) {
            if (i == 0) {
                sb.append('(');
            }
            sb.append("\"").append(urls.get(i)).append("\"");
            if (i == urls.size() - 1) {
                sb.append(");");
            } else {
                sb.append(',');
            }

        }

        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(SQLQuery.SQL_QUERY_PRS_BY_URLS.value() + sb.toString())) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String url = rs.getString("url");
                        Double rank = rs.getDouble("pageRank");
                        result.put(url, rank);
                    }
                }
            }
        }
        return urls.stream().map(url -> result.getOrDefault(url, -1.0)).collect(Collectors.toList());
    }
}
