package com.punchh.server.LP1Test;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.GmailConnection;
import com.punchh.server.utilities.MessagesConstants;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class AdminValidationTest {
	static Logger logger = LogManager.getLogger(AdminValidationTest.class);
	public WebDriver driver;
	private String userEmail;
	private PageObj pageObj;
	private String run = "ui";
	private String env;
	private String baseUrl;
	private String sTCName;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private Properties prop;
	private String adminID, businessID;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		adminID = null;
		businessID = dataSet.get("business_id");
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(groups = {
			"regression", "dailyrun"}, description = "SQ-T2566, To test invite admin page by leaving one of the fields blank and user clicks on the create button || SQ-T2567 "
					+ "Invite / Edit admin: To test and verify, on accessible location field, the location selected are showing properly or not", priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2566_verifyAdminMandatoryFieldsAndLocationField() throws InterruptedException {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String timeStamp = CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().settingsLink();
		// pageObj.menupage().adminUsersLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().verifyInviteAdminMandatoryFields();
		pageObj.AdminUsersPage().configureNewAdmin(userEmail, timeStamp, dataSet.get("userRole"));
		pageObj.AdminUsersPage().selectLocations(dataSet.get("locationList"));
		pageObj.AdminUsersPage().clickCreateAdmin();
		String actualErrorMessage = pageObj.locationPage().getErrorSuccessMessage();
		Assert.assertEquals(actualErrorMessage, dataSet.get("expErrorMessage"),
				"Admin Users is not created successfully");
		utils.logit("Admin Users is created successfully and showing success message :- " + actualErrorMessage);
		pageObj.AdminUsersPage().verifyNewlyCreatedAdmin(userEmail, dataSet.get("userRole"),
				"Fname" + timeStamp + " " + "Lname" + timeStamp);
		pageObj.AdminUsersPage().editAdmin(userEmail, timeStamp);
		pageObj.AdminUsersPage().verifyLocationsInEdit(dataSet.get("locationList"));
	}

	@Test(groups = { "regression", "dailyrun" }, description = "SQ-T2564, Validate invite admin by filling all the details || "
			+ "SQ-T2565, To test and verify whether admin is being created or not"
			+ " || SQ-T2568 Edit admin: To test and verify, if user tries to update one of the details, and hit the update button, whether the details are getting updated or not || "
			+ "SQ-T2559, Invite admin: To test and verify, once user clicks on the functionality of 'Generate business key'", priority = 1)
	@Owner(name = "Amit Kumar")
	public void createNewAdminAndUpdate() throws InterruptedException {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		String timeStamp = CreateDateTime.getTimeDateString();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// pageObj.menupage().settingsLink();
		// pageObj.menupage().adminUsersLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().clickOnAddNewAdmin();
		pageObj.AdminUsersPage().configureNewAdmin(userEmail, timeStamp, dataSet.get("userRole"));
		pageObj.AdminUsersPage().setAdminFranchise();
		pageObj.AdminUsersPage().clickCreateAdmin();
		pageObj.AdminUsersPage().verifyNewlyCreatedAdmin(userEmail, dataSet.get("userRole"),
				"Fname" + timeStamp + " " + "Lname" + timeStamp);
		// updating admin
		pageObj.AdminUsersPage().editAdmin(userEmail, timeStamp);
		pageObj.AdminUsersPage().updateAdminDetailsAndVerify(userEmail, timeStamp);
		pageObj.AdminUsersPage().verifyUpdatedAdmin(userEmail, dataSet.get("userRole"), timeStamp);
		// verify generate admin key
		pageObj.AdminUsersPage().verifyGenerateBusinessAdminkey(userEmail, timeStamp);
		pageObj.AdminUsersPage().verifyReGenerateBusinessAdminkey(userEmail, timeStamp);
	}

	@Test(description = "SQ-T4238: Validate that pagination added to super admins/admins page.; "
			+ "SQ-T7274: Verify that admin invite emails are sent from the new corporate domain", groups = "regression", priority = 2)
	@Owner(name = "Vaibhav Agnihotri")
	public void T4238_verifyAdminsPagination() throws Exception {
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admins");
		utils.waitTillPagePaceDone();

		// verify the Admin table headers
		String adminTableHeadersCount = pageObj.segmentsBetaPage().verifyAndGetTextsCount(
				utils.getLocatorList("adminUserPage.adminTableHeaders"), dataSet.get("adminTableHeaders"));
		Assert.assertEquals(adminTableHeadersCount, "6");
		utils.logPass("Admin table headers verified successfully");

		// verify that max 20 users should be present on single page
		int adminTableUserRowsCount = utils.getLocatorList("adminUserPage.adminTableUserRows").size();
		Assert.assertEquals(adminTableUserRowsCount, 20);
		// verify that table should have pagination
		boolean isPaginationPresent = utils.checkElementPresent(utils.getLocator("adminUserPage.adminTablePagination"));
		Assert.assertTrue(isPaginationPresent, "Pagination is not present when user count is 20");
		utils.logPass("Admin table user rows count is 20 and pagination is present");

		// verify the colors for page numbers on pagination
		String selectedPageNumberColor = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("adminUserPage.adminTablePaginationNumber", "color", "1");
		Assert.assertEquals(selectedPageNumberColor, dataSet.get("selectedPageNumberColor"));
		String selectedPageNumberCircleColor = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("adminUserPage.adminTablePaginationSelectedNumber", "background-color", "1");
		Assert.assertEquals(selectedPageNumberCircleColor, dataSet.get("selectedPageNumberCircleColor"));
		String unselectedPageNumberColor = pageObj.iframeConfigurationPage()
				.getCssPropertyValue("adminUserPage.adminTablePaginationNumber", "color", "2");
		Assert.assertEquals(unselectedPageNumberColor, dataSet.get("unselectedPageNumberColor"));
		utils.logPass("Pagination colors verified successfully");

		// verify that users can be searched by business select dropdown
		utils.selectDrpDwnValueNew(utils.getLocator("adminUserPage.adminSearchDropdown"), dataSet.get("business_id"));
		// verify that when users are <20, then no pagination should be present
		isPaginationPresent = pageObj.segmentsBetaPage().verifyPresenceAndClick("adminUserPage.adminTablePagination");
		Assert.assertFalse(isPaginationPresent, "Pagination is present when users are <20");
		utils.logPass("Pagination is not present when users are less than 20");

		// verify that users can be searched by name or email
		utils.selectDrpDwnValue(utils.getLocator("adminUserPage.adminSearchDropdown"), "All Businesses");
		int searchResultCount = pageObj.AdminUsersPage().searchAdmin(dataSet.get("adminName"));
		Assert.assertTrue(searchResultCount > 0);
		searchResultCount = pageObj.AdminUsersPage().searchAdmin(prop.getProperty("userName"));
		Assert.assertTrue(searchResultCount > 0);
		utils.logPass("Verified that Admin users can be searched by name or email");

		// Invite new admin
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		String timeStamp = CreateDateTime.getTimeDateString();
		String senderEmail = dataSet.get("senderEmail");
		userEmail = dataSet.get("userEmail").replace("$num", timeStamp);
		String adminName = "Fname" + timeStamp + " " + "Lname" + timeStamp;
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().clickOnAddNewAdmin();
		pageObj.AdminUsersPage().configureNewAdmin(userEmail, timeStamp, dataSet.get("adminRole"));
		pageObj.AdminUsersPage().clickCreateAdmin();
		pageObj.AdminUsersPage().verifyNewlyCreatedAdmin(userEmail, dataSet.get("adminRole"), adminName);
		adminID = pageObj.AdminUsersPage().getAdminID(adminName);

		// Verify email received for admin creation
		String emailBody = GmailConnection.getGmailEmailBody("toAndFromEmail", userEmail + "," + senderEmail, true);
		String expectedEmailText = MessagesConstants.adminInviteInstructionPartialText;
		Assert.assertTrue(emailBody.contains(expectedEmailText));
		utils.logit("Verified email body which has '" + senderEmail + "' as sender email id");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		// Delete the created business admin user
		DBUtils.deleteAdminUser(env, adminID, businessID);
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}
