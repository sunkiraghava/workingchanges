package com.punchh.server.utilities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

public class DBUtils {
	static Logger logger = LogManager.getLogger(DBUtils.class);

	private static final int DEFAULT_POLL_INTERVAL_MS = 2000; // 2 seconds
	private static final int DEFAULT_TIMEOUT_MS = 60000; // 60 seconds
	public static Utilities utils;

	/*
	 * 🟢 Purpose - Executes a simple SQL SELECT query (no parameters) and returns
	 * all rows as a List of Maps. 🧩 When to Use When you want to fetch multiple
	 * rows and columns from a table — e.g. verifying data after an operation. 📦
	 * Returns Each row as a Map<String, Object> → {columnName=value}.
	 */
	private DBUtils() {
		throw new UnsupportedOperationException("DBUtils is a utility class and cannot be instantiated");
	}

	// ====================================================================================
	// 🔹 Query Execution Utilities
	// ====================================================================================

	/**
	 * 🟢 Purpose Executes a simple SQL SELECT query (no parameters) and returns all
	 * rows as a List of Maps.
	 * 
	 * 🧩 When to Use When you want to fetch multiple rows and columns from a table
	 * — e.g. verifying data after an operation.
	 * 
	 * 📦 Returns Each row as a Map<String, Object> → {columnName=value}.
	 * 
	 * @throws Exception
	 */
//	public static List<Map<String, Object>> executeQuery(String env, String dbName, String query) throws Exception {
//		List<Map<String, Object>> results = new ArrayList<>();
//		Connection conn = DBManager.getConnection(env, dbName);
//
//		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
//
//			ResultSetMetaData meta = rs.getMetaData();
//			int colCount = meta.getColumnCount();
//
//			while (rs.next()) {
//				Map<String, Object> row = new LinkedHashMap<>();
//				for (int i = 1; i <= colCount; i++) {
//					row.put(meta.getColumnLabel(i), rs.getObject(i));
//				}
//				results.add(row);
//			}
//
//		} catch (SQLException e) {
//			System.err.println("[DBUtils] ❌ Query failed on " + env + "-" + dbName + ": " + query);
//			throw e;
//		}
//		return results;
//	}

	/**
	 * 🟢 Purpose Executes INSERT, UPDATE, or DELETE statements and returns number
	 * of affected rows.
	 * 
	 * 🧩 When to Use Use this when you need to modify data in your test setup,
	 * teardown, or validations.
	 * 
	 * 📦 Returns int = count of rows affected.
	 * 
	 * @throws Exception
	 */

	public static int executeUpdate(String env, String dbName, String query) throws Exception {
		Connection conn = DBManager.getConnection(env, dbName);
		try (Statement stmt = conn.createStatement()) {
			return stmt.executeUpdate(query);
		} catch (SQLException e) {
			System.err.println("[DBUtils] ❌ Update failed on " + env + "-" + dbName + ": " + query);
			throw e;
		}
	}

	/**
	 * 🟢 Purpose Executes a parameterized SQL SELECT query using a
	 * PreparedStatement.
	 * 
	 * 🧩 When to Use When your query includes dynamic parameters, e.g. WHERE id=?,
	 * to avoid SQL injection and make reusable queries.
	 * 
	 * 📦 Returns List of rows as List<Map<String, Object>>.
	 * 
	 * @throws Exception
	 */
	public static List<Map<String, Object>> executePreparedQuery(String env, String dbName, String query,
			Object... params) throws Exception {
		List<Map<String, Object>> results = new ArrayList<>();
		Connection conn = DBManager.getConnection(env, dbName);
		try (PreparedStatement ps = conn.prepareStatement(query)) {
			for (int i = 0; i < params.length; i++) {
				ps.setObject(i + 1, params[i]);
			}

			try (ResultSet rs = ps.executeQuery()) {
				ResultSetMetaData meta = rs.getMetaData();
				int colCount = meta.getColumnCount();

				while (rs.next()) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (int i = 1; i <= colCount; i++) {
						row.put(meta.getColumnLabel(i), rs.getObject(i));
					}
					results.add(row);
				}
			}
		} catch (SQLException e) {
			System.err.println("[DBUtils] ❌ Prepared query failed on " + env + "-" + dbName + ": " + query);
			throw e;
		}
		return results;
	}

	// ====================================================================================
	// 🔹 Value and Record Fetch Helpers
	// ====================================================================================

	/**
	 * 🟢 Purpose Fetches a single column value from the first row of a query
	 * result.
	 * 
	 * 🧩 When to Use When you need to verify or use a single scalar value (e.g.,
	 * user email, status, ID) in a test.
	 * 
	 * 📦 Returns String value of the specified column (or null if not found).
	 */
//	public static String getSingleValue(String env, String dbName, String query, String columnName)
//			throws SQLException {
//		List<Map<String, Object>> rows = executeQuery(env, dbName, query);
//		if (!rows.isEmpty() && rows.get(0).containsKey(columnName)) {
//			return String.valueOf(rows.get(0).get(columnName));
//		}
//		return null;
//	}

	// ====================================================================================
	// 🔹 Polling Helpers (for async DB updates)
	// ====================================================================================

	/**
	 * 🟢 Purpose Polls the database until a column reaches an expected value or
	 * timeout occurs.
	 * 
	 * 🧩 When to Use Useful for async updates — e.g., when a background job updates
	 * a record after a few seconds.
	 * 
	 * 📦 Returns true if value matched within timeout; otherwise false.
	 */
//	public static boolean waitForValue(String env, String dbName, String query, String columnName, String expectedValue,
//			int timeoutMs, int intervalMs) throws SQLException, InterruptedException {
//		long start = System.currentTimeMillis();
//		while (System.currentTimeMillis() - start < timeoutMs) {
//			String currentValue = getSingleValue(env, dbName, query, columnName);
//			if (currentValue != null && currentValue.equalsIgnoreCase(expectedValue)) {
//				System.out.println("[DBUtils] ✅ Value matched: " + currentValue);
//				return true;
//			}
//			Thread.sleep(intervalMs);
//		}
//		System.err.println("[DBUtils] ⏰ Timeout waiting for value: expected=" + expectedValue);
//		return false;
//	}
	/*
	 * 🟢 Purpose - Simplified version of the above with default timeout (60s) and
	 * poll interval (2s).
	 */
	/** Simplified version with default timeout (60s) and poll interval (2s). */
//	public static boolean waitForValue(String env, String dbName, String query, String columnName, String expectedValue)
//			throws SQLException, InterruptedException {
//		return waitForValue(env, dbName, query, columnName, expectedValue, DEFAULT_TIMEOUT_MS,
//				DEFAULT_POLL_INTERVAL_MS);
//	}

	/**
	 * 🟢 Purpose Polls until the given query returns at least one record, or
	 * timeout occurs.
	 * 
	 * 🧩 When to Use When testing workflows where a new record should appear after
	 * some delay (e.g., audit entry, job completion).
	 * 
	 * 📦 Returns true if record found; otherwise false.
	 */
//	public static boolean waitForRecord(String env, String dbName, String query, int timeoutMs, int intervalMs)
//			throws SQLException, InterruptedException {
//		long start = System.currentTimeMillis();
//		while (System.currentTimeMillis() - start < timeoutMs) {
//			List<Map<String, Object>> rows = executeQuery(env, dbName, query);
//			if (!rows.isEmpty()) {
//				System.out.println("[DBUtils] ✅ Record found.");
//				return true;
//			}
//			Thread.sleep(intervalMs);
//		}
//		System.err.println("[DBUtils] ⏰ Timeout waiting for record: " + query);
//		return false;
//	}

	/*
	 * 🟢 Purpose - Simplified version with default timeout and poll interval.
	 */
	/** Simplified version with default timeout and poll interval. */
//	public static boolean waitForRecord(String env, String dbName, String query)
//			throws SQLException, InterruptedException {
//		return waitForRecord(env, dbName, query, DEFAULT_TIMEOUT_MS, DEFAULT_POLL_INTERVAL_MS);
//	}

	// Moved from SingletonDBUtils.java class
	// *******************************************
	public static Connection getDBConnection(String envName) throws Exception {
		return DBManager.getConnection(envName, null);
	}

	public static Connection getDBConnection(String envName, String hostidentifier) throws Exception {
		return DBManager.getConnection(envName, hostidentifier);
	}

	public static int executeUpdateQuery(String envName, String sqlQuery) throws Exception {
		return executeUpdateQuery(envName, null, sqlQuery);
	}

	public static int executeUpdateQuery(String envName, String hostIdentifier, String sqlQuery) throws Exception {
		return DBManager.executeUpdateQuery(envName, hostIdentifier, sqlQuery);
	}

	// without host identifier
	public static String executeQueryAndGetColumnValue(String envName, String sql, String colName) throws Exception {
		return executeQueryAndGetColumnValue(envName, null, sql, colName);
	}

	// with host identifier
	public static String executeQueryAndGetColumnValue(String envName, String hostIdentifier, String sql,
			String colName) throws Exception {
		return DBManager.executeQueryAndGetColumnValue(envName, hostIdentifier, sql, colName);
	}

	public static String businessesPreferenceFlag(String expColValue, String flagName) {
		String flag = "";
		if (expColValue.contains(":" + flagName + ": false")) {
			flag = "false";
		} else if (expColValue.contains(":" + flagName + ": true")) {
			flag = "true";
		}
		return flag;
	}

	public static boolean updateBusinessesPreference(String envName, String expColValue, String choice, String flagName,
			String b_id) throws Exception {
		boolean flagIndicator = false;
		String currentFlagValue = businessesPreferenceFlag(expColValue, flagName);
		// ✅ Step 1: If key doesn't exist, add it first
		if (currentFlagValue == null || currentFlagValue.isEmpty()) {
			String addKeyQuery = "UPDATE businesses " + "SET preferences = CONCAT(preferences, ':" + flagName + ": "
					+ choice + "') " + "WHERE id = " + b_id;
			int addedRows = DBManager.executeUpdateQuery(envName, addKeyQuery);
			Assert.assertEquals(addedRows, 1, "Failed to add missing flag: " + flagName);
			flagIndicator = true;
			return flagIndicator;
		}

		// Step 2: Compare and update if necessary
		if (currentFlagValue.equalsIgnoreCase(choice)) {
			// No update needed
			flagIndicator = true;
		} else {
			String updateQuery = "UPDATE businesses SET preferences = REPLACE(preferences, ':" + flagName + ": "
					+ currentFlagValue + "', ':" + flagName + ": " + choice + "') WHERE id = " + b_id;
			int updatedRows = DBManager.executeUpdateQuery(envName, updateQuery);
			Assert.assertEquals(updatedRows, 1, "Failed to update flag " + flagName + " to " + choice);
			flagIndicator = true;
		}

		return flagIndicator;
	}

	public static List<Map<String, String>> executeQueryAndGetMultipleColumns(String envName, String sql,
			String[] colNames) throws Exception {
		List<Map<String, String>> result = new ArrayList<>();
		logger.info("Executing query: " + sql);
		TestListeners.extentTest.get().info("Executing query: " + sql);

		try (Connection conn = getDBConnection(envName);
				Statement stmt = conn.createStatement();
				ResultSet resultSet = stmt.executeQuery(sql)) {

			while (resultSet.next()) {
				Map<String, String> values = new HashMap<>();
				for (String columnName : colNames) {
					values.put(columnName, resultSet.getString(columnName));
				}
				result.add(values);
			}
			DBManager.closeConnection();
		}
		if (result.isEmpty()) {
			logger.info("No value found for the query: " + sql);
			TestListeners.extentTest.get().info("No value found for the query: " + sql);
		} else {
			logger.info("Retrieved fields map: " + result);
			TestListeners.extentTest.get().info("Retrieved fields map: " + result);
		}
		return result;
	}

	public static void updatePreference(String env, String expColValue, String date, String id,
			String flagName,String tableName) throws Exception {
		
		String currentValue = getPreferencesKeyValue(expColValue, flagName);
	    if (currentValue == null) {
	        logger.info("Flag '" + flagName + "' not present in preferences. No update required.");
	        TestListeners.extentTest.get()
	                .info("Flag '" + flagName + "' not present in preferences. No update required.");
	        return;
	    }
	    if (currentValue.equalsIgnoreCase(date)) {
	        logger.info("Preference '" + flagName + "' already set to " + date);
	        TestListeners.extentTest.get()
	                .info("Preference '" + flagName + "' already set to " + date);
	        return;
	    }
		int rs = 0;
		List<String> dateToDetermineMembershipLevel = Utilities.getPreferencesKeyValue(expColValue, flagName);
		String dateToDetermineMembershipLevelVal = dateToDetermineMembershipLevel.get(0).replace("[", "")
				.replace("]", "").replace("'", "");

		if (!(dateToDetermineMembershipLevelVal.equalsIgnoreCase(date))) {
			String dateToDetermineMembershipLevelquery = "UPDATE " + tableName + " SET preferences = REPLACE(preferences, "
					+ "':" + flagName + ": ''" + dateToDetermineMembershipLevelVal + "''', " + "':" + flagName + ": ''"
					+ date + "''') " + "WHERE preferences LIKE '%:" + flagName + ": ''"
					+ dateToDetermineMembershipLevelVal + "''%' " + "AND id = " + id;

			rs = executeUpdateQuery(env, dateToDetermineMembershipLevelquery);
			Assert.assertEquals(rs, 1);
			logger.info(
					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
			TestListeners.extentTest.get().info(
					"The value of date_to_determine_membership_level from business.preference is updated to " + date);
		}
	}

	public static boolean updateSingleValueForGivenParameter(String envName, String expColValue, String keyValue,
			String flagName, String b_id) throws Exception {
		boolean flagIndicator = false;
// Get existing value for the key
		List<String> existingValues = Utilities.getPreferencesKeyValue(expColValue, flagName);
		String currentValue = (existingValues == null || existingValues.isEmpty()) ? null : existingValues.get(0);

// ✅ Step 1: If key doesn't exist, add it first
		if (currentValue == null || currentValue.isEmpty()) {
			String addKeyQuery = "UPDATE businesses " + "SET preferences = CONCAT(preferences, ':" + flagName + ": "
					+ keyValue + "') " + "WHERE id = " + b_id;
			int addedRows = executeUpdateQuery(envName, addKeyQuery);
			Assert.assertEquals(addedRows, 1, "Failed to add missing key: " + flagName);
			flagIndicator = true;
			return flagIndicator;
		}

// ✅ Step 2: If current value is different, update it
		if (!currentValue.equalsIgnoreCase(keyValue)) {
			String updateQuery = "UPDATE businesses SET preferences = REPLACE(preferences, ':" + flagName + ": "
					+ currentValue + "', ':" + flagName + ": " + keyValue + "') WHERE id = " + b_id;
			int updatedRows = executeUpdateQuery(envName, updateQuery);
			Assert.assertEquals(updatedRows, 1, "Failed to update key value for " + flagName);
			flagIndicator = true;
		} else {
// ✅ Step 3: Already correct — no update needed
			flagIndicator = true;
		}

		return flagIndicator;
	}

	public static List<String> getValueFromColumnInList(String envName, String sqlQuery, String columnName)
			throws Exception {
		Connection conn = getDBConnection(envName);
		TestListeners.extentTest.get().info("Executing query: " + sqlQuery);
		logger.info("Using query ==> " + sqlQuery);
		Statement stmt = conn.createStatement();
		List<String> listOfValues = new ArrayList<String>();
		try (ResultSet resultSet = stmt.executeQuery(sqlQuery)) {
			while (resultSet.next()) {
				String value = resultSet.getString(columnName);
				TestListeners.extentTest.get()
						.info("Retrieved value '" + value + "' from the column '" + columnName + "'");
				logger.info("Retrieved value '" + value + "' from the column '" + columnName + "'");
				listOfValues.add(value);
			}

		} catch (SQLException e) {
			TestListeners.extentTest.get().info("Error executing the select statement: " + sqlQuery);
			logger.error("Error executing the select statement: " + sqlQuery, e);
			DBManager.closeConnection();
			throw e;
		}
		DBManager.closeConnection();
		return listOfValues;
	}

	public static ResultSet getResultSet(String envName, String sqlQuery) throws Exception {
		return getResultSet(envName, null, sqlQuery);
	}

	public static ResultSet getResultSet(String envName, String hostIdentifier, String sqlQuery) throws Exception {
		Connection conn = getDBConnection(envName, hostIdentifier);
		String dbInfo = envName + (hostIdentifier != null ? "." + hostIdentifier : "");
		logger.info("Using query ==> " + sqlQuery + " on: " + dbInfo);
		TestListeners.extentTest.get().info("Using query ==> " + sqlQuery + " on: " + dbInfo);
		Statement stmt = conn.createStatement();
		ResultSet resultSet = stmt.executeQuery(sqlQuery);
	//	DBManager.closeConnection();
		return resultSet;
	}

	// Using this to execute delete query
	public static boolean executeQuery(String envName, String sqlQuery) throws Exception {
		return executeQuery(envName, null, sqlQuery);
	}

	public static boolean executeQuery(String envName, String hostIdentifier, String sqlQuery) throws Exception {
		String dbInfo = envName + (hostIdentifier != null ? "." + hostIdentifier : "");
		logger.info("Using query ==> " + sqlQuery + " on: " + dbInfo);
		TestListeners.extentTest.get().info("Using query ==> " + sqlQuery + " on: " + dbInfo);
		Connection conn = getDBConnection(envName, hostIdentifier);
		Statement stmt = conn.createStatement();
		int resultSet = stmt.executeUpdate(sqlQuery);
		if (resultSet >= 1) {
			logger.info(sqlQuery + " is executed successfully on: " + dbInfo);
			TestListeners.extentTest.get().info(sqlQuery + " is executed successfully on: " + dbInfo);
			DBManager.closeConnection();
			return true;
		} else {
			logger.info("Unsucessful execution of  " + sqlQuery + " on: " + dbInfo);
			TestListeners.extentTest.get().info("Unsucessful execution of  " + sqlQuery + " on: " + dbInfo);
			DBManager.closeConnection();
			return false;
		}
	}

	public static String executeQueryAndGetColumnValuePollingUsed(String envName, String sql, String colName, int n)
			throws Exception {
		return executeQueryAndGetColumnValuePollingUsed(envName, null, sql, colName, n);
	}
	/*
	 * Key improvements Use try-with-resources for Statement and ResultSet to ensure
	 * they close after each attempt. Recreate the Statement inside the loop, not
	 * outside — that prevents reuse of a potentially closed statement. Keeps the
	 * same Connection alive (safe if it’s pooled and short-lived). Adds better
	 * error handling and logs.
	 */

	public static String executeQueryAndGetColumnValuePollingUsed(String envName, String hostIdentifier, String sql,
			String colName, int n) throws Exception {

		String dbInfo = envName + (hostIdentifier != null ? "." + hostIdentifier : "");
		logger.info("Executing query using polling: " + sql + " on: " + dbInfo);
		TestListeners.extentTest.get().info("Executing query using polling: " + sql + " on: " + dbInfo);

		String value = "";
		int attempts = 0;

		while (attempts < n) {
			try (Connection conn = getDBConnection(envName, hostIdentifier);
					Statement stmt = conn.createStatement();
					ResultSet resultSet = stmt.executeQuery(sql)) {

				logger.info("Attempt " + (attempts + 1) + " of " + n + " for query on: " + dbInfo);
				TestListeners.extentTest.get()
						.info("Attempt " + (attempts + 1) + " of " + n + " for query on: " + dbInfo);

				if (resultSet.next()) {
					value = resultSet.getString(colName);
					if (value != null) {
						logger.info("Success: Retrieved value '" + value + "' from column '" + colName + "'");
						TestListeners.extentTest.get()
								.info("Success: Retrieved value '" + value + "' from column '" + colName + "'");
						break;
					}
				} else {
					logger.info("No result found on attempt: " + (attempts + 1));
				}
//				conn.close();
				DBManager.closeConnection();
			} catch (SQLException e) {
				logger.error("SQL exception on attempt " + (attempts + 1) + " for " + dbInfo, e);
				TestListeners.extentTest.get()
						.fail("SQL exception on attempt " + (attempts + 1) + ": " + e.getMessage());
			}

			attempts++;
			if (attempts < n) {
				logger.info("Waiting 3 seconds before next polling attempt...");
				Utilities.longWait(3000);
			}

		}

		logger.info("Query result is: " + value + " on: " + dbInfo);
		return value;
	}

	public static int executeQueryAndGetCount(String envName, String sql) throws Exception {
		int count = 0;
		logger.info("Executing query for count: " + sql);
		TestListeners.extentTest.get().info("Executing query for count: " + sql);

		try (Connection conn = getDBConnection(envName);
				Statement stmt = conn.createStatement();
				ResultSet resultSet = stmt.executeQuery(sql)) {

			if (resultSet.next()) {
				count = resultSet.getInt(1);
			}
//			conn.close();
			DBManager.closeConnection();
		}

		logger.info("Retrieved count: " + count);
		TestListeners.extentTest.get().info("Retrieved count: " + count);
		return count;
	}

	public static List<Map<String, String>> executeQueryAndGetMultipleColumns(String envName, String hostIdentifier,
			String sql, String[] colNames) throws Exception {
		String value = "";
		logger.info(
				"Executing query: " + sql + " on: " + envName + (hostIdentifier != null ? "." + hostIdentifier : ""));
		TestListeners.extentTest.get().info(
				"Executing query: " + sql + " on: " + envName + (hostIdentifier != null ? "." + hostIdentifier : ""));
		List<Map<String, String>> result = new ArrayList<>();

		try (Connection conn = getDBConnection(envName, hostIdentifier);
				Statement stmt = conn.createStatement();
				ResultSet resultSet = stmt.executeQuery(sql)) {

			while (resultSet.next()) {
				Map<String, String> values = new HashMap<>();
				for (String columnName : colNames) {
					values.put(columnName, resultSet.getString(columnName));
				}
				result.add(values);
			}
//			conn.close();
			DBManager.closeConnection();
		}

		if (result.isEmpty()) {
			logger.info("No value found for the query: " + sql);
			TestListeners.extentTest.get().info("No value found for the query: " + sql);
		} else {
			logger.info("Retrieved fields map: " + result);
			TestListeners.extentTest.get().info("Retrieved fields map: " + result);
		}
		return result;
	}

	public static List<Map<String, String>> executeQueryAndGetAllRows(String envName, String hostIdentifier,
			String sql) throws Exception {
		logger.info(
				"Executing query: " + sql + " on: " + envName + (hostIdentifier != null ? "." + hostIdentifier : ""));
		TestListeners.extentTest.get().info(
				"Executing query: " + sql + " on: " + envName + (hostIdentifier != null ? "." + hostIdentifier : ""));
		List<Map<String, String>> result = new ArrayList<>();

		try (Connection conn = getDBConnection(envName, hostIdentifier);
				Statement stmt = conn.createStatement();
				ResultSet resultSet = stmt.executeQuery(sql)) {

			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();

			while (resultSet.next()) {
				Map<String, String> values = new HashMap<>();
				for (int i = 1; i <= columnCount; i++) {
					String columnName = metaData.getColumnName(i);
					values.put(columnName, resultSet.getString(columnName));
				}
				result.add(values);
			}
			DBManager.closeConnection();
		}

		if (result.isEmpty()) {
			logger.info("No value found for the query: " + sql);
			TestListeners.extentTest.get().warning("No value found for the query: " + sql);
		} else {
			logger.info("Retrieved fields map: " + result);
			TestListeners.extentTest.get().info("Retrieved fields map: " + result);
		}
		return result;
	}

	public static List<Map<String, String>> executeQueryAndGetAllRows(String envName, String sql) throws Exception {
		return executeQueryAndGetAllRows(envName, null, sql);
	}

	// Returns the value (true, false, or number) of the specified flag from the
	// preferences string.
	public static String businessesPreferenceFlagValue(String expColValue, String flagName) {
		Pattern pattern = Pattern.compile(":" + flagName + ":\\s*([^:,\\s]+)");
		Matcher matcher = pattern.matcher(expColValue);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}

	// New method to poll for all expected values in a single row
	public static boolean pollQueryForExpectedValues(String envName, String sql, String[] colNames, int n,
			String[] expVals) throws Exception {
		logger.info("Executing query: " + sql);
		Connection conn = getDBConnection(envName, null);
		Statement stmt = conn.createStatement();
		ResultSet resultSet = null;
		List<String> actualValues = new ArrayList<>();

		for (int attempt = 0; attempt < n; attempt++) {
			resultSet = stmt.executeQuery(sql);
			while (resultSet.next()) {
				boolean match = true;
				for (int i = 0; i < colNames.length; i++) {
					String actual = resultSet.getString(colNames[i]);
					actualValues.add(actual);
					String expected = expVals[i];
					if (!actual.contains(expected)) {
						match = false;
						break;
					}
				}
				if (match) {
					for (int i = 0; i < colNames.length; i++) {
						logger.info("Column: " + colNames[i] + ", Expected: " + expVals[i] + ", Actual: "
								+ actualValues.get(i));
					}
					logger.info("Match found on attempt " + (attempt + 1));
					DBManager.closeConnection();
					return true;
				}
			}
			if (attempt < n - 1) {
				logger.info("No match found on attempt " + (attempt + 1) + ", retrying...");
				TestListeners.extentTest.get().info("No match found on attempt " + (attempt + 1) + ", retrying...");
				Utilities.longWait(1000); // wait 1 second before next attempt
			}
		}
		for (int i = 0; i < colNames.length; i++) {
			logger.info("Column: " + colNames[i] + ", Expected: " + expVals[i] + ", Actual: " + actualValues.get(i));
		}
		logger.info("No match found after " + n + " attempts.");
		DBManager.closeConnection();
		return false;
	}

	public static void deleteGiftCardData(String env, String gcid) {
		try {
			// 1) Delete from gift_card_versions
			String deleteFromVersions = "DELETE FROM gift_card_versions WHERE gift_card_id='" + gcid + "';";
			executeUpdateQuery(env, deleteFromVersions);

			// 2) Delete from user_cards
			String deleteFromUserCards = "DELETE FROM user_cards WHERE gift_card_id='" + gcid + "';";
			executeUpdateQuery(env, deleteFromUserCards);

			// 3) Delete from gift_cards
			String deleteFromGiftCards = "DELETE FROM gift_cards WHERE id='" + gcid + "';";
			executeUpdateQuery(env, deleteFromGiftCards);

			logger.info("Successfully deleted Gift Card data for gift_card_id: " + gcid);
		} catch (Exception e) {
			logger.error("Error while deleting Gift Card data for gift_card_id: " + gcid, e);
			Assert.fail("Failed to delete Gift Card data for gift_card_id: " + gcid);
		}
	}

	public static boolean verifyAlertSubscribersEmptyInDB(String envName, String sqlQuery) {
		try (Connection connection = getDBConnection(envName);
				Statement s = connection.createStatement();
				ResultSet rs = s.executeQuery(sqlQuery)) {

			if (!rs.next()) {
				logger.info("alert_subscribers: [] (no row) -> treating as empty");
				DBManager.closeConnection();
				return true;
			}

			String pref = rs.getString("preferences");
			if (pref == null || pref.isBlank()) {
				logger.info("alert_subscribers: [] (preferences empty) -> treating as empty");
				DBManager.closeConnection();
				return true;
			}

			Matcher head = Pattern.compile("(?im)^\\s*:?alert_subscribers\\s*:\\s*(.*)$").matcher(pref);
			if (!head.find()) {
				logger.info("alert_subscribers: key missing -> treating as empty");
				DBManager.closeConnection();
				return true; // change to false if key must exist
			}

			List<String> emails = new ArrayList<>();
			String tail = head.group(1).trim();

			if (!tail.isEmpty()) {
				if (tail.startsWith("[") && tail.endsWith("]"))
					tail = tail.substring(1, tail.length() - 1);
				for (String part : tail.split(",")) {
					String v = part.trim().replaceAll("^['\"]|['\"]$", "");
					if (!v.isEmpty())
						emails.add(v);
				}
			} else {
				try (Scanner sc = new Scanner(pref.substring(head.end()))) {
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						if (line.matches("^\\s*[^\\s:#][^:]*:\\s*.*$"))
							break; // next key
						Matcher item = Pattern.compile("^\\s*-\\s+(.*?)\\s*$").matcher(line);
						if (item.find()) {
							String v = item.group(1).trim().replaceAll("^['\"]|['\"]$", "");
							if (!v.isEmpty())
								emails.add(v);
						}
					}
				}
			}

			if (emails.isEmpty()) {
				logger.info("alert_subscribers: [] (empty)");
				return true;
			} else {
				logger.error("Email found");
				logger.error("alert_subscribers list: " + emails);
				return false;
			}

		} catch (Exception e) {
			logger.error("Error verifying empty alert_subscribers: " + e.getMessage(), e);
			return false;
		}
	}

	public static boolean verifyEmailSubscriptionInDB(String envName, String sqlQuery, String email) {
		logger.info("Executing query to verify alert_subscribers: " + sqlQuery);
		TestListeners.extentTest.get().info("Executing query: " + sqlQuery);

		boolean found = false;
		Connection conn = null;

		try {
			conn = getDBConnection(envName);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sqlQuery);

			if (rs.next()) {
				// Use the correct column for your table: "preferences" or "preference"
				String preference = rs.getString("preferences");
				if (preference != null && !preference.isEmpty()) {
					// Accepts "alert_subscribers:" and ":alert_subscribers:"
					Pattern p = Pattern.compile("(?im)^\\s*:?alert_subscribers\\s*:\\s*(.*)$");
					Matcher m = p.matcher(preference);
					List<String> emails = new ArrayList<>();

					if (m.find()) {
						String tail = m.group(1).trim();

						if (!tail.isEmpty()) {
							tail = tail.replace("[", "").replace("]", "");
							for (String part : tail.split(",")) {
								String s = part.trim().replaceAll("^['\"]|['\"]$", "");
								if (!s.isEmpty())
									emails.add(s);
							}
						} else {
							// Block list with '-'
							try (Scanner sc = new Scanner(preference.substring(m.end()))) {
								while (sc.hasNextLine()) {
									String line = sc.nextLine();
									if (line.matches("^\\s*[^\\s:#][^:]*:\\s*.*$"))
										break;
									Matcher item = Pattern.compile("^\\s*-\\s+(.*?)\\s*$").matcher(line);
									if (item.find()) {
										String s = item.group(1).trim().replaceAll("^['\"]|['\"]$", "");
										if (!s.isEmpty())
											emails.add(s);
									}
								}
							}
						}
					}

					// Log the list only
					logger.info("alert_subscribers found: " + emails);
					TestListeners.extentTest.get().info("alert_subscribers found: " + emails);

					found = emails.stream().anyMatch(e -> e.equalsIgnoreCase(email));

				}
			}

		} catch (Exception e) {
			logger.error("Error verifying email subscription: " + e.getMessage(), e);
			TestListeners.extentTest.get().fail("Exception while verifying email subscription: " + e.getMessage());
		}
		DBManager.closeConnection();
		return found;
	}

	public static boolean verifyColumnExistsInQuery(String envName, String query, String expectedColumn) {
		logger.info("Running query to verify column: " + query);
		boolean value = false;
		// retrieve column name from the query and check if it exists
		try (Connection connection = getDBConnection(envName);
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(query);) {
			ResultSetMetaData metaData = rs.getMetaData();
			int columnCount = metaData.getColumnCount();

			for (int i = 1; i <= columnCount; i++) {
				String columnName = metaData.getColumnName(i);
				if (columnName.equalsIgnoreCase(expectedColumn)) {
					logger.info("Found expected column: " + expectedColumn);
					TestListeners.extentTest.get().pass("Found expected column: " + expectedColumn);
					value = true;
					break;
				}
			}

			if (!value) {
				logger.error("Column not found: " + expectedColumn);
				TestListeners.extentTest.get().fail("Column not found: " + expectedColumn);
			}
			DBManager.closeConnection();
		} catch (Exception e) {
			logger.error("Error while checking column: " + e.getMessage());
			TestListeners.extentTest.get().fail("Exception: " + e.getMessage());
			e.printStackTrace();
		}
		return value;
	}

	public static boolean verifyDBValueWithPolling(String environment, String query, String colName,
			String expectedValue, int interval, int maxAttempts) throws Exception {

		logger.info("Using query ==> " + query);
		int attempt = 0;

		while (attempt < maxAttempts) {
			String actualValue = executeQueryAndGetColumnValue(environment, query, colName);
			if (expectedValue.equalsIgnoreCase(String.valueOf(actualValue))) {
				logger.info("After updating the UI, the value is also updated in the DB");
				return true;
			}

			attempt++;
			if (attempt < maxAttempts) {
				logger.info("Value not matched yet. Attempt " + attempt + " of " + maxAttempts
						+ ". Waiting for " + interval + " seconds before retrying...");
				int intervalInMilliseconds = interval * 1000;
				Thread.sleep(intervalInMilliseconds);
			}
		}

		String message = String.format("Even after %d seconds, the value is not updated in the DB",
				interval * maxAttempts);
		logger.info(message);
		return false;
	}

	public static boolean verifyValueFromDBUsingPolling(String environment, String query, String colName,
			String expectedValue) throws Exception {
		logger.info("Using query ==> " + query);
		TestListeners.extentTest.get().info("Using query ==> " + query);
		boolean value = false;
		int counter = 0;
		do {
			String statusColValue = executeQueryAndGetColumnValue(environment, query, colName);
			if (statusColValue.equalsIgnoreCase(expectedValue)) {
				Assert.assertEquals(statusColValue, expectedValue);
				logger.info("After updating the UI the value is also updated in the DB");
				TestListeners.extentTest.get().pass("After updating the UI the value is also updated in the DB");
				value = true;
				return value;
			}
			counter++;
			Utilities.longWait(1000);
			if (counter == 19) {
				Assert.assertEquals(statusColValue, expectedValue,
						"even after 20 seconds the value is not updated in the db");
				logger.warn("Even after 20 seconds the value is not updated in the db");
				TestListeners.extentTest.get().fail("Even after 20 seconds the value is not updated in the db");
			}
		} while (counter < 20);
		return value;
	}

	public static void updateBusinessFlag(String env, String expColValue, String choice,
            String flagName, String b_id) throws Exception {
		boolean result = DBUtils.updateBusinessesPreference(env, expColValue, choice, flagName, b_id);

		Assert.assertTrue(result, flagName + " value is not updated to " + choice);
		logger.info(flagName + " updated to " + choice);
		TestListeners.extentTest.get().info(flagName + " updated to " + choice);
	}
    public static List<Map<String, String>> executeQueryAndGetMultipleColumnsUsingPolling(
            String envName,
            String hostIdentifier,
            String sql,
            String[] colNames,
            int intervalInSeconds,
            int maxAttempts) throws Exception {

        logger.info("Executing query with polling: " + sql +
                " on: " + envName + (hostIdentifier != null ? "." + hostIdentifier : ""));

        TestListeners.extentTest.get().info("Executing query with polling: " + sql);

        List<Map<String, String>> result = new ArrayList<>();
        int attempt = 0;

        while (attempt < maxAttempts) {

            try (Connection conn = getDBConnection(envName, hostIdentifier);
                 Statement stmt = conn.createStatement();
                 ResultSet resultSet = stmt.executeQuery(sql)) {

                result.clear();  // clear old results before each attempt

                while (resultSet.next()) {
                    Map<String, String> rowMap = new HashMap<>();

                    for (String colName : colNames) {
                        rowMap.put(colName, resultSet.getString(colName));
                    }

                    result.add(rowMap);
                }

                DBManager.closeConnection();
            }

            // If we found data, stop polling and return it
            if (!result.isEmpty()) {
                logger.info("Retrieved fields map: " + result);
                TestListeners.extentTest.get().info("Retrieved fields map: " + result);
                return result;
            }

            // Nothing found → wait and retry
            attempt++;
            if (attempt < maxAttempts) {
                logger.info("No data found yet. Attempt " + attempt + " of " + maxAttempts
                        + ". Waiting " + intervalInSeconds + " seconds before retrying...");

                TestListeners.extentTest.get().info("No data found yet. Attempt " + attempt + " of "
                        + maxAttempts + ". Waiting " + intervalInSeconds + " seconds before retrying...");

                Thread.sleep(intervalInSeconds * 1000);
            }
        }

        // After all attempts, still no data
        String message = String.format(
                "After polling for %d seconds (%d attempts), no data was found for query: %s",
                intervalInSeconds * maxAttempts, maxAttempts, sql);

        logger.info(message);
        TestListeners.extentTest.get().info(message);

        return result;
    }

	// Delete Admin User from DB tables
	public static void deleteAdminUser(String env, String adminId, String businessId) throws Exception {
		if (adminId != null && !adminId.isEmpty()) {
			// Delete from admins table
			String adminsQuery = "DELETE FROM admins WHERE id = " + adminId + ";";
			DBUtils.executeQuery(env, adminsQuery);
			// Delete from business_admins table
			String businessAdminsQuery = "DELETE FROM business_admins WHERE admin_id = " + adminId
					+ " AND business_id = " + businessId + ";";
			DBUtils.executeQuery(env, businessAdminsQuery);
			logger.info("Admin User ID [" + adminId + "] is deleted successfully");
			TestListeners.extentTest.get().info("Admin User ID [" + adminId + "] is deleted successfully");
		}
	}
	public static void deleteCollectibleCategoriesByBusiness(String env, String businessId) throws Exception {

	    if (businessId != null && !businessId.isEmpty()) {

	        String query = "DELETE FROM collectible_categories WHERE business_id = " + businessId + ";";
	        DBUtils.executeQuery(env, query);

	        logger.info("Collectible categories deleted for business_id: " + businessId);
	        TestListeners.extentTest.get()
	            .info("Collectible categories deleted for business_id: " + businessId);
	    }
	}

	
	

    public static List<Map<String, String>> executeQueryAndGetMultipleColumnsUsingPolling(
            String envName,
            String sql,
            String[] colNames,
            int intervalInSeconds,
            int maxAttempts) throws Exception {
            return executeQueryAndGetMultipleColumnsUsingPolling(envName, null, sql, colNames, intervalInSeconds, maxAttempts);
    }
    public static String getPreferencesKeyValue(String preferences, String key) {
        if (preferences == null || preferences.isEmpty() || key == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(":" + Pattern.quote(key) + ":\\s*([^:]+)");
        Matcher matcher = pattern.matcher(preferences);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null; 
    }
}
