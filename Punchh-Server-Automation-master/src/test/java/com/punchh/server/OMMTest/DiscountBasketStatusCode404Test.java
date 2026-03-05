package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DiscountBasketStatusCode404Test {
	private static Logger logger = LogManager.getLogger(DiscountBasketStatusCode404Test.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;

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
	}

	@Test(description = "SQ-T3723 [Auth Fetch Active Basket] Verify if user doesn't have any active basket than user get 404 as Response error code for /discounts/active || SQ-T3727 [Auth - Remove Discounts From Basket] Verify if user doesn't have any active basket than user get 404 as Response error code for /discounts/active || SQ-T3725 [API2 - Remove Discounts From Basket] Verify if user doesn't have any active basket than user get 404 as Response error code for /discounts/active || SQ-T3721 [API2 - Fetch Active Basket] Verify if user doesn't have any active basket than user get 404 as Response error code for /discounts/active || SQ-T3728 [POS - Basket Unlock] Verify if user doesn't have any active basket than user get 404 as Response error code for /discounts/active || SQ-T3722 [POS - Fetch Active Basket] Verify if user doesn't have any active basket than user get 404 as Response error code for /discounts/active || SQ-T3720 [Secure API - Fetch Active Basket] Verify if user doesn't have any active basket than user get 404 as Response error code for /discounts/active || SQ-T3726 [POS - Remove Discounts From Basket] Verify if user doesn't have any active basket than user get 404 as Response error code for /discounts/active || SQ-T3724 [Secure API - Remove Discounts From Basket] Verify if user doesn't have any active basket than user get 404 as Response error code for /discounts/active", groups = {"regression", "dailyrun"})
	@Owner(name = "Hardik Bhardwaj")
	public void T3723_Auth_Fetch_Active_Basket() throws InterruptedException {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions", "check");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_allow_multiple_redemptions_on_all_locations",
				"check");
		pageObj.dashboardpage().updateCheckBox();

		// Click Cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().onEnableRewardLocking();

		// navigate to locations in settings
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage()
				.clickOnSelectedLocationAndEnableMultipleRedemption(dataSet.get("location_name_winghouse"));
		pageObj.utils().logit("Flag for Allow Location for Multiple Redemption on UI is Checked");

		// Secure Api fetch active basket
		Response basketDiscountDetailsResponse = pageObj.endpoints().getDiscountBasketDetailsOfUsersAPIMobile(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_NOT_FOUND, basketDiscountDetailsResponse.getStatusCode(),
				"Status code 404 did not matched for fetch active basket error that state User don't have any active basket");
		pageObj.utils().logPass("Secure Api fetch active basket is giving correct error");

		// Secure Api remove discount from basket
		String expdiscount_basket_item_id = Integer.toString(Utilities.getRandomNoFromRange(100000, 500000));
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserAPI(token, dataSet.get("client"),
				dataSet.get("secret"), expdiscount_basket_item_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_NOT_FOUND, deleteBasketResponse.getStatusCode(),
				"Status code 404 did not matched for remove discount from basket error that state User don't have any active basket");
		pageObj.utils().logPass("Secure Api remove discount from basket is giving correct error");

		// API2 fetch active basket
		Response basketDiscountDetailsResponse1 = pageObj.endpoints().getUserDiscountBasketDetailsUsingAPI2(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_NOT_FOUND, basketDiscountDetailsResponse1.getStatusCode(),
				"Status code 404 did not matched for fetch active basket error that state User don't have any active basket");
		boolean isApi2FetchActiveBasketNotFoundSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NoActiveBasketErrorSchema, basketDiscountDetailsResponse1.asString());
		Assert.assertTrue(isApi2FetchActiveBasketNotFoundSchemaValidated,
				"API v2 Fetch active discount basket schema validation failed");
		pageObj.utils().logPass("API2 fetch active basket is giving correct error");

		// API2 remove discount from basket
		String expDiscountBasketItemId = Integer.toString(Utilities.getRandomNoFromRange(1000000, 5000000));
		Response deleteBasketResponse1 = pageObj.endpoints().deleteDiscountBasketForUserAPI2(token,
				dataSet.get("client"), dataSet.get("secret"), expDiscountBasketItemId);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_NOT_FOUND, deleteBasketResponse1.getStatusCode(),
				"Status code 404 did not matched for remove discount from basket error that state User don't have any active basket");
		boolean isApi2RemoveDiscountBasketNotFoundSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2NoActiveBasketErrorSchema, deleteBasketResponse1.asString());
		Assert.assertTrue(isApi2RemoveDiscountBasketNotFoundSchemaValidated,
				"API2 Remove discount from basket Schema Validation failed");
		pageObj.utils().logPass("API2 remove discount from basket is giving correct error");

		// Auth API fetch active basket
		Response basketDiscountDetailsResponse2 = pageObj.endpoints().getUserDiscountBasketDetailsUsingAUTH(token,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_NOT_FOUND, basketDiscountDetailsResponse2.getStatusCode(),
				"Status code 404 did not matched for fetch active basket error that state User don't have any active basket");
		boolean isAuthFetchActiveBasketNotFoundSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, basketDiscountDetailsResponse2.asString());
		Assert.assertTrue(isAuthFetchActiveBasketNotFoundSchemaValidated,
				"Auth fetch active basket Schema Validation failed");
		pageObj.utils().logPass("Auth Api fetch active basket is giving correct error");

		// Auth remove discount from basket
		pageObj.utils().logit("Auth remove discount from basket");
		String expdiscount_basket_item_id2 = Integer.toString(Utilities.getRandomNoFromRange(10000000, 50000000));
		Response deleteBasketResponse2 = pageObj.endpoints().deleteDiscountBasketForUserAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), expdiscount_basket_item_id2);
		Assert.assertEquals(deleteBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for remove discount from basket error that state User don't have any active basket");
		boolean isAuthRemoveDiscountBasketNoActiveBasketSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authMessageCodeErrorObjectSchema, deleteBasketResponse2.asString());
		Assert.assertTrue(isAuthRemoveDiscountBasketNoActiveBasketSchemaValidated,
				"Auth Api remove discount from basket's error is verfied");
		pageObj.utils().logPass("Auth Api remove discount from basket's error is verfied");

		// POS fetch active basket
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(500000, 1000000));
		Response basketDiscountDetailsResponse3 = pageObj.endpoints().fetchActiveBasketPOSAPI(userID,
				dataSet.get("locationkey"), externalUID);
		Assert.assertEquals(basketDiscountDetailsResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for fetch active basket error that state User don't have any active basket");
		boolean isPosFetchActiveBasketNotFoundSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, basketDiscountDetailsResponse3.asString());
		Assert.assertTrue(isPosFetchActiveBasketNotFoundSchemaValidated,
				"POS fetch active basket Schema Validation failed");
		pageObj.utils().logPass("POS Api fetch active basket is giving correct error");

		// POS remove discount from basket
		String expdiscount_basket_item_id3 = Integer.toString(Utilities.getRandomNoFromRange(100000000, 500000000));
		String externalUID1 = Integer.toString(Utilities.getRandomNoFromRange(5000000, 10000000));
		Response deleteBasketResponse3 = pageObj.endpoints().removeDiscountFromBasketPOSAPI(dataSet.get("locationkey"),
				userID, expdiscount_basket_item_id3, externalUID1);
		Assert.assertEquals(deleteBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not matched for remove discount from basket error that state User don't have any active basket");
		boolean isRemoveDiscountBasketItemNotFoundSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, deleteBasketResponse3.asString());
		Assert.assertTrue(isRemoveDiscountBasketItemNotFoundSchemaValidated,
				"POS remove discount from basket Schema Validation failed");
		pageObj.utils().logPass("POS Api remove discount from basket is giving correct error");

		// POS basket Unlock API
		String externalUID2 = Integer.toString(Utilities.getRandomNoFromRange(50000000, 100000000));
		Response basketUnlockResponse = pageObj.endpoints().basketUnlockPOSAPI(dataSet.get("locationkey"), userID,
				externalUID2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_NOT_FOUND, basketUnlockResponse.getStatusCode(),
				"Status code 404 did not matched for POS basket Unlock error that state User don't have any active basket");
		pageObj.utils().logPass("POS Api POS basket Unlock is giving correct error");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().offEnableRewardLocking();

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
