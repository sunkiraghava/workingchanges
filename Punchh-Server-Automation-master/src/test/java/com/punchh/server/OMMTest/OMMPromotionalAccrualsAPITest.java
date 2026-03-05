package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.apimodel.redeemable.RedeemableReceiptRule;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

//Shashank sharma
public class OMMPromotionalAccrualsAPITest {

	static Logger logger = LogManager.getLogger(OMMPromotionalAccrualsAPITest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	private Utilities utils;
	private ApiPayloadObj apipayloadObj;
	private String lisExternalID, qcExternalID, redeemableExternalID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		lisExternalID = null;
		qcExternalID = null;
		redeemableExternalID = null;
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
		apipayloadObj = new ApiPayloadObj();
	}

	public String getMenuItemsBasedOnItemID(String input, String target) {

		String result = null;

		// Split by '^' to separate items
		String[] parts = input.split("\\^");
		for (String part : parts) {
			if (part.contains(target)) {
				result = part;
				break;
			}
		}

		if (result != null) {
			utils.logPass("Matched Segment: " + result);
		} else {
			utils.logPass("Target not found!");
		}

		return result;
	}

	@Test(description = "OMM-T4525 Verify that Valid \"X-Promo-Key\" & \"X-Promo-Signature\" is generated for Business"
			+ "OMM-T4527 Verify the Error handling case for Missing, Invalid \"X-Promo-Key\" for Business"
			+ "OMM-T4553 Verify the 'Provider' Key for Rewards object which should be punchh_discount or external_discount"
			+ "OMM-T4619  Verify the Earning Qualifiers in Checkin for the Menu Items Qualification", priority = 1, groups = {
					"regression", "dailyrun" ,"nonNightly"}, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMMTT4525() throws Exception {		
		String lisName = "Automation_LIS_OMM_T4525_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_OMM_T4525_" + Utilities.getTimestamp();
		String redeemableName = "Automation_Redeemable_OMM_T4525_" + Utilities.getTimestamp();

		// Create LIS Payload with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null for " + lisName);
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty for " + lisName);
		utils.logPass(lisName +" LIS is Created with External ID: " + lisExternalID);

		//Create QC payload 
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setStackDiscounting(true).setReuseQualifyingItems(true)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null for " + qcname);
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty for " + qcname);
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);
		
		// Create Redeemable Payload with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName + " redeemable is Created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID is null in DB for Redeemable External ID: " + redeemableExternalID);
		
		String couponID = dataSet.get("couponID");
		Map<String, Object> mapOfDetails = new HashMap<>();

		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("coupons", dataSet.get("couponsObject"));
	//	mapOfDetails.put("productID_AsQCID", dataSet.get("productID"));
		mapOfDetails.put("itemID", dataSet.get("invalidItemID"));
	
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();

		// Gift Reedemable to User
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableId);
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for api2 send message to user");
		utils.logPass("Sent redeemable to newly signed up user ID: " + userID);

		// Get reward_id
		String rewardID = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), redeemableId);
		Assert.assertNotNull(rewardID, "Reward Id is null");
		Assert.assertFalse(rewardID.isEmpty(), "Reward Id is empty");
		utils.logPass("Reward id " + rewardID + " is generated successfully");
		String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", rewardID);
		mapOfDetails.put("rewards", rewardsObjectString);

		// when receipt not qualified or QC is invalid
		Response userBalanceResponseInvalidProductID = pageObj.endpoints().authApiGetPromotionsAccruals(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(userBalanceResponseInvalidProductID.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code is not matched with 400 for Get Promotions Accounts balance API");
		String expErrorMessageForInvalidItemID = dataSet.get("errorMessageForInvalidItemID");
		String actualErrorMessageForInvalidItemID = userBalanceResponseInvalidProductID.jsonPath().getString("details")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualErrorMessageForInvalidItemID, expErrorMessageForInvalidItemID,
				actualErrorMessageForInvalidItemID + " Error message is not matched for invalid itemID - "
						+ expErrorMessageForInvalidItemID);
		utils.logPass(actualErrorMessageForInvalidItemID + " Error message is matched for invalid itemID - "
				+ expErrorMessageForInvalidItemID);

		// with Valid information , valid itemID
		mapOfDetails.put("itemID", dataSet.get("itemID"));
		Response userBalanceResponse = pageObj.endpoints().authApiGetPromotionsAccruals(userID, dataSet.get("client"),
				dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(userBalanceResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code is not matched with 200 for Get Promotions Accounts balance API");
		String checkinID = userBalanceResponse.jsonPath().getString("transaction.id");
		Assert.assertNotNull(checkinID, "Checkin id is null in Promotional accruals API");
		utils.logPass("Checkin id from promotional accruals api is : " + checkinID);

		String queryToGetMenuItemsFromReceiptDetailsTable = "select menu_items from receipt_details where checkin_id='"
				+ checkinID + "'";
		String menuItemsFromReceiptDetails = DBUtils.executeQueryAndGetColumnValue(env,
				queryToGetMenuItemsFromReceiptDetailsTable, "menu_items");
		utils.logInfo("Menu items from receipt details: " + menuItemsFromReceiptDetails);

		String finalValueForReward = getMenuItemsBasedOnItemID(menuItemsFromReceiptDetails, rewardID);
		utils.logInfo("Reward details from Menu items from receipt details: " + finalValueForReward);
		Assert.assertTrue(finalValueForReward.startsWith("punchh_discount"),
				"punchh_discount is not present in menu items for rewardID id : " + rewardID);
		utils.logPass("Verified that punchh_discount is present in menu items for rewardID id : " + rewardID);

		// Validate that coupon is present in menu items start with "other_discount"
		// when provider is not punchh
		String finalValueForCoupon = getMenuItemsBasedOnItemID(menuItemsFromReceiptDetails, couponID);
		utils.logInfo("Reward details from Menu items from receipt details: " + finalValueForCoupon);
		Assert.assertTrue(finalValueForCoupon.startsWith("other_discount"),
				"other_discount is not present in menu items for rewardID id : " + couponID);
		utils.logPass("Verified that other_discount is present in menu items for couponID id : " + couponID);

		Assert.assertTrue(userBalanceResponse.asString().contains("transaction"),
				"Status code is matched but transaction is not coming in response");
		utils.logPass("verified that transaction is present in response for Get Promotions Accounts balance API");

		Response deleteResponse = pageObj.endpoints().authApiGetPromotionsAccrualsDelete(checkinID, userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		String deletedTransactionID = deleteResponse.jsonPath().getString("transaction.id");
		Assert.assertNotNull(deletedTransactionID, checkinID + " Checkin id is not deleted");
		utils.logPass("Verified that checkin id : " + checkinID + " is deleted successfully");

		Response userBalanceResponse1 = pageObj.endpoints().authApiGetPromotionsAccruals(userID, "",
				dataSet.get("secret"), mapOfDetails);
		// validate status code and error messages
		Assert.assertEquals(userBalanceResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_FORBIDDEN,
				"Failed Get Promotions Accounts balance API response");
		utils.logPass(userBalanceResponse1.getStatusCode()
				+ " status code is matched with 403 for Get Promotions Accounts balance API");

		String actualErrorMessage = userBalanceResponse1.jsonPath().getString("message").replace("[", "").replace("]",
				"");
		String expectedErrorMessage = dataSet.get("expErrorMessage");
		Assert.assertEquals(actualErrorMessage, expectedErrorMessage,
				"Error message did not match for Get Promotions Accounts balance API");
		utils.logPass(actualErrorMessage + " is matched with expected error message: " + expectedErrorMessage);

		String actualDetailsMessage = userBalanceResponse1.jsonPath().getString("details").replace("[", "").replace("]",
				"");
		String expectedDetailsMessage = dataSet.get("expDetailsMessage");
		Assert.assertEquals(actualDetailsMessage, expectedDetailsMessage,
				"Details message did not match for Get Promotions Accounts balance API");
		utils.logPass(actualDetailsMessage + " is matched with expected details message: " + expectedDetailsMessage);

	}

	@Test(description = "OMM-T4541 Verify the positive functional cases for key 'order.ID' for various formats (int, float, alphanumeric, characters, string with hyphens, etc ) for the key 'order.ID' in Accurals API"
			+ "OMM-T4554 Verify that valid 'store_number' give the expected response in API "
			+ "OMM-T4556 Verify the valid input, subtotal amount for the receipt with discount & tax"
			+ "OMM-T4566 Verify that 'placed' time is getting stored in checkins table as receipt_date"
			+ "OMM-T4615 Verify when OnlineOrder location is set in business with store number, checkin gets created for the set location", priority = 2, groups = {
					"regression", "dailyrun" }, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4541() throws Exception {
		// Create LIS
		String lisName1 = "Automation_LIS_OMM_T4541_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "111")
				.addModifierClause("item_id", "==", "$%^^$")
				.setMaxDiscountUnits(2).setProcessingMethod("max_price")
				.build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalID = response1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null for " + lisName1);
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty for " + lisName1);
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalID);
		// Create QC
		String qcname1 = "Automation_QC_OMM_T4541_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null for " + qcname1);
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty for " + qcname1);
		// Get QC ID
		String query = OfferIngestionUtilities.idFromQCQuery.replace("$external_id", qcExternalID);
		String qcID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(qcID, "QC ID is not generated in DB for QC External ID: " + qcExternalID);
		Assert.assertFalse(qcID.isEmpty(), "QC ID is empty in DB for QC External ID: " + qcExternalID);
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalID + " and ID: " + qcID);

		String orderID = CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase();
		Map<String, Object> mapOfDetails = new HashMap<>();

		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("coupons", dataSet.get("couponsObject"));
		mapOfDetails.put("orderId", orderID);
		mapOfDetails.put("productID_AsQCID", qcID);
		mapOfDetails.put("itemID", dataSet.get("itemID"));

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();

		Response userBalanceResponseString = pageObj.endpoints().authApiGetPromotionsAccruals(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		// Now check with alphanumeric value for orderId
		Assert.assertEquals(userBalanceResponseString.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed Get Promotions Accounts balance API response");
		utils.logPass(userBalanceResponseString.getStatusCode()
				+ " status code is matched with 200 for Get Promotions Accounts balance API");
		Assert.assertTrue(userBalanceResponseString.asString().contains("transaction"),
				"Status code is matched but transaction is not coming in response");
		utils.logPass("verified that transaction is present in response for Get Promotions Accounts balance API");

		// verify the location id for the store number
		String checkinID = userBalanceResponseString.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(checkinID, "Checkin id is null in API response");
		long expLocationID = Long.parseLong(dataSet.get("locationID"));

		String getLocationIDQuery = "select location_id from checkins where id ='" + checkinID + "'";
		long actualLocationID = 0;
		String dbValue = "";
		int counter = 0;
		while ((dbValue == null || dbValue.isEmpty()) && counter < 10) {
			dbValue = DBUtils.executeQueryAndGetColumnValue(env, getLocationIDQuery, "location_id");
			utils.logInfo(counter + " : Checking location_id is coming from DB -- " + dbValue);
			counter++;
			utils.longWaitInSeconds(2);
		}
		Assert.assertNotNull(dbValue, "Location ID is not coming from DB for the checkin id: " + checkinID);

		if (dbValue != null && !dbValue.isEmpty()) {
			actualLocationID = Long.parseLong(dbValue);
			logger.info("Final location_id from DB: " + actualLocationID);
		} else {
			logger.error("location_id not found after " + counter + " retries.");
		}

		Assert.assertEquals(actualLocationID, expLocationID,
				"Location ID is not matching for the store number: " + expLocationID);
		utils.logPass("Location ID is matching for the store number: " + expLocationID);

		// Now check with int value for orderId
		mapOfDetails.put("orderId", 12345);
		Response userBalanceResponseInt = pageObj.endpoints().authApiGetPromotionsAccruals(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		// validate status code
		Assert.assertEquals(userBalanceResponseInt.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed Get Promotions Accounts balance API response");
		utils.logPass(userBalanceResponseInt.getStatusCode()
				+ " status code is matched with 200 for Get Promotions Accounts balance API");
		Assert.assertTrue(userBalanceResponseInt.asString().contains("transaction"),
				"Status code is matched but transaction is not coming in response");
		utils.logPass("transaction is present in response for Get Promotions Accounts balance API");

		// Now check with Float value for orderId
		mapOfDetails.put("orderId", 2.2f);
		Response userBalanceResponseFloat = pageObj.endpoints().authApiGetPromotionsAccruals(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		// validate status code
		Assert.assertEquals(userBalanceResponseFloat.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed Get Promotions Accounts balance API response");
		utils.logPass(userBalanceResponseFloat.getStatusCode()
				+ " status code is matched with 200 for Get Promotions Accounts balance API");
		Assert.assertTrue(userBalanceResponseFloat.asString().contains("transaction"),
				"Status code is matched but transaction is not coming in response");
		utils.logPass("verified that transaction is present in response for Get Promotions Accounts balance API");

		// Now check with alphanumeric with hyphen value for orderId
		mapOfDetails.put("orderId", "ORD-1234-5678");
		Response userBalanceResponseHyphens = pageObj.endpoints().authApiGetPromotionsAccruals(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		// validate status code
		Assert.assertEquals(userBalanceResponseHyphens.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed Get Promotions Accounts balance API response");
		utils.logPass(userBalanceResponseHyphens.getStatusCode()
				+ " status code is matched with 200 for Get Promotions Accounts balance API");
		Assert.assertTrue(userBalanceResponseHyphens.asString().contains("transaction"),
				"Status code is matched but transaction is not coming in response");
		utils.logPass("verified that transaction is present in response for Get Promotions Accounts balance API");

		// Verify the valid input, subtotal amount for the receipt with discount & tax
		String getReceiptDetailsQuery = "SELECT receipt_amount, receipt_date FROM checkins where id = '" + checkinID + "'";
		List<Map<String, String>> values = DBUtils.executeQueryAndGetMultipleColumns(env, getReceiptDetailsQuery, 
				new String[] { "receipt_amount", "receipt_date" });
		double actualReceiptSubtotalFromDB = Double.parseDouble(values.get(0).get("receipt_amount"));
		String subtotal = dataSet.get("subtotal");
		String tax = dataSet.get("tax");
		String discount = dataSet.get("discount");
		double expReceiptSubtotal = Double.parseDouble(subtotal) - Double.parseDouble(discount)
				+ Double.parseDouble(tax);
		Assert.assertEquals(actualReceiptSubtotalFromDB, expReceiptSubtotal,
				actualReceiptSubtotalFromDB + "Receipt subtotal is not matching for the checkin id: " + checkinID);
		utils.logPass("Verified that receipt subtotal " + actualReceiptSubtotalFromDB
				+ " is matching for the checkin id: " + checkinID);

		// OMM-T4566 Verify that 'placed' time is getting stored in checkins table as
		// receipt_date
		String actualPlaceFromDB = values.get(0).get("receipt_date");
		Assert.assertEquals(actualPlaceFromDB, dataSet.get("expPlace"),
				"Place is not matching for the checkin id: " + checkinID);
		utils.logPass("Verified that place " + actualPlaceFromDB + " is matching for the checkin id: " + checkinID);

	}

//	@Test(description = "OMM-T4561 Verify the Checkin Expiry for the User based on points balance for Point Unlock Business",priority = 3,  groups = {"regression", "dailyrun"}, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4561() throws Exception {
		String orderID = CreateDateTime.getTimeDateString() + CreateDateTime.getRandomString(6).toLowerCase();
		Map<String, Object> mapOfDetails = new HashMap<>();

		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("coupons", dataSet.get("couponsObject"));
		mapOfDetails.put("orderId", orderID);
		mapOfDetails.put("productID_AsQCID", dataSet.get("productID"));
		mapOfDetails.put("itemID", dataSet.get("itemID"));

		String b_id = dataSet.get("business_id");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "450", "",
				"", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount of 450 to user is successful");

		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "450", "",
				"", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount of 450 to user is successful");

		String txn = "123456" + CreateDateTime.getTimeDateString();
		String date = CreateDateTime.getCurrentDate() + "T17:57:32Z";
		String key = CreateDateTime.getTimeDateString();

		Response checkinResponse = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"),
				dataSet.get("amount"));
		Assert.assertEquals(checkinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match with POS Checkin ");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		pageObj.schedulespage().selectScheduleType(dataSet.get("scheduleName"));
		pageObj.schedulespage().runRecallScheule();

		utils.longWait(2000);

		int count2 = pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "AccountExpireWorker", 10);
		Assert.assertTrue(count2 > 0, "AccountExpireWorker is not called in sidekiq");
		utils.logit("AccountExpireWorker is called in sidekiq");

		int count = pageObj.sidekiqPage().checkSidekiqJob(baseUrl, "BusinessRollingCheckinsWorker", 10);
		Assert.assertTrue(count > 0, "BusinessRollingCheckins is not called in sidekiq");
		utils.logit("BusinessRollingCheckins is called in sidekiq");

		Response userBalanceResponseHyphens = pageObj.endpoints().authApiGetPromotionsAccruals(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);

		// validate status code
		Assert.assertEquals(userBalanceResponseHyphens.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed Get Promotions Accounts balance API response");
		utils.logPass(userBalanceResponseHyphens.getStatusCode()
				+ " status code is matched with 200 for Get Promotions Accounts balance API");

	}// end of validateOMM_T4561

	@Test(description = "OMM-T4648 Verify the API response for Void checkin response for valid request"
			+ "OMM-T4652 Verify the API response Void checkin response for Invalid request"
			+ "OMM-T4653 Verify the API response Void checkin response when the Checkin is already deleted", priority = 4, groups = {
					"regression", "dailyrun","nonNightly" }, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4648() throws Exception {
		// Create LIS
		String lisName1 = "Automation_LIS_OMM_T4648_" + Utilities.getTimestamp();
		String lisPayload1 = apipayloadObj.lineItemSelectorBuilder().setName(lisName1)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_AND_MODIFIERS)
				.addBaseItemClause("item_id", "==", "111")
				.addModifierClause("item_id", "==", "$%^^$")
				.setMaxDiscountUnits(2).setProcessingMethod("max_price")
				.build();
		Response response1 = pageObj.endpoints().createLIS(lisPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(response1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		lisExternalID = response1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null for " + lisName1);
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty for " + lisName1);
		utils.logPass(lisName1 + " LIS is created with External ID: " + lisExternalID);
		// Create QC
		String qcname1 = "Automation_QC_OMM_T4648_" + Utilities.getTimestamp();
		String qcPayload1 = apipayloadObj.qualificationCriteriaBuilder().setName(qcname1).setStackDiscounting(true)
				.setReuseQualifyingItems(true).setQCProcessingFunction("sum_amounts")
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse1 = pageObj.endpoints().createQC(qcPayload1, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse1.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null for " + qcname1);
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty for " + qcname1);
		// Get QC ID
		String query = OfferIngestionUtilities.idFromQCQuery.replace("$external_id", qcExternalID);
		String qcID = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(qcID, "QC ID is not generated in DB for QC External ID: " + qcExternalID);
		Assert.assertFalse(qcID.isEmpty(), "QC ID is empty in DB for QC External ID: " + qcExternalID);
		utils.logPass(qcname1 + " QC is created with External ID: " + qcExternalID + " and ID: " + qcID);

		// Create Redeemable Payload
		String redeemableName = "Automation_Redeemable_OMM_T4648_" + CreateDateTime.getTimeDateString();
		utils.logit("Redeemable Name: " + redeemableName);
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName).setAutoApplicable(false)
				.setReceiptRule(
						RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(2.0).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName + " redeemable is Created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID is null in DB for Redeemable External ID: " + redeemableExternalID);
		
		Map<String, Object> mapOfDetails = new HashMap<>();
		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("coupons", dataSet.get("couponsObject"));
		mapOfDetails.put("productID_AsQCID", qcID);

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();

		// Gift Redeemable to User
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableId);
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 send message to user");
		utils.logPass("API2 send redeemable to user is successful for user ID: " + userID);

		// Get reward_id
		String rewardID = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), redeemableId);
		Assert.assertNotNull(rewardID, "Reward Id is null");
		Assert.assertFalse(rewardID.isEmpty(), "Reward Id is empty");
		utils.logPass("Reward id " + rewardID + " is generated successfully ");

		String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", rewardID);
		mapOfDetails.put("rewards", rewardsObjectString);

		// with Valid information , valid itemID
		mapOfDetails.put("itemID", dataSet.get("itemID"));
		Response userBalanceResponse = pageObj.endpoints().authApiGetPromotionsAccruals(userID, dataSet.get("client"),
				dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(userBalanceResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code is not matched with 200 for Get Promotions Accounts balance API");
		String checkinID = userBalanceResponse.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(checkinID, "Checkin id is null in API response");
		utils.logPass("Checkin id from promotional accruals api is : " + checkinID);

		String invalidCheckinID = checkinID + "22";
		// OMM-T4652 Verify the API response Void checkin response for Invalid request
		// invalid checkin id
		Response deleteInvalidCheckinIdResponse = pageObj.endpoints().authApiGetPromotionsAccrualsDelete(
				invalidCheckinID, userID, dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(deleteInvalidCheckinIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_NOT_FOUND,
				"Status code 404 did not match for API2 send message to user");
		utils.logPass("404 status code matched for invalid checkin id");

		String actualErrorMessage = deleteInvalidCheckinIdResponse.jsonPath().getString("details").replace("[", "")
				.replace("]", "");
		String expErrorMessage = dataSet.get("expErrorMessage");
		Assert.assertEquals(actualErrorMessage, expErrorMessage,
				actualErrorMessage + " Error message is not matched for invalid checkin id - " + expErrorMessage);
		utils.logPass(actualErrorMessage + " Error message is matched for invalid checkin id - " + expErrorMessage);

		String actualTransactionID = deleteInvalidCheckinIdResponse.jsonPath().getString("id") + "";
		Assert.assertEquals(actualTransactionID, String.valueOf(ApiConstants.HTTP_STATUS_NOT_FOUND), 
				invalidCheckinID + " Invalid Checkin id is deleted");
		utils.logPass("Verified that invalid checkin id : " + invalidCheckinID + " is not deleted");

		// OMM-T4648 Verify the API response for Void checkin response for valid request
		// with valid checkin id and user id
		Response deleteResponse = pageObj.endpoints().authApiGetPromotionsAccrualsDelete(checkinID, userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(deleteResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code mismatch for Delete Promotions Accruals API");
		String deletedTransactionID = deleteResponse.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(deletedTransactionID, checkinID + " Checkin id is not deleted");
		utils.logPass("Verified that checkin id : " + checkinID + " is deleted successfully");

		// delete again the same checkin id which is already deleted
		// OMM-T4653 Verify the API response Void checkin response when the Checkin is
		// already deleted
		Response deleteAgainCheckinIdResponse = pageObj.endpoints().authApiGetPromotionsAccrualsDelete(checkinID,
				userID, dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(deleteAgainCheckinIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 400 did not match for API2 send message to user");
		utils.logPass("400 status code matched for invalid checkin id");

		String actualErrorMessage1 = deleteAgainCheckinIdResponse.jsonPath().getString("details").replace("[", "")
				.replace("]", "");
		String expErrorMessage1 = dataSet.get("expErrorMessageForDeletedCheckinIDAgain").replace("${checkinId}",
				checkinID);
		Assert.assertEquals(actualErrorMessage1, expErrorMessage1,
				actualErrorMessage1 + " Error message is not matched for invalid checkin id - " + expErrorMessage1);
		utils.logPass(actualErrorMessage1 + " Error message is matched for invalid checkin id - " + expErrorMessage1);

		String actualTransactionID1 = deleteAgainCheckinIdResponse.jsonPath().getString("id") + "";
		Assert.assertEquals(actualTransactionID1, String.valueOf(ApiConstants.HTTP_STATUS_BAD_REQUEST),
				checkinID + " Invalid Checkin id is deleted");
		utils.logPass("Verified that invalid checkin id : " + checkinID + " is not deleted");

	}

	@Test(description = "OMM-T4602 Verify the Pending Checkin functionality for the user with Accurals API" , priority = 5, groups = {
			"regression", "dailyrun" ,"nonNightly"}, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4602() throws InterruptedException {
		Map<String, Object> mapOfDetails = new HashMap<>();
		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("coupons", dataSet.get("couponsObject"));
		mapOfDetails.put("productID_AsQCID", "2358119");
		mapOfDetails.put("itemID", "101");
		
		// Navigate to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		// set Pending Checkin Strategy
		pageObj.earningPage().selectPendingCheckinStrategy("Automatic after a configured time delay", "4");
		utils.logPass("Checkin strategy updated successfully to create pending checkin");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();

		// send reward amount to user
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "100", "",
				"", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// when receipt not qualified or QC is invalid
		Response userBalanceResponse = pageObj.endpoints().authApiGetPromotionsAccruals(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(userBalanceResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code is not matched with 200 for Get Promotions Accounts balance API");

		String checkinID = userBalanceResponse.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(checkinID, checkinID + " Checkin id not blank ");
		utils.logPass("Verified that checkin id : " + checkinID + " is generated successfully");
		
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickAccountHistory();
		String pointsPending = pageObj.guestTimelinePage().getPendingPoints();
		Assert.assertTrue(pointsPending.contains("Points Pending"),
				"Points pending did not appeared in account history");
		utils.logPass("Pending checkin verified on user time line and account history");
		
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Earning");
		// set Pending Checkin Strategy
		pageObj.earningPage().selectPendingCheckinStrategy("No pending checkins", "0");
		utils.logPass("checkin strategy updated successfully to create pending checkin");
		
		Response deleteResponse = pageObj.endpoints().authApiGetPromotionsAccrualsDelete(checkinID, userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		String deletedTransactionID = deleteResponse.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(deletedTransactionID, checkinID + " Checkin id is not deleted");
		utils.logPass("Verified that checkin id : " + checkinID + " is deleted successfully");

	}
	
	//authApiGetPromotionsAccrualsValidate api
	@Test(description = "OMM-T4731 Validate the request with Valid Account.Id key"
			+ "OMM-T4864 Verify that valid 'store_number' give the expected response in API for Redemptions and Redemptions Validate" , priority = 5, groups = {
			"regression", "dailyrun" }, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4731() throws Exception {
		String lisName = "Automation_LIS_OMM_T4731_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_OMM_T4731_" + Utilities.getTimestamp();
		String redeemableName = "Automation_Redeemable_OMM_T4731_" + Utilities.getTimestamp();
		
		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null for " + lisName);
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty for " + lisName);
		utils.logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setStackDiscounting(true).setReuseQualifyingItems(true)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null for " + qcname);
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty for " + qcname);
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalID);
		
		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName
				+ " redeemable is Created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID is not generated in DB");

		Map<String, Object> mapOfDetails = new HashMap<>();
		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("coupons", dataSet.get("couponsObject"));
		mapOfDetails.put("productID_AsQCID", "101");
		mapOfDetails.put("itemID", "101");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();

		// Send Redeemable to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemableId, "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for api2 send message to user");
		utils.logPass("Sent redeemable to newly signed up user ID: " + userID);
		
		// Get reward_id
		String rewardID = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), redeemableId);
		Assert.assertNotNull(rewardID, "Reward Id is null");
		Assert.assertFalse(rewardID.isEmpty(), "Reward Id is empty");
		long expRewardID = Long.parseLong(rewardID);
		utils.logPass("Reward id " + rewardID + " is generated successfully ");
		
		String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", rewardID);
		mapOfDetails.put("rewards", rewardsObjectString);
		
		Response promotionsAccrualsValidateResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(promotionsAccrualsValidateResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code is not matched with 200 for Get Promotions Accounts balance API");
		String transactionID = promotionsAccrualsValidateResponse.jsonPath().getString("transaction.id");
		Assert.assertNotNull(transactionID, transactionID + " id not blank ");
		utils.logPass("Verified that transaction ID : " + transactionID + " is generated successfully");
		
		long actualRewardID  =  Long.parseLong( promotionsAccrualsValidateResponse.jsonPath().getString("transaction.promotions.id").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRewardID, expRewardID,actualRewardID + " actual transaction reward ID is not matched in response with expected reward id "+ expRewardID);
		utils.logPass("Verified that actual transaction reward ID "+ actualRewardID + " is matched in response with expected reward id "+ expRewardID);

	}

	@Test(description = "OMM-T4732 Verify the response for receipt when the for Basket for Rewards & Coupon Object"
			+ "OMM-T4739 Verify the basket with valid Coupon Campaign and verify the discount_basket_items & discount_basket table for uuid & discount_type" , priority = 5, groups = {
			"regression", "dailyrun", "nonNightly" }, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4732() throws Exception {
		String lisName = "Automation_LIS_OMM_T4732_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_OMM_T4732_" + Utilities.getTimestamp();
		String redeemableName = "Automation_Redeemable_OMM_T4732_" + Utilities.getTimestamp();
		
		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null for " + lisName);
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty for " + lisName);
		utils.logPass(lisName +" LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setStackDiscounting(true).setReuseQualifyingItems(true)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null for " + qcname);
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty for " + qcname);
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalID);
		
		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName
				+ " redeemable is Created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID is not generated in DB");

		Map<String, Object> mapOfDetails = new HashMap<>();
		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("productID_AsQCID", "101");
		mapOfDetails.put("itemID", "101");
		
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		
		// passing uuid for campaign having future "start_date"
		String futureCampUuid = "89178f3b7e6e5c1396f6713b1185bc81fa2cab15";
		Response postDynamicCouponScheduledEmailResponse = pageObj.endpoints()
				.postDynamicCoupon(dataSet.get("apiKey"), userEmail, futureCampUuid);
		String generatedCodeName = postDynamicCouponScheduledEmailResponse.jsonPath().getString("coupon");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"450", redeemableId, "", "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");
		
		// Get reward_id
		String rewardID = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), redeemableId);
		Assert.assertNotNull(rewardID, "Reward Id is null");
		Assert.assertFalse(rewardID.isEmpty(), "Reward Id is empty");
		long expRewardID = Long.parseLong(rewardID);
		utils.logPass("Reward id " + rewardID + " is generated successfully ");	
		
		String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", rewardID);
		mapOfDetails.put("rewards", rewardsObjectString);
		String couponObjectString = dataSet.get("couponsObject").replace("${couponCode}", generatedCodeName);
		mapOfDetails.put("coupons", couponObjectString);
		
		Response userBalanceResponse = pageObj.endpoints().authApiGetPromotionsAccrualsValidate(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(userBalanceResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code is not matched with 200 for Get Promotions Accounts balance API");
		String checkinID = userBalanceResponse.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(checkinID, checkinID + " Checkin id not blank ");
		utils.logPass("Verified that checkin id : " + checkinID + " is generated successfully");
		
		long actualRewardID  =  Long.parseLong( userBalanceResponse.jsonPath().getString("transaction.promotions.id[0]").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualRewardID, expRewardID,actualRewardID + " actual reward ID is not matched in response with expected reward id "+ expRewardID);
		utils.logPass("Verified that actual reward ID "+ actualRewardID + " is matched in response with expected reward id "+ expRewardID);
		
		String actualPromotionsType = userBalanceResponse.jsonPath().getString("transaction.promotions.type[0]").replace("[", "").replace("]", "");
		Assert.assertEquals(actualPromotionsType, "Reward",actualPromotionsType + " actual promotion type is not matched in response with expected promotion type Reward");
		utils.logPass("Verified that actual promotion type "+ actualPromotionsType + " is matched in response with expected promotion type Reward");
		
		String actualCouponID  =  userBalanceResponse.jsonPath().getString("transaction.promotions.id[1]").replace("[", "").replace("]", "");
		Assert.assertEquals(actualCouponID, generatedCodeName,actualCouponID + " actual reward ID is not matched in response with expected reward id "+ generatedCodeName);
		utils.logPass("Verified that actual Coupon ID "+ actualCouponID + " is matched in response with expected reward id "+ generatedCodeName);
		
		String actualPromotionsCouponType = userBalanceResponse.jsonPath().getString("transaction.promotions.type[1]").replace("[", "").replace("]", "");
		Assert.assertEquals(actualPromotionsCouponType, "Coupon",actualPromotionsCouponType + " actual promotion type is not matched in response with expected promotion type Coupon");
		utils.logPass("Verified that actual promotion type "+ actualPromotionsCouponType + " is matched in response with expected promotion type Coupon");

	}
	
	@Test(description = "OMM-T4737  Verify the basket with valid Redeemable and verify the discount_basket_items & discount_basket"
			+ "OMM-T4737 Verify the basket with valid Reward and verify the discount_basket_items & discount_basket "
			+ "OMM-T4844 Verify the error logs when Invalid Reward.ID is added in the API Request"
			+ "OMM-T4864 Verify that valid 'store_number' give the expected response in API for Redemptions and Redemptions Validate" , priority = 5, groups = {
			"regression", "dailyrun" }, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4736() throws Exception {
		
		String lisName = "Automation_LIS_OMM_T4736_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_OMM_T4736_" + Utilities.getTimestamp();
		String redeemableName = "Automation_Redeemable_OMM_T4736_" + Utilities.getTimestamp();

		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null for " + lisName);
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty for " + lisName);
		utils.logPass(lisName +" LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setStackDiscounting(true).setReuseQualifyingItems(true)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null for " + qcname);
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty for " + qcname);
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalID);
		
		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setPoints(5).setApplicable_as_loyalty_redemptionFlag(true)
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName
				+ " redeemable is Created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID is not generated in DB");

		Map<String, Object> mapOfDetails = new HashMap<>();
		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("productID_AsQCID", "101");
		mapOfDetails.put("itemID", "101");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();

		// send reward amount to user
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"100");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 send message to user");
		utils.logPass("API2 send reward amount is successful for user ID: " + userID);
		
		String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", redeemableId);
		mapOfDetails.put("rewards", rewardsObjectString);
		Response userBalanceResponse = pageObj.endpoints().authApiGetPromotionsAccrualsRedemption(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(userBalanceResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code is not matched with 200 for Get Promotions Accounts balance API");

		String actualUUIDFromResponse = userBalanceResponse.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(actualUUIDFromResponse, actualUUIDFromResponse + " Checkin id not blank ");
		utils.logPass("Verified that redemption id : " + actualUUIDFromResponse + " is generated successfully");
		
		String actualPromotionsType = userBalanceResponse.jsonPath().getString("transaction.promotions.type[0]").replace("[", "").replace("]", "");
		Assert.assertEquals(actualPromotionsType, "Reward",actualPromotionsType + " actual promotion type is not matched in response with expected promotion type Reward");
		utils.logPass("Verified that actual promotion type "+ actualPromotionsType + " is matched in response with expected promotion type Reward");

		String discountTypeFromDBQuery = OfferIngestionUtilities.discountBasketItemForUserQuery.replace("$user_id", userID);
		String discountTypeFromDBResult = DBUtils.executeQueryAndGetColumnValue(env, discountTypeFromDBQuery,"discount_type");
		Assert.assertEquals(discountTypeFromDBResult, "redeemable",discountTypeFromDBResult + " actual discount_type is not matched in DB with expected discount_type redeemable");
		utils.logPass("Verified that actual discount_type "+ discountTypeFromDBResult + " is matched in DB with expected discount_type redeemable");

		String uuidFromDBQuery = OfferIngestionUtilities.getUuidFromDiscountBasketsQuery.replace("$user_id", userID);
		String UUIDFromDBResult = DBUtils.executeQueryAndGetColumnValue(env, uuidFromDBQuery,"uuid");
		Assert.assertEquals(UUIDFromDBResult, actualUUIDFromResponse,UUIDFromDBResult + " actual uuid is not matched in DB with expected uuid from response "+ actualUUIDFromResponse);
		utils.logPass("Verified that actual UUID "+ UUIDFromDBResult + " is matched in DB with expected UUID "+ actualUUIDFromResponse);
		
	}
	
	@Test(description = "OMM-T4738 Verify the basket with valid Subscription and verify the discount_basket_items & discount_basket table for uuid & discount_type"
			+ "OMM-T4798 Verify the basket with valid Discount amount and verify that entries should be created in database tables" , priority = 5, groups = {
			"regression", "dailyrun", "nonNightly" }, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4738() throws Exception {
		String endDateTime = CreateDateTime.getFutureDate(1) + " 10:00 PM";
		Map<String, Object> mapOfDetails = new HashMap<>();
		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("productID_AsQCID", "101");
		mapOfDetails.put("itemID", "101");

		//String spName = "Do Not Delete Subscription_OMMT2527";
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
		String PlanID = dataSet.get("planID");

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 subscription purchase");
		int subscription_id = Integer
				.parseInt(purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		utils.logPass(userEmail + " purchased " + subscription_id + ". Plan id = " + PlanID);

		String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", subscription_id+"");
		mapOfDetails.put("rewards", rewardsObjectString);
		Response userBalanceResponse = pageObj.endpoints().authApiGetPromotionsAccrualsRedemption(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(userBalanceResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code is not matched with 200 for Get Promotions Accounts balance API");

		String actualUUIDFromResponse = userBalanceResponse.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(actualUUIDFromResponse, actualUUIDFromResponse + " Checkin id not blank ");
		utils.logPass("Verified that redemption id : " + actualUUIDFromResponse + " is generated successfully");
		
		String actualPromotionsType = userBalanceResponse.jsonPath().getString("transaction.promotions.type[0]").replace("[", "").replace("]", "");
		Assert.assertEquals(actualPromotionsType, "Reward",actualPromotionsType + " actual promotion type is not matched in response with expected promotion type Reward");
		utils.logPass("Verified that actual promotion type "+ actualPromotionsType + " is matched in response with expected promotion type Reward");

		String discountTypeFromDBQuery = OfferIngestionUtilities.discountBasketItemForUserQuery.replace("$user_id", userID);
		String discountTypeFromDBResult = DBUtils.executeQueryAndGetColumnValue(env, discountTypeFromDBQuery,"discount_type");
		Assert.assertEquals(discountTypeFromDBResult, "subscription",discountTypeFromDBResult + " actual discount_type is not matched in DB with expected discount_type redeemable");
		utils.logPass("Verified that actual discount_type "+ discountTypeFromDBResult + " is matched in DB with expected discount_type redeemable");

		String uuidFromDBQuery = OfferIngestionUtilities.getUuidFromDiscountBasketsQuery.replace("$user_id", userID);
		String UUIDFromDBResult = DBUtils.executeQueryAndGetColumnValue(env, uuidFromDBQuery,"uuid");
		Assert.assertEquals(UUIDFromDBResult, actualUUIDFromResponse,UUIDFromDBResult + " actual uuid is not matched in DB with expected uuid from response "+ actualUUIDFromResponse);
		utils.logPass("Verified that actual UUID "+ UUIDFromDBResult + " is matched in DB with expected UUID "+ actualUUIDFromResponse);
	
	}
	
	@Test(description = "OMM-T4758 Verify the discount calculation for QC- Sum of amounts & validate the menu items in database for Redemptions API", priority = 1, groups = {
					"regression", "dailyrun" }, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4758() throws Exception {
		String lisName = "Automation_LIS_OMM_T4758_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_OMM_T4758_" + Utilities.getTimestamp();
		String redeemableName = "Automation_Redeemable_OMM_T4758_" + Utilities.getTimestamp();
		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null for " + lisName);
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty for " + lisName);
		utils.logPass(lisName +" LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 10% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setStackDiscounting(true).setReuseQualifyingItems(true)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null for " + qcname);
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty for " + qcname);
		utils.logPass(qcname + " QC is created with External ID: " + qcExternalID);
		
		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("activate_now", true).setBooleanField("indefinetely", true).setDiscountChannel("all")
				.setPoints(5).setApplicable_as_loyalty_redemptionFlag(true)
				.addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName
				+ " redeemable is created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID is not generated in DB");
		Assert.assertFalse(redeemableId.isEmpty(), "Redeemable ID is empty in DB");

		Map<String, Object> mapOfDetails = new HashMap<>();
		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("productID_AsQCID", "101");
		mapOfDetails.put("itemID", "101");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();

		// Gift Reedemable to User
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableId);
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 send message to user");
		utils.logPass("Api2  send reward reedemable to user is successful");

		// Get reward_id
		String rewardID = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), redeemableId);
		Assert.assertNotNull(rewardID, "Reward Id is null");
		Assert.assertFalse(rewardID.isEmpty(), "Reward Id is empty");
		utils.logPass("Reward id " + rewardID + " is generated successfully");

		String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", rewardID);
		mapOfDetails.put("rewards", rewardsObjectString);

		Response userBalanceResponse = pageObj.endpoints().authApiGetPromotionsAccrualsRedemption(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(userBalanceResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code is not matched with 200 for Get Promotions Accounts balance API");
		
		double expTotalAmount = Double.parseDouble(dataSet.get("expAppliedDiscount"));
		double actualTotalAmount = Double.parseDouble(userBalanceResponse.jsonPath().getString("transaction.promotions.discount").replace("[", "").replace("]", ""));
		
		Assert.assertEquals(actualTotalAmount, expTotalAmount,actualTotalAmount + " actual total discount amount is not matched in response with expected total discount amount "+ expTotalAmount);
		utils.logPass("Verified that actual total discount amount "+ actualTotalAmount + " is matched in response with expected total discount amount "+ expTotalAmount);
		
		String actualUUIDFromResponse = userBalanceResponse.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(actualUUIDFromResponse, actualUUIDFromResponse + " Checkin id not blank ");
		utils.logPass("Verified that redemption id : " + actualUUIDFromResponse + " is generated successfully");
		
		String actualPromotionsType = userBalanceResponse.jsonPath().getString("transaction.promotions.type[0]").replace("[", "").replace("]", "");
		Assert.assertEquals(actualPromotionsType, "Reward",actualPromotionsType + " actual promotion type is not matched in response with expected promotion type Reward");
		utils.logPass("Verified that actual promotion type "+ actualPromotionsType + " is matched in response with expected promotion type Reward");

	}
	
	@Test(description = "OMM-T4759 Verify the discount calculation for QC- Rate Rollback & validate the menu items in database for Redemptions API"
			+ "OMM-T4777 Successful void of a valid redemption", priority = 1, groups = { "regression",
					"dailyrun" }, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4759() throws Exception {
		String lisName = "Automation_LIS_OMM_T4759_" + Utilities.getTimestamp();
		String qcname = "Automation_QC_OMM_T4759_" + Utilities.getTimestamp();
		String redeemableName_AutoRedemptionFlagON = "Automation_Redeemable_OMM_T4759_" + Utilities.getTimestamp();

		// Create LIS with base item 101
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY).addBaseItemClause("item_id", "in", "101")
				.build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(lisResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get LIS External ID
		lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(lisExternalID, "LIS External ID is null for " + lisName);
		Assert.assertFalse(lisExternalID.isEmpty(), "LIS External ID is empty for " + lisName);
		utils.logPass(lisName +" LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 10% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setStackDiscounting(true).setReuseQualifyingItems(true)
				.setQCProcessingFunction("rate_rollback").setUnitDiscount(2.0).addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(qcExternalID, "QC External ID is null for " + qcname);
		Assert.assertFalse(qcExternalID.isEmpty(), "QC External ID is empty for " + qcname);
		utils.logPass(qcname + "QC is created with External ID: " + qcExternalID);

		// Create Redeemable with above QC
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName_AutoRedemptionFlagON)
				.setAutoApplicable(true)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing")
						.redeeming_criterion_id(qcExternalID).build())
				.setPoints(5).setApplicable_as_loyalty_redemptionFlag(true).setBooleanField("activate_now", true)
				.setBooleanField("indefinetely", true).setDiscountChannel("all").addCurrentData().build();
		// Create Redeemable
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		Assert.assertNotNull(redeemableExternalID, "Redeemable External ID is null");
		Assert.assertFalse(redeemableExternalID.isEmpty(), "Redeemable External ID is empty");
		utils.logPass(redeemableName_AutoRedemptionFlagON
				+ " redeemable is created with External ID: " + redeemableExternalID);
		// Get Redeemable ID from DB
		String query = OfferIngestionUtilities.getRedeemableIdQuery.replace("$external_id", redeemableExternalID);
		String redeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		Assert.assertNotNull(redeemableId, "Redeemable ID is not generated in DB");

		Map<String, Object> mapOfDetails = new HashMap<>();
		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("productID_AsQCID", "101");
		mapOfDetails.put("itemID", "101");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String token = signUpResponseUser.jsonPath().get("access_token.token").toString();
		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();

		// Gift Redeemable to User
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				redeemableId);
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not match for API2 send message to user");
		utils.logPass("API2 send redeemable to user is successful for user ID: " + userID);

		// Get reward_id
		String rewardID = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"),
				dataSet.get("secret"), redeemableId);
		Assert.assertNotNull(rewardID, "Reward Id is null");
		Assert.assertFalse(rewardID.isEmpty(), "Reward Id is empty");
		utils.logPass("Reward id " + rewardID + " is generated successfully ");

		String rewardsObjectString = dataSet.get("rewardsObject").replace("${rewardID}", rewardID);
		mapOfDetails.put("rewards", rewardsObjectString);

		Response userBalanceResponse = pageObj.endpoints().authApiGetPromotionsAccrualsRedemption(userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(userBalanceResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code is not matched with 200 for Get Promotions Accounts balance API");
		
		double expTotalAmount = Double.parseDouble(dataSet.get("expAppliedDiscount"));
		double actualTotalAmount = Double.parseDouble(userBalanceResponse.jsonPath().getString("transaction.promotions.discount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualTotalAmount, expTotalAmount,actualTotalAmount + " actual total discount amount is not matched in response with expected total discount amount "+ expTotalAmount);
		utils.logPass("Verified that actual total discount amount "+ actualTotalAmount + " is matched in response with expected total discount amount "+ expTotalAmount);
		
		String actualUUIDFromResponse = userBalanceResponse.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(actualUUIDFromResponse, actualUUIDFromResponse + " Checkin id not blank ");
		utils.logPass("Verified that redemption id : " + actualUUIDFromResponse + " is generated successfully");
		
		String actualPromotionsType = userBalanceResponse.jsonPath().getString("transaction.promotions.type[0]").replace("[", "").replace("]", "");
		Assert.assertEquals(actualPromotionsType, "Reward",actualPromotionsType + " actual promotion type is not matched in response with expected promotion type Reward");
		utils.logPass("Verified that actual promotion type "+ actualPromotionsType + " is matched in response with expected promotion type Reward");
		
		mapOfDetails.put("rewards", rewardID);
		Response voidRedemptionDeleteResponse = pageObj.endpoints().authApiGetPromotionsRedemptionDelete(actualUUIDFromResponse, userID,
				dataSet.get("client"), dataSet.get("secret"), mapOfDetails);
		Assert.assertEquals(voidRedemptionDeleteResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code is not matched with 200 for Get Promotions Accounts balance API");
		
		String actualUUIDFromResponse1 = voidRedemptionDeleteResponse.jsonPath().getString("transaction.id") + "";
		Assert.assertNotNull(actualUUIDFromResponse1, actualUUIDFromResponse1 + " Checkin id not blank ");
		utils.logPass("Verified that void redemption id : " + actualUUIDFromResponse1 + " is generated successfully");
		
	}
	
	@Test(description = "OMM-T4844  Verify the error logs when Invalid reward.id is added in the API Request"
			+ "OMM-T4858 Verify the error logs when Invalid Redeemable.ID is added in the API Request"
			+ "OMM-T4859 Verify the error logs when Invalid Subscription.ID is added in the API Request"
			+ "OMM-T4860 Verify the error logs when Invalid Coupon.ID is added in the API Request", priority = 5, groups = {
			"regression", "dailyrun" }, enabled = true)
	@Owner(name = "Shashank Sharma")
	public void validateOMM_T4844() throws Exception {

		Map<String, Object> mapOfDetails = new HashMap<>();
		mapOfDetails.put("storeNumber", dataSet.get("locationStoreNumber"));
		mapOfDetails.put("productID_AsQCID", "101");
		mapOfDetails.put("itemID", "101");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser, "API 2 user signup");
		String userID = signUpResponseUser.jsonPath().get("user.user_id").toString();
		utils.logPass("API2 user signup is successful for user ID: " + userID);

		// OMM-T4844 Verify the error logs when Invalid Reward.ID is added in the API
		// Request :- Start
		String rewardsObjectStringInvalid = dataSet.get("rewardsObject").replace("${rewardID}", "989769")
				.replace("redeemable", "reward");
		mapOfDetails.put("rewards", rewardsObjectStringInvalid);

		Response userBalanceResponseInvalidRewardIdResponse = pageObj.endpoints()
				.authApiGetPromotionsAccrualsRedemption(userID, dataSet.get("client"), dataSet.get("secret"),
						mapOfDetails);
		Assert.assertEquals(userBalanceResponseInvalidRewardIdResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code is not matched with 400 for Get Promotions Accounts balance API with invalid reward id");
		String actualErrorMessage = userBalanceResponseInvalidRewardIdResponse.jsonPath().getString("code")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualErrorMessage, dataSet.get("expErrorMessageForInvalidrewardID"),
				actualErrorMessage + " Error message is not matched for invalid reward id");
		utils.logPass("Verified that error message : " + actualErrorMessage + " is generated successfully for invalid reward id");
		// OMM-T4844 Verify the error logs when Invalid Reward.ID is added in the API
		// Request :- END

		// OMM-T4858 Verify the error logs when Invalid Redeemable.ID is added in the
		// API Request -- START
		String redeemableObjectStringInvalid = dataSet.get("rewardsObject").replace("${rewardID}", "989769");
		mapOfDetails.put("rewards", redeemableObjectStringInvalid);

		Response userBalanceResponseInvalidRedeemableIdResponse = pageObj.endpoints()
				.authApiGetPromotionsAccrualsRedemption(userID, dataSet.get("client"), dataSet.get("secret"),
						mapOfDetails);
		Assert.assertEquals(userBalanceResponseInvalidRedeemableIdResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code is not matched with 400 for Get Promotions Accounts balance API with invalid reward id");

		String actualErrorMessageRedeemable = userBalanceResponseInvalidRedeemableIdResponse.jsonPath()
				.getString("code").replace("[", "").replace("]", "");
		Assert.assertEquals(actualErrorMessageRedeemable, dataSet.get("expErrorMessageForInvalidrewardID"),
				actualErrorMessageRedeemable + " Error message is not matched for invalid redeemable id");
		utils.logPass("Verified that error message : " + actualErrorMessageRedeemable + " is generated successfully for invalid redeemable id");
		// OMM-T4858 Verify the error logs when Invalid Redeemable.ID is added in the
		// API Request -- END

		// OMM-T4844 Verify the error logs when Invalid Reward.ID is added in the API
		// Request :- Start
		String rsubscriptionObjectStringInvalid = dataSet.get("rewardsObject").replace("${rewardID}", "989769")
				.replace("redeemable", "subscription");
		mapOfDetails.put("rewards", rsubscriptionObjectStringInvalid);

		Response userBalanceResponseInvalidSubscriptionIdResponse = pageObj.endpoints()
				.authApiGetPromotionsAccrualsRedemption(userID, dataSet.get("client"), dataSet.get("secret"),
						mapOfDetails);
		Assert.assertEquals(userBalanceResponseInvalidSubscriptionIdResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code is not matched with 400 for Get Promotions Accounts balance API with invalid reward id");

		String actualErrorMessageSubscription = userBalanceResponseInvalidSubscriptionIdResponse.jsonPath()
				.getString("code").replace("[", "").replace("]", "");
		Assert.assertEquals(actualErrorMessageSubscription, dataSet.get("expErrorMessageForInvalidrewardID"),
				actualErrorMessageSubscription + " Error message is not matched for invalid reward id");
		utils.logPass("Verified that error message : " + actualErrorMessageSubscription + " is generated successfully for invalid subscription id");
		// OMM-T4844 Verify the error logs when Invalid Reward.ID is added in the API
		// Request :- END

		// OMM-T4860 Verify the error logs when Invalid Coupon.ID is added in the API
		// Request :- Start
		String couponObjectStringInvalid = dataSet.get("rewardsObjectInvalidCoupon").replace("${rewardID}",
				"C729GIA28");

		mapOfDetails.put("rewards", couponObjectStringInvalid);

		Response userBalanceResponseInvalidCouponIdResponse = pageObj.endpoints()
				.authApiGetPromotionsAccrualsRedemption(userID, dataSet.get("client"), dataSet.get("secret"),
						mapOfDetails);
		Assert.assertEquals(userBalanceResponseInvalidCouponIdResponse.statusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code is not matched with 400 for Get Promotions Accounts balance API with invalid reward id");

		String actualErrorMessageCoupon = userBalanceResponseInvalidCouponIdResponse.jsonPath().getString("code")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualErrorMessageCoupon, dataSet.get("expErrorMessageForInvalidrewardID"),
				actualErrorMessageCoupon + " Error message is not matched for invalid reward id");
		utils.logPass("Verified that error message : " + actualErrorMessageCoupon + " is generated successfully for invalid coupon id");
		// OMM-T4860 Verify the error logs when Invalid Coupon.ID is added in the API
		// Request :- END

	}
	
	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
		pageObj.utils().clearDataSet(dataSet);
		if (driver != null) {
			driver.quit();
		}
		logger.info("Browser closed");
	}

}