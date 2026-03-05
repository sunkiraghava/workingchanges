package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.Arrays;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.UpdateLocationPayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.updatelocation.UpdateLocationStoreTime;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class TD4050LocationUpdateWithRedemptionFlagRequestTest {
	private static Logger logger = LogManager.getLogger(TD4050LocationUpdateWithRedemptionFlagRequestTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String rewardId = "";
	String rewardId1 = "";
	String rewardId2 = "";
	String discount_details0 = "";
	String externalUID = "";
	UpdateLocationStoreTime mon = null;
	String city = "Mountain View";
	private Utilities utils;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		mon = UpdateLocationStoreTime.builder().day("Tue").startTime("8:00 AM").endTime("11:00 PM").build();
		utils = new Utilities();
	}

	@Test(description = "OMM-T4849 Update the Location with the PATCH request by disabling the flag (flag set to: true, in Create API)", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Rahul Garg")
	public void T4849_createLocationenables_updateLocationDoesNotSendParam()
			throws InterruptedException, JsonProcessingException {

		String store_number = CreateDateTime.getTimeDateString();
		String location_name = "Test Location" + store_number;
		String locationId = Integer.toString(Utilities.getRandomNoFromRange(0, 1000));

		// Create Location Api
		Response createLocationresponse = pageObj.endpoints().createLocationMultipleRedemption(location_name,
				store_number, dataSet.get("apiKey"), locationId, "true");
		int location_id = createLocationresponse.jsonPath().getInt("location_id");
		String locationString = createLocationresponse.jsonPath().getString("location_id");
		String storeNumber = createLocationresponse.jsonPath().getString("store_number");

		boolean allow_location_for_multiple_redemptions = createLocationresponse.jsonPath()
				.getBoolean("multiple_redemption_on_location");
		utils.logInfo(
				"Location created with location_id is : " + location_id + " and store number is : " + storeNumber);

		Assert.assertEquals(createLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location");
		utils.logPass("PLATFORM FUNCTIONS API Create Location is successful");

		Assert.assertNotNull(location_id, "Location id is expected to be not null!");
		utils.logInfo("Location id is not null and location id is : " + location_id);

		Assert.assertEquals(allow_location_for_multiple_redemptions, true,
				"Allow Location for Multiple Redemptions flag is expected to be true!");
		utils.logPass("Allow Location for Multiple Redemptions flag is set to true in create-location api response");

		// Update Location Payload using Builder class
		String jsonBody = new UpdateLocationPayloadBuilder().withLocationId(locationString).withCity(city)
				.withStoreTimes(Arrays.asList(mon)).buildJson();

		// hit Update Location Api with payload
		Response updateLocationresponse = pageObj.endpoints().updateLocationUsingPOJO(jsonBody, dataSet.get("apiKey"));
		Assert.assertEquals(updateLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Location");

		boolean allow_location_for_multiple_redemptionsFromUpdateAPI = updateLocationresponse.jsonPath()
				.getBoolean("multiple_redemption_on_location");
		Assert.assertEquals(allow_location_for_multiple_redemptionsFromUpdateAPI, true,
				"Allow Location for Multiple Redemptions flag is expected to be true!");
		utils.logPass("Allow Location for Multiple Redemptions flag is set to true in update-location api response");

		// Delete location
		Response deleteLocationresponse = pageObj.endpoints().deleteLocation(locationString, store_number,
				dataSet.get("apiKey"));
		Assert.assertEquals(deleteLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NO_CONTENT,
				"Status code 204 did not match for PLATFORM FUNCTIONS API Delete location");
		utils.logPass("PLATFORM FUNCTIONS API Delete locationis successful");
	}

	@Test(description = "OMM-T4850 Update the Location with the PATCH request by disabling the flag (flag set to: true, in Create API)", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Rahul Garg")
	public void T4850_createLocationDisables_updateLocationDoesNotSendParam()
			throws InterruptedException, JsonProcessingException {

		String store_number = CreateDateTime.getTimeDateString();
		String location_name = "Test Location" + store_number;
		String locationId = Integer.toString(Utilities.getRandomNoFromRange(0, 1000));

		// Create Location Api
		Response createLocationresponse = pageObj.endpoints().createLocationMultipleRedemption(location_name,
				store_number, dataSet.get("apiKey"), locationId, "false");
		int location_id = createLocationresponse.jsonPath().getInt("location_id");
		String locationString = createLocationresponse.jsonPath().getString("location_id");
		String storeNumber = createLocationresponse.jsonPath().getString("store_number");

		boolean allow_location_for_multiple_redemptions = createLocationresponse.jsonPath()
				.getBoolean("multiple_redemption_on_location");
		utils.logInfo(
				"Location created with location_id is : " + location_id + " and store number is : " + storeNumber);

		Assert.assertEquals(createLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location");
		utils.logPass("PLATFORM FUNCTIONS API Create Location is successful");
		Assert.assertNotNull(location_id, "Location id is expected to be not null!");
		utils.logInfo("Location id is not null and location id is : " + location_id);
		Assert.assertEquals(allow_location_for_multiple_redemptions, false,
				"Allow Location for Multiple Redemptions flag is expected to be true!");
		utils.logPass("Allow Location for Multiple Redemptions flag is set to true in create-location api response");

		// Update Location Payload using Builder class
		String jsonBody = new UpdateLocationPayloadBuilder().withLocationId(locationString)
				.withStoreNumber(store_number).withCity(city).withStoreTimes(Arrays.asList(mon)).buildJson();

		// Update Location Api
		Response updateLocationresponse = pageObj.endpoints().updateLocationUsingPOJO(jsonBody, dataSet.get("apiKey"));
		Assert.assertEquals(updateLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Location");
		boolean allow_location_for_multiple_redemptionsFromUpdateAPI = updateLocationresponse.jsonPath()
				.getBoolean("multiple_redemption_on_location");
		Assert.assertEquals(allow_location_for_multiple_redemptionsFromUpdateAPI, false,
				"Allow Location for Multiple Redemptions flag is expected to be true!");
		utils.logPass("Allow Location for Multiple Redemptions flag is set to true in update-location api response");

		// Delete location
		Response deleteLocationresponse = pageObj.endpoints().deleteLocation(locationString, store_number,
				dataSet.get("apiKey"));
		Assert.assertEquals(deleteLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NO_CONTENT,
				"Status code 204 did not match for PLATFORM FUNCTIONS API Delete location");
		utils.logPass("PLATFORM FUNCTIONS API Delete locationis successful");
	}

	@Test(description = "(OMM-T4847 Verify that in Update Location API, the flag- multiple_redemptions is updated to \"true\" and changes reflect on UI", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Rahul Garg")
	public void T4847_createLocationDisables_updateLocationEnables()
			throws InterruptedException, JsonProcessingException {

		String store_number = CreateDateTime.getTimeDateString();
		String location_name = "Test Location" + store_number;
		String locationId = Integer.toString(Utilities.getRandomNoFromRange(0, 1000));

		// Create Location Api
		Response createLocationresponse = pageObj.endpoints().createLocationMultipleRedemption(location_name,
				store_number, dataSet.get("apiKey"), locationId, "false");
		int location_id = createLocationresponse.jsonPath().getInt("location_id");
		String locationString = createLocationresponse.jsonPath().getString("location_id");
		String storeNumber = createLocationresponse.jsonPath().getString("store_number");
		boolean allow_location_for_multiple_redemptions = createLocationresponse.jsonPath()
				.getBoolean("multiple_redemption_on_location");
		utils.logInfo(
				"Location created with location_id is : " + location_id + " and store number is : " + storeNumber);

		Assert.assertEquals(createLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location");
		utils.logPass("PLATFORM FUNCTIONS API Create Location is successful");

		Assert.assertNotNull(location_id, "Location id is expected to be not null!");
		utils.logInfo("Location id is not null and location id is : " + location_id);

		Assert.assertEquals(allow_location_for_multiple_redemptions, false,
				"Allow Location for Multiple Redemptions flag is expected to be true!");
		utils.logPass("Allow Location for Multiple Redemptions flag is set to true in create-location api response");

		// Update Location Payload using Builder class
		String jsonBody = new UpdateLocationPayloadBuilder().withLocationId(locationString)
				.withStoreNumber(store_number).withCity(city).withStoreTimes(Arrays.asList(mon))
				.withEnableMultipleRedemptions(true).buildJson();

		// Update Location Api
		Response updateLocationresponse = pageObj.endpoints().updateLocationUsingPOJO(jsonBody, dataSet.get("apiKey"));
		Assert.assertEquals(updateLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Location");

		// Verify Allow Location for Multiple Redemptions flag is set to true
		boolean allow_location_for_multiple_redemptionsFromUpdateAPI = updateLocationresponse.jsonPath()
				.getBoolean("multiple_redemption_on_location");
		Assert.assertEquals(allow_location_for_multiple_redemptionsFromUpdateAPI, true,
				"Allow Location for Multiple Redemptions flag is expected to be true!");
		utils.logPass("Allow Location for Multiple Redemptions flag is set to true in update-location api response");

		// Delete location
		Response deleteLocationresponse = pageObj.endpoints().deleteLocation(locationString, store_number,
				dataSet.get("apiKey"));
		Assert.assertEquals(deleteLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NO_CONTENT,
				"Status code 204 did not matched for PLATFORM FUNCTIONS API Delete location");
		utils.logPass("PLATFORM FUNCTIONS API Delete locationis successful");
	}

	@Test(description = "(OMM-T4848 Verify that in Update Location API, the flag- multiple_redemptions is updated to \"false\" and changes reflect on UI", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Rahul Garg")
	public void T4848_createLocationDisables_updateLocationDisables()
			throws InterruptedException, JsonProcessingException {

		String store_number = CreateDateTime.getTimeDateString();
		String location_name = "Test Location" + store_number;
		String locationId = Integer.toString(Utilities.getRandomNoFromRange(0, 1000));

		// Create Location Api
		Response createLocationresponse = pageObj.endpoints().createLocationMultipleRedemption(location_name,
				store_number, dataSet.get("apiKey"), locationId, "false");
		int location_id = createLocationresponse.jsonPath().getInt("location_id");
		String locationString = createLocationresponse.jsonPath().getString("location_id");
		String storeNumber = createLocationresponse.jsonPath().getString("store_number");
		boolean allow_location_for_multiple_redemptions = createLocationresponse.jsonPath()
				.getBoolean("multiple_redemption_on_location");
		utils.logInfo(
				"Location created with location_id is : " + location_id + " and store number is : " + storeNumber);

		Assert.assertEquals(createLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for PLATFORM FUNCTIONS API Create Location");
		utils.logPass("PLATFORM FUNCTIONS API Create Location is successful");
		Assert.assertNotNull(location_id, "Location id is expected to be not null!");
		utils.logInfo("Location id is not null and location id is : " + location_id);
		Assert.assertEquals(allow_location_for_multiple_redemptions, false,
				"Allow Location for Multiple Redemptions flag is expected to be false!");
		utils.logPass("Allow Location for Multiple Redemptions flag is set to false in create-location api response");

		// Update Location Payload using Builder class
		String jsonBody = new UpdateLocationPayloadBuilder().withLocationId(locationString)
				.withStoreNumber(store_number).withCity(city).withStoreTimes(Arrays.asList(mon))
				.withEnableMultipleRedemptions(false).buildJson();

		// Update Location Api
		Response updateLocationresponse = pageObj.endpoints().updateLocationUsingPOJO(jsonBody, dataSet.get("apiKey"));
		Assert.assertEquals(updateLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for PLATFORM FUNCTIONS API Update Location");
		boolean allow_location_for_multiple_redemptionsFromUpdateAPI = updateLocationresponse.jsonPath()
				.getBoolean("multiple_redemption_on_location");
		Assert.assertEquals(allow_location_for_multiple_redemptionsFromUpdateAPI, false,
				"Allow Location for Multiple Redemptions flag is expected to be true!");
		utils.logPass("Allow Location for Multiple Redemptions flag is set to true in update-location api response");

		// Delete location
		Response deleteLocationresponse = pageObj.endpoints().deleteLocation(locationString, store_number,
				dataSet.get("apiKey"));
		Assert.assertEquals(deleteLocationresponse.getStatusCode(), ApiConstants.HTTP_STATUS_NO_CONTENT,
				"Status code 204 did not match for PLATFORM FUNCTIONS API Delete location");
		utils.logPass("PLATFORM FUNCTIONS API Delete locationis successful");
	}

	@AfterMethod(alwaysRun = true)
	public void afterMethod() {
		pageObj.utils().clearDataSet(dataSet);
	}
}