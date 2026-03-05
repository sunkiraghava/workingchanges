package com.punchh.server.OMMTest;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.Listeners;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
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
public class ValidationInBatchRedemptionProcessAPI {
	static Logger logger = LogManager.getLogger(ValidationInBatchRedemptionProcessAPI.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	// private boolean actRedeemableNameAutoSelect;
	public boolean GlobalBenefitRedemptionThrottlingToggle;

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
	}

	@Test(description = "OMM-T72 - STEP1 - Verify functionality validations for \"discount_type -> redeemable\" "
			+ "in Batch Redemption Process API added to basket which has Flat discount $0")
	public void validateRedeemableInBatchRedemptionProcessAPIWithZeroFlatDiscount() {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"20");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode());

		pageObj.endpoints().authListDiscountBasketAddedAUTH(token, dataSet.get("client"), dataSet.get("secret"),
				"redeemable", dataSet.get("redeemableID"));

		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationKey"), userID, "");

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath().getString("failures.message")
				.replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualFailureMessage, dataSet.get("expectedFailureMessage"));
	}

	@Test(description = "OMM-T72 - STEP2 - Verify functionality validations for \"discount_type -> redeemable\" "
			+ "in Batch Redemption Process API with QC")
	public void validateRedeemableInBatchRedemptionProcessAPIWithQC() {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		System.out.println("userID" + userID);

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"50");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode());

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redeemable", dataSet.get("redeemableID"));

		System.out.println(discountBasketResponse.asPrettyString());

		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationKey"), userID, dataSet.get("itemID"));

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath().getString("failures.message")
				.replace("[[", "").replace("]]", "");

		Assert.assertTrue(actualFailureMessage.contains(dataSet.get("expectedFailureMessage")));
	}

	@Test(description = "OMM-T72 - STEP3 - Verify functionality validations for \"discount_type -> redeemable\" "
			+ "in Batch Redemption Process API with Location")
	public void validateRedeemableInBatchRedemptionProcessAPIWithLocation() {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		System.out.println("userID" + userID);

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				"50");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode());

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), "redeemable", dataSet.get("redeemableID"));

		System.out.println(discountBasketResponse.asPrettyString());

		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationKey"), userID, "101");

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath().getString("failures.message")
				.replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualFailureMessage, dataSet.get("expectedFailureMessage"));
	}

	@Test(description = "OMM-T72 - STEP4 - Verify functionality validations for \"discount_type -> redeemable\" "
			+ "in Batch Redemption Process API with Multiple Points")
	public void validateRedeemableInBatchRedemptionProcessAPIWithMultiplePoints() {

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		System.out.println("userID" + userID);

		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		pageObj.utils().logPass("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "", "", "",
				dataSet.get("giftPoints"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode());

		pageObj.endpoints().authListDiscountBasketAddedAUTH(token, dataSet.get("client"), dataSet.get("secret"),
				"redeemable", dataSet.get("redeemableID"));

		// Force Redeem
		Response forceRedeemResponse = pageObj.endpoints().forceRedeem(dataSet.get("apiKey"), "", userID);
		Assert.assertEquals(forceRedeemResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for Platform Api Force Redeem");
		TestListeners.extentTest.get().pass("PLATFORM FUNCTIONS API Force Redeem is successful");

		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationKey"), userID, dataSet.get("itemID"));
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isBatchRedemptionInsufficientBalanceSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorObjectSchema, batchRedemptionProcessResponse.asString());
		Assert.assertTrue(isBatchRedemptionInsufficientBalanceSchemaValidated,
				"POS Batch Redemption Schema Validation failed");
		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath().getString("error").replace("[[", "")
				.replace("]]", "");

		Assert.assertEquals(actualFailureMessage, dataSet.get("expectedFailureMessage"));
	}

	@Test(description = "OMM-T71 STEP-1 Verify functionality validations for \"discount_type -> reward\" in Batch Redemption Process API")
	public void validateRewardInBatchRedeemptionFlatZeroDiscount() {
		System.out.println(dataSet.get("slug"));
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
				dataSet.get("redeemableID"), "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		pageObj.utils().logit("Reward id " + rewardID + " is generated successfully ");

		pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"), dataSet.get("secret"), "reward",
				rewardID);

		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationKey"), userID, "");

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath().getString("failures.message")
				.replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualFailureMessage, dataSet.get("expectedFailureMessage"));

	}

	@Test(description = "OMM-T71 STEP-2 Verify functionality validations for \"discount_type -> reward\" in Batch Redemption Process API"
			+ "OMM-T59 / SQ-T313 [Batched Redemptions] Verify functionality cases for \"discount_type -> reward\" in \"Add Discounts To Basket\" API (POS)")
	public void validateRewardInBatchRedeemptionQC() {
		System.out.println(dataSet.get("slug"));
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
				dataSet.get("redeemableID"), "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));

		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");
		pageObj.utils().logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponseReward = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPI(dataSet.get("locationKey"), userID, "reward", rewardID);

		String actRedeemableNameAutoSelect = discountBasketResponseReward.jsonPath()
				.getString("discount_basket_items[0].discount_details.name").replace("[", "").replace("]", "");
		Assert.assertEquals(actRedeemableNameAutoSelect, dataSet.get("redempetionName"),
				actRedeemableNameAutoSelect + " redeemable name is not matched in API response");
		pageObj.utils().logPass(actRedeemableNameAutoSelect + " redeemableName is matched with auto select API response");

		Response discountBasketResponseRewardAgain = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPI(dataSet.get("locationKey"), userID, "reward", rewardID);
		System.out
				.println("discountBasketResponseRewardAgain == " + discountBasketResponseRewardAgain.asPrettyString());

		String actualFailureMessage2 = discountBasketResponseRewardAgain.jsonPath().getString("error").replace("[[", "")
				.replace("]]", "");
		Assert.assertTrue(actualFailureMessage2.contains(dataSet.get("expectedFailureMessage2")),
				actualFailureMessage2 + " is not matched with expected failure message");

		pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"), dataSet.get("secret"), "reward",
				rewardID);

		Response batchRedemptionProcessResponse = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationKey"), userID, "");

		String actualFailureMessage = batchRedemptionProcessResponse.jsonPath().getString("failures.message")
				.replace("[[", "").replace("]]", "");

		Assert.assertTrue(actualFailureMessage.contains(dataSet.get("expectedFailureMessage")));

	}

	@Test(description = "OMM-T73 (1.0) STEP-1 Verify functionality validations for \"discount_type -> redemption_code\" in Batch Redemption Process API")
	public void validateCouponCampaignRedemptionCodeInBatchProcess() throws InterruptedException {

		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		System.out.println("coupanCampaignName == " + coupanCampaignName);

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), dataSet.get("locationA"),
				"BatchRedmeption_QC", GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		TestListeners.extentTest.get().pass("Coupon campaign created successfuly");

		Thread.sleep(8000);
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		String generatedCodeName = pageObj.campaignspage().getPreGeneratedCuponList().get(0);
		logger.info("Coupon code generated :- " + generatedCodeName);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"), dataSet.get("secret"),
				"redemption_code", generatedCodeName);

		// Given Location B key , and verifying the failure message
		Response batchRedemptionProcessResponseLocationB = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationB_Key"), userID, "");

		String actualFailureMessage = batchRedemptionProcessResponseLocationB.jsonPath().getString("failures.message")
				.replace("[[", "").replace("]]", "");

		// Given Location A Key and verifying that batch should be processed
		// successfully

		Assert.assertEquals(actualFailureMessage, dataSet.get("expectedFailureMessage"));

		Response batchRedemptionProcessResponseLocationA = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationA_Key"), userID, "");

		Assert.assertEquals(batchRedemptionProcessResponseLocationA.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Given invalid item id and so that QC get failed and verifying the status
		Response batchRedemptionProcessResponseQC = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationA_Key"), userID, "104");

		System.out.println("batchRedemptionProcessResponseQC=== " + batchRedemptionProcessResponseQC.asPrettyString());

		String actualFailureMessageQCFail = batchRedemptionProcessResponseQC.jsonPath().getString("failures.message")
				.replace("[[", "").replace("]]", "");

		Assert.assertEquals(batchRedemptionProcessResponseQC.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(actualFailureMessageQCFail.contains("Discount qualification on receipt failed"));

		// Given invalid item id and so that QC get failed and verifying the status
		Response batchRedemptionProcessResponseQCPass = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationA_Key"), userID, "101");

		System.out.println(
				"batchRedemptionProcessResponseQC=== " + batchRedemptionProcessResponseQCPass.asPrettyString());

		batchRedemptionProcessResponseQCPass.jsonPath().getString("failures.message").replace("[[", "").replace("]]",
				"");

		Assert.assertEquals(batchRedemptionProcessResponseQC.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

	}

	@Test(description = "OMM-T73 (1.0) STEP-2  Verify functionality validations for \\\"discount_type -> redemption_code\\\" in Batch Redemption Process API")
	public void validatePromoRedemptionCodeInBatchProcess() throws InterruptedException, ParseException {

		String promoCode = CreateDateTime.getRandomString(6).toUpperCase() + Utilities.getRandomNoFromRange(500, 2000);
		String coupanCampaignName = "Auto_PromoCampaign" + CreateDateTime.getTimeDateString();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// Login to instance
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

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		TestListeners.extentTest.get().pass("Coupon campaign created successfuly");

		pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(dataSet.get("locationA_Key"), userID,
				"redemption_code", promoCode);

		// Given location B and verified that batch process should failed
		Response batchRedemptionProcessResponseLocationB = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationB_Key"), userID, "");

		String actualFailureMessage = batchRedemptionProcessResponseLocationB.jsonPath().getString("failures.message")
				.replace("[[", "").replace("]]", "");

		Assert.assertEquals(actualFailureMessage, dataSet.get("expectedFailureMessage"));

		// Given LocationA and verified that is should work
		Response batchRedemptionProcessResponseLocationA = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationA_Key"), userID, "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, batchRedemptionProcessResponseLocationA.getStatusCode());

		// QC failed and verify the error message

		// Given invalid item id and so that QC get failed and verifying the status
		Response batchRedemptionProcessResponseQC = pageObj.endpoints()
				.processBatchRedemptionOfBasketPOS(dataSet.get("locationA_Key"), userID, "104");

		System.out.println("batchRedemptionProcessResponseQC=== " + batchRedemptionProcessResponseQC.asPrettyString());

		String actualFailureMessageQCFail = batchRedemptionProcessResponseQC.jsonPath().getString("failures.message")
				.replace("[[", "").replace("]]", "");

		Assert.assertEquals(batchRedemptionProcessResponseQC.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		Assert.assertTrue(actualFailureMessageQCFail.contains("Discount qualification on receipt failed"));

	}

	@Test(description = "OMM-T73 (1.0) STEP-1 Verify functionality validations for \"discount_type -> redemption_code\" in Batch Redemption Process API")
	public void validateCouponCampaignRedemptionCodeInBatchProcessMultiProcess() throws InterruptedException {
		String userEmailUser1;

		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		System.out.println("coupanCampaignName == " + coupanCampaignName);
		GlobalBenefitRedemptionThrottlingToggle = true;
//		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), dataSet.get("locationA"),
				"BatchRedmeption_QC", GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		TestListeners.extentTest.get().pass("Coupon campaign created successfuly");

		Thread.sleep(8000);
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		String generatedCodeName = pageObj.campaignspage().getPreGeneratedCuponList().get(0);
		System.out.println("generatedCodeName == " + generatedCodeName);

		// Create user 1

		userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponseUser1 = pageObj.endpoints().Api2SignUp(userEmailUser1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponseUser1, "API 2 user signup");
		String tokenUser1 = signUpResponseUser1.jsonPath().get("access_token.token").toString();

		String userIDUser1 = signUpResponseUser1.jsonPath().get("user.user_id").toString();

		pageObj.endpoints().authListDiscountBasketAdded(tokenUser1, dataSet.get("client"), dataSet.get("secret"),
				"redemption_code", generatedCodeName);

		pageObj.endpoints().processBatchRedemptionOfBasketPOS(dataSet.get("locationA_Key"), userIDUser1, "101");

		Response discountBasketResponseUser1Again = pageObj.endpoints().authListDiscountBasketAdded(tokenUser1,
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", generatedCodeName);

		int statusCode2 = discountBasketResponseUser1Again.getStatusCode();
		Assert.assertEquals(statusCode2, ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);

	}

	@AfterMethod(alwaysRun = true)
	public void teraDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}