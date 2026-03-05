package com.punchh.server.utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Listeners;

//Author - Shashank Sharma 
@Listeners(TestListeners.class)
public class SingletonDBUtils {

	static Logger logger = LogManager.getLogger(SingletonDBUtils.class);

	// Connection pool to support multiple databases per environment
	private static Map<String, Connection> connectionPool = new ConcurrentHashMap<>();

	public static Properties prop;
	public static Utilities utils;
	public static String host, port, userName, password = null, params, dbName;
	private static final Object LOCK = new Object();
	public static BrowserUtilities brw = null;

	public SingletonDBUtils() {
		prop = Utilities.loadPropertiesFile("dbConfig.properties");
		utils = new Utilities();
	}

//	public static Connection getDBConnection(String envName) throws Exception {
//		return getDBConnection(envName, null);
//	}

	/**
	 * Get database connection for environment
	 * 
	 * @param envName        Environment name - can be "pp", "qa" (uses default
	 *                       host)
	 * @param hostIdentifier Host identifier (e.g., "guestIdentity") - null for
	 *                       default host, "pp.guestIdentity" (Different host)
	 * @return Database connection
	 * @throws Exception if connection fails
	 */
//	public static Connection getDBConnection(String envName, String hostIdentifier) throws Exception {
//		brw = new BrowserUtilities();
//		String runType = brw.getRunType();
//		prop = Utilities.loadPropertiesFile("dbConfig.properties");
//		if (utils == null) {
//			utils = new Utilities();
//		}
//
//		String instance;
//		String actualDbName;
//		String connectionKey;
//
//		if (hostIdentifier != null) {
//			// For specific host like "pp.guestIdentity"
//			instance = Utilities.getInstance(envName) + "." + hostIdentifier;
//		} else {
//			// For default host like "pp"
//			instance = Utilities.getInstance(envName);
//		}
//
//		connectionKey = instance;
//		// Check if connection exists and is valid, remove if invalid
//		Connection connection = connectionPool.get(connectionKey);
//		if (connection != null) {
//			if (!connection.isClosed()) {
//				return connection;
//			} else {
//				// Remove invalid connection from pool
//				connectionPool.remove(connectionKey);
//			}
//		}
//
//		if (envName.equalsIgnoreCase("pp") || envName.equalsIgnoreCase("qa")) {
//			instance = instance + "." + runType;
//		} else {
//			instance = Utilities.getInstance(envName);
//		}
//
//		actualDbName = prop.getProperty(instance + ".DBName");
//		try {
//			host = prop.getProperty(instance + ".Host");
//			port = prop.getProperty(instance + ".Port");
//			params = prop.getProperty(instance + ".Params");
//			userName = utils.decrypt(prop.getProperty(instance + ".UserName"));
//			password = utils.decrypt(prop.getProperty(instance + ".Password"));
//
//			if (actualDbName == null || actualDbName.isEmpty()) {
//				throw new IllegalArgumentException("Database name not found for " + connectionKey
//						+ ". Expected property: " + instance + ".DBName");
//			}
//		} catch (Exception ne) {
//			logger.error(connectionKey + " DB details not found in DB config file. Instance: " + instance);
//			TestListeners.extentTest.get()
//					.fail(connectionKey + " DB details not found in DB config file. Instance: " + instance);
//			Assert.fail(connectionKey + " env DB details not found in DB config file ");
//		}
//
//		// Create new connection
//		Class.forName("com.mysql.cj.jdbc.Driver");
//		String db_URL = "jdbc:mysql://" + host + ":" + port + "/" + actualDbName + "?" + params;
//		connection = DriverManager.getConnection(db_URL, userName, password);
//
//		// Store in connection pool
//		connectionPool.put(connectionKey, connection);
//
//		logger.info("Created new database connection for " + connectionKey);
//		if (TestListeners.extentTest.get() != null) {
//			TestListeners.extentTest.get().info("Created new database connection for " + connectionKey);
//		}
//		return connection;
//	}

//	public static String executeQueryAndGetColumnValue(String envName, String sql, String colName) throws Exception {
//		return executeQueryAndGetColumnValue(envName, null, sql, colName);
//	}

//	public static String executeQueryAndGetColumnValue(String envName, String hostIdentifier, String sql,
//			String colName) throws Exception {
//		String value = "";
//		logger.info(
//				"Executing query: " + sql + " on: " + envName + (hostIdentifier != null ? "." + hostIdentifier : ""));
//		if (TestListeners.extentTest.get() != null)
//			TestListeners.extentTest.get().info("Executing query: " + sql + " on: " + envName
//					+ (hostIdentifier != null ? "." + hostIdentifier : ""));
//		Connection conn =  getDBConnection(envName, hostIdentifier);
//		Statement stmt = conn.createStatement();
//		ResultSet resultSet = stmt.executeQuery(sql);
//		if (resultSet.next()) {
//			value = resultSet.getString(colName);
//			logger.info("Retrieved value '" + value + "' from the column '" + colName + "'");
//			if (TestListeners.extentTest.get() != null)
//				TestListeners.extentTest.get()
//						.info("Retrieved value '" + value + "' from the column '" + colName + "'");
//
//			return value;
//		}
//		logger.info("No value found for the query: " + sql);
//		if (TestListeners.extentTest.get() != null)
//			TestListeners.extentTest.get().info("No value found for the query: " + sql);
//
//		return value;
//	}

//	// Use this to run Update query
//	public static int executeUpdateQuery(String envName, String sqlQuery) throws Exception {
//		return executeUpdateQuery(envName, null, sqlQuery);
//	}

//	public static int executeUpdateQuery(String envName, String hostIdentifier, String sqlQuery) throws Exception {
//		Connection conn = getDBConnection(envName, hostIdentifier);
//		Statement stmt = conn.createStatement();
//		int rs = stmt.executeUpdate(sqlQuery);
//		String dbInfo = envName + (hostIdentifier != null ? "." + hostIdentifier : "");
//		if (rs == 1) {
//			logger.info(sqlQuery + " is executed successfully on: " + dbInfo);
//			TestListeners.extentTest.get().info(sqlQuery + " is executed successfully on: " + dbInfo);
//			return rs;
//		} else {
//			logger.info("Unsucessful execution of  " + sqlQuery + " on: " + dbInfo);
//			TestListeners.extentTest.get().info("Unsucessful execution of  " + sqlQuery + " on: " + dbInfo);
//			return rs;
//		}
//	}

	/**
	 * Close all database connections in the pool
	 * 
	 * @throws SQLException if any connection close fails
	 */
//	public static void closeAllConnections() throws SQLException {
//		for (Map.Entry<String, Connection> entry : connectionPool.entrySet()) {
//			Connection connection = entry.getValue();
//			if (connection != null && !connection.isClosed()) {
//				connection.close();
//				logger.info("DB Connection closed successfully for " + entry.getKey());
//			}
//		}
//		connectionPool.clear();
//		logger.info("All DB connections closed successfully");
//	}

//	public static String businessesPreferenceFlag(String expColValue, String flagName) {
//		String flag = "";
//		if (expColValue.contains(":" + flagName + ": false")) {
//			flag = "false";
//		} else if (expColValue.contains(":" + flagName + ": true")) {
//			flag = "true";
//		}
//		return flag;
//	}

//	public static boolean updateBusinessesPreference(String envName, String expColValue, String choice, String flagName,
//			String b_id) throws Exception {
//		boolean flagIndicator = false;
//		String currentFlagValue = businessesPreferenceFlag(expColValue, flagName);
//		// ✅ Step 1: If key doesn't exist, add it first
//		if (currentFlagValue == null || currentFlagValue.isEmpty()) {
//			String addKeyQuery = "UPDATE businesses " + "SET preferences = CONCAT(preferences, ':" + flagName + ": "
//					+ choice + "') " + "WHERE id = " + b_id;
//			int addedRows = executeUpdateQuery(envName, addKeyQuery);
//			Assert.assertEquals(addedRows, 1, "Failed to add missing flag: " + flagName);
//			flagIndicator = true;
//			return flagIndicator;
//		}
//
//		// Step 2: Compare and update if necessary
//		if (currentFlagValue.equalsIgnoreCase(choice)) {
//			// No update needed
//			flagIndicator = true;
//		} else {
//			String updateQuery = "UPDATE businesses SET preferences = REPLACE(preferences, ':" + flagName + ": "
//					+ currentFlagValue + "', ':" + flagName + ": " + choice + "') WHERE id = " + b_id;
//			int updatedRows = executeUpdateQuery(envName, updateQuery);
//			Assert.assertEquals(updatedRows, 1, "Failed to update flag " + flagName + " to " + choice);
//			flagIndicator = true;
//		}
//
//		return flagIndicator;
//	}

//	public static String executeQueryAndGetColumnValuePollingUsed(String envName, String sql, String colName, int n)
//			throws Exception {
//		return executeQueryAndGetColumnValuePollingUsed(envName, null, sql, colName, n);
//	}
	/*
	 * That’s the root cause of the error: → You created the Statement once, outside
	 * the loop. → The ResultSet inside the loop was never closed. → MySQL driver
	 * sometimes auto-closes the Statement after the ResultSet becomes stale → Next
	 * iteration throws: java.sql.SQLException: No operations allowed after
	 * statement closed.
	 */

	/*
	 * public static String executeQueryAndGetColumnValuePollingUsed(String envName,
	 * String hostIdentifier, String sql, String colName, int n) throws Exception {
	 * String dbInfo = envName + (hostIdentifier != null ? "." + hostIdentifier :
	 * ""); logger.info("Executing query using polling: " + sql + " on: " + dbInfo);
	 * TestListeners.extentTest.get().info("Executing query using polling: " + sql +
	 * " on: " + dbInfo);
	 * 
	 * String value = ""; Connection conn = getDBConnection(envName,
	 * hostIdentifier); Statement stmt = conn.createStatement(); int attempts = 0;
	 * 
	 * while (attempts < n) { logger.info("Attempt " + (attempts + 1) + " of " + n +
	 * " for query on: " + dbInfo); TestListeners.extentTest.get().info("Attempt " +
	 * (attempts + 1) + " of " + n + " for query on: " + dbInfo); ResultSet
	 * resultSet = stmt.executeQuery(sql); if (resultSet.next()) { value =
	 * resultSet.getString(colName); if (value != null) {
	 * logger.info("Success: Retrieved value '" + value + "' from column '" +
	 * colName + "' on: " + dbInfo); TestListeners.extentTest.get().info(
	 * "Success: Retrieved value '" + value + "' from column '" + colName + "' on: "
	 * + dbInfo); break; } else logger.info( "Column '" + colName +
	 * "'  returned null on attempt " + (attempts + 1) + ". Retrying...");
	 * TestListeners.extentTest.get() .info("Column '" + colName +
	 * "'  returned null on attempt " + (attempts + 1) + ". Retrying..."); }
	 * logger.info("No result found on attempt : " + (attempts + 1));
	 * TestListeners.extentTest.get().info("No result found on attempt : " +
	 * (attempts + 1)); attempts++; if (attempts < n) {
	 * logger.info("Waiting 3 seconds before next polling attempt...");
	 * TestListeners.extentTest.get().
	 * info("Waiting 3 seconds before next polling attempt...");
	 * Utilities.longWait(3000); } } logger.info("Query result is: " + value +
	 * " on: " + dbInfo); TestListeners.extentTest.get().info("Query result is: " +
	 * value + " on: " + dbInfo); return value; }
	 */

	/*
	 * Key improvements Use try-with-resources for Statement and ResultSet to ensure
	 * they close after each attempt. Recreate the Statement inside the loop, not
	 * outside — that prevents reuse of a potentially closed statement. Keeps the
	 * same Connection alive (safe if it’s pooled and short-lived). Adds better
	 * error handling and logs.
	 */

//	public static String executeQueryAndGetColumnValuePollingUsed(String envName, String hostIdentifier, String sql,
//			String colName, int n) throws Exception {
//
//		String dbInfo = envName + (hostIdentifier != null ? "." + hostIdentifier : "");
//		logger.info("Executing query using polling: " + sql + " on: " + dbInfo);
//		TestListeners.extentTest.get().info("Executing query using polling: " + sql + " on: " + dbInfo);
//
//		String value = "";
//		int attempts = 0;
//
//		while (attempts < n) {
//			try (Connection conn = getDBConnection(envName, hostIdentifier);
//					Statement stmt = conn.createStatement();
//					ResultSet resultSet = stmt.executeQuery(sql)) {
//
//				logger.info("Attempt " + (attempts + 1) + " of " + n + " for query on: " + dbInfo);
//				TestListeners.extentTest.get()
//						.info("Attempt " + (attempts + 1) + " of " + n + " for query on: " + dbInfo);
//
//				if (resultSet.next()) {
//					value = resultSet.getString(colName);
//					if (value != null) {
//						logger.info("Success: Retrieved value '" + value + "' from column '" + colName + "'");
//						TestListeners.extentTest.get()
//								.info("Success: Retrieved value '" + value + "' from column '" + colName + "'");
//						break;
//					}
//				} else {
//					logger.info("No result found on attempt: " + (attempts + 1));
//				}
//
//			} catch (SQLException e) {
//				logger.error("SQL exception on attempt " + (attempts + 1) + " for " + dbInfo, e);
//				TestListeners.extentTest.get()
//						.fail("SQL exception on attempt " + (attempts + 1) + ": " + e.getMessage());
//			}
//
//			attempts++;
//			if (attempts < n) {
//				logger.info("Waiting 3 seconds before next polling attempt...");
//				Utilities.longWait(3000);
//			}
//		}
//
//		logger.info("Query result is: " + value + " on: " + dbInfo);
//		return value;
//	}

//	public static ResultSet getResultSet(String envName, String sqlQuery) throws Exception {
//		return getResultSet(envName, null, sqlQuery);
//	}
//
//	public static ResultSet getResultSet(String envName, String hostIdentifier, String sqlQuery) throws Exception {
//		Connection conn = getDBConnection(envName, hostIdentifier);
//		String dbInfo = envName + (hostIdentifier != null ? "." + hostIdentifier : "");
//		logger.info("Using query ==> " + sqlQuery + " on: " + dbInfo);
//		TestListeners.extentTest.get().info("Using query ==> " + sqlQuery + " on: " + dbInfo);
//		Statement stmt = conn.createStatement();
//		ResultSet resultSet = stmt.executeQuery(sqlQuery);
//		return resultSet;
//	}

//	public static boolean verifyValueFromDBUsingPolling(String environment, String query, String colName,
//			String expectedValue) throws Exception {
//		logger.info("Using query ==> " + query);
//		TestListeners.extentTest.get().info("Using query ==> " + query);
//		boolean value = false;
//		int counter = 0;
//		do {
//			String statusColValue = executeQueryAndGetColumnValue(environment, query, colName);
//			if (statusColValue.equalsIgnoreCase(expectedValue)) {
//				Assert.assertEquals(statusColValue, expectedValue);
//				logger.info("After updating the UI the value is also updated in the DB");
//				TestListeners.extentTest.get().pass("After updating the UI the value is also updated in the DB");
//				value = true;
//				return value;
//			}
//			counter++;
//			Utilities.longWait(1000);
//			if (counter == 19) {
//				Assert.assertEquals(statusColValue, expectedValue,
//						"even after 20 seconds the value is not updated in the db");
//				logger.warn("Even after 20 seconds the value is not updated in the db");
//				TestListeners.extentTest.get().fail("Even after 20 seconds the value is not updated in the db");
//			}
//		} while (counter < 20);
//		return value;
//	}

//	// Using this to execute delete query
//	public static boolean executeQuery(String envName, String sqlQuery) throws Exception {
//		return executeQuery(envName, null, sqlQuery);
//	}
//
//	public static boolean executeQuery(String envName, String hostIdentifier, String sqlQuery) throws Exception {
//		String dbInfo = envName + (hostIdentifier != null ? "." + hostIdentifier : "");
//		logger.info("Using query ==> " + sqlQuery + " on: " + dbInfo);
//		TestListeners.extentTest.get().info("Using query ==> " + sqlQuery + " on: " + dbInfo);
//		Connection conn = getDBConnection(envName, hostIdentifier);
//		Statement stmt = conn.createStatement();
//		int resultSet = stmt.executeUpdate(sqlQuery);
//		if (resultSet >= 1) {
//			logger.info(sqlQuery + " is executed successfully on: " + dbInfo);
//			TestListeners.extentTest.get().info(sqlQuery + " is executed successfully on: " + dbInfo);
//			return true;
//		} else {
//			logger.info("Unsucessful execution of  " + sqlQuery + " on: " + dbInfo);
//			TestListeners.extentTest.get().info("Unsucessful execution of  " + sqlQuery + " on: " + dbInfo);
//			return false;
//		}
//	}

//	public static List<String> getValueFromColumnInList(String envName, String sqlQuery, String columnName)
//			throws Exception {
//		Connection conn = getDBConnection(envName);
//		TestListeners.extentTest.get().info("Executing query: " + sqlQuery);
//		logger.info("Using query ==> " + sqlQuery);
//		Statement stmt = conn.createStatement();
//		List<String> listOfValues = new ArrayList<String>();
//		try (ResultSet resultSet = stmt.executeQuery(sqlQuery)) {
//			while (resultSet.next()) {
//				String value = resultSet.getString(columnName);
//				TestListeners.extentTest.get()
//						.info("Retrieved value '" + value + "' from the column '" + columnName + "'");
//				logger.info("Retrieved value '" + value + "' from the column '" + columnName + "'");
//				listOfValues.add(value);
//			}
//
//		} catch (SQLException e) {
//			TestListeners.extentTest.get().info("Error executing the select statement: " + sqlQuery);
//			logger.error("Error executing the select statement: " + sqlQuery, e);
//			throw e;
//		}
//		return listOfValues;
//	}

	

//	public boolean verifyColumnExistsInQuery(String envName, String query, String expectedColumn) {
//		logger.info("Running query to verify column: " + query);
//		boolean value = false;
//		// retrieve column name from the query and check if it exists
//		try (Connection connection = getDBConnection(envName);
//				Statement stmt = connection.createStatement();
//				ResultSet rs = stmt.executeQuery(query);) {
//			ResultSetMetaData metaData = rs.getMetaData();
//			int columnCount = metaData.getColumnCount();
//
//			for (int i = 1; i <= columnCount; i++) {
//				String columnName = metaData.getColumnName(i);
//				if (columnName.equalsIgnoreCase(expectedColumn)) {
//					logger.info("Found expected column: " + expectedColumn);
//					TestListeners.extentTest.get().pass("Found expected column: " + expectedColumn);
//					value = true;
//					break;
//				}
//			}
//
//			if (!value) {
//				logger.error("Column not found: " + expectedColumn);
//				TestListeners.extentTest.get().fail("Column not found: " + expectedColumn);
//			}
//
//		} catch (Exception e) {
//			logger.error("Error while checking column: " + e.getMessage());
//			TestListeners.extentTest.get().fail("Exception: " + e.getMessage());
//			e.printStackTrace();
//		}
//		return value;
//	}
//
//	public boolean verifyDBValueWithPolling(String environment, String query, String colName, String expectedValue,
//			int interval, int maxAttempts) throws Exception {
//
//		logger.info("Using query ==> " + query);
//		int attempt = 0;
//
//		while (attempt < maxAttempts) {
//			String actualValue = executeQueryAndGetColumnValue(environment, query, colName);
//
//			if (expectedValue.equalsIgnoreCase(String.valueOf(actualValue))) {
//				logger.info("After updating the UI, the value is also updated in the DB");
//				return true;
//			}
//
//			attempt++;
//			if (attempt < maxAttempts) {
//				utils.longWaitInSeconds(interval);
//			}
//		}
//
//		String message = String.format("Even after %d seconds, the value is not updated in the DB",
//				interval * maxAttempts);
//		logger.info(message);
//		return false;
//	}

//	public static List<Map<String, String>> executeQueryAndGetMultipleColumns(String envName, String sql,
//			String[] colNames) throws Exception {
//		List<Map<String, String>> result = new ArrayList<>();
//		logger.info("Executing query: " + sql);
//		TestListeners.extentTest.get().info("Executing query: " + sql);
//
//		try (Connection conn = getDBConnection(envName);
//				Statement stmt = conn.createStatement();
//				ResultSet resultSet = stmt.executeQuery(sql)) {
//
//			while (resultSet.next()) {
//				Map<String, String> values = new HashMap<>();
//				for (String columnName : colNames) {
//					values.put(columnName, resultSet.getString(columnName));
//				}
//				result.add(values);
//			}
//		}
//
//		if (result.isEmpty()) {
//			logger.info("No value found for the query: " + sql);
//			TestListeners.extentTest.get().info("No value found for the query: " + sql);
//		} else {
//			logger.info("Retrieved fields map: " + result);
//			TestListeners.extentTest.get().info("Retrieved fields map: " + result);
//		}
//
//		return result;
//	}

//	public static Map<String, String> parseDetailsToMap(String details) {
//		Map<String, String> detailsMap = new HashMap<>();
//		for (String line : details.split("\n")) {
//			String[] parts = line.split(":", 2);
//			if (parts.length == 2) {
//				String key = parts[0].trim();
//				String value = parts[1].trim().replaceAll("^'+|'+$", "");
//				detailsMap.put(key, value);
//			}
//		}
//		return detailsMap;
//	}

//	public static void updateDateToDetermineMembershipLevel(String env, String expColValue, String date, String b_id,
//			String flagName) throws Exception {
//		int rs = 0;
//		List<String> dateToDetermineMembershipLevel = Utilities.getPreferencesKeyValue(expColValue, flagName);
//		String dateToDetermineMembershipLevelVal = dateToDetermineMembershipLevel.get(0).replace("[", "")
//				.replace("]", "").replace("'", "");
//
//		if (!(dateToDetermineMembershipLevelVal.equalsIgnoreCase(date))) {
//			String dateToDetermineMembershipLevelquery = "UPDATE businesses SET preferences = REPLACE(preferences, "
//					+ "':" + flagName + ": ''" + dateToDetermineMembershipLevelVal + "''', " + "':" + flagName + ": ''"
//					+ date + "''') " + "WHERE preferences LIKE '%:" + flagName + ": ''"
//					+ dateToDetermineMembershipLevelVal + "''%' " + "AND id = " + b_id;
//
//			rs = DBUtils.executeUpdateQuery(env, dateToDetermineMembershipLevelquery);
//			Assert.assertEquals(rs, 1);
//			logger.info(
//					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
//			TestListeners.extentTest.get().info(
//					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
//		}
//	}

//	public static boolean updateSingleValueForGivenParameter(String envName, String expColValue, String keyValue,
//			String flagName, String b_id) throws Exception {
//		boolean flagIndicator = false;
//
//// Get existing value for the key
//		List<String> existingValues =Utilities.getPreferencesKeyValue(expColValue, flagName);
//		String currentValue = (existingValues == null || existingValues.isEmpty()) ? null : existingValues.get(0);
//
//// ✅ Step 1: If key doesn't exist, add it first
//		if (currentValue == null || currentValue.isEmpty()) {
//			String addKeyQuery = "UPDATE businesses " + "SET preferences = CONCAT(preferences, ':" + flagName + ": "
//					+ keyValue + "') " + "WHERE id = " + b_id;
//			int addedRows = executeUpdateQuery(envName, addKeyQuery);
//			Assert.assertEquals(addedRows, 1, "Failed to add missing key: " + flagName);
//			flagIndicator = true;
//			return flagIndicator;
//		}
//
//// ✅ Step 2: If current value is different, update it
//		if (!currentValue.equalsIgnoreCase(keyValue)) {
//			String updateQuery = "UPDATE businesses SET preferences = REPLACE(preferences, ':" + flagName + ": "
//					+ currentValue + "', ':" + flagName + ": " + keyValue + "') WHERE id = " + b_id;
//			int updatedRows = executeUpdateQuery(envName, updateQuery);
//			Assert.assertEquals(updatedRows, 1, "Failed to update key value for " + flagName);
//			flagIndicator = true;
//		} else {
//// ✅ Step 3: Already correct — no update needed
//			flagIndicator = true;
//		}
//
//		return flagIndicator;
//	}

//	public boolean verifyEmailSubscriptionInDB(String envName, String sqlQuery, String email) {
//		logger.info("Executing query to verify alert_subscribers: " + sqlQuery);
//		TestListeners.extentTest.get().info("Executing query: " + sqlQuery);
//
//		boolean found = false;
//
//		try {
//			Connection conn = getDBConnection(envName);
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery(sqlQuery);
//
//			if (rs.next()) {
//				// Use the correct column for your table: "preferences" or "preference"
//				String preference = rs.getString("preferences");
//				if (preference != null && !preference.isEmpty()) {
//					// Accepts "alert_subscribers:" and ":alert_subscribers:"
//					Pattern p = Pattern.compile("(?im)^\\s*:?alert_subscribers\\s*:\\s*(.*)$");
//					Matcher m = p.matcher(preference);
//					List<String> emails = new ArrayList<>();
//
//					if (m.find()) {
//						String tail = m.group(1).trim();
//
//						if (!tail.isEmpty()) {
//							tail = tail.replace("[", "").replace("]", "");
//							for (String part : tail.split(",")) {
//								String s = part.trim().replaceAll("^['\"]|['\"]$", "");
//								if (!s.isEmpty())
//									emails.add(s);
//							}
//						} else {
//							// Block list with '-'
//							try (Scanner sc = new Scanner(preference.substring(m.end()))) {
//								while (sc.hasNextLine()) {
//									String line = sc.nextLine();
//									if (line.matches("^\\s*[^\\s:#][^:]*:\\s*.*$"))
//										break;
//									Matcher item = Pattern.compile("^\\s*-\\s+(.*?)\\s*$").matcher(line);
//									if (item.find()) {
//										String s = item.group(1).trim().replaceAll("^['\"]|['\"]$", "");
//										if (!s.isEmpty())
//											emails.add(s);
//									}
//								}
//							}
//						}
//					}
//
//					// Log the list only
//					logger.info("alert_subscribers found: " + emails);
//					TestListeners.extentTest.get().info("alert_subscribers found: " + emails);
//
//					found = emails.stream().anyMatch(e -> e.equalsIgnoreCase(email));
//				}
//			}
//		} catch (Exception e) {
//			logger.error("Error verifying email subscription: " + e.getMessage(), e);
//			TestListeners.extentTest.get().fail("Exception while verifying email subscription: " + e.getMessage());
//		}
//
//		return found;
//	}

//	public boolean verifyAlertSubscribersEmptyInDB(String envName, String sqlQuery) {
//		try (Connection c = getDBConnection(envName);
//				Statement s = c.createStatement();
//				ResultSet rs = s.executeQuery(sqlQuery)) {
//
//			if (!rs.next()) {
//				logger.info("alert_subscribers: [] (no row) -> treating as empty");
//				return true;
//			}
//
//			String pref = rs.getString("preferences");
//			if (pref == null || pref.isBlank()) {
//				logger.info("alert_subscribers: [] (preferences empty) -> treating as empty");
//				return true;
//			}
//
//			Matcher head = Pattern.compile("(?im)^\\s*:?alert_subscribers\\s*:\\s*(.*)$").matcher(pref);
//			if (!head.find()) {
//				logger.info("alert_subscribers: key missing -> treating as empty");
//				return true; // change to false if key must exist
//			}
//
//			List<String> emails = new ArrayList<>();
//			String tail = head.group(1).trim();
//
//			if (!tail.isEmpty()) {
//				if (tail.startsWith("[") && tail.endsWith("]"))
//					tail = tail.substring(1, tail.length() - 1);
//				for (String part : tail.split(",")) {
//					String v = part.trim().replaceAll("^['\"]|['\"]$", "");
//					if (!v.isEmpty())
//						emails.add(v);
//				}
//			} else {
//				try (Scanner sc = new Scanner(pref.substring(head.end()))) {
//					while (sc.hasNextLine()) {
//						String line = sc.nextLine();
//						if (line.matches("^\\s*[^\\s:#][^:]*:\\s*.*$"))
//							break; // next key
//						Matcher item = Pattern.compile("^\\s*-\\s+(.*?)\\s*$").matcher(line);
//						if (item.find()) {
//							String v = item.group(1).trim().replaceAll("^['\"]|['\"]$", "");
//							if (!v.isEmpty())
//								emails.add(v);
//						}
//					}
//				}
//			}
//
//			if (emails.isEmpty()) {
//				logger.info("alert_subscribers: [] (empty)");
//				return true;
//			} else {
//				logger.error("Email found");
//				logger.error("alert_subscribers list: " + emails);
//				return false;
//			}
//
//		} catch (Exception e) {
//			logger.error("Error verifying empty alert_subscribers: " + e.getMessage(), e);
//			return false;
//		}
//	}

//	public int executeQueryAndGetCount(String envName, String sql) throws Exception {
//		int count = 0;
//		logger.info("Executing query for count: " + sql);
//		TestListeners.extentTest.get().info("Executing query for count: " + sql);
//
//		try (Connection conn = getDBConnection(envName);
//				Statement stmt = conn.createStatement();
//				ResultSet resultSet = stmt.executeQuery(sql)) {
//
//			if (resultSet.next()) {
//				count = resultSet.getInt(1);
//			}
//		}
//
//		logger.info("Retrieved count: " + count);
//		TestListeners.extentTest.get().info("Retrieved count: " + count);
//
//		return count;
//	}

//	public void deleteGiftCardData(String env, String gcid) {
//		try {
//			// 1) Delete from gift_card_versions
//			String deleteFromVersions = "DELETE FROM gift_card_versions WHERE gift_card_id='" + gcid + "';";
//			//executeUpdateQuery(env, deleteFromVersions);
//
//			// 2) Delete from user_cards
//			String deleteFromUserCards = "DELETE FROM user_cards WHERE gift_card_id='" + gcid + "';";
//		//	executeUpdateQuery(env, deleteFromUserCards);
//
//			// 3) Delete from gift_cards
//			String deleteFromGiftCards = "DELETE FROM gift_cards WHERE id='" + gcid + "';";
//		//	executeUpdateQuery(env, deleteFromGiftCards);
//
//			logger.info("Successfully deleted Gift Card data for gift_card_id: " + gcid);
//		} catch (Exception e) {
//			logger.error("Error while deleting Gift Card data for gift_card_id: " + gcid, e);
//			Assert.fail("Failed to delete Gift Card data for gift_card_id: " + gcid);
//		}
//	}

//	public static void closeAllConnectionsUpdated() {
//		synchronized (LOCK) {
//			if (connectionPool == null || connectionPool.isEmpty()) {
//				logger.info("No DB connections to close.");
//				return;
//			}
//
//			for (Map.Entry<String, Connection> entry : connectionPool.entrySet()) {
//				Connection connection = entry.getValue();
//				if (connection != null) {
//					try {
//						if (!connection.isClosed()) {
//							connection.close();
//							logger.info("DB Connection closed successfully for {}", entry.getKey());
//						}
//					} catch (SQLException e) {
//						logger.error("Error closing DB connection for {}", entry.getKey(), e);
//					}
//				}
//			}
//
//			connectionPool.clear();
//			logger.info("All DB connections closed successfully.");
//		}
//	}

	// New method to poll for all expected values in a single row
//	public static boolean pollQueryForExpectedValues(String envName, String sql, String[] colNames, int n,
//			String[] expVals) throws Exception {
//		logger.info("Executing query: " + sql);
//		Connection conn = getDBConnection(envName, null);
//		Statement stmt = conn.createStatement();
//		ResultSet resultSet = null;
//		List<String> actualValues = new ArrayList<>();
//
//		for (int attempt = 0; attempt < n; attempt++) {
//			resultSet = stmt.executeQuery(sql);
//			while (resultSet.next()) {
//				boolean match = true;
//				for (int i = 0; i < colNames.length; i++) {
//					String actual = resultSet.getString(colNames[i]);
//					actualValues.add(actual);
//					String expected = expVals[i];
//					if (!actual.contains(expected)) {
//						match = false;
//						break;
//					}
//				}
//				if (match) {
//					for (int i = 0; i < colNames.length; i++) {
//						logger.info("Column: " + colNames[i] + ", Expected: " + expVals[i] + ", Actual: "
//								+ actualValues.get(i));
//					}
//					logger.info("Match found on attempt " + (attempt + 1));
//					return true;
//				}
//			}
//			if (attempt < n - 1) {
//				logger.info("No match found on attempt " + (attempt + 1) + ", retrying...");
//				TestListeners.extentTest.get().info("No match found on attempt " + (attempt + 1) + ", retrying...");
//				Utilities.longWait(1000); // wait 1 second before next attempt
//			}
//		}
//		for (int i = 0; i < colNames.length; i++) {
//			logger.info("Column: " + colNames[i] + ", Expected: " + expVals[i] + ", Actual: " + actualValues.get(i));
//		}
//		logger.info("No match found after " + n + " attempts.");
//		return false;
//	}

	// Returns the value (true, false, or number) of the specified flag from the
	// preferences string.
//	public static String businessesPreferenceFlagValue(String expColValue, String flagName) {
//		Pattern pattern = Pattern.compile(":" + flagName + ":\\s*([^:,\\s]+)");
//		Matcher matcher = pattern.matcher(expColValue);
//		if (matcher.find()) {
//			return matcher.group(1);
//		}
//		return "";
//	}

//	public void deleteLISQCRedeemable(String env, String actualExternalIdLIS, String actualExternalIdQC,
//			String actualExternalIdRedeemable) throws Exception {
//
//		// // Delete LIS 1
//		String deleteLISQuery1 = "Delete from line_item_selectors where external_id ='" + actualExternalIdLIS + "';";
//		//executeQuery(env, deleteLISQuery1);
//
//		// Delete Qualifying Expressions
//		String getQCIDQuery = "select id from qualification_criteria where external_id ='" + actualExternalIdQC + "';";
//		String qcID = executeQueryAndGetColumnValue(env, getQCIDQuery, "id");
//		String deleteQCFromQualifying_expressions = "delete from qualifying_expressions where qualification_criterion_id ='"
//				+ qcID + "';";
//		//executeQuery(env, deleteQCFromQualifying_expressions);
//
//		// Delete QC
//		String deleteQCFromQualification_criteria = "delete from qualification_criteria where external_id = '"
//				+ actualExternalIdQC + "';";
//		//executeQuery(env, deleteQCFromQualification_criteria);
//
//		// Delete from redeemable tables
//		String deleteRedeemableQuery = "delete from redeemables where uuid ='" + actualExternalIdRedeemable + "';";
//		//executeQuery(env, deleteRedeemableQuery);
//
//		logger.info("LIS, QC and redeemable has been deleted successfully");
//		TestListeners.extentTest.get().pass("LIS, QC and redeemable has been deleted successfully");
//
//	}

//	public static List<Map<String, String>> executeQueryAndGetMultipleColumns(String envName, String hostIdentifier,
//			String sql, String[] colNames) throws Exception {
//		String value = "";
//		logger.info(
//				"Executing query: " + sql + " on: " + envName + (hostIdentifier != null ? "." + hostIdentifier : ""));
//		TestListeners.extentTest.get().info(
//				"Executing query: " + sql + " on: " + envName + (hostIdentifier != null ? "." + hostIdentifier : ""));
//		List<Map<String, String>> result = new ArrayList<>();
//
//		try (Connection conn = getDBConnection(envName, hostIdentifier);
//				Statement stmt = conn.createStatement();
//				ResultSet resultSet = stmt.executeQuery(sql)) {
//
//			while (resultSet.next()) {
//				Map<String, String> values = new HashMap<>();
//				for (String columnName : colNames) {
//					values.put(columnName, resultSet.getString(columnName));
//				}
//				result.add(values);
//			}
//		}
//
//		if (result.isEmpty()) {
//			logger.info("No value found for the query: " + sql);
//			TestListeners.extentTest.get().info("No value found for the query: " + sql);
//		} else {
//			logger.info("Retrieved fields map: " + result);
//			TestListeners.extentTest.get().info("Retrieved fields map: " + result);
//		}
//
//		return result;
//	}
	

	public static void main(String[] args) {
	}
}