package com.punchh.server.Integration1;

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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class YextLocationTest {
	static Logger logger = LogManager.getLogger(YextLocationTest.class);
	public WebDriver driver;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String timeStamp;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private String userEmail;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
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
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	// Yext location api validation with Geomodifier
	@Test(description = "SQ-T6916 Yext location api validation with Geomodifier")
	@Owner(name = "Pradeep Kumar")
	public void T6916_yextLocationsWithGeomodifierApiValidation() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Select Geomodifier the Yext Location Mapping
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Locations");
		pageObj.cockpitLocationPage()
				.selectGeomodifierForYextLocationMapping(dataSet.get("YextLocationMappingGeomodifier"));

		// Create Location Api
		String store_number = CreateDateTime.getTimeDateString();
		String location_name_input = "Test_Location";

		Response createLocationresponse = pageObj.endpoints().createLocationCallsFromYext(location_name_input,
				store_number, dataSet.get("adminKey"));
		Assert.assertEquals(createLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location");

		String location_id = createLocationresponse.jsonPath().getString("location_id");
		String storeNumber = createLocationresponse.jsonPath().getString("store_number");
		String locationName = createLocationresponse.jsonPath().getString("name");
		String location_name = "Test_Location ";

		Assert.assertTrue(!location_id.isEmpty(), "location_id is empty");
		Assert.assertEquals(storeNumber, store_number, "store_number does not match");
		Assert.assertEquals(locationName, location_name,
				"location name did not match in PLATFORM FUNCTIONS API Create Location");

		utils.logPass("PLATFORM FUNCTIONS API Create Location is successful");

		// Update Location Api
		String name = "Test_Location_Updated";
		String nameAfterUpdate = "Test_Location_Updated " + store_number;

		Response updateLocationresponse = pageObj.endpoints().updateLocationCallsFromYext(location_id, storeNumber,
				dataSet.get("adminKey"), name);
		Assert.assertEquals(updateLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for PLATFORM FUNCTIONS API Update Location");

		String locationNameAfterUpdate = updateLocationresponse.jsonPath().getString("name");

		Assert.assertEquals(locationNameAfterUpdate, nameAfterUpdate,
				"location name did not match in PLATFORM FUNCTIONS API Create Location");

		utils.logPass("PLATFORM FUNCTIONS API Update Location is successful");

		// Delete location
		Response deleteLocationresponse = pageObj.endpoints().deleteLocationCallsFromYext(location_id, storeNumber,
				dataSet.get("adminKey"));
		Assert.assertEquals(deleteLocationresponse.getStatusCode(), 204,
				"Status code 200 did not match for PLATFORM FUNCTIONS API Delete location");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete location is successful");

	}

	// Yext location api validation without Geomodifier
	@Test(description = "SQ-T6916 Yext location api validation without Geomodifier")
	@Owner(name = "Pradeep Kumar")
	public void T6916_yextLocationsWithoutGeomodifierApiValidation() throws Exception {

		String b_id = dataSet.get("business_id");

		String businessPreferenceLiveQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ b_id + "'";
		String businessPreferenceLiveExpColValue = DBUtils.executeQueryAndGetColumnValue(env,
				businessPreferenceLiveQuery, "preferences");

		boolean businessLiveFlag = DBUtils.updateBusinessesPreference(env, businessPreferenceLiveExpColValue, "true",
				"enable_new_sidenav_header_and_footer", b_id);
		Assert.assertTrue(businessLiveFlag, "enable_new_sidenav_header_and_footer value is not updated to true");
		utils.logit("enable_new_sidenav_header_and_footer value is updated to true");

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Set Default(Blank) the Yext Location Mapping
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Locations");
		pageObj.cockpitLocationPage().deSelectGeomodifierForYextLocationMapping();

		// Create Location Api
		String store_number = CreateDateTime.getTimeDateString();
		String location_name = "Test_Location";

		Response createLocationresponse = pageObj.endpoints().createLocationCallsFromYext(location_name, store_number,
				dataSet.get("adminKey"));
		Assert.assertEquals(createLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location");

		String location_id = createLocationresponse.jsonPath().getString("location_id");
		String storeNumber = createLocationresponse.jsonPath().getString("store_number");
		String locationName = createLocationresponse.jsonPath().getString("name");

		Assert.assertTrue(!location_id.isEmpty(), "location_id is empty");
		Assert.assertEquals(storeNumber, store_number, "store_number does not match");
		Assert.assertEquals(locationName, location_name,
				"location name did not match in PLATFORM FUNCTIONS API Create Location");

		utils.logPass("PLATFORM FUNCTIONS API Create Location is successful");

		// Update Location Api
		String name = "Test_Location" + "_Updated";
		String nameAfterUpdate = "Test_Location" + "_Updated";

		Response updateLocationresponse = pageObj.endpoints().updateLocationCallsFromYext(location_id, storeNumber,
				dataSet.get("adminKey"), name);
		Assert.assertEquals(updateLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for PLATFORM FUNCTIONS API Update Location");

		String locationNameAfterUpdate = updateLocationresponse.jsonPath().getString("name");

		Assert.assertEquals(locationNameAfterUpdate, nameAfterUpdate,
				"location name did not match in PLATFORM FUNCTIONS API Create Location");

		utils.logPass("PLATFORM FUNCTIONS API Update Location is successful");

		// Delete location
		Response deleteLocationresponse = pageObj.endpoints().deleteLocationCallsFromYext(location_id, storeNumber,
				dataSet.get("adminKey"));
		Assert.assertEquals(deleteLocationresponse.getStatusCode(), 204,
				"Status code 200 did not match for PLATFORM FUNCTIONS API Delete location");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Delete location is successful");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}