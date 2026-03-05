package com.punchh.server.OMMTest;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;

import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;
import io.restassured.response.Response;

/*
 * @Author :- Shashank Sharma
*/

@Listeners(TestListeners.class)
public class ValidationInBatchRedemptionProcessOmm266_TC_T194 {
	static Logger logger = LogManager.getLogger(ValidationInBatchRedemptionProcessOmm266_TC_T194.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	private boolean GlobalBenefitRedemptionThrottlingToggle;

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

//	Guest Discount Basket ->
//	Reward 1 -> Receipt Subtotal Amount (Amount Cap)
//	Reward 2 -> Receipt Subtotal Amount (Amount Cap)
//  Reward 3 -> Receipt Subtotal Amount (Amount Cap)
	@Test(description = "OMM-T199 (1.0) STEP -3 Verify cases for QC -> Processing Function \"Receipt Subtotal Amount\"")
	public void validateReceiptSubtoalAmountAcountCapRedeemptionDiscountLookup() {

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
		pageObj.utils().logit("API2 Signup is successful");

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
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logPass(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logPass(rewardID3 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationKey"), userID, "101", "17", "10",
						"4", "3");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption2 =" + actualRedeemption2DiscountAmt);

		double actualRedeemption3DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[2].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption3DiscountAmt, expRedeemption3DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption3 =" + actualRedeemption3DiscountAmt);

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt
				+ actualRedeemption3DiscountAmt;

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

		String actualDiscountType = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[0].discount_type").replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountType, "reward");

		pageObj.utils().logPass("Verified the discount type in bath process response");

	}

//	Guest Discount Basket ->
//	Reward 1 -> Receipt Subtotal Amount (Percentage)
//	Reward 2 -> Receipt Subtotal Amount (Percentage)
//	Reward 3 -> Receipt Subtotal Amount (Percentage)
	@Test(description = "OMM-T199 (1.0) STEP -2 Verify cases for QC -> Processing Function \"Receipt Subtotal Amount\"")
	public void validateReceiptSubtoalAmountPercentageRedeemptionDiscountLookup() {

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
		pageObj.utils().logit("API2 Signup is successful");

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
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logPass(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logPass(rewardID3 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationKey"), userID, "101", "17", "10",
						"4", "3");

		System.out
				.println("batchRedemptionProcessResponseUser1=" + batchRedemptionProcessResponseUser1.asPrettyString());

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		double actualRedeemption2DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[1].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption2DiscountAmt, expRedeemption2DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption2 =" + actualRedeemption2DiscountAmt);

		double actualRedeemption3DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[2].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption3DiscountAmt, expRedeemption3DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption3 =" + actualRedeemption3DiscountAmt);

		double expTotalDiscountAmount = Double.parseDouble(dataSet.get("expTotalDiscountAmount"));

		double actualTotalDiscountAmount = actualRedeemption1DiscountAmt + actualRedeemption2DiscountAmt
				+ actualRedeemption3DiscountAmt;

		Assert.assertEquals(actualTotalDiscountAmount, expTotalDiscountAmount);
		pageObj.utils().logPass("Verified the overall total discount amount for the redeemption=" + actualTotalDiscountAmount);

		String actualDiscountType = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[0].discount_type").replace("[", "").replace("]", "");
		Assert.assertEquals(actualDiscountType, "reward");

		pageObj.utils().logPass("Verified the discount type in bath process response");

	}

	@Test(description = "OMM-T199 (1.0) STEP -1 Verify cases for QC -> Processing Function \"Receipt Subtotal Amount\"")
	public void validateReceiptSubtoalAmountDiscountLookup() {

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

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
		pageObj.utils().logPass(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID2);
		pageObj.utils().logPass(rewardID2 + " rewardid is added to the basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID3);
		pageObj.utils().logPass(rewardID3 + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOSDiscountLookup(dataSet.get("locationKey"), userID, "101", "21", "10",
						"7", "4");

		double expRedeemption1DiscountAmt = Double.parseDouble(dataSet.get("expRedeemptionOneDiscountAmount"));

		double actualRedeemption1DiscountAmt = Double.parseDouble(batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[0].discount_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualRedeemption1DiscountAmt, expRedeemption1DiscountAmt);
		pageObj.utils().logPass("Verified the total discount amount for the redeemption1 =" + actualRedeemption1DiscountAmt);

		String expErrorMessage = dataSet.get("expErrorMessage");
		String actualRedeemption2ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[1].message").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemption2ErrorMessage, expErrorMessage);
		pageObj.utils().logPass("Verified the error message for the redeemption2");

		String actualRedeemption3ErrorMessage = batchRedemptionProcessResponseUser1.jsonPath()
				.getString("selected_discounts[2].message").replace("[", "").replace("]", "");
		Assert.assertEquals(actualRedeemption3ErrorMessage, expErrorMessage);

		pageObj.utils().logPass("Verified the error message for the redeemption3");

	}

	@Test(description = "OMM-196 (1.0) STEP-2  Verify functionality validations for \\\"discount_type -> redemption_code\\\" in Batch Redemption Process API")
	public void validatePromoRedemptionCodeInBatchProcessDiscountLookup() throws InterruptedException {

		String expectedErrorMessage = dataSet.get("expectedFailureMessage");

		// String generatedCodeName ="2PMAUY639" ; dataSet.get("couponGeneratedCode");

		String promoCode = CreateDateTime.getRandomString(6).toUpperCase() + Utilities.getRandomNoFromRange(500, 2000);
		String coupanCampaignName = "Auto_PromoCampaign" + CreateDateTime.getTimeDateString();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

//		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsPromoCampaign(coupanCampaignName, promoCode);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));

		pageObj.signupcampaignpage().setPromoCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("giftType"), dataSet.get("amount"), dataSet.get("locationA"), "BatchRedmeption_QC");

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		TestListeners.extentTest.get().pass("Coupon campaign created successfuly");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationA_Key"), userID, "redemption_code", promoCode);

		// Given location B and verified that batch process should failed
		Response batchRedemptionProcessResponseLocationB = pageObj.endpoints()
				.POSDiscountLookup(dataSet.get("locationB_Key"), userID, "");

		String actualFailureMessage = batchRedemptionProcessResponseLocationB.jsonPath()
				.getString("selected_discounts[0].message").replace("[[", "").replace("]]", "");

		Assert.assertTrue(actualFailureMessage.contains(expectedErrorMessage));
		pageObj.utils().logPass("Verify the actual failure message ' " + actualFailureMessage
				+ "' with the expected error message '" + expectedErrorMessage + "'");

		String actualDiscount_type = batchRedemptionProcessResponseLocationB.jsonPath()
				.getString("selected_discounts[0].discount_type").replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualDiscount_type, "redemption_code");
		pageObj.utils().logPass("Verify the actual discount_type' " + actualDiscount_type
				+ "' with the expected discount_type 'redemption_code'");

		String actualCouponCampaginName = batchRedemptionProcessResponseLocationB.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualCouponCampaginName, coupanCampaignName);
		pageObj.utils().logPass("Verify the actual coupon  name ' " + actualCouponCampaginName
				+ "' with the expected coupon name '" + coupanCampaignName + "'");

		String actualDiscountID = batchRedemptionProcessResponseLocationB.jsonPath()
				.getString("selected_discounts[0].discount_id").replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualDiscountID, promoCode);
		pageObj.utils().logPass("Verify the actual coupon  generated code  ' " + actualDiscountID
				+ "' with the expected coupon generated code '" + promoCode + "'");

//		// Given Location A Key and verifying that batch should be processed
//		// successfully

		String expectedQualified = "true";
		Response batchRedemptionProcessResponseLocationA = pageObj.endpoints()
				.POSDiscountLookup(dataSet.get("locationA_Key"), userID, "41414121080");

		System.out.println("batchRedemptionProcessResponseLocationA -- "
				+ batchRedemptionProcessResponseLocationA.asPrettyString());

		Assert.assertEquals(batchRedemptionProcessResponseLocationA.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualQualified = batchRedemptionProcessResponseLocationA.jsonPath()
				.getString("selected_discounts[0].qualified").replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualQualified, expectedQualified);

		pageObj.utils().logPass("Verified the actual qualified value " + actualQualified
				+ " with the expected qualified value  " + expectedQualified);

//		// Given invalid item id and so that QC get failed and verifying the status
		Response batchRedemptionProcessResponseQC = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationA_Key"),
				userID, "104");

		String actualFailureMessageQCFail = batchRedemptionProcessResponseQC.jsonPath()
				.getString("selected_discounts[0].message").replace("[[", "").replace("]]", "");

		Assert.assertEquals(batchRedemptionProcessResponseQC.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(actualFailureMessageQCFail.contains("Discount qualification on receipt failed"));

		pageObj.utils().logPass("Verified status if QC failed ");

	}

	@Test(description = "OMM-T196 (1.0) STEP-1 Verify functionality validations for \"discount_type -> redemption_code\" in Batch Redemption Process API")
	public void validateCouponCampaignRedemptionCodeInBatchProcessDiscountLookup() throws InterruptedException {
		String expectedErrorMessage = dataSet.get("expectedFailureMessage");

		String coupanCampaignName = dataSet.get("coupanCampaignName");
		String generatedCodeName = dataSet.get("couponGeneratedCode");

		System.out.println("coupanCampaignName == " + coupanCampaignName);
//		GlobalBenefitRedemptionThrottlingToggle = true;
//
//
//		// Login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();
//		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
//		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
//
//		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
//		pageObj.campaignspage().clickNewCampaignBtn();
//		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
//		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
//		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
//		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
//				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), dataSet.get("locationA"),
//				"BatchRedmeption_QC", GlobalBenefitRedemptionThrottlingToggle);
//
//		pageObj.signupcampaignpage().createWhenDetailsCampaign();
//		boolean status = pageObj.campaignspage().validateSuccessMessage();
//		Assert.assertTrue(status, "Campaign created success message did not displayed....");
//		TestListeners.extentTest.get().pass("Coupon campaign created successfuly");
//
//		Thread.sleep(8000);
//		pageObj.campaignspage().searchCampaign(coupanCampaignName);
//		String generatedCodeName = pageObj.campaignspage().getPreGeneratedCuponList().get(0);
//		logger.info("Coupon code generated :- " + generatedCodeName);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "redemption_code", generatedCodeName);

		// Given Location B key , and verifying the failure message
		Response batchRedemptionProcessResponseLocationB = pageObj.endpoints()
				.POSDiscountLookup(dataSet.get("locationB_Key"), userID, "");

		String actualFailureMessage = batchRedemptionProcessResponseLocationB.jsonPath()
				.getString("selected_discounts[0].message").replace("[[", "").replace("]]", "");

		Assert.assertTrue(actualFailureMessage.contains(expectedErrorMessage));
		pageObj.utils().logPass("Verify the actual failure message ' " + actualFailureMessage
				+ "' with the expected error message '" + expectedErrorMessage + "'");

		String actualDiscount_type = batchRedemptionProcessResponseLocationB.jsonPath()
				.getString("selected_discounts[0].discount_type").replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualDiscount_type, "redemption_code");
		pageObj.utils().logPass("Verify the actual discount_type' " + actualDiscount_type
				+ "' with the expected discount_type 'redemption_code'");

		String actualCouponCampaginName = batchRedemptionProcessResponseLocationB.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualCouponCampaginName, coupanCampaignName);
		pageObj.utils().logPass("Verify the actual coupon  name ' " + actualCouponCampaginName
				+ "' with the expected coupon name '" + coupanCampaignName + "'");

		String actualDiscountID = batchRedemptionProcessResponseLocationB.jsonPath()
				.getString("selected_discounts[0].discount_id").replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualDiscountID, generatedCodeName);
		pageObj.utils().logPass("Verify the actual coupon  generated code  ' " + actualDiscountID
				+ "' with the expected coupon generated code '" + generatedCodeName + "'");

//		// Given Location A Key and verifying that batch should be processed
//		// successfully

		String expectedQualified = "true";
		Response batchRedemptionProcessResponseLocationA = pageObj.endpoints()
				.POSDiscountLookup(dataSet.get("locationA_Key"), userID, "41414121080");

		System.out.println("batchRedemptionProcessResponseLocationA -- "
				+ batchRedemptionProcessResponseLocationA.asPrettyString());

		Assert.assertEquals(batchRedemptionProcessResponseLocationA.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualQualified = batchRedemptionProcessResponseLocationA.jsonPath()
				.getString("selected_discounts[0].qualified").replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualQualified, expectedQualified);

		pageObj.utils().logPass("Verified the actual qualified value " + actualQualified
				+ " with the expected qualified value  " + expectedQualified);

//		// Given invalid item id and so that QC get failed and verifying the status
		Response batchRedemptionProcessResponseQC = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationA_Key"),
				userID, "104");

		System.out.println("batchRedemptionProcessResponseQC=== " + batchRedemptionProcessResponseQC.asPrettyString());

		String actualFailureMessageQCFail = batchRedemptionProcessResponseQC.jsonPath()
				.getString("selected_discounts[0].message").replace("[[", "").replace("]]", "");

		Assert.assertEquals(batchRedemptionProcessResponseQC.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(actualFailureMessageQCFail.contains("Discount qualification on receipt failed"));

		pageObj.utils().logPass("Verified status if QC failed ");

//		// Given invalid item id and so that QC get failed and verifying the status
		Response batchRedemptionProcessResponseQCPass = pageObj.endpoints()
				.POSDiscountLookup(dataSet.get("locationA_Key"), userID, "41414121080");

		System.out.println(
				"batchRedemptionProcessResponseQCPASS=== " + batchRedemptionProcessResponseQCPass.asPrettyString());

		String actualdiscount_amountQCPass = batchRedemptionProcessResponseQCPass.jsonPath()
				.getString("selected_discounts[0].discount_amount").replace("[[", "").replace("]]", "");

		double expDiscountAmount = Double.parseDouble("10.0");
		double actualdiscount_amountQCPassDouble = Double.parseDouble(actualdiscount_amountQCPass);

		Assert.assertEquals(batchRedemptionProcessResponseQC.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		Assert.assertEquals(actualdiscount_amountQCPassDouble, expDiscountAmount);

		pageObj.utils().logPass("Verified the discount amount " + actualdiscount_amountQCPassDouble
				+ " with the expected discount amount " + expDiscountAmount);

	}

	@Test(description = "OMM-T195 - STEP4 - Verify functionality validations for \"discount_type -> redeemable\" "
			+ "in Batch Redemption Process API with Multiple Points")
	public void validateRedeemableInBatchRedemptionProcessAPIWithMultiplePointsDiscountLookup() {

		String expectedErrorMessage = dataSet.get("expectedFailureMessage");
		String expectedRedeemableID = dataSet.get("redeemableID");
		String expectedRedeemableName = dataSet.get("redeemableName");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				dataSet.get("giftPoints"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode());

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redeemable", dataSet.get("redeemableID"));

		// Force Redeem
		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(dataSet.get("apiKey"), "", userID);
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Platform Api Force Redeem");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Force Redeem is successful");

		Response batchRedemptionProcessResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationKey"),
				userID, dataSet.get("itemID"));
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].message").replace("[[", "").replace("]]", "");

		String actualDiscount_type = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_type").replace("[[", "").replace("]]", "");

		String actualRedeemableName = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[[", "").replace("]]", "");

		Assert.assertTrue(actualFailureMessage.contains(expectedErrorMessage));
		pageObj.utils().logPass("Verify the actual failure message ' " + actualFailureMessage
				+ "' with the expected error message '" + expectedErrorMessage + "'");

		Assert.assertEquals(actualDiscount_type, "redeemable");
		pageObj.utils().logPass("Verify the actual discount_type' " + actualDiscount_type
				+ "' with the expected discount_type 'redeemable'");

		Assert.assertEquals(actualRedeemableName, dataSet.get("redeemableName"));
		pageObj.utils().logPass("Verify the actual redeemable name ' " + actualRedeemableName
				+ "' with the expected redeemable name '" + expectedRedeemableName + "'");

		String actualRedeemableID = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.item_id").replace("[[", "").replace("]]", "");

		double actualRedeemableIDDouble = Double.parseDouble(actualRedeemableID);
		double expectedRedeemableIDDouble = Double.parseDouble(expectedRedeemableID);
		Assert.assertEquals(actualRedeemableIDDouble, expectedRedeemableIDDouble);
		pageObj.utils().logPass("Redeemable  id " + actualRedeemableID
				+ " is verified with the expected redeemable id " + expectedRedeemableID);
	}

	@Test(description = "OMM-T195 - STEP2 - Verify functionality validations for \"discount_type -> redeemable\" "
			+ "in Batch Redemption Process API with Location")
	public void validateRedeemableInBatchRedemptionProcessAPIWithLocation() {
		String expectedErrorMessage = dataSet.get("expectedFailureMessage");
		String expectedRedeemableID = dataSet.get("redeemableID");
		String expectedRedeemableName = dataSet.get("redeemableName");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		System.out.println("userID" + userID);

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"50");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode());

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redeemable", dataSet.get("redeemableID"));

		System.out.println(discountBasketResponse.asPrettyString());

		Response batchRedemptionProcessResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationB_Key"),
				userID, "101");

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].message").replace("[[", "").replace("]]", "");

		String actualDiscount_type = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_type").replace("[[", "").replace("]]", "");

		String actualRedeemableName = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[[", "").replace("]]", "");

		Assert.assertTrue(actualFailureMessage.contains(expectedErrorMessage));
		pageObj.utils().logPass("Verify the actual failure message ' " + actualFailureMessage
				+ "' with the expected error message '" + expectedErrorMessage + "'");

		Assert.assertEquals(actualDiscount_type, "redeemable");
		pageObj.utils().logPass("Verify the actual discount_type' " + actualDiscount_type
				+ "' with the expected discount_type 'redeemable'");

		Assert.assertEquals(actualRedeemableName, expectedRedeemableName);
		pageObj.utils().logPass("Verify the actual redeemable name ' " + actualRedeemableName
				+ "' with the expected redeemable name '" + expectedRedeemableName + "'");

		String actualRedeemableID = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.item_id").replace("[[", "").replace("]]", "");

		double actualRedeemableIDDouble = Double.parseDouble(actualRedeemableID);
		double expectedRedeemableIDDouble = Double.parseDouble(expectedRedeemableID);

		Assert.assertEquals(actualRedeemableIDDouble, expectedRedeemableIDDouble);
		pageObj.utils().logPass("Redeemable  id " + actualRedeemableID
				+ " is verified with the expected redeemable id " + expectedRedeemableIDDouble);

	}

	@Test(description = "OMM-T195 (1.0) - STEP3 - Verify functionality validations for \"discount_type -> redeemable\" "
			+ "in Batch Redemption Process API with QC")
	public void validateRedeemableInBatchRedemptionProcessAPIWithQC() {
		String expectedErrorMessage = dataSet.get("expectedFailureMessage");
		String expectedRedeemableID = dataSet.get("redeemableID");
		String expectedRedeemableName = dataSet.get("redeemableName");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		System.out.println("userID" + userID);

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"50");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode());

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redeemable", dataSet.get("redeemableID"));

		System.out.println(discountBasketResponse.asPrettyString());

		Response batchRedemptionProcessResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationKey"),
				userID, dataSet.get("itemID"));

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].message").replace("[[", "").replace("]]", "");

		String actualDiscount_type = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_type").replace("[[", "").replace("]]", "");

		String actualRedeemableName = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[[", "").replace("]]", "");

		Assert.assertTrue(actualFailureMessage.contains(expectedErrorMessage));
		pageObj.utils().logPass("Verify the actual failure message ' " + actualFailureMessage
				+ "' with the expected error message '" + expectedErrorMessage + "'");

		Assert.assertEquals(actualDiscount_type, "redeemable");
		pageObj.utils().logPass("Verify the actual discount_type' " + actualDiscount_type
				+ "' with the expected discount_type 'redeemable'");

		Assert.assertEquals(actualRedeemableName, expectedRedeemableName);
		pageObj.utils().logPass("Verify the actual redeemable name ' " + actualRedeemableName
				+ "' with the expected redeemable name '" + expectedRedeemableName + "'");

		String actualRedeemableID = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.item_id").replace("[[", "").replace("]]", "");

		double actualRedeemableIDDouble = Double.parseDouble(actualRedeemableID);
		double expectedRedeemableIDDouble = Double.parseDouble(expectedRedeemableID);

		Assert.assertEquals(actualRedeemableIDDouble, expectedRedeemableIDDouble);
		pageObj.utils().logPass("Redeemable  id " + actualRedeemableID
				+ " is verified with the expected redeemable id " + expectedRedeemableIDDouble);

	}

	@Test(description = "OMM-T195 (1.0) - STEP1 - Verify functionality validations for \"discount_type -> redeemable\" "
			+ "in Batch Redemption Process API added to basket which has Flat discount $0")
	public void validateRedeemableInBatchRedemptionProcessAPIWithZeroFlatDiscount() {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"20");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode());

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redeemable", dataSet.get("redeemableID"));

		Response batchRedemptionProcessResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationKey"),
				userID, "");

		System.out.println("batchRedemptionProcessResponse***** " + batchRedemptionProcessResponse.asPrettyString());

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].message").replace("[", "").replace("]", "").replace("[", "")
				.replace("]", "");

		Assert.assertEquals(actualFailureMessage, dataSet.get("expectedFailureMessage"));
	}

	@Test(description = "OMM-T194 (1.0) STEP-2")
	public void validateRewardInBatchRedeemptionQC() {
		String expectedErrorMessage = dataSet.get("expectedFailureMessage");
		String expectedRedeemableID = dataSet.get("redeemableID");
		String expectedRedeemableName = dataSet.get("redempetionName");
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemableID"), "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID);

		// Lookup call
		Response batchRedemptionProcessResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationKey"),
				userID, "");

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].message").replace("[[", "").replace("]]", "");

		String actualDiscount_type = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_type").replace("[[", "").replace("]]", "");

		String actualRedeemableName = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[[", "").replace("]]", "");

		Assert.assertTrue(actualFailureMessage.contains(expectedErrorMessage));
		pageObj.utils().logPass("Verify the actual failure message ' " + actualFailureMessage
				+ "' with the expected error message '" + expectedErrorMessage + "'");

		Assert.assertEquals(actualDiscount_type, "reward");
		pageObj.utils().logPass("Verify the actual discount_type' " + actualDiscount_type
				+ "' with the expected discount_type 'reward'");

		Assert.assertEquals(actualRedeemableName, expectedRedeemableName);
		pageObj.utils().logPass("Verify the actual redeemable name ' " + actualRedeemableName
				+ "' with the expected redeemable name '" + expectedRedeemableName + "'");

		String actualRedeemableID = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.item_id").replace("[[", "").replace("]]", "");

		double actualRedeemableIDDouble = Double.parseDouble(actualRedeemableID);
		double expectedRedeemableIDDouble = Double.parseDouble(expectedRedeemableID);

		Assert.assertEquals(actualRedeemableIDDouble, expectedRedeemableIDDouble);
		pageObj.utils().logPass("Redeemable  id " + actualRedeemableID
				+ " is verified with the expected redeemable id " + expectedRedeemableIDDouble);

	}

	@Test(description = "OMM-T194 (1.0) STEP1 = ")
	public void validateRewardInBatchRedeemptionFlatZeroDiscount() {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logit("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemableID"), "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardID);

		Response batchRedemptionProcessResponse = pageObj.endpoints().POSDiscountLookup(dataSet.get("locationKey"),
				userID, "");

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].message").replace("[[", "").replace("]]", "");

		String actualDiscount_type = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_type").replace("[[", "").replace("]]", "");

		String actualRedeemableName = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.name").replace("[[", "").replace("]]", "");

		Assert.assertTrue(actualFailureMessage.contains(dataSet.get("expectedFailureMessage")));
		pageObj.utils().logPass("Verify the actual failure message ' " + actualFailureMessage
				+ "' with the expected error message '" + dataSet.get("expectedFailureMessage") + "'");

		Assert.assertEquals(actualDiscount_type, "reward");
		pageObj.utils().logPass("Verify the actual discount_type' " + actualDiscount_type
				+ "' with the expected discount_type 'reward'");

		Assert.assertEquals(actualRedeemableName, dataSet.get("redeemableName"));
		pageObj.utils().logPass("Verify the actual redeemable name ' " + actualRedeemableName
				+ "' with the expected redeemable name '" + dataSet.get("redeemableName") + "'");

		String actualRedeemableID = batchRedemptionProcessResponse.jsonPath()
				.getString("selected_discounts[0].discount_details.item_id").replace("[[", "").replace("]]", "");

		double actualRedeemableIDDouble = Double.parseDouble(actualRedeemableID);
		double expectedRedeemableIDDouble = Double.parseDouble(dataSet.get("redeemableID"));
		Assert.assertEquals(actualRedeemableIDDouble, expectedRedeemableIDDouble);
		pageObj.utils().logPass("Redeemable  id " + actualRedeemableID
				+ " is verified with the expected redeemable id " + dataSet.get("redeemableID"));
	}

	@AfterMethod(alwaysRun = true)
	public void teraDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}
