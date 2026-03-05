package com.punchh.server.Test;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import com.punchh.server.utilities.DBManager;
import com.punchh.server.utilities.DBUtils;

public class SampleDBTest {

	@Test(priority = 1)
	public void testSelectQuery() throws Exception {
//		System.out.println("▶ Running testSelectQuery...");
//		String query = "SELECT id, name, status FROM users LIMIT 5";
//
//		List<Map<String, Object>> results = DBUtils.executeQueryAndGetMultipleColumns  ("env", "dbname", query);
//		Assert.assertFalse(results.isEmpty(), "No results found!");
//
//		for (Map<String, Object> row : results) {
//			System.out.println("Row => " + row);
//		}
	}

	@Test(priority = 2)
	public void testExecuteUpdate() throws Exception {
		System.out.println("▶ Running testExecuteUpdate...");
		String updateQuery = "UPDATE users SET status = 'active' WHERE id = 1";

		int updatedRows = DBUtils.executeUpdate("env", "dbname", updateQuery);
		System.out.println("Rows updated: " + updatedRows);

		Assert.assertTrue(updatedRows >= 0, "Update failed!");
	}

	@Test(priority = 3)
	public void testPreparedQuery() throws Exception {
		System.out.println("▶ Running testPreparedQuery...");
		String query = "SELECT id, name FROM users WHERE status = ? AND city = ?";
		List<Map<String, Object>> result = DBUtils.executePreparedQuery(query, "active", "Mumbai");

		Assert.assertNotNull(result);
		System.out.println("Results found: " + result.size());
		if (!result.isEmpty()) {
			System.out.println("First record: " + result.get(0));
		}
	}

	@Test(priority = 4)
	public void testGetSingleValue() throws Exception {
//		System.out.println("▶ Running testGetSingleValue...");
//		String query = "SELECT email FROM users WHERE id = 1";
//		String email = DBUtils.getSingleValue("env", "dbname", query, "email");
//
//		System.out.println("Fetched email: " + email);
//		Assert.assertNotNull(email, "Email should not be null!");
	}

	@Test(priority = 5)
	public void testWaitForValue() throws Exception {
//		System.out.println("▶ Running testWaitForValue...");
//		String query = "SELECT status FROM users WHERE id = 1";
//		boolean isMatched = DBUtils.waitForValue("env", "dbname", query, "status", "active");
//
//		Assert.assertTrue(isMatched, "Expected value 'active' not found within timeout!");
	}

	@Test(priority = 6)
	public void testWaitForRecord() throws Exception {
//		System.out.println("▶ Running testWaitForRecord...");
//		String query = "SELECT * FROM users WHERE city = 'Delhi'";
//		boolean recordExists = DBUtils.waitForRecord("env", "dbname", query);
//
//		Assert.assertTrue(recordExists, "No matching record found within timeout!");
	}

	@Test(priority = 7)
	public void verifyUserRecord() throws Exception {
//		String query = "SELECT * FROM users WHERE email = 'test@example.com'";
//		List<Map<String, Object>> result = DBUtils.executeQuery("env", "dbname", query);
//
//		Assert.assertFalse(result.isEmpty(), "No records found!");
//		String name = (String) result.get(0).get("name");
//		System.out.println("User name = " + name);
	}

	@Test(priority = 8)
	public void verifyCampaignStatusUpdated() throws Exception {
		String campaignId = "12345";

		String query = "SELECT status FROM campaigns WHERE id = " + campaignId;

//		boolean statusUpdated = DBUtils.waitForValue("env", "dbname", query, "status", "ACTIVE", // expected value
//				120000, // timeout 2 min
//				5000 // poll every 5 sec
//		);
//
//		assert statusUpdated : "Campaign status did not become ACTIVE within timeout!";
	}

	@AfterMethod(alwaysRun = true)
	public void cleanupConnection() {
		DBManager.closeConnection();
	}

	@AfterSuite(alwaysRun = true)
	public void shutdownPool() {
		DBManager.shutdownAllPools();
	}
}
