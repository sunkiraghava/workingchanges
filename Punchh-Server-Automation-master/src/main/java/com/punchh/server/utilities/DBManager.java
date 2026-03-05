package com.punchh.server.utilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

public class DBManager {
	public static int counter = 0;
	private static final Logger logger = LogManager.getLogger(DBManager.class);
	// Map of named DB pools (punch_production, guestIdentity)
	private static final Map<String, HikariDataSource> dataSourceMap = new ConcurrentHashMap<>();

	// Thread-local connections (safe for parallel runs)
	private static final ThreadLocal<Connection> connectionThreadLocal = new ThreadLocal<>();
	private static BrowserUtilities brw;
	private static String runType;
	private static String localuserName;

	private DBManager() {
	}

	static {
		brw = new BrowserUtilities();
		runType = brw.getRunType().toLowerCase();
		if (runType.equalsIgnoreCase("local")) {
			localuserName = getTshLoginUserEmail();
		}
	}

	/**
	 * Get or create a connection for the given DB key. Maintains per-DB limits and
	 * a global cap of 40.
	 */
	/**
	 * Main method to get connection (reuses existing pools)
	 */
	public static synchronized Connection getConnection(String envName, String hostIdentifier) throws Exception {
		String connectionKey = getConnectionKey(envName, hostIdentifier);
		Connection conn;
		if (!dataSourceMap.containsKey(connectionKey)) {
			createDataSource(envName, hostIdentifier);
		}
		conn = connectionThreadLocal.get();
		if (conn == null || conn.isClosed()) {
			conn = dataSourceMap.get(connectionKey).getConnection(); // Borrow from pool
			connectionThreadLocal.set(conn);
		}
		return conn;
	}

	/**
	 * Helper method to standardize pool key
	 */
	private static String getConnectionKey(String envName, String hostIdentifier) {
		String instance = Utilities.getInstance(envName);
		instance = instance + "." + runType;
		return (hostIdentifier != null) ? instance + "." + hostIdentifier : instance;
	}

	/**
	 * Create (once per key) a Hikari DataSource
	 */
	private static HikariDataSource createDataSource(String envName, String hostIdentifier) throws Exception {
		Properties props = Utilities.loadPropertiesFile("dbConfig.properties");
		Utilities utils = new Utilities();
		String instance = getConnectionKey(envName, hostIdentifier);
		String userName, password = null;
		if (dataSourceMap.containsKey(instance)) {
			return dataSourceMap.get(instance);
		}
		String dbName = props.getProperty(instance + ".DBName");
		String host = props.getProperty(instance + ".Host");
		String port = props.getProperty(instance + ".Port");
		String params = props.getProperty(instance + ".Params");

		if (runType.equalsIgnoreCase("local")) {
			userName = localuserName;
		} else {
			userName = utils.decrypt(props.getProperty(instance + ".UserName"));
			password = utils.decrypt(props.getProperty(instance + ".Password"));
		}

		if (dbName == null || dbName.isEmpty()) {
			throw new IllegalArgumentException("DBName not found for: " + instance);
		}
		int poolSize = getPoolSizeForDB(instance);
		HikariConfig config = new HikariConfig();

		String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?" + params;
		config.setJdbcUrl(jdbcUrl);
		config.setUsername(userName);
		config.setPassword(password);
		config.setMaximumPoolSize(poolSize);
		config.setMinimumIdle(Math.min(3, poolSize));
		config.setConnectionTimeout(30000);
		config.setIdleTimeout(120000);
		config.setMaxLifetime(600000);
		config.setLeakDetectionThreshold(20000);
		config.setConnectionTestQuery("SELECT 1");
		config.setPoolName("DBPool-" + instance);

		HikariDataSource ds = new HikariDataSource(config);
		dataSourceMap.put(instance, ds);
		logger.info("[DB]Created new pool for key: " + instance + " with size " + poolSize);
		return ds;
	}

	/**
	 * Determine per-DB pool size
	 */
	private static int getPoolSizeForDB(String hostIdentifier) {
		if (hostIdentifier.toLowerCase().contains("guestIdentity")) {
			return 5;
		} else {
			return 5; // default safety net
		}
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
		Connection conn = getConnection(envName, hostIdentifier);
		try {
			Statement stmt = conn.createStatement();
			ResultSet resultSet = stmt.executeQuery(sql);
			if (resultSet.next()) {
				value = resultSet.getString(colName);
				logger.info("Retrieved value '" + value + "' from the column '" + colName + "'");
				if (TestListeners.extentTest.get() != null)
					TestListeners.extentTest.get()
							.info("Retrieved value '" + value + "' from the column '" + colName + "'");
				closeConnection();
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
		try {
			Statement stmt = conn.createStatement();
			int rs = stmt.executeUpdate(sqlQuery);
			String dbInfo = envName + (hostIdentifier != null ? "." + hostIdentifier : "");
			if (rs > 0) {
				logger.info(sqlQuery + " is executed successfully on: " + dbInfo);
				TestListeners.extentTest.get().info(sqlQuery + " is executed successfully on: " + dbInfo);
				return rs;
			} else {
				logger.info("Unsucessful execution of  " + sqlQuery + " on: " + dbInfo);
				TestListeners.extentTest.get().info("Unsucessful execution of  " + sqlQuery + " on: " + dbInfo);
				return rs;
			}
		} finally {
			closeConnection();
		}

	}

	public static String getTshLoginUserEmail() {

		String line;
		String email = null;
		// Set full path to tsh executable
		// Example Linux/Mac: "/usr/local/bin/tsh"
		// Example Windows: "C:\\Program Files\\Teleport\\tsh.exe"
		String tshPath = "/usr/local/bin/tsh";

		try {
			// Command to run tsh login
			ProcessBuilder pb = new ProcessBuilder(tshPath, "login", "--proxy=par-nonprod.teleport.sh:443",
					"par-nonprod.teleport.sh");

			pb.redirectErrorStream(true);
			Process process = pb.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			// System.out.println("===== TSH OUTPUT START =====");

			while ((line = reader.readLine()) != null) {
				// System.out.println(line);

				// Simple generic email pattern finder
				if (line.contains("@")) {
					String[] words = line.split("\\s+");
					for (String word : words) {
						if (word.contains("@") && word.contains(".")) {
							email = word.replace(",", "").trim();
						}
					}
				}
			}

			// System.out.println("===== TSH OUTPUT END =====");

			int exitCode = process.waitFor();
			// System.out.println("Process Exit Code: " + exitCode);

			if (email != null) {
				System.out.println("Extracted Email: " + email);
			} else {
				System.out.println("No email found in output.");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return email;
	}

}
