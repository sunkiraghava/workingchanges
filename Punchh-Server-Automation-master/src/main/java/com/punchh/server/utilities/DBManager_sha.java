package com.punchh.server.utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

// DB Connection Manager (Singleton + ThreadLocal) amit
public class DBManager_sha {
    private static final Logger logger = LogManager.getLogger(DBManager_sha.class);
	// Each env+dbName pair gets its own pool
	private static final Map<String, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();

	// Thread-local connections (safe for parallel runs)
	private static final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();
	 private static String runType;
	 private static BrowserUtilities brw;
	 static {
			brw = new BrowserUtilities();
			runType = brw.getRunType().toLowerCase();
		}
	private DBManager_sha() {
		throw new UnsupportedOperationException("DBManager is a utility class and cannot be instantiated");
	}

	/**
	 * 🧠 Initializes a connection pool for a given environment and database name.
	 */
	private static synchronized HikariDataSource initializePool(String envName, String hostIdentifier) {
		Properties props = Utilities.loadPropertiesFile("dbConfig.properties");
		Utilities utils = new Utilities();
		String instance = getConnectionKey(envName, hostIdentifier);
		if (dataSourceMap.containsKey(instance)) {
			return dataSourceMap.get(instance);
		}
		String dbName = props.getProperty(instance + ".DBName");
		String host = props.getProperty(instance + ".Host");
		String port = props.getProperty(instance + ".Port");
		String params = props.getProperty(instance + ".Params");
		String userName = utils.decrypt(props.getProperty(instance + ".UserName"));
		String password = utils.decrypt(props.getProperty(instance + ".Password"));

		if (dbName == null || dbName.isEmpty()) {
			throw new IllegalArgumentException("DBName not found for: " + instance);
		}

		HikariConfig config = new HikariConfig();
		config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?" + params);
		config.setUsername(userName);
		config.setPassword(password);
		config.setMaximumPoolSize(20);
		config.setMinimumIdle(Math.min(3, 20));
		config.setConnectionTimeout(30000);
		config.setIdleTimeout(120000);
		config.setMaxLifetime(600000);
		config.setLeakDetectionThreshold(20000);
		config.setConnectionTestQuery("SELECT 1");
		config.setPoolName("DBPool-" + instance);

		HikariDataSource ds = new HikariDataSource(config);
		dataSourceMap.put(instance, ds);
		System.out.println("[DBManager] ✅ Initialized pool for " + instance);

		return ds;
	}
	/**
     * Helper method to standardize pool key
     */
    private static String getConnectionKey(String envName, String hostIdentifier) {
    	String instance = Utilities.getInstance(envName);
    	instance = instance+"."+runType;
        return (hostIdentifier != null) ? instance + "." + hostIdentifier : instance;
    }
	/**
	 * 🔌 Returns a thread-local connection for the given env & dbName.
	 */
	public static Connection getConnection(String envName, String hostIdentifier) throws SQLException {
		String key = getConnectionKey(envName, hostIdentifier);

		if (!dataSourceMap.containsKey(key)) {
			initializePool(envName, hostIdentifier);
		}

		Connection conn = connectionThreadLocal.get();
		if (conn == null || conn.isClosed()) {
			conn = dataSourceMap.get(key).getConnection();
			connectionThreadLocal.set(conn);
		}
		return conn;
	}

	/**
	 * 🧹 Closes the thread-local connection (returns to pool).
	 */
	public static void closeConnection() {
		Connection conn = connectionThreadLocal.get();
		try {
			if (conn != null && !conn.isClosed())
				conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			connectionThreadLocal.remove();
		}
	}

	/**
	 * 🧨 Closes all HikariCP pools at the end of suite.
	 */
	public static void shutdownAllPools() {
		for (Map.Entry<String, HikariDataSource> entry : dataSourceMap.entrySet()) {
			try {
				entry.getValue().close();
				System.out.println("[DBManager] 🔻 Pool closed for " + entry.getKey());
			} catch (Exception ignored) {
			}
		}
		dataSourceMap.clear();
	}
	  public static String executeQueryAndGetColumnValue(String envName, String sql, String colName) throws Exception {
			return executeQueryAndGetColumnValue(envName, null, sql, colName);
		}
	    public static String executeQueryAndGetColumnValue(String envName, String hostIdentifier, String sql,
				String colName) throws Exception {
			String value = "";
			logger.info(
					"Executing query: " + sql + " on: " + envName + (hostIdentifier != null ? "." + hostIdentifier : ""));
			if (TestListeners.extentTest.get() != null)
				TestListeners.extentTest.get().info("Executing query: " + sql + " on: " + envName
						+ (hostIdentifier != null ? "." + hostIdentifier : ""));
			Connection conn =  getConnection(envName, hostIdentifier);
			try {
				Statement stmt = conn.createStatement();
				ResultSet resultSet = stmt.executeQuery(sql);
				if (resultSet.next()) {
					value = resultSet.getString(colName);
					logger.info("Retrieved value '" + value + "' from the column '" + colName + "'");
					if (TestListeners.extentTest.get() != null)
						TestListeners.extentTest.get()
								.info("Retrieved value '" + value + "' from the column '" + colName + "'");
//					closeConnection();
					return value;
				}
				logger.info("No value found for the query: " + sql);
				if (TestListeners.extentTest.get() != null)
					TestListeners.extentTest.get().info("No value found for the query: " + sql);
			} finally {
				closeConnection();
			}
			
			return value;
		}
	    
	 // Use this to run Update query
	 	public static int executeUpdateQuery(String envName, String sqlQuery) throws Exception {
	 		return executeUpdateQuery(envName, null, sqlQuery);
	 	}

	 	public static int executeUpdateQuery(String envName, String hostIdentifier, String sqlQuery) throws Exception {
	 		Connection conn = getConnection(envName, hostIdentifier);
	 		Statement stmt = conn.createStatement();
	 		int rs = stmt.executeUpdate(sqlQuery);
	 		String dbInfo = envName + (hostIdentifier != null ? "." + hostIdentifier : "");
	 		if (rs == 1) {
	 			logger.info(sqlQuery + " is executed successfully on: " + dbInfo);
	 			TestListeners.extentTest.get().info(sqlQuery + " is executed successfully on: " + dbInfo);
	 			closeConnection();
//	 			SingletonDBUtils_new.closeConnection();
	 			return rs;
	 		} else {
	 			logger.info("Unsucessful execution of  " + sqlQuery + " on: " + dbInfo);
	 			TestListeners.extentTest.get().info("Unsucessful execution of  " + sqlQuery + " on: " + dbInfo);
	 			closeConnection();
	 			//SingletonDBUtils_new.closeConnection();
	 			return rs;
	 		}
	 	}
}