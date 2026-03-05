package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class LocationFlagDisableForRedemption2Test {
	private static Logger logger = LogManager.getLogger(LocationFlagDisableForRedemption2Test.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	String rewardId = "";
	String rewardId1 = "";
	String rewardId2 = "";
	String discount_details0 = "";
	String externalUID = "";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
	}

	@Test(description = "SQ-T3297 [Batched Redemptions-OMM-T947(592)] Verify Redemptions 2.0 API's (location specific) are not functional on locations having Allow Location for Multiple Redemption Off", groups = {"regression", "dailyrun"}, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T3297_LocationFlagDisableRedemption2() throws InterruptedException {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_auto_redemption", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_auto_unlock", "check");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.dashboardpage().updateCheckBox();

		// navigate to locations in settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		String result = pageObj.locationPage().clickOnSelectedLocation(dataSet.get("location_name"));
		pageObj.utils().logit("Flag for Allow Location for Multiple Redemption on UI is " + result);
		if (result == null) {
			result = "false";
		}

		Assert.assertEquals(result, "false",
				"Flag for Allow Location for Multiple Redemption on UI is not same as API");
		pageObj.utils().logPass("Location Flag for redeemption 2.0 is disable");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "16",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		pageObj.utils().logPass("Reward id " + rewardId + " is generated successfully ");

		// POS Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationKey2_0_off"), userID, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match with add discount to basket ");
		boolean isPosAddDiscountFeatureUnsupportedSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, discountBasketResponse.asString());
		Assert.assertTrue(isPosAddDiscountFeatureUnsupportedSchemaValidated,
				"POS API Add discount item to basket Schema Validation failed");
		Assert.assertEquals(discountBasketResponse.jsonPath().getString("error"),
				"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
		pageObj.utils().logPass("POS add discount to basket error verification is successful");

		// POS fetch active basket
		Response basketDiscountDetailsResponse3 = pageObj.endpoints().fetchActiveBasketPOSAPI(userID,
				dataSet.get("locationKey2_0_off"), externalUID);
		Assert.assertEquals(basketDiscountDetailsResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for POS fetch active basket");
		boolean isPosFetchActiveBasketFeatureUnsupportedSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, basketDiscountDetailsResponse3.asString());
		Assert.assertTrue(isPosFetchActiveBasketFeatureUnsupportedSchemaValidated,
				"POS API fetch active basket Schema Validation failed");
		Assert.assertEquals(basketDiscountDetailsResponse3.jsonPath().getString("error"),
				"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
		pageObj.utils().logPass("POS fetch active basket error verification is successful");

		// POS remove discount from basket
		String expdiscount_basket_item_id1 = Integer.toString(Utilities.getRandomNoFromRange(500, 1000));
		Response deleteBasketResponse5 = pageObj.endpoints().removeDiscountFromBasketPOSAPI(
				dataSet.get("locationKey2_0_off"),
				userID, expdiscount_basket_item_id1, externalUID);
		Assert.assertEquals(deleteBasketResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match with remove discount from basket ");
		boolean isPosRemoveDiscountFeatureUnsupportedSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteBasketResponse5.asString());
		Assert.assertTrue(isPosRemoveDiscountFeatureUnsupportedSchemaValidated,
				"POS API Remove discount item from basket Schema Validation failed");
		Assert.assertEquals(deleteBasketResponse5.jsonPath().getString("error"),
				"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
		pageObj.utils().logPass("POS remove discount basket error verification is successful");

		// POS user lookUp
		Thread.sleep(10000);
		Response userLookupResponse = pageObj.endpoints().userLookupPosApi("email", userEmail,
				dataSet.get("locationKey2_0_off"), externalUID);
		Assert.assertEquals(userLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with POS user lookUp ");
		pageObj.utils().logPass("POS user lookUp is successful");

		// POS Discount Lookup Api
		Thread.sleep(10000);
		Response discountLookupResponse = pageObj.endpoints().discountLookUpPosApi(dataSet.get("locationKey2_0_off"),
				userID,
				dataSet.get("item_id"), "30", externalUID);
		Assert.assertEquals(discountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match with Discount Lookup Api ");
		boolean isPosDiscountLookupFeatureUnsupportedSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, discountLookupResponse.asString());
		Assert.assertTrue(isPosDiscountLookupFeatureUnsupportedSchemaValidated,
				"POS API Discount Lookup Schema Validation failed");
		Assert.assertEquals(discountLookupResponse.jsonPath().getString("error"),
				"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
		pageObj.utils().logPass("POS Discount Lookup error verification is successful");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints().processBatchRedemptionPosApiPayload(
				dataSet.get("locationKey2_0_off"), userID, "30", "1", "12003", externalUID);
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match with Process Batch Redemption ");
		boolean isPosBatchRedemptionFeatureUnsupportedSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, batchRedemptionProcessResponseUser2.asString());
		Assert.assertTrue(isPosBatchRedemptionFeatureUnsupportedSchemaValidated,
				"POS API Process Batch Redemption Schema Validation failed");
		Assert.assertEquals(batchRedemptionProcessResponseUser2.jsonPath().getString("error"),
				"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
		pageObj.utils().logPass("POS Process Batch Redemption Api error verification is successful");

		// POS fetch active basket
		Response batchRedemptionResponse = pageObj.endpoints().posBatchRedemptionWithQueryTrue(
				dataSet.get("locationKey2_0_off"), userID, dataSet.get("item_id"), externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, batchRedemptionResponse.getStatusCode(),
				"Status code 422 did not matched for POS fetch active basket");
		Assert.assertEquals(batchRedemptionResponse.jsonPath().getString("error"),
				"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
		pageObj.utils().logPass("POS fetch active Redemption Api error verification is successful");

		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", externalUID,
				dataSet.get("locationKey2_0_off"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match with Auto Unlock ");
		boolean isPosAutoUnlockFeatureUnsupportedSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, autoUnlockResponse1.asString());
		Assert.assertTrue(isPosAutoUnlockFeatureUnsupportedSchemaValidated,
				"POS API Auto Unlock Schema Validation failed");
		Assert.assertEquals(autoUnlockResponse1.jsonPath().getString("error"),
				"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
		pageObj.utils().logPass("POS Auto Unlock Api error verification is successful");

		// POS basket Unlock API
		Response basketUnlockResponse = pageObj.endpoints().discountUnlockPOSAPI(dataSet.get("locationKey2_0_off"),
				userID,
				externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, basketUnlockResponse.getStatusCode(),
				"Status code 422 did not matched for POS basket Unlock error that state User don't have any active basket");
		Assert.assertEquals(basketUnlockResponse.jsonPath().getString("error"),
				"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
		pageObj.utils().logPass("POS Api POS basket Unlock error verification is successful");

		// AUTH Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse5.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount is successful");
		String discount_basket_item_id = discountBasketResponse5.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();

		// Auth API fetch active basket
		Response basketDiscountDetailsResponse2 = pageObj.endpoints().fetchActiveBasketAuthApi(token,
				dataSet.get("client"), dataSet.get("secret"), externalUID);
		Assert.assertEquals(basketDiscountDetailsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with fetch active basket ");
		pageObj.utils().logPass("AUTH fetch active basket is successful");

//		// Auth Process Batch Redemption
//		Response batchRedemptionProcessResponse2 = pageObj.endpoints().processBatchRedemptionAUTHAPI(
//				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationKey"), token, userID, "12003",
//				externalUID);
//		Assert.assertEquals(batchRedemptionProcessResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
//				"Status code 422 did not match with Process Batch Redemption ");
//		boolean isAuthBatchRedemptionFeatureUnsupportedSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, batchRedemptionProcessResponse2.asString());
//		Assert.assertTrue(isAuthBatchRedemptionFeatureUnsupportedSchemaValidated,
//				"Auth API Process Batch Redemption Schema Validation failed");
//		Assert.assertEquals(batchRedemptionProcessResponse2.jsonPath().getString("error.message"),
//				"Your current loyalty program configuration does not support this feature. Please connect with your Customer Success representative for resolution of the issue.");
//		TestListeners.extentTest.get().pass("Auth Process Batch Redemption Api error verification is successful");
//		logger.info("Auth Process Batch Redemption Api error verification is successful");

		// Auth remove discount from basket
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserWithExt_UidAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id, externalUID);
		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");
		pageObj.utils().logPass("Auth remove discount basket is successful");
		
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug1"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions_on_all_locations",
				"check");
		pageObj.dashboardpage().updateCheckBox();

		// navigate to locations in settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage()
				.clickOnSelectedLocationAndEnableMultipleRedemption(dataSet.get("location_name_winghouse"));
		pageObj.utils().logit("Flag for Allow Location for Multiple Redemption on UI is Checked");

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client1"),
				dataSet.get("secret1"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();

		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKey1"), "16",
				dataSet.get("redeemable_id1"), "", "");
		pageObj.utils().logPass("Send redeemable to the user successfully");
		Assert.assertEquals(sendRewardResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId1 = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("client1"), dataSet.get("secret1"),
				dataSet.get("redeemable_id1"));
		pageObj.utils().logPass("Reward id " + rewardId1 + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse55 = pageObj.endpoints().addDiscountToBasketAUTH(token1, dataSet.get("client1"),
				dataSet.get("secret1"), "reward", rewardId1, externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse55.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		pageObj.utils().logPass("AUTH add discount is successful");

		// Auth Process Batch Redemption
		Response batchRedemptionResponse45 = pageObj.endpoints()
				.posBatchRedemptionWithQueryTrue(dataSet.get("locationKey2"), userID1, "12003", externalUID);
		Assert.assertEquals(batchRedemptionResponse45.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String redemption_ref = batchRedemptionResponse45.jsonPath().get("redemption_ref").toString();
		pageObj.utils().logPass("Auth Process Batch Redemption Api is successful");

//		// Auth void Process Batch Redemption
//		Response batchRedemptionProcessResponse = pageObj.endpoints().voidProcessBatchRedemptionOfBasketAUTHAPI(
//				dataSet.get("client1"), dataSet.get("secret1"), token1, redemption_ref);
//		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), 202,
//				"Status code 202 did not match with Process Batch Redemption ");
//		TestListeners.extentTest.get().pass("Auth Void Process Batch Redemption Api is successful");
//		logger.info("Auth void Process Batch Redemption Api is successful");

	}

	@Test(description = "SQ-T3300 [Batched Redemptions-OMM-T945(592)] Verify enable_multiple_redemptions parameter is returned in auth/cards API response when allow_multiple_redemptions is On", priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T3300_EnableMultipleRedemption() throws InterruptedException {
		int j = 0;
		int k = 0;
		int l = 0;
		List<Object> obj = new ArrayList<Object>();
		Response authCardsResponse = pageObj.endpoints().authCardsAPI(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, authCardsResponse.getStatusCode(),
				"Status code 200 did not match with Process Batch Redemption ");
		pageObj.utils().logPass("Auth Cards is successful");

		obj = authCardsResponse.jsonPath().getList("locations");
		for (int i = 0; i < obj.size(); i++) {
			String location = authCardsResponse.jsonPath().getString("locations[" + i + "].name");
			if (location.equalsIgnoreCase(dataSet.get("locationName1"))) {
				j = i;
				break;
			}
		}
		try {
			String locationRed2 = authCardsResponse.jsonPath()
					.get("locations[" + j + "].location_extra.multiple_redemption_on_location").toString();
			Assert.assertEquals(locationRed2, "true", "multiple_redemption_on_location is not enable for location");
			pageObj.utils().logPass("multiple_redemption_on_location is ENABLED for location " + dataSet.get("locationName1"));

		} catch (Exception e) {
			pageObj.utils().logPass("multiple_redemption_on_location is DISABLED for location "
					+ dataSet.get("locationName1") + " because feature flag is off");
		}

		List<Object> obj2 = new ArrayList<Object>();
		Response authCardsResponse2 = pageObj.endpoints().authCardsAPI(dataSet.get("client2"), dataSet.get("secret2"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, authCardsResponse2.getStatusCode(),
				"Status code 200 did not match with Process Batch Redemption ");
		pageObj.utils().logPass("Auth Cards is successful");

		obj2 = authCardsResponse2.jsonPath().getList("locations");
		for (int i = 0; i < obj2.size(); i++) {
			String location = authCardsResponse2.jsonPath().getString("locations[" + i + "].name");
			if (location.equalsIgnoreCase(dataSet.get("locationName2_1"))) {
				k = i;
//				break;
			}
			if (location.equalsIgnoreCase(dataSet.get("locationName2_2"))) {
				l = i;
//				break;
			}
		}

		String locationRed2_1 = authCardsResponse2.jsonPath()
				.get("locations[" + k + "].location_extra.multiple_redemption_on_location").toString();
		Assert.assertEquals(locationRed2_1, "true", "multiple_redemption_on_location is not enable for location");
		pageObj.utils().logPass("multiple_redemption_on_location is ENABLED for location " + dataSet.get("locationName2_1"));
		String locationRed2_2 = authCardsResponse2.jsonPath()
				.get("locations[" + l + "].location_extra.multiple_redemption_on_location").toString();
		Assert.assertEquals(locationRed2_2, "false", "multiple_redemption_on_location is enable for location");
		pageObj.utils().logPass("multiple_redemption_on_location is DISABLED for location " + dataSet.get("locationName2_2"));

	}

	@Test(description = "SQ-T3299 [Batched Redemptions-OMM-T944(592)] Verify enable_multiple_redemptions parameter is returned in pos/locations/configuration API response when allow_multiple_redemptions is On", groups = {"regression", "dailyrun"}, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T3299_EnableMultipleRedemption() throws InterruptedException {
		Response posLocConfigResponse = pageObj.endpoints().posLocationConfig(dataSet.get("locationKey1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, posLocConfigResponse.getStatusCode(),
				"Status code 200 did not matched for pos possible redemption api");
		try {
			String locationRed2 = posLocConfigResponse.jsonPath().get("multiple_redemption_on_location").toString();
			Assert.assertEquals(locationRed2, "true", "multiple_redemption_on_location is not enable for location");
			pageObj.utils().logPass("multiple_redemption_on_location is ENABLED for location " + dataSet.get("locationName1"));

		} catch (Exception e) {
			pageObj.utils().logPass("multiple_redemption_on_location is DISABLED for location "
					+ dataSet.get("locationName1") + " because feature flag is off");
		}

		Response posLocConfigResponse1 = pageObj.endpoints().posLocationConfig(dataSet.get("locationKey2_1"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, posLocConfigResponse1.getStatusCode(),
				"Status code 200 did not matched for pos possible redemption api");

		String locationRed2_1 = posLocConfigResponse1.jsonPath().get("multiple_redemption_on_location").toString();
		Assert.assertEquals(locationRed2_1, "true", "multiple_redemption_on_location is not enable for location");
		pageObj.utils().logPass("multiple_redemption_on_location is ENABLED for location " + dataSet.get("locationName2_1"));

		Response posLocConfigResponse2 = pageObj.endpoints().posLocationConfig(dataSet.get("locationKey2_2"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, posLocConfigResponse2.getStatusCode(),
				"Status code 200 did not matched for pos possible redemption api");

		String locationRed2_2 = posLocConfigResponse2.jsonPath().get("multiple_redemption_on_location").toString();
		Assert.assertEquals(locationRed2_2, "false", "multiple_redemption_on_location is enable for location");
		pageObj.utils().logPass("multiple_redemption_on_location is DISABLED for location " + dataSet.get("locationName2_2"));

	}

	@Test(description = "SQ-T3301 [Batched Redemptions-OMM-T942(592)] Verify Allow Location for Multiple Redemption checkbox (Settings > Locations > Location) is visible only when allow_multiple_redemptions is On", groups = {"regression", "dailyrun"}, priority = 3, dataProvider = "TestDataProvider")
	@Owner(name = "Hardik Bhardwaj")
	public void T3301_AllowLocationForMultipleRedemptionCheckbox(String slug, String location_name)
			throws InterruptedException {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slug);

		// navigate to locations in settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		try {
			String result = pageObj.locationPage().clickOnSelectedLocation(location_name);
			pageObj.utils().logPass("Flag for Allow Location for Multiple Redemption on UI is " + result);
		} catch (Exception e) {
			pageObj.utils().logPass(
					"Flag for Allow Location for Multiple Redemption on UI is not found as business feature flag is disabled");
		}
	}

	@DataProvider(name = "TestDataProvider")
	public Object[][] testDataProvider() {

		return new Object[][] {
				// {"slug", "location_name"},
				{ "deloitte", "Deloitte Seattle" }, { "farmerboys", "Automation" }, };

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
