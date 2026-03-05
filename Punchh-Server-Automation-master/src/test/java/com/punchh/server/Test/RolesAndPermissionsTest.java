package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.NewMenu;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

@Listeners(TestListeners.class)
public class RolesAndPermissionsTest {
	private static Logger logger = LogManager.getLogger(RolesAndPermissionsTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	ApiUtils apiUtils;
	String redeemableName = "AutomationRedeemable";
	String redeemableName1 = "AutomationRedeemable_Test";
	Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		utils = new Utilities(driver);
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		logger.info(sTCName + " ==>" + dataSet);
		apiUtils = new ApiUtils();
	}

	@Test(description = "SQ-T5546 Roles and permissions testing for Business Owner", groups = { "regression",
			"dailyrun" }, priority = 0)
	public void T5546_VerifyPermissionsForBusinessOwner() throws Exception {
		List<String> expectedMarketingAutomationSubMenus = List.of("Campaign Management", "Coupon Report");
		List<String> expectedGuestsSubMenus = List.of("All Guest Profiles", "All Segments", "Deactivated", "RFM Slabs",
				"Reactivation Requested", "Receipt Tags");
		List<String> expectedLoyaltyProgramSubMenus = List.of("Dynamic Rewards", "Gifting Metrics", "System Messages");
		List<String> expectedOffersSubMenus = List.of("All Redeemables", "Line Item Selectors", "Product Catalog",
				"Qualification Criteria", "Redemption Report");
		List<String> expectedFraudSubMenus = List.of("Banned Guests");
		List<String> expectedStoreLocationsSubMenus = List.of("All Store Locations", "Location Channels",
				"Location Groups", "Location Scoreboard", "Twitter Mapping");
		List<String> expectedWalletAndPassesSubMenus = List.of("Subscription Cancellation Reasons",
				"Subscription Plans");
		List<String> expectedDataSharingSubMenus = List.of("FTP Endpoints");
		List<String> expectedAdministrationSubMenus = List.of("All Users, Roles and Permissions", "Business Profile",
				"Day Parts", "Facebook Feedback Settings");
		List<String> expectedDiagnosticSubMenus = List.of("Barcode Lookup", "Checkin Failures", "Coupon Lookup",
				"Heartbeats", "POS Stats", "Redemption Codes", "Redemption Failures", "Redemption Log",
				"SSF UUID Lookup", "Schedules", "Test Barcodes");
		List<String> expectedWhitelabelSubMenus = List.of("API Messages", "Mobile Configuration", "OAuth Apps",
				"Version Notes", "iFrame Configuration");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(dataSet.get("userName"), dataSet.get("password"));
		utils.waitTillPagePaceDone();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().clickRole(dataSet.get("userName"), dataSet.get("role"));
		pageObj.menupage().pinSidenavMenu();
		// validating the expected disabled permissions with the actual disabled
		// permissions
		List<String> ActualDisabledPermissions = pageObj.AdminUsersPage().verifyDisabledPermissions();
		Assert.assertEquals(ActualDisabledPermissions, dataSet.get("expectedDisabledPermissions"),
				"Actual disabled permissions does not match with the expected disabled permissions");
		logger.info("All the permissions which are expected to be disabled for business owner role are disabled");
		TestListeners.extentTest.get()
				.pass("All the permissions which are expected to be disabled for business owner role are disabled");

		// validating all the submenus are present in their respective menu items or not
		List<String> actualMarketingAutomationSubMenus = pageObj.menupage()
				.subMenuItems(NewMenu.menu_MarketingAutomation);
		Assert.assertEquals(actualMarketingAutomationSubMenus, expectedMarketingAutomationSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_MarketingAutomation + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_MarketingAutomation);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_MarketingAutomation);
		List<String> actualGuestsSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Guests);
		Assert.assertEquals(expectedGuestsSubMenus, actualGuestsSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Guests + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Guests);
		TestListeners.extentTest.get().pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Guests);
		List<String> actualLoyaltyProgramSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_LoyaltyProgram);
		Assert.assertEquals(expectedLoyaltyProgramSubMenus, actualLoyaltyProgramSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_LoyaltyProgram + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_LoyaltyProgram);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_LoyaltyProgram);
		List<String> actualOffersSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Offers);
		Assert.assertEquals(expectedOffersSubMenus, actualOffersSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Offers + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Offers);
		TestListeners.extentTest.get().pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Offers);
		List<String> actualFraudSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Fraud);
		Assert.assertEquals(expectedFraudSubMenus, actualFraudSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Fraud + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Fraud);
		TestListeners.extentTest.get().pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Fraud);
		List<String> actualStoreLocationsSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_StoreLocations);
		Assert.assertEquals(expectedStoreLocationsSubMenus, actualStoreLocationsSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_StoreLocations + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_StoreLocations);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_StoreLocations);
		List<String> actualWalletAndPassesSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_WalletAndPasses);
		Assert.assertEquals(expectedWalletAndPassesSubMenus, actualWalletAndPassesSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_WalletAndPasses + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_WalletAndPasses);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_WalletAndPasses);
		List<String> actualDataSharingSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_DataSharing);
		Assert.assertEquals(expectedDataSharingSubMenus, actualDataSharingSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_DataSharing + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_DataSharing);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_DataSharing);
		List<String> actualAdministrationSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Administration);
		Assert.assertEquals(expectedAdministrationSubMenus, actualAdministrationSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Administration + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Administration);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Administration);
		List<String> actualDiagnosticSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Diagnostic);
		Assert.assertEquals(expectedDiagnosticSubMenus, actualDiagnosticSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Diagnostic + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Diagnostic);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Diagnostic);
		List<String> actualWhitelabelSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Whitelabel);
		Assert.assertEquals(expectedWhitelabelSubMenus, actualWhitelabelSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Whitelabel + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Whitelabel);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Whitelabel);
	}

	@Test(description = "SQ-T5660 Roles and permissions testing for Business Manager", groups = { "regression",
			"dailyrun" }, priority = 1)
	public void T5660_VerifyPermissionsForBusinessManager() throws Exception {
		List<String> expectedMarketingAutomationSubMenus = List.of("Campaign Management", "Coupon Report");
		List<String> expectedGuestsSubMenus = List.of("All Guest Profiles", "All Segments", "Deactivated", "RFM Slabs",
				"Reactivation Requested", "Receipt Tags");
		List<String> expectedLoyaltyProgramSubMenus = List.of("Dynamic Rewards", "Gifting Metrics", "System Messages");
		List<String> expectedOffersSubMenus = List.of("All Redeemables", "Line Item Selectors", "Product Catalog",
				"Qualification Criteria", "Redemption Report");
		List<String> expectedFraudSubMenus = List.of("Banned Guests");
		List<String> expectedStoreLocationsSubMenus = List.of("All Store Locations", "Location Channels",
				"Location Groups", "Location Scoreboard");
		List<String> expectedWalletAndPassesSubMenus = List.of("Subscription Cancellation Reasons",
				"Subscription Plans");
		List<String> expectedDataSharingSubMenus = List.of("FTP Endpoints");
		List<String> expectedAdministrationSubMenus = List.of("All Users, Roles and Permissions", "Business Profile",
				"Day Parts", "Facebook Feedback Settings");
		List<String> expectedDiagnosticSubMenus = List.of("Barcode Lookup", "Checkin Failures", "Coupon Lookup",
				"Heartbeats", "POS Stats", "Redemption Codes", "Redemption Failures", "Redemption Log",
				"SSF UUID Lookup", "Schedules", "Test Barcodes");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(dataSet.get("userName"), dataSet.get("password"));
		utils.waitTillPagePaceDone();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().clickRole(dataSet.get("userName"), dataSet.get("role"));
		pageObj.menupage().pinSidenavMenu();
		// validating the expected disabled permissions with the actual disabled
		// permissions
		List<String> ActualDisabledPermissions = pageObj.AdminUsersPage().verifyDisabledPermissions();
		Assert.assertEquals(ActualDisabledPermissions, dataSet.get("expectedDisabledPermissions"),
				"Actual disabled permissions does not match with the expected disabled permissions");
		logger.info("All the permissions which are expected to be disabled for business manager role are disabled");
		TestListeners.extentTest.get()
				.pass("All the permissions which are expected to be disabled for business manager role are disabled");

		// validating all the submenus are present in their respective menu items or not
		List<String> actualMarketingAutomationSubMenus = pageObj.menupage()
				.subMenuItems(NewMenu.menu_MarketingAutomation);
		Assert.assertEquals(expectedMarketingAutomationSubMenus, actualMarketingAutomationSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_MarketingAutomation + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_MarketingAutomation);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_MarketingAutomation);
		List<String> actualGuestsSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Guests);
		Assert.assertEquals(expectedGuestsSubMenus, actualGuestsSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Guests + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Guests);
		TestListeners.extentTest.get().pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Guests);
		List<String> actualLoyaltyProgramSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_LoyaltyProgram);
		Assert.assertEquals(expectedLoyaltyProgramSubMenus, actualLoyaltyProgramSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_LoyaltyProgram + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_LoyaltyProgram);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_LoyaltyProgram);
		List<String> actualOffersSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Offers);
		Assert.assertEquals(expectedOffersSubMenus, actualOffersSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Offers + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Offers);
		TestListeners.extentTest.get().pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Offers);
		List<String> actualFraudSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Fraud);
		Assert.assertEquals(expectedFraudSubMenus, actualFraudSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Fraud + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Fraud);
		TestListeners.extentTest.get().pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Fraud);
		List<String> actualStoreLocationsSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_StoreLocations);
		Assert.assertEquals(expectedStoreLocationsSubMenus, actualStoreLocationsSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_StoreLocations + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_StoreLocations);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_StoreLocations);
		List<String> actualWalletAndPassesSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_WalletAndPasses);
		Assert.assertEquals(expectedWalletAndPassesSubMenus, actualWalletAndPassesSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_WalletAndPasses + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_WalletAndPasses);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_WalletAndPasses);
		List<String> actualDataSharingSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_DataSharing);
		Assert.assertEquals(expectedDataSharingSubMenus, actualDataSharingSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_DataSharing + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_DataSharing);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_DataSharing);
		List<String> actualAdministrationSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Administration);
		Assert.assertEquals(expectedAdministrationSubMenus, actualAdministrationSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Administration + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Administration);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Administration);
		List<String> actualDiagnosticSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Diagnostic);
		Assert.assertEquals(expectedDiagnosticSubMenus, actualDiagnosticSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Diagnostic + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Diagnostic);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Diagnostic);
	}

	@Test(description = "SQ-T5661 Roles and permissions testing for Site Admin", groups = { "regression",
			"dailyrun" }, priority = 2)
	public void T5661_VerifyPermissionsForSiteAdmin() throws Exception {
		List<String> expectedMarketingAutomationSubMenus = List.of("Coupon Report");
		List<String> expectedLoyaltyProgramSubMenus = List.of("Dynamic Rewards", "Gifting Metrics", "System Messages");
		List<String> expectedOffersSubMenus = List.of("All Redeemables", "Line Item Selectors",
				"Qualification Criteria", "Redemption Report");
		List<String> expectedStoreLocationsSubMenus = List.of("All Store Locations", "Location Scoreboard");
		List<String> expectedWalletAndPassesSubMenus = List.of("Subscription Cancellation Reasons");
		List<String> expectedDataSharingSubMenus = List.of("FTP Endpoints");
		List<String> expectedAdministrationSubMenus = List.of("All Users, Roles and Permissions", "Business Profile",
				"Day Parts", "Facebook Feedback Settings");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance(dataSet.get("userName"), dataSet.get("password"));
		utils.waitTillPagePaceDone();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Admin Users");
		pageObj.AdminUsersPage().clickRole(dataSet.get("userName"), dataSet.get("role"));
		pageObj.menupage().pinSidenavMenu();
		// validating the expected Enabled permissions with the actual disabled
		// permissions
		List<String> ActualEnabledPermissions = pageObj.AdminUsersPage().verifyEnabledPermissions();
		Assert.assertEquals(ActualEnabledPermissions, dataSet.get("expectedEnabledPermissions"),
				"Actual disabled permissions does not match with the expected disabled permissions");
		logger.info("Verfied all applicable permissions for site admin role");
		TestListeners.extentTest.get().pass("Verfied all applicable permissions for site admin role");

		// validating all the submenus are present in their respective menu items or not
		List<String> actualMarketingAutomationSubMenus = pageObj.menupage()
				.subMenuItems(NewMenu.menu_MarketingAutomation);
		Assert.assertEquals(expectedMarketingAutomationSubMenus, actualMarketingAutomationSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_MarketingAutomation + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_MarketingAutomation);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_MarketingAutomation);
		List<String> actualLoyaltyProgramSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_LoyaltyProgram);
		Assert.assertEquals(expectedLoyaltyProgramSubMenus, actualLoyaltyProgramSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_LoyaltyProgram + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_LoyaltyProgram);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_LoyaltyProgram);
		List<String> actualOffersSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Offers);
		Assert.assertEquals(expectedOffersSubMenus, actualOffersSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Offers + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Offers);
		TestListeners.extentTest.get().pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Offers);
		List<String> actualStoreLocationsSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_StoreLocations);
		Assert.assertEquals(expectedStoreLocationsSubMenus, actualStoreLocationsSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_StoreLocations + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_StoreLocations);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_StoreLocations);
		List<String> actualWalletAndPassesSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_WalletAndPasses);
		Assert.assertEquals(expectedWalletAndPassesSubMenus, actualWalletAndPassesSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_WalletAndPasses + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_WalletAndPasses);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_WalletAndPasses);
		List<String> actualDataSharingSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_DataSharing);
		Assert.assertEquals(expectedDataSharingSubMenus, actualDataSharingSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_DataSharing + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_DataSharing);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_DataSharing);
		List<String> actualAdministrationSubMenus = pageObj.menupage().subMenuItems(NewMenu.menu_Administration);
		Assert.assertEquals(expectedAdministrationSubMenus, actualAdministrationSubMenus,
				"SubMenu Items present under menu " + NewMenu.menu_Administration + " does not match");
		logger.info("All the sub-menu items are present under menu:- " + NewMenu.menu_Administration);
		TestListeners.extentTest.get()
				.pass("All the sub-menu items are present under menu:- " + NewMenu.menu_Administration);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}