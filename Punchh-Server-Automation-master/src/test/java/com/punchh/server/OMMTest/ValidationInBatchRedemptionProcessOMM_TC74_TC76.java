package com.punchh.server.OMMTest;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Listeners;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import io.restassured.response.Response;

/*
 * @Author :- Shashank Sharma
 * 
 * TC - OMM-T76 (1.0) & OMM-T74 (1.0)
*/

@Listeners(TestListeners.class)
public class ValidationInBatchRedemptionProcessOMM_TC74_TC76 {
	static Logger logger = LogManager.getLogger(ValidationInBatchRedemptionProcessOMM_TC74_TC76.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	private List<String> codeNameList;

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
		GlobalBenefitRedemptionThrottlingToggle = false;
		codeNameList = new ArrayList<String>();

	}

	// User has 3 redemption_codes in basket (giving 10% discount each and no QC
	// attached in campaign)

	// Redeemable name 10%OffPizza
	// Redeemable ID = 29007
	// Receipt Tag Name =Pizza10%Tag

	@Test(description = "OMM-T74 STEP3  --- User processes the basket successfully (to check auto checkin gets created) & verified receipt tag ")
	public void validateAutoCheckinIsCreatedAfterRedemption() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Post Redemption");
		pageObj.cockpitRedemptionsPage().clickedOnAutoCheckinRedemptionCheckBox("check");

		double expAmount = Double.parseDouble(dataSet.get("expAutoCheckinAmount"));
		String expReceiptTagName = dataSet.get("expReceiptTagName");

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID);
		pageObj.utils().logit(rewardID + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationKey"), userID, "101");

		pageObj.utils().logit(userEmail + " User process the basket");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean postCheckinLavelIsDisplayed = pageObj.guestTimelinePage().verifyPostCheckinIsDispayedOnTimeLine();
		Assert.assertTrue(postCheckinLavelIsDisplayed, "Post cehckin lavel is not displayed on timeline page ");

		pageObj.utils().logPass("Verified that post checkin is displayed on user timeline ");

		double actAmount = pageObj.guestTimelinePage().getAutoCheckinAmountOnTimeLinePage();
		Assert.assertEquals(actAmount, expAmount);
		pageObj.utils().logPass("Verified that autocheckin amount " + expAmount + " on timeline page ");

		boolean isTagDisplayed = pageObj.guestTimelinePage()
				.verifyReceiptTagIsDisplayedForAutoCheckin(expReceiptTagName);
		Assert.assertTrue(isTagDisplayed, "Receipt Tag is not displayed ");

		pageObj.utils().logPass("Verified that receipt tag " + expReceiptTagName + "is displayed on user timeline page ");

	}

	@Test(description = "OMM-T74 STEP2  --- User processes the basket successfully (to check no auto checkin gets created and only remote receipt is created)")
	public void validateAutoCheckinIsNotCreatedAfterRedemption() throws InterruptedException {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Post Redemption");
		pageObj.cockpitRedemptionsPage().clickedOnAutoCheckinRedemptionCheckBox("uncheck");

		double expAmount = Double.parseDouble(dataSet.get("expAutoCheckinAmount"));
		String expReceiptTagName = dataSet.get("expReceiptTagName");

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID);
		pageObj.utils().logit(rewardID + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationKey"), userID, "101");

		pageObj.utils().logit(userEmail + " User process the basket");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		boolean postCheckinLavelIsDisplayed = pageObj.guestTimelinePage().verifyPostCheckinIsDispayedOnTimeLine();
		Assert.assertFalse(postCheckinLavelIsDisplayed, "Post cehckin lavel is not displayed on timeline page ");

	}

//	Max Redemption -> 1
//
//	1.User processes the 1st basket containing rewards
//	2.User processes the 2nd basket containing rewards
//	3.User processes the 3rd basket containing rewards

	@Test(description = "OMM-T74 STEP-6  --- ​Max Redemption limit is not applied on reward / redemption_code / redeemable redemption ")
	public void validateMultipleRedemptionTakingPlace() throws InterruptedException {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Post Redemption");
		pageObj.cockpitRedemptionsPage().clickedOnAutoCheckinRedemptionCheckBox("check");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("1");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMaxRedemptionAmount("5");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
		double expAmount = Double.parseDouble(dataSet.get("expAutoCheckinAmount"));
		String expReceiptTagName = dataSet.get("expReceiptTagName");

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
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		pageObj.utils().logit("Send redeemable to the user successfully");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionOfBasketPOSAPI(
				dataSet.get("locationKey"), userID, "101", "20", "110", "111", "112");

		// pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("20");

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Validations");
		pageObj.cockpitRedemptionsPage().updateMaxRedemptionAmount("100");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	// Test cases T_76
//	Pre-condition -> Turn On Discount Stacking On (for all rewards)
//	Guest Discount Basket ->
//	Reward 1 -> Receipt Subtotal Amount
//	Reward 2 -> Receipt Subtotal Amount Reward 3 -> Receipt Subtotal Amount

	@Test(description = "OMM-T76 (1.0) STEP -1 Verify cases for QC -> Processing Function \"Receipt Subtotal Amount\"")
	public void validateReceiptSubtoalAmount() {

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
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		String rewardID3 = rewardResponse.jsonPath().getString("id[2]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + " & " + rewardID3 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logit(rewardID3 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "101", "21", "10", "7", "4");

		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount"));

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[0].message").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemption2ErrorMessage, expErrorMessage);
		pageObj.utils().logPass("Verified the error message for the redeemption2");

		String actualRedeemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("failures[1].message").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemption3ErrorMessage, expErrorMessage);

		pageObj.utils().logPass("Verified the error message for the redeemption3");

	}

//	Guest Discount Basket ->
//	Reward 1 -> Receipt Subtotal Amount (Percentage)
//	Reward 2 -> Receipt Subtotal Amount (Percentage)
//	Reward 3 -> Receipt Subtotal Amount (Percentage)
	@Test(description = "OMM-T76 (1.0) STEP -2 Verify cases for QC -> Processing Function \"Receipt Subtotal Amount\"")
	public void validateReceiptSubtoalAmountPercentageRedeemption() {

		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		double expRedeemption2DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount2"));

		double expRedeemption3DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount3"));

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
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		String rewardID3 = rewardResponse.jsonPath().getString("id[2]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + " & " + rewardID3 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logit(rewardID3 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "101", "17", "10", "4", "3");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption2 =" + actualRedeemption2DiscountAmt);

		double actualRedeemption3DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[2].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption3DiscountAmt, expRedeemption3DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption3 =" + actualRedeemption3DiscountAmt);

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt
				+ actualRedeemption3DiscountAmt;

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

		String actualDiscountType = batchRedemptionProcessResponseUser1.jsonPath().getString("success[2].discount_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountType, "reward");

		pageObj.utils().logPass("Verified the discount type in bath process response");

	}

//	Guest Discount Basket ->
//	Reward 1 -> Receipt Subtotal Amount (Amount Cap)
//	Reward 2 -> Receipt Subtotal Amount (Amount Cap)
//  Reward 3 -> Receipt Subtotal Amount (Amount Cap)
	@Test(description = "OMM-T76 (1.0) STEP -3 Verify cases for QC -> Processing Function \"Receipt Subtotal Amount\"")
	public void validateReceiptSubtoalAmountAcountCapRedeemption() {

		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount1"));

		double expRedeemption2DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount2"));

		double expRedeemption3DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount3"));

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
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get()
				.pass("Api2  send reward " + dataSet.get("redeemable_id") + " to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");
		String rewardID3 = rewardResponse.jsonPath().getString("id[2]").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID1 + " & " + rewardID2 + " & " + rewardID3 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID1);
		pageObj.utils().logit(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logit(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logit(rewardID3 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSAPI(dataSet.get("locationKey"), userID, "101", "17", "10", "4", "3");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption2 =" + actualRedeemption2DiscountAmt);

		double actualRedeemption3DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("success[2].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption3DiscountAmt, expRedeemption3DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption3 =" + actualRedeemption3DiscountAmt);

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt
				+ actualRedeemption3DiscountAmt;

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

		String actualDiscountType = batchRedemptionProcessResponseUser1.jsonPath().getString("success[2].discount_type")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountType, "reward");

		pageObj.utils().logPass("Verified the discount type in bath process response");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
