package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PlatformFunctionsAPILocationListTest {
	private static Logger logger = LogManager.getLogger(PlatformFunctionsAPILocationListTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
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
		logger.info(sTCName + " ==> " + dataSet);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T3730 [Platform Functions API-> Get Location List] Verify enable_multiple_redemptions flag in the Location API", groups = {"regression", "dailyrun"})
	@Owner(name = "Hardik Bhardwaj")
	public void T3730_PlatformFunctionsAPIGetLocationList() throws InterruptedException {

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions_on_all_locations",
				"check");
		pageObj.dashboardpage().updateButton();

		// Get Location List
		Response getLocationresponse = pageObj.endpoints().getLocationList(dataSet.get("apiKey"));
		Assert.assertEquals(getLocationresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Get Location List");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Get Location List is successful");
		String locationName = getLocationresponse.jsonPath().get("[0].name").toString();
		String multipleRedemptionOnLocationFlag = getLocationresponse.jsonPath()
				.get("[0].multiple_redemption_on_location").toString();
		logger.info("Flag for Allow Location for Multiple Redemption on API is " + multipleRedemptionOnLocationFlag);
		TestListeners.extentTest.get()
				.info("Flag for Allow Location for Multiple Redemption on API is " + multipleRedemptionOnLocationFlag);
		logger.info("Location Name is " + locationName);
		TestListeners.extentTest.get().info("Location Name is " + locationName);

		// navigate to locations in settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		String result = pageObj.locationPage().clickOnSelectedLocation(locationName);
		logger.info("Flag for Allow Location for Multiple Redemption on UI is " + result);
		TestListeners.extentTest.get().info("Flag for Allow Location for Multiple Redemption on UI is " + result);
		if (result == null) {
			result = "false";
		}

		Assert.assertEquals(result, multipleRedemptionOnLocationFlag,
				"Flag for Allow Location for Multiple Redemption on UI is not same as API");
		Assert.assertEquals(multipleRedemptionOnLocationFlag, result);
		logger.info("Flag for Allow Location for Multiple Redemption on UI is same as API");
		TestListeners.extentTest.get().pass("Flag for Allow Location for Multiple Redemption on UI is same as API");

	}

	@Test(description = "SQ-T3731 [Platform Functions API-> Create Location] Verify when enable_multiple_redemptions flag is sent as false then redemption 2.0 will be disable at the location || "
			+ "SQ-T3732 [Platform Functions API-> Create Location] Verify when enable_multiple_redemptions flag is sent as true then redemption 2.0 will be enable at the location", groups = {"regression", "dailyrun"}, dataProvider = "TestDataProvider")
	@Owner(name = "Hardik Bhardwaj")
	public void T3731_32_PlatformFunctionsAPICreateLocationWithMultipleLocation(String enable_multiple_redemptions)
			throws Exception {

		// login to instance
		// pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		// pageObj.instanceDashboardPage().loginToInstance();

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for enable_taxation_support_in_subscription
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_taxation_support_in_subscription", b_id);

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions", "check");
		if (enable_multiple_redemptions.equalsIgnoreCase("true")) {
			pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions_on_all_locations",
					"check");
		}

		if (enable_multiple_redemptions.equalsIgnoreCase("false")) {
			pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions_on_all_locations",
					"uncheck");
		}
		pageObj.dashboardpage().updateButton();

		// Create Location Api
		String location_name = "location_name " + CreateDateTime.getTimeDateString()
				+ CreateDateTime.getRandomString(6).toLowerCase();
		String store_number = Integer.toString(Utilities.getRandomNoFromRange(100, 10000));
		String locationId = Integer.toString(Utilities.getRandomNoFromRange(0, 1000));

		Response createLocationresponse = pageObj.endpoints().createLocationMultipleRedemption(location_name,
				store_number, (dataSet.get("apiKey")), locationId, enable_multiple_redemptions);
		String multipleRedemptionOnLocationFlag = createLocationresponse.jsonPath()
				.get("multiple_redemption_on_location").toString();
		String location_name1 = createLocationresponse.jsonPath().get("name").toString();
		Assert.assertEquals(createLocationresponse.getStatusCode(), 201,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Create Location");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Create Location is successful");
		logger.info("Flag for Allow Location for Multiple Redemption on API is " + multipleRedemptionOnLocationFlag);
		TestListeners.extentTest.get()
				.info("Flag for Allow Location for Multiple Redemption on API is " + multipleRedemptionOnLocationFlag);
		logger.info("Location Name in API response is " + location_name1);
		TestListeners.extentTest.get().info("Location Name in API response is " + location_name1);

		Assert.assertEquals(location_name1, location_name,
				"Location name in API response is not same as Location name we enter");
		Assert.assertEquals(multipleRedemptionOnLocationFlag, enable_multiple_redemptions,
				"Multiple Redemption Location Flag in API response is not same as Multiple Redemption Location Flag we enter");
		TestListeners.extentTest.get().pass(
				"Location name and Multiple Redemption Location Flag in API response is same as Location name and Multiple Redemption Location Flag we enter ");

		// navigate to locations in settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		String result = pageObj.locationPage().clickOnSelectedLocation(location_name);
		logger.info("Flag for Allow Location for Multiple Redemption on UI is " + result);
		TestListeners.extentTest.get().info("Flag for Allow Location for Multiple Redemption on UI is " + result);
		if (result == null) {
			result = "false";
		}

		Assert.assertEquals(multipleRedemptionOnLocationFlag, result,
				"Flag for Allow Location for Multiple Redemption on UI is not same as API");
		Assert.assertEquals(enable_multiple_redemptions, result,
				"Flag for Allow Location for Multiple Redemption on UI is not same as API");
		TestListeners.extentTest.get().pass("Flag for Allow Location for Multiple Redemption on UI is same as API");

	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {
				// {"enable_multiple_redemptions"},
				{ "false" },
				{ "true" }, 
		};

	}

	@Test(description = "SQ-T3733 [Platform Functions API-> Update Location] Verify when enable_multiple_redemptions flag is sent as false then redemption 2.0 will be disable at the location || "
			+ "SQ-T3734 [Platform Functions API-> Update Location] Verify when enable_multiple_redemptions flag is sent as true then redemption 2.0 will be enable at the location", groups = {"regression", "dailyrun"}, dataProvider = "TestDataProvider1")
	@Owner(name = "Hardik Bhardwaj")
	public void T3733_PlatformFunctionsAPIUpdateLocationWithMultipleLocation(String flag,
			String enable_multiple_redemptions) throws Exception {

		String location_name = dataSet.get("location_name");
		String locationId = dataSet.get("locationId");

		String b_id = dataSet.get("business_id");

		String preferenceQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='" + b_id
				+ "'";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, preferenceQuery, "preferences");

		// updating the business preference for enable_taxation_support_in_subscription
		DBUtils.updateBusinessFlag(env, expColValue, "false", "enable_taxation_support_in_subscription", b_id);

		// login to instance
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions", "check");
		if (enable_multiple_redemptions.equalsIgnoreCase("true")) {
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions_on_all_locations",
				"check");
		}
		pageObj.dashboardpage().updateButton();

		// navigate to locations in settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickOnLocationName(location_name);

		pageObj.dashboardpage()
				.checkBoxFlagOnOffAndClick("location_location_extra_attributes_enable_multiple_redemptions", flag);
		pageObj.dashboardpage().updateCheckBox();
		// pageObj.menupage().clickDashboardMenu();
//		pageObj.locationPage().onEnableMultipleRedemptipnCheckbox(location_name);

		// Update Location
		Response updateLocationresponse = pageObj.endpoints().updateLocationMultipleRedemption(locationId,
				(dataSet.get("apiKey")), enable_multiple_redemptions);
		String multipleRedemptionOnLocationFlag = updateLocationresponse.jsonPath()
				.get("multiple_redemption_on_location").toString();
		String location_name1 = updateLocationresponse.jsonPath().get("name").toString();
		Assert.assertEquals(updateLocationresponse.getStatusCode(), 200,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Location");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Update Location is successful");
		logger.info("Flag for Allow Location for Multiple Redemption on API is " + multipleRedemptionOnLocationFlag);
		TestListeners.extentTest.get()
				.info("Flag for Allow Location for Multiple Redemption on API is " + multipleRedemptionOnLocationFlag);
		logger.info("Location Name in API response is " + location_name1);
		TestListeners.extentTest.get().info("Location Name in API response is " + location_name1);

		Assert.assertEquals(location_name1, location_name,
				"Location name in API response is not same as Location name we enter");
		Assert.assertEquals(multipleRedemptionOnLocationFlag, enable_multiple_redemptions,
				"Multiple Redemption Location Flag in API response is not same as Multiple Redemption Location Flag we enter");
		TestListeners.extentTest.get().pass(
				"Location name and Multiple Redemption Location Flag in API response is same as Location name and Multiple Redemption Location Flag we enter ");
		// navigate to locations in settings

		// pageObj.menupage().clickSettingsMenu();
		// pageObj.menupage().clickLocationsLink();
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		String result = pageObj.locationPage().clickOnSelectedLocation(location_name);
		logger.info("Flag for Allow Location for Multiple Redemption on UI is " + result);
		TestListeners.extentTest.get().info("Flag for Allow Location for Multiple Redemption on UI is " + result);
		if (result == null) {
			result = "false";
		}

		Assert.assertEquals(multipleRedemptionOnLocationFlag, result,
				"Flag for Allow Location for Multiple Redemption on UI is not same as API");
		Assert.assertEquals(enable_multiple_redemptions, result,
				"Flag for Allow Location for Multiple Redemption on UI is not same as API");
		TestListeners.extentTest.get().pass("Flag for Allow Location for Multiple Redemption on UI is same as API");
	}

	@DataProvider(name = "TestDataProvider1")
	public Object[][] testDataProvider1() {

		return new Object[][] {
				// {"flag", "enable_multiple_redemptions"},
				{ "check", "false" }, { "uncheck", "true" }, };

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
