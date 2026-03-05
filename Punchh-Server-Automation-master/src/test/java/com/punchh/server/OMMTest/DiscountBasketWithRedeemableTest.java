package com.punchh.server.OMMTest;

import static com.punchh.server.pages.CockpitLocationPage.isValidUUID;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class DiscountBasketWithRedeemableTest {
	private static Logger logger = LogManager.getLogger(DiscountBasketWithRedeemableTest.class);
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
	String discount_details0, discount_details1, discount_details2, discount_details3 = "";
	String externalUID;
	Properties prop;
	private Utilities utils;

	private static Map<String, String> DataSet;
	public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
	public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
	String lisDeleteBaseQuery = "Delete from line_item_selectors where external_id = '${externalID}' and business_id='${businessID}'";
	String getQC_idString = "select id from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCFromQualification_criteriaQuery = "delete from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCQueryFromQualifying_expressionsQuery = "delete from qualifying_expressions where qualification_criterion_id ='$qcID'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {

		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
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

    @Test(
        description = "SQ-T3665 [Batched Redemptions] Verify if there are multiple non-loyalty rewards, one having end date and other have end date as blank then reward with end date should get added first in the discount_basket",
        priority = 0, groups = {"regression", "dailyrun"})
	@Owner(name = "Hardik Bhardwaj")
	public void T3665_RewardWithEndDate() throws Exception {

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

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"16", dataSet.get("redeemable_id"), "", "20", "");

		utils.logit("Send redeemable to the user successfully");

		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id 1 " + rewardId + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"16", dataSet.get("redeemable_id"), "", "20", "2040-11-20");

		utils.logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id 2 " + rewardId1 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"16", dataSet.get("redeemable_id"), "", "", "2040-11-31");

		utils.logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id 3 " + rewardId2 + " is generated successfully ");

		String query4 = "UPDATE rewards SET end_time = '2020-12-02 04:59:59', status = 'expired' WHERE id = '"
				+ rewardId2 + "'";
		int rs4 = DBUtils.executeUpdateQuery(env, query4);
		Assert.assertEquals(rs4, 1);

		// send reward amount to user Reedemable
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"16", dataSet.get("redeemable_id"), "", "", "2040-12-21");

		utils.logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id 4 " + rewardId3 + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "redeemable", dataSet.get("redeemable_id"), externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		// POS Auto Unlock
		Response autoUnlockResponse3 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", externalUID,
				dataSet.get("locationkey"));
		Assert.assertEquals(autoUnlockResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		utils.logit("POS Auto Select Api is successful");

//		DBUtils.closeConnection();
	}

    @Test(
        description = "SQ-T3666 [Batched Redemptions] Verify if there are multiple non-loyalty rewards and all have end date as blank then auto offer should be applicable based on sequence (FIFO) i.e. reward_id",
        priority = 1, groups = {"regression", "dailyrun"})
	@Owner(name = "Hardik Bhardwaj")
	public void T3666_RewardWithOutEndDate() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"), "", "20", "");

		utils.logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id 1 " + rewardId + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"), "", "", "");

		utils.logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId1 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id 2 " + rewardId1 + " is generated successfully ");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", dataSet.get("redeemable_id"), "", "", "");

		utils.logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// get reward id
		rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemable_id"));

		utils.logit("Reward id 3 " + rewardId2 + " is generated successfully ");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "redeemable", dataSet.get("redeemable_id"), externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		// POS Auto Unlock
		Response autoUnlockResponse3 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "12003", externalUID,
				dataSet.get("locationkey"));
		Assert.assertEquals(autoUnlockResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		utils.logit("POS Auto Select Api is successful");

		String reward_id = autoUnlockResponse3.jsonPath().get("discount_basket_items[1].discount_id").toString();
		Assert.assertEquals(reward_id, rewardId, "reward is not found at expected sequence no 1");
		utils.logPass("reward is found at expected sequence no 1");

		String reward_id1 = autoUnlockResponse3.jsonPath().get("discount_basket_items[2].discount_id").toString();
		Assert.assertEquals(reward_id1, rewardId1, "reward is not found at expected sequence no 2");
		utils.logPass("reward is found at expected sequence no 2");

		String reward_id2 = autoUnlockResponse3.jsonPath().get("discount_basket_items[3].discount_id").toString();
		Assert.assertEquals(reward_id2, rewardId2, "reward is not found at expected sequence no 3");
		utils.logPass("reward is found at expected sequence no 3");

	}

    @Test(
        description = "SQ-T3286 Step-4 [Batched Redemptions-OMM-T728(495)] Verify dashboard / DB logic when redemption is done Void (POS Void Batched Redemption API)",
        priority = 1, groups = {"regression", "dailyrun"})
	@Owner(name = "Hardik Bhardwaj")
	public void T3286_POSVoidBatchedRedemptionAPI_Step4() throws Exception {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", "", "", "20", "");
		utils.logit("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// AUTH Add Discount to Basket
		Response discountBasketResponse = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "redeemable", dataSet.get("redeemable_id"), externalUID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		utils.logPass("AUTH add discount to basket is successful");

		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse1 = pageObj.endpoints().processBatchRedemptionAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), token, userID, "12003",
				externalUID);
		Assert.assertEquals(batchRedemptionProcessResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		String redemption_ref1 = batchRedemptionProcessResponse1.jsonPath().get("redemption_ref").toString();
		System.out.println("POS Process Batch Redemption api is working properly and redemption refrence code is - "
				+ redemption_ref1);
		utils.logPass("Auth Process Batch Redemption Api is successful");

		// POS void redemption
		Response voidRedemptionResponse = pageObj.endpoints().voidProcessBatchRedemptionOfBasketPOSAPI(
				dataSet.get("client"), dataSet.get("secret"), userID, dataSet.get("locationkey"), redemption_ref1);
		Assert.assertEquals(voidRedemptionResponse.getStatusCode(), 202,
				"Status code 200 did not match with Void Batch Redemption ");

		String query = "Select status from discount_baskets where user_id = '" + userID + "'";
		String statusColValue = DBUtils.executeQueryAndGetColumnValue(env, query, "status");
		Assert.assertEquals(statusColValue, "3", "Value is not present at status column in discount basket ");
		utils.logPass("Value is present at status column in discount basket ");

		String query1 = "Select status from discount_basket_items where user_id = '" + userID + "'";
		String statusColValue1 = DBUtils.executeQueryAndGetColumnValue(env, query1, "status");
		Assert.assertEquals(statusColValue1, "0", "Value is not present at status column in discount basket ");
		utils.logPass("Value is present at status column in discount basket ");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// navigate to user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean title = pageObj.guestTimelinePage().verifyTitleFromTimeline("Void Honored Redemption");

		try {
			Assert.assertTrue(title, "Void Honored Redemption Title did not displayed...");
			utils.logPass("Void Honored Redemption Title is displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Void Honored Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Void Honored Redemption Title on timeline" + e);
		}

		boolean title1 = pageObj.guestTimelinePage().verifyTitleFromTimeline("Redeemed Redemption");

		try {
			Assert.assertFalse(title1, "Redeemed Redemption Title did displayed...");
			utils.logPass("Redeemed Redemption Title is not displayed successfully on timeline");
		} catch (Exception e) {
			logger.error("Error in validating Redeemed Redemption Title on timeline" + e);
			TestListeners.extentTest.get().fail("Error in validating Redeemed Redemption Title on timeline" + e);
		}
		List<Object> obj = new ArrayList<Object>();
		String eventValue;
		int j = 0;
		Response accountHistoryResponse = pageObj.endpoints().Api2UserAccountHistory(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, accountHistoryResponse.getStatusCode(),
				"Status code 200 did not matched for api2 point conversion");
		obj = accountHistoryResponse.jsonPath().getList("description");
		for (int i = 0; i < obj.size(); i++) {
			eventValue = accountHistoryResponse.jsonPath().getString("[" + i + "].event_value");
			if (eventValue.contains("+Item")) {
				i = j;
				break;
			}
		}
		String points = accountHistoryResponse.jsonPath().get("[" + j + "].total_points").toString();
		Assert.assertEquals(points, "20", "points is not reverted back to user account (Account History)");
		utils.logPass("points is reverted back to user account (Account History)");

//	   DBUtils.closeConnection();
	}
	@Test(description = "SQ-T5832 POS Batch Redemption>Validate that when redeeming with discount_type 'card_completion' via the batch redemption API, the generated redemption_code is in UUID format.", groups = {
			"regression" })
	@Owner(name = "Vansham Mishra")
	public void T5832_VerifyGeneratedPosRedemptionCodeIsInUUIDFormat() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Locations");
		pageObj.cockpitLocationPage().SelectOnlineOrderDefaultLocation("Redemption 2.0");

		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Response sendAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"10");
		Assert.assertEquals(sendAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		// Adding subscription into discount basket

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationkey2"), userID, "card_completion", discountID);

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[card_completion]");

		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "").replace("]", "")
				.replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, "null");

		String discount_details = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_details")
				.replace("[", "").replace("]", "").replace("]", "").replace("]", "");
		Assert.assertEquals(discount_details, "null");

		String dsiscountValue = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_value")
				.replace("[", "").replace("]", "").replace("]", "").replace("]", "");
		Assert.assertEquals(dsiscountValue, "null");

		String expDiscountBasketItemId = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");
		utils.logPass("Verified that user is able to add card completion in discount basket");

		// Hit POS discount lookup API and verify the qualification of card_completion discount type
		Response posDiscountLookupResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationkey2"), userID, "");
		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String discount_type = posDiscountLookupResponse.jsonPath().getString("selected_discounts.discount_type");
		Assert.assertEquals(discount_type, "[card_completion]");
		utils.logPass("Verified that qualified discount type is Card Completion");

		// POS - Hit batch redemption API to redeem card_completion discount type
		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSNew(dataSet.get("locationkey2"), userID);
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String actual_discount_type_batchRedemptionProcessResponse = batchRedemptionProcessResponse.jsonPath()
				.getString("success[0].discount_type");
		Assert.assertEquals(actual_discount_type_batchRedemptionProcessResponse, "card_completion");
		utils.logPass("Verified that user is able to hit batch redemption API to redeem card_completion discount type");

		// create the query to fetch internal_tracking_code from redemptions table where user_id=userID
		String query = "select internal_tracking_code from redemptions where user_id = '" + userID + "'";
		String internal_tracking_code = DBUtils.executeQueryAndGetColumnValue(env, query,
				"internal_tracking_code");

		// create the query to fetch redemption_token from redemption_codes table where user_id=userID and redemption_token=internal_tracking_code
		String query1 = "select redemption_token from redemption_codes where redemption_token = '" + internal_tracking_code + "'";
		String internal_tracking_code1 = DBUtils.executeQueryAndGetColumnValue(env, query1,
				"redemption_token");

		// verify the generated redemption_code is in UUID format
		Boolean isValidUUID = isValidUUID(internal_tracking_code1);
		Assert.assertTrue(isValidUUID,"Generated redemption_code is not in UUID format in redemptions and redemption_code table: "+internal_tracking_code1);
		utils.logPass("Verified that generated redemption_code is in UUID format in redemptions and redemption_code table: "+internal_tracking_code1);

		// verify that redemption code is displayed on the guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean isRedemptionCodeDisplayed = pageObj.guestTimelinePage().verifyRedemptionCode(internal_tracking_code1);
		Assert.assertTrue(isRedemptionCodeDisplayed,"Redemption code is not displayed on the guest timeline: "+internal_tracking_code1);
		utils.logPass("Verified that redemption code is displayed on the guest timeline: "+internal_tracking_code1);

	}

	@Test(description = "SQ-T5835 Auth Batch Redemption>Validate that when redeeming with discount_type 'card_completion' via the batch redemption API, the generated redemption_code is in UUID format.", groups = {
			"regression" })
	@Owner(name = "Vansham Mishra")
	public void T5835_VerifyGeneratedAuthRedemptionCodeIsInUUIDFormat() throws Exception {

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Locations");
		pageObj.cockpitLocationPage().SelectOnlineOrderDefaultLocation("Redemption 2.0");

		String discountID = Utilities.getRandomNoFromRange(1, 4) + "";
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Response sendAmountResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"10");
		Assert.assertEquals(sendAmountResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		// Adding subscription into discount basket

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationkey2"), userID, "card_completion", discountID);

		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actual_discount_type_addBasketResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");

		Assert.assertEquals(actual_discount_type_addBasketResponse, "[card_completion]");

		String actual_discount_discount_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "").replace("]", "")
				.replace("]", "");
		Assert.assertEquals(actual_discount_discount_id, "null");

		String discount_details = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_details")
				.replace("[", "").replace("]", "").replace("]", "").replace("]", "");
		Assert.assertEquals(discount_details, "null");

		String dsiscountValue = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_value")
				.replace("[", "").replace("]", "").replace("]", "").replace("]", "");
		Assert.assertEquals(dsiscountValue, "null");

		String expDiscountBasketItemId = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id");
		utils.logPass("Verified that user is able to add card completion in discount basket");

		// Hit POS discount lookup API and verify the qualification of card_completion discount type
		Response posDiscountLookupResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationkey2"), userID, "");
		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String discount_type = posDiscountLookupResponse.jsonPath().getString("selected_discounts.discount_type");
		Assert.assertEquals(discount_type, "[card_completion]");
		utils.logPass("Verified that qualified discount type is Card Completion");

		// AUTH - Hit batch redemption API to redeem card_completion discount type
		Response batchRedemptionResponse = pageObj.endpoints().processBatchRedemptionAUTHAPI(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("locationkey2"), token, userID, "12003", externalUID);
		Assert.assertEquals(batchRedemptionResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String actual_discount_type_batchRedemptionProcessResponse = batchRedemptionResponse.jsonPath()
				.getString("success[0].discount_type");
		Assert.assertEquals(actual_discount_type_batchRedemptionProcessResponse, "card_completion");
		utils.logPass("Verified that user is able to hit batch redemption API to redeem card_completion discount type");

		// create the query to fetch internal_tracking_code from redemptions table where user_id=userID
		String query = "select internal_tracking_code from redemptions where user_id = '" + userID + "'";
		String internal_tracking_code = DBUtils.executeQueryAndGetColumnValue(env, query,
				"internal_tracking_code");

		// create the query to fetch redemption_token from redemption_codes table where user_id=userID and redemption_token=internal_tracking_code
		String query1 = "select redemption_token from redemption_codes where redemption_token = '" + internal_tracking_code + "'";
		String internal_tracking_code1 = DBUtils.executeQueryAndGetColumnValue(env, query1,
				"redemption_token");

		// verify the generated redemption_code is in UUID format
		Boolean isValidUUID = isValidUUID(internal_tracking_code1);
		Assert.assertTrue(isValidUUID,"Generated redemption_code is not in UUID format in redemptions and redemption_code table: "+internal_tracking_code1);
		utils.logPass("Verified that generated redemption_code is in UUID format in redemptions and redemption_code table: "+internal_tracking_code1);
		
		// verify that redemption code is displayed on the guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean isRedemptionCodeDisplayed = pageObj.guestTimelinePage().verifyRedemptionCode(internal_tracking_code1);
		Assert.assertTrue(isRedemptionCodeDisplayed,"Redemption code is not displayed on the guest timeline: "+internal_tracking_code1);
		utils.logPass("Verified that redemption code is displayed on the guest timeline: "+internal_tracking_code1);

	}
	@Test(description = "SQ-T6309 Verify redemption_codes for the business generated are in not_assigned status." +
			"SQ-T6316 Verify on doing a redemption request, unassigned codes are picked for the business.", groups = {
			"regression" })
	@Owner(name = "Vansham Mishra")
	public void T6309_verifyRedemptionCodesNotAssignedStatusForBusiness() throws Exception {
		String business_id = dataSet.get("business_id");
		//fetch the redemption token of the first record FROM redemption_codes WHERE business_id =  AND status = 'unassigned' ORDER BY created_at DESC
		String query = "SELECT redemption_token FROM redemption_codes WHERE business_id = '"+business_id+"' AND status = 'unassigned' ORDER BY created_at DESC LIMIT 1";
		String redemptionToken = DBUtils.executeQueryAndGetColumnValue(env, query, "redemption_token");
		Assert.assertNotNull(redemptionToken, "Redemption token is null or not found in the database.");
		utils.logit("Fetched redemption token: " + redemptionToken);
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Support", "Redemption Codes");
		pageObj.redemptionsPage().selectCalendarRangeType(dataSet.get("calendarType"));
		pageObj.redemptionsPage().searchRedemptionCode(redemptionToken);
		String redemptionCodeStatus = pageObj.redemptionsPage().getRedemptionCodeStatus(redemptionToken);
		Assert.assertEquals(redemptionCodeStatus, dataSet.get("redemptionCodeStatus"), "Redemption code status is not 'Not assigned yet'");
		utils.logPass("Verified that redemption code status is 'Not assigned yet'");
		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();
		//naviafte to guesttimeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().giftRedeemableToUser("redeemable", "Redeemable_T6309");
		// get list of redemption_tokens from redemption_codes table where business_id='"+business_id+"' AND status = 'unassigned and location_id='401186';
		String query1 = "SELECT redemption_token FROM redemption_codes WHERE business_id = '"+business_id+"' AND status = 'unassigned' AND location_id = '401186'";
		List<String> redemptionTokens = DBUtils.getValueFromColumnInList(env, query1, "redemption_token");
		//fetch id from rewards table where userid = '"+userID+"';
		String query2 = "SELECT id FROM rewards WHERE user_id = '"+userID+"' ORDER BY created_at DESC LIMIT 1";
		String rewardId = DBUtils.executeQueryAndGetColumnValue(env, query2, "id");
		Assert.assertNotNull(rewardId, "Reward ID is null or not found in the database.");
		// perform redemption first time
		Response posRedeem = pageObj.endpoints().posRedemptionOfRewardWithItemID(userEmail,dataSet.get("locationKey2"), rewardId, dataSet.get("itemID"));
		Assert.assertEquals(posRedeem.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code did not match for POS redemption of redeemable");
		// fetch redemption_code from posRedeem
		String redemptionCode = posRedeem.jsonPath().get("redemption_code").toString();
		// verify redemptionCode is present in redemptionTokens
		Assert.assertTrue(redemptionTokens.contains(redemptionCode), "Redemption code is not present in the list of redemption tokens.");
		utils.logPass("Verified that the redemption code generated had status unassigned and is present in the list of redemption tokens: " + redemptionCode);
		String query3 = "SELECT status FROM redemption_codes WHERE redemption_token = '"+redemptionCode+"'";
		String redemptionTokenStatus = DBUtils.executeQueryAndGetColumnValue(env, query3, "status");
		Assert.assertEquals(redemptionTokenStatus, "processed", "Redemption token status is not 'assigned' after redemption");
		utils.logPass("Verified that on doing a redemption request, unassigned codes are picked for the business.");
		}
	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
