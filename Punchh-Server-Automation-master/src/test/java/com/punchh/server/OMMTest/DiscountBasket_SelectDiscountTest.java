package com.punchh.server.OMMTest;

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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DiscountBasket_SelectDiscountTest {
	private static Logger logger = LogManager.getLogger(AutoSelectKeyTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String baseUrl;
	String blankString = "";
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Utilities utils;
	String externalUID;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
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
		utils = new Utilities(driver);
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ-T3290 [Batched Redemptions-OMM-T916(607)] Verify error message returned in Add Discount API response is fetched from API Messages -> Discount Basket", groups = {
			"regression", "dailyrun" }, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T3290_SelectDiscountBasket() throws InterruptedException {

		// login to instance
//      pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//      pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Multiple Redemption Tab and check the flags
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_enable_discount_locking", "check");
		pageObj.dashboardpage().updateCheckBox();

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// Adding Amount into discount basket
		Response discountBasketResponse21 = pageObj.endpoints().addDiscountAmountToDiscountBasket(token,
				dataSet.get("client"), dataSet.get("secret"), "discount_amount", "100");
		Assert.assertEquals(discountBasketResponse21.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		Assert.assertEquals(discountBasketResponse21.jsonPath().getString("errors.discount_value[0]"),
				"Balance is insufficent to process request.");
		TestListeners.extentTest.get().pass("Secure Api Select Discount error verification is successful");
		logger.info("Secure Api Select Discount error verification is successful");

		// Adding amount into discount basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAPI2(token, dataSet.get("client"),
				dataSet.get("secret"), "discount_amount", "100");
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2AddDiscountBasketLowBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2DiscountBasketValueErrorSchema, discountBasketResponse.asString());
		Assert.assertTrue(isApi2AddDiscountBasketLowBalanceSchemaValidated,
				"API v2 Add Selection to Discount Basket schema validation failed");
		Assert.assertEquals(discountBasketResponse.jsonPath().getString("errors.discount_value[0]"),
				"Balance is insufficent to process request.");
		TestListeners.extentTest.get().pass("Mobile Api Select Discount error verification is successful");
		logger.info("Mobile Api Select Discount error verification is successful");

		// // send reward amount to user Reedemable
		// Response sendRewardResponse =
		// pageObj.endpoints().Api2SendMessageToUser(userID, dataSet.get("apiKey"),
		// "100", "");
		//
		// logger.info("Send redeemable to the user successfully");
		// TestListeners.extentTest.get().pass("Send redeemable to the user
		// successfully");
		//
		// Assert.assertEquals(201, sendRewardResponse.getStatusCode(),
		// "Status code 201 did not matched for api2 send message to user");
		// TestListeners.extentTest.get().pass("Api2 send reward amount to user is
		// successful");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
		// POS Add Discount to Basket
//		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
//				dataSet.get("locationkey"), userID, "discount_amount", "100", externalUID);
//		Assert.assertEquals(discountBasketResponse1.getStatusCode(), 400,
//				"Status code 400 did not match with add discount to basket ");
//		boolean isAddDiscountBasketInvalidExtUidSchemaValidated = Utilities.validateJsonAgainstSchema(
//				ApiResponseJsonSchema.apiErrorObjectSchema, discountBasketResponse1.asString());
//		Assert.assertTrue(isAddDiscountBasketInvalidExtUidSchemaValidated,
//				"Add Discount to Basket API response schema validation failed");
//		Assert.assertEquals(discountBasketResponse1.jsonPath().getString("error"),
//				"Required parameter missing or the value is empty: external_uid");
//		TestListeners.extentTest.get().pass("POS Api Select Discount error verification is successful");
//		logger.info("POS Api Select Discount error verification is successful");

		// Deprecated https://punchhdev.atlassian.net/browse/OMM-1256
		// Adding amount into discount basket
//		Response discountBasketResponse2 = pageObj.endpoints().addDiscountAmountToDiscountBasketAUTH(token,
//				dataSet.get("client"), dataSet.get("secret"), "discount_amount", "100");
//		Assert.assertEquals(discountBasketResponse2.getStatusCode(), 400);
//		Assert.assertEquals(discountBasketResponse2.jsonPath().getString("error"),
//				"Required parameter missing or the value is empty: external_uid");
//		TestListeners.extentTest.get().pass("Auth Api Select Discount error verification is successful");
//		logger.info("Auth Api Select Discount error verification is successful");

		// part two

		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("clientNativegrill"),
				dataSet.get("secretNativegrill"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String token1 = signUpResponse1.jsonPath().get("access_token.token").toString();

		String userID1 = signUpResponse1.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID1, dataSet.get("apiKeyNativegrill"),
				"16", dataSet.get("redeemable_idNativegrill"), "", "");

		utils.logPass("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token1, dataSet.get("clientNativegrill"),
				dataSet.get("secretNativegrill"), dataSet.get("redeemable_idNativegrill"));

		utils.logPass("Reward id " + rewardId + " is generated successfully ");

		// Adding Amount into discount basket
		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token1,
				dataSet.get("clientNativegrill"), dataSet.get("secretNativegrill"), "reward", rewardId);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Adding Amount into discount basket
		Response discountBasketResponse7 = pageObj.endpoints().authListDiscountBasketAdded(token1,
				dataSet.get("clientNativegrill"), dataSet.get("secretNativegrill"), "reward", rewardId);
		Assert.assertEquals(discountBasketResponse7.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		Assert.assertTrue(
				discountBasketResponse7.jsonPath().getString("errors[\"discount_basket_items.discount_id\"][0]")
						.contains("Discount basket items has already been taken for the user."));
		TestListeners.extentTest.get().pass("Secure Api Select Discount error verification is successful");
		logger.info("Secure Api Select Discount error verification is successful");

		// Adding amount into discount basket
		Response discountBasketResponse4 = pageObj.endpoints().addDiscountToBasketAPI2(token1,
				dataSet.get("clientNativegrill"), dataSet.get("secretNativegrill"), "reward", rewardId);
		Assert.assertEquals(discountBasketResponse4.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2AddDiscountBasketAlreadyTakenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2DiscountBasketIdErrorSchema, discountBasketResponse4.asString());
		Assert.assertTrue(isApi2AddDiscountBasketAlreadyTakenSchemaValidated,
				"API v2 Add Selection to Discount Basket schema validation failed");
		Assert.assertTrue(
				discountBasketResponse4.jsonPath().getString("errors[\"discount_basket_items.discount_id\"][0]")
						.contains("Discount basket items has already been taken for the user."));
		TestListeners.extentTest.get().pass("Mobile Api Select Discount error verification is successful");
		logger.info("Mobile Api Select Discount error verification is successful");

		// POS Add Discount to Basket
		Response discountBasketResponse5 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationNativegrill"), userID1, "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse5.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match with add discount to basket ");
		boolean isPosAddDiscountBasketAlreadyTakenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.posDiscountBasketDiscountIdErrorSchema, discountBasketResponse5.asString());
		Assert.assertTrue(isPosAddDiscountBasketAlreadyTakenSchemaValidated,
				"Add Discount to Basket API response schema validation failed");
		Assert.assertTrue(
				discountBasketResponse5.jsonPath().getString("error[\"discount_basket_items.discount_id\"][0]")
						.contains("Discount basket items has already been taken for the user."));
		TestListeners.extentTest.get().pass("POS Api Select Discount error verification is successful");
		logger.info("POS Api Select Discount error verification is successful");

		// Adding amount into discount basket
		Response discountBasketResponse6 = pageObj.endpoints().addDiscountToBasketAUTH(token1,
				dataSet.get("clientNativegrill"), dataSet.get("secretNativegrill"), "reward", rewardId, externalUID);
		Assert.assertEquals(discountBasketResponse6.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isAuthAddDiscountBasketAlreadyTakenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.authDiscountBasketDiscountIdErrorSchema, discountBasketResponse6.asString());
		Assert.assertTrue(isAuthAddDiscountBasketAlreadyTakenSchemaValidated,
				"Add Discount to Basket API response schema validation failed");
		Assert.assertTrue(
				discountBasketResponse6.jsonPath().getString("error.message[\"discount_basket_items.discount_id\"][0]")
						.contains("Discount basket items has already been taken for the user."));
		TestListeners.extentTest.get().pass("Auth Api Select Discount error verification is successful");
		logger.info("Auth Api Select Discount error verification is successful");

	}

	// author=Anant
	@Test(description = "SQ-T4193: Validate that correct discount amount is applied on the purchase when 'Maximum Discounted Modifiers' value is configured in LIS and Filter item set is selected as 'Base Item and Modifiers'", groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void PS_T55_validateCorrectDiscountAmount() throws InterruptedException {
		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// settings-> qualification criteria
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");

		// select campaign
		pageObj.campaignspage().searchAndSelectCamapign1(dataSet.get("QCname"));
		pageObj.campaignspage().clickTestQualificationWithReceipt();

		// edit the field
		pageObj.campaignspage()
				.editMenuItemString(dataSet.get("testQC1") + dataSet.get("testQC2") + dataSet.get("testQC3")
						+ dataSet.get("testQC4") + dataSet.get("testQC5") + dataSet.get("testQC6")
						+ dataSet.get("testQC7"));

		pageObj.campaignspage().clickEvaluateBtn();
		String result = pageObj.campaignspage().getEvaluateResult();
		Assert.assertTrue(result.contains(dataSet.get("expResult")), "expected result value is not equal");
		utils.logPass(result + ": expected result value is equal");
	}

	@Test(description = "SQ-T3719: Verify that the Audit Log is visible for Line Item Selector", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Hardik Bhardwaj")
	public void T3719_VerifyThatTheAuditLogIsVisibleForLineItemSelector() throws InterruptedException {

		String lineItemID1 = Integer.toString(Utilities.getRandomNoFromRange(100, 2000));
		String lineItemFilterName1 = "Automation_LF_ItemID1_" + lineItemID1;

		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to Line Item Selector page and Create a LIS
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().enterDetailsNewLineItemSelectorPage(lineItemFilterName1, lineItemID1,
				"Only Base Items");

		// navigate to Line Item Selector page and Modify a LIS by adding quantity
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().searchAndSelectLineItemSelector(lineItemFilterName1);
		pageObj.lineItemSelectorPage().setLineItemQuantity("is more than", "2");

		// Verify Audit Log Page is Visible within the selected Line item selector
		pageObj.lineItemSelectorPage().auditLogsOfSelectedLIS();

		// navigate to Line Item Selector page and Delete a LIS
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().deleteLineItemSelectors(lineItemFilterName1);

		// Verify Audit Log Page is Visible on selected Line item selector Page
		pageObj.menupage().navigateToSubMenuItem("Settings", "Line Item Selectors");
		pageObj.lineItemSelectorPage().auditLogsOfSelectedLIS();

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