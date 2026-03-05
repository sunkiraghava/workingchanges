package com.punchh.server.loyalty2;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DeleteDeactivateUserTest {
	static Logger logger = LogManager.getLogger(DeleteDeactivateUserTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private String userEmail;
	private static Map<String, String> dataSet;
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		utils = new Utilities(driver);
	}

	// Anant
	@Test(description = "SQ-T4603 Verify flag(Restrict Guest Profile Reactivation) should be visible on the user --> edit profile"
			+ "SQ-T4601 Verify user should be to disable flag(Restrict Guest Profile Reactivation)"
			+ "SQ-T4602 Verify user should be to enable flag(Restrict Guest Profile Reactivation)"
			+ "SQ-T4604 Verify audit logs have info for reactivation in user -->edit --> audit logs"
			+ "SQ-T4605 Verify after deactivating user Who deactivated is coming on notes or not"
			+ "SQ-T4606 Verify after deactivating user Reason of deactivation is coming on notes or not"
			+ "SQ-T4608 Verify after deactivating user, Notes are displaying on timeline with all the details & check DB for the same", groups = {"regression", "dailyrun"})
	@Owner(name = "Vansham Mishra")
	public void T4603_verifyFlagRestrictGuestProfileReactivation() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// create a user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		utils.logPass("Api1 user signup is successful");

		// nagivate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");

		// deactivate the guest
		pageObj.guestTimelinePage().deactivateGuestWithAllowReactivationOrNot(dataSet.get("allowReactivation"));

		// uncheck the flag
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("user_restrict_reactivation", "check");
		pageObj.guestTimelinePage().updateBtn();

		// check the flag
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("user_restrict_reactivation", "uncheck");
		pageObj.guestTimelinePage().updateBtn();

		// create a another user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		String userID = signUpResponse1.jsonPath().get("user_id").toString();
		utils.logPass("Api1 user signup is successful");

		// nagivate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");

		// deactivate the guest
		pageObj.guestTimelinePage().deactivateGuestWithAllowReactivationOrNot(dataSet.get("allowReactivation"));

		// check the timeline
		pageObj.guestTimelinePage().navigateToTabs("Timeline");

		boolean val = pageObj.guestTimelinePage().deactivationReasonVisible(dataSet.get("deactivationReason"));
		Assert.assertTrue(val, "deactivation reason is not visible");
		utils.logPass("Verfied deactivation reason is visible");

		boolean val2 = pageObj.guestTimelinePage().reactivationAllowedTimeline(dataSet.get("reactivation"));
		Assert.assertTrue(val2, "allow reactivation value is not match");
		utils.logPass("Verfied allow reactivation value is match");

		boolean val3 = pageObj.guestTimelinePage().adminDeactivateUserVisible(dataSet.get("adminName"));
		Assert.assertTrue(val3, "admin name did not match");
		utils.logPass("Verified admin is matched");

		// check note
		pageObj.guestTimelinePage().navigateToTabs("1 Note");
		boolean val4 = pageObj.guestTimelinePage().deactivateReasonVisibleInNote(dataSet.get("deactivationReason"),
				dataSet.get("adminName"));
		Assert.assertTrue(val4, "deactivation reason and the admin name is not visible on the note tab");
		utils.logit("Verfied deactivation reason and the admin name is visible on the note tab");

		// db verification
		String query = "Select * from user_notes where user_id=" + userID;
		String commentValue = DBUtils.executeQueryAndGetColumnValue(env, query, "comment");
		Assert.assertEquals(commentValue, dataSet.get("deactivationReason"),
				"in db deactivation reason is not updated");

		String statusValue = DBUtils.executeQueryAndGetColumnValue(env, query, "status");
		Assert.assertEquals(statusValue, dataSet.get("status"), "in db user status is not updated");

		String reactivationValue = DBUtils.executeQueryAndGetColumnValue(env, query,
				"reactivation_allowed");
		Assert.assertEquals(reactivationValue, "1", "in db reactivation allowed is not updated");

		utils.logPass("Verified all the fields in the DB have been updated");

		// reactivate the guest
		pageObj.guestTimelinePage().navigateToTabs("Edit Profile");
		pageObj.guestTimelinePage().reactivateGuestFromTimeline();

		// clicked on audit log
		pageObj.guestTimelinePage().clickAuditLog();
		boolean visible = pageObj.guestTimelinePage().checkReactivationLogs(prop.getProperty("userName"));
		Assert.assertTrue(visible, "logs are not visible");
		utils.logPass("Verified audit logs for the user reactivation is visible");
	}

	// Anant
	@Test(description = "SQ-T4680 verify a new key is added `allow_guest_to_raise_reactivation` with status true when flag is enabled from cockpit --> guest --> reactivation tab"
			+ "SQ-T4681 verify a new key is added `allow_guest_to_raise_reactivation` with status false when flag is disabled from cockpit --> guest --> reactivation tab", groups = { "regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T4680_VerifyNewkeyWhenFlagIsOnOrOff() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// go to cockpit--> guests --> guest reactivation
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Guest Reactivation");

		// uncheck the flag
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_guest_to_raise_reactivation", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		// api
		Response res1 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(res1.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String val1 = res1.jsonPath().get("allow_guest_to_raise_reactivation").toString();
		Assert.assertEquals("false", val1,
				"When the flag is off then also the value is not update in the api response");
		utils.logPass(
				"Verified when the flag is off then allow_guest_to_raise_reactivation value is also updated in the response");

		// go to cockpit--> guests --> guest reactivation
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Guest Reactivation");

		// uncheck the flag
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_guest_to_raise_reactivation", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		utils.refreshPage();
		utils.waitTillPagePaceDone();
		// api
		Response res2 = pageObj.endpoints().Api2Meta(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(res2.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String val2 = res2.jsonPath().get("allow_guest_to_raise_reactivation").toString();
		Assert.assertEquals("true", val2, "When the flag is ON then also the value is not update in the api response");
		utils.logPass(
				"Verified when the flag is ON then allow_guest_to_raise_reactivation value is also updated in the response");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
