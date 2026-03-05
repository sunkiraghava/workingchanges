package com.punchh.server.verificationPortalTest;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@SuppressWarnings("unused")
@Listeners(TestListeners.class)
public class verificationPortalTest {
	private static Logger logger = LogManager.getLogger(verificationPortalTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl, verificationsURL;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	private Utilities utils;
	public static BrowserUtilities brw  = null ;
	private String verificationsDBName;

	@BeforeClass
	public void setup() throws Exception {
		
		utils = new Utilities();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv().toLowerCase();
		logger.info("Using env as ==> " + env);
		brw = new BrowserUtilities();
		String runType = brw.getRunType();
		
		verificationsDBName = Utilities.getDBConfigProperty(env+"."+runType, "verificationsDBName");
		logger.info("Using verificationsDBName as ==> " + verificationsDBName);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		// utils = new Utilities(driver);
		// prop = utils.loadPropertiesFile("Config.properties");
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		// env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		verificationsURL = Utilities.getConfigProperty(env + ".verificationsUrl");
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
	}

	// Status - To approve receipt or disapprove receipt
	public void receipt_verification_test(String status) throws Exception {
		// iFrame user Signup
		pageObj.iframeSingUpPage().navigateToIframe(baseUrl + dataSet.get("whiteLabel") + dataSet.get("slug"));
		String userEmail = pageObj.iframeSingUpPage().iframeSignUp();
		String location = dataSet.get("receiptCheckInLocation");
		// checkin
		pageObj.iframeSingUpPage().receiptCheckIn(location);

		// Get user_id for created user
		// String userEmail = "autoiframe22035521082023gfnkol@punchh.com";
		String query = "SELECT id FROM users WHERE email = '" + userEmail + "'";
		String user_id = DBUtils.executeQueryAndGetColumnValue(env, query, "id"); // dbUtils.getValueFromColumn(query,
																										// "id");
		logger.info("User_id ==> " + user_id);

		// Get checkin id for checkin done via receipt
		query = "SELECT id FROM checkins WHERE user_id = '" + user_id + "'";
		String checkin_id = DBUtils.executeQueryAndGetColumnValue(env, query, "id"); // dbUtils.getValueFromColumn(query,
																											// "id");
		logger.info("Checkin_id ==> " + checkin_id);

		// Verify verifications table in punchhDB
		// String checkin_id = "877167211" ;
		String query_punchh_verifications = "SELECT status FROM verifications WHERE checkin_id = '" + checkin_id + "'";

		String statusResult = DBUtils.executeQueryAndGetColumnValue(env,
				query_punchh_verifications, "status");

		Assert.assertEquals(statusResult, "pending", "query_punchh_verifications query result is not matched");
		// dbUtils.verifyColumnValue(query_punchh_verifications, "status", "pending");

		// Enqueue receipt verification job in sidekiq
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.sidekiqPage().navigateToSidekiqScheduled(baseUrl);
		pageObj.sidekiqPage().filterByJob("RequestReceiptImageVerificationWorker");
		pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
		pageObj.sidekiqPage().clickAddToQueue();

		// Verify checkin reached verifications DB
		query = "SELECT id FROM `" + verificationsDBName + "`.payloads WHERE external_uid = '" + checkin_id
				+ "' limit 1";
		String verifications_id = "";
		for (int retryCount = 0; retryCount < 20; retryCount++) {
			verifications_id = DBUtils.executeQueryAndGetColumnValue(env, query, "id"); // dbUtils.getValueFromColumn(query,
																												// "id");
			if (verifications_id != null && !verifications_id.isEmpty()) {
				logger.info("Verifications id ==> " + verifications_id);
				break;
			}
			logger.info("Verification id is null. Waiting for 10 seconds and will retry");
			Utilities.sleep(10000);

		}
		if (verifications_id == null || verifications_id.isEmpty()) {
			Assert.assertNotEquals(verifications_id, null, "Verifications id is null or empty after 20 retries");
			logger.error("Verificatins id is null or empty: " + verifications_id);
			TestListeners.extentTest.get().fail("Verifications id is null or empty after 20 retries");
		}

		// Navigate and login
		pageObj.verificationsPortalPage().navigateToVerificationsPortal(verificationsURL);
		String username = utils.decrypt(Utilities.getConfigProperty(env + ".verificationsUserName"));
		String password = utils.decrypt(Utilities.getConfigProperty(env + ".verificationsPassword"));
		pageObj.verificationsPortalPage().enterLoginEmail(username);
		pageObj.verificationsPortalPage().enterLoginPassword(password);
		pageObj.verificationsPortalPage().clickLoginButton();
		Assert.assertTrue(pageObj.verificationsPortalPage().verifyLogin());

		// Search checkin in verifications portal
		pageObj.verificationsPortalPage().searchCheckIn(checkin_id);
		pageObj.verificationsPortalPage().enterReceiptAmount(Utilities.getRandomNoFromRange(10, 20) + "");
		pageObj.verificationsPortalPage().enterReceiptNumber(Utilities.getRandomNoFromRange(100000, 200000) + "");
		String selectedComment = pageObj.verificationsPortalPage().selectRandomComment();

		if (status.toLowerCase().equals("approve"))
			pageObj.verificationsPortalPage().clickValidBtn();
		else
			pageObj.verificationsPortalPage().clickInvalidBtn();

		// Enqueue receipt approve verification job in verifications sidekiq
		pageObj.sidekiqPage().navigateToSidekiqScheduled(verificationsURL);
		pageObj.sidekiqPage().checkSelectAllJobsCheckBox();
		pageObj.sidekiqPage().clickAddToQueue();
		Utilities.sleep(10000);

		// Verify verifications table status in punchhDB
		if (status.toLowerCase().equals("approve")) {
			String value = DBUtils.executeQueryAndGetColumnValue(env, query_punchh_verifications,
					"status");
			Assert.assertEquals(value, "valid"); // dbUtils.verifyColumnValue(query_punchh_verifications, "status",
													// "valid");
		} else {
			String value = DBUtils.executeQueryAndGetColumnValue(env, query_punchh_verifications,
					"status");
			Assert.assertEquals(value, "invalid");
		}
		// dbUtils.verifyColumnValue(query_punchh_verifications, "status", "invalid");

		// Verify checkins table approve in punchhDB
		String query1 = "SELECT approved FROM checkins WHERE id = '" + checkin_id + "'";
		if (status.toLowerCase().equals("approve")) {
			String rst = DBUtils.executeQueryAndGetColumnValue(env, query1, "approved");
			Assert.assertEquals(rst, "1", query + " result is not matched ");
		}
		// dbUtils.verifyColumnValue(query, "approved", "1");
		else {
			String rst1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "approved");
			Assert.assertEquals(rst1, "0", query + " result is not matched ");
			// dbUtils.verifyColumnValue(query, "approved", "0");
		}

		// Navigate to guest timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		if (status.toLowerCase().equals("approve"))
			pageObj.guestTimelinePage().verifyApprovedReceiptImageCheckin(checkin_id);
		else
			pageObj.guestTimelinePage().verifyDisapprovedReceiptImageCheckin(checkin_id, selectedComment);

		TestListeners.extentTest.get().pass("Receipt verification approve test passed");
	}

	@Test(description = "SQ-T3099_TD-157 | TD-583 | Verifications | Validate the receipt and check the same in punchh dashboard user timeline")
	public void SQ_T3099_Validate_Receipt_Test() throws Exception {
		receipt_verification_test("Approve");
	}

	@Test(description = "SQ-T3100_TD-157 | TD-583 | Verifications | Invalidate the receipt and check the same in punchh dashboard user timeline")
	public void SQ_T3100_Invalidate_Receipt_Test() throws Exception {
		receipt_verification_test("Disapprove");
	}

	@Test(description = "SQ-3104_EPS-5822 | Verifications | Verify deactivated user is not able to login in")
	public void SQ_T3104_Login_With_Deactivated_User_Test() throws Exception {
		pageObj.verificationsPortalPage().navigateToVerificationsPortal(verificationsURL);
		pageObj.verificationsPortalPage().enterLoginEmail(utils.decrypt(dataSet.get("deactivatedEmail")));
		pageObj.verificationsPortalPage().enterLoginPassword(utils.decrypt(dataSet.get("password")));
		pageObj.verificationsPortalPage().clickLoginButton();
		Assert.assertTrue(pageObj.verificationsPortalPage().verifyDeactivatedLogin());
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Test Case: " + sTCName + " finished");
		driver.quit();
		logger.info("Browser closed");
	}

}
