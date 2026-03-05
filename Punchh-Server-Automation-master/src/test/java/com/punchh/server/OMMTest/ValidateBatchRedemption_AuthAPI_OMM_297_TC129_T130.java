package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.OfferIngestionUtilityClass.OfferIngestionUtilities;
import com.punchh.server.annotations.Owner;
import com.punchh.server.api.payloadbuilder.RedeemablePayloadBuilder;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
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

public class ValidateBatchRedemption_AuthAPI_OMM_297_TC129_T130 {
	static Logger logger = LogManager.getLogger(ValidateBatchRedemption_AuthAPI_OMM_297_TC129_T130.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	boolean enableMenuItemAggregatorFlag;
	String location1, location2;
	String filterSetName;
	Utilities utils;
	boolean GlobalBenefitRedemptionThrottlingToggle;
	private List<String> codeNameList;
	private ApiPayloadObj apipayloadObj;
	private OfferIngestionUtilities offerUtils;
	private String lisExternalID, qcExternalID, redeemableExternalID;

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) throws Exception {
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
		utils = new Utilities(driver);
		utils.logit(sTCName + " ==>" + dataSet);
		enableMenuItemAggregatorFlag = false;
		location1 = " Jacksonville - 3816";
		location2 = " Daphne - 66233";
		filterSetName = "Only Base Items";
		GlobalBenefitRedemptionThrottlingToggle = false;
		codeNameList = new ArrayList<String>();
		apipayloadObj = new ApiPayloadObj();
		offerUtils = new OfferIngestionUtilities(driver);

	}

	/**
	 * For below Test Case: Verify Business Validations and Auto Checkin After Redemption Cases
	 *
	 * Business Notes:
	 * 1. On Redemptions -> Post Redemptions:
	 *    - The checkbox "Allow to initiate checkin after redemption code is processed (via POS)" we are disabling in the below code.
	 *
	 * 2. On Redemptions -> Multiple Redemptions:
	 *    - The flag "Enable Reward Locking" should be disabled.
	 */
	@Test(description = "SQ-T7474 - Step 2: Verify Business Validations and Auto Checkin After Redemption Cases", priority=1)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T130_Step2() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Post Redemption");
		pageObj.cockpitRedemptionsPage().clickedOnAutoCheckinRedemptionCheckBox("uncheck");
		
		// =====================  Create LIS =====================
		String lisName = "Automation_BatchRedmp_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);
		
		// =====================️ Create QC =====================
		String qcname = "Automation_BatchRedmp_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts").setStackDiscounting(true)
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);
		
		// ===================== Create Redeemable =====================
		// Create Redeemable with above QC
		String redeemableName = "Automation_BatchRedmp_Redeemable_"+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing").redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);
		

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));
		
		
		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"), dataSet.get("apiKey"), "",dbRedeemableId, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");


		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(userInfo.get("token"), dataSet.get("client"),dataSet.get("secret"));
		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");
		utils.logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"), dataSet.get("client"),dataSet.get("secret"), "reward", rewardID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),"Status code 200 did not matched for discount basket added API");
		utils.logPass(rewardID + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationKey"), userInfo.get("token"), userInfo.get("userID"), dataSet.get("item_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, batchRedemptionProcessResponse.getStatusCode(),"Status code 200 did not matched for processBatchRedemptionOfBasketAUTHAPI");
		utils.logPass(userEmail + " User process the basket");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.logit("Navigated to Guest timeline ");

		boolean postCheckinLavelIsDisplayed = pageObj.guestTimelinePage().verifyPostCheckinIsDispayedOnTimeLine();
		Assert.assertFalse(postCheckinLavelIsDisplayed, "Post checkin label is not displayed on timeline page ");
		utils.logPass("Verified that post checkin is displayed on user timeline ");

	}
	
	/**
	 * For below Test Case: Verify Business Validations and Auto Checkin After Redemption Cases
	 *
	 * Business Notes:
	 * 1. On Redemptions -> Post Redemptions:
	 *    - The checkbox "Allow to initiate checkin after redemption code is processed (via POS)" we are enabling in the below code.
	 *
	 * 2. On Redemptions -> Multiple Redemptions:
	 *    - The flag "Enable Reward Locking" should be disabled.
	 */

	@Test(description = "SQ-T7474 - Step 1: Verify Business Validations and Auto Checkin After Redemption Cases", priority=2)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T130_Step1() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Post Redemption");
		pageObj.cockpitRedemptionsPage().clickedOnAutoCheckinRedemptionCheckBox("check");

		double expAmount = Double.parseDouble(dataSet.get("expAutoCheckinAmount"));
		utils.logit("Expected Auto-Checkin Amount: " + expAmount);
		String expReceiptTagName = "Pizza10%Tag-"+RandomStringUtils.randomAlphanumeric(3);
		utils.logit("Generated Receipt Tag Name: " + expReceiptTagName);

		// =====================  Create LIS =====================
		String lisName = "Automation_BatchRedmp_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "Automation_BatchRedmp_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts").setStackDiscounting(true)
				.addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);
		
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(qcname);
		pageObj.qualificationcriteriapage().setReceiptTagName(expReceiptTagName);
		pageObj.qualificationcriteriapage().updateButton();
		pageObj.qualificationcriteriapage().activateTag(qcname);

		// ===================== Create Redeemable =====================
		// Create Redeemable with above QC
		String redeemableName = "Automation_BatchRedmp_Redeemable_"+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("existing").redeeming_criterion_id(qcExternalID).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);


		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmail, dataSet.get("client"), dataSet.get("secret"));

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"), dataSet.get("apiKey"), "",dbRedeemableId, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");


		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(userInfo.get("token"), dataSet.get("client"),dataSet.get("secret"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, rewardResponse.getStatusCode(),"Status code 200 did not matched for authListAvailableRewardsNew API");
		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");
		utils.logit("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"), dataSet.get("client"),dataSet.get("secret"), "reward", rewardID);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse.getStatusCode(),"Status code 200 did not matched for discount basket added API");
		utils.logPass(rewardID + " rewardid is added to the basket ");

		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), userInfo.get("token"), userInfo.get("userID"),dataSet.get("item_id"));
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, batchRedemptionProcessResponse.getStatusCode(),"Status code 200 did not matched for processBatchRedemptionOfBasketAUTHAPI");
		utils.logPass(userEmail + " User process the basket");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		utils.logit("Navigated to Guest timeline ");

		boolean postCheckinLavelIsDisplayed = pageObj.guestTimelinePage().verifyPostCheckinIsDispayedOnTimeLine();
		Assert.assertTrue(postCheckinLavelIsDisplayed, "Post checkin label is not displayed on timeline page ");
		utils.logPass("Verified that post checkin is displayed on user timeline ");

		double actAmount = pageObj.guestTimelinePage().getAutoCheckinAmountOnTimeLinePage();
		Assert.assertEquals(actAmount, expAmount);
		utils.logPass("Verified that autocheckin amount " + expAmount + " on timeline page ");

		boolean isTagDisplayed = pageObj.guestTimelinePage().verifyReceiptTagIsDisplayedForAutoCheckin(expReceiptTagName);
		Assert.assertTrue(isTagDisplayed, "Receipt Tag is not displayed ");
		utils.logPass(expReceiptTagName + "is displayed on user timeline page ");
		
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().deactivateTagFromQc(qcname);

	}

	@Test(description = "SQ-T7477 - Step 5:Verify functionality validations for Multiple Redemptions configuration in Batch Redemption Process API", priority=3)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T129_Step5() throws Exception {

		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmailUser1, dataSet.get("client"), dataSet.get("secret"));

		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("coupanCampaignName == " + coupanCampaignName);
		
		// =====================  Create LIS =====================
		String lisName = "Automation_BatchRedmp_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);
		
		// =====================️ Create QC =====================
		String qcname = "Automation_BatchRedmp_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts").setStackDiscounting(true).setReuseQualifyingItems(true)
				.addLineItemFilter(lisExternalID, "max_price", 2)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);
		
		
		// Create Redeemable with above QC
		String redeemableName = "Automation_BatchRedmp_Redeemable_"+ Utilities.getTimestamp();
		RedeemablePayloadBuilder builder = apipayloadObj.redeemableBuilder();
		String redeemablePayload = builder.startNewData().setName(redeemableName)
				.setBooleanField("available_as_template", false)
				.setBooleanField("allow_for_support_gifting", true)
				.setDistributable(false).setAutoApplicable(false)
				.setReceiptRule(RedeemableReceiptRule.builder().qualifier_type("flat_discount").discount_amount(20.0).build())
				.setBooleanField("indefinetely", true).setDiscountChannel("all")
				.addCurrentData().build();
		Response redeemableResponse = pageObj.endpoints().createRedeemable(redeemablePayload, dataSet.get("apiKey"));
		Assert.assertEquals(redeemableResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createRedeemable API");

		// Get Redeemable External ID
		redeemableExternalID = redeemableResponse.jsonPath().getString("results[0].external_id");
		utils.logit("Created Redeemable External ID: " + redeemableExternalID);

		// Get Redeemable ID from DB
		String query = "SELECT id FROM redeemables WHERE uuid = '" + redeemableExternalID + "'";
		String dbRedeemableId = DBUtils.executeQueryAndGetColumnValue(env, query, "id");
		utils.logit("DB Redeemable ID: " + dbRedeemableId);
				

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Earned Rewards", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Pre-Purchased Discount", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Coupons & Promos", "3");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();
		utils.waitTillCompletePageLoad();
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", qcname, GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		utils.longWaitInSeconds(20);
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		String couponCode1 = codeNameList.get(0);
		utils.logit("Coupon Code 1: " + couponCode1);

		String couponCode2 = codeNameList.get(1);
		utils.logit("Coupon Code 2: " + couponCode2);

		// send reward amount to user Reedemable
		Response sendRewardResponse1 = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"), dataSet.get("apiKey"), "",dbRedeemableId, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse1.getStatusCode(),"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userInfo.get("userID"), dataSet.get("apiKey"), "",dbRedeemableId, "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Api2  send reward amount to user is successful");

		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(userInfo.get("token"), dataSet.get("client"),dataSet.get("secret"));
		String rewardID1 = rewardResponse.jsonPath().getString("id[0]").replace("[", "").replace("]", "");
		String rewardID2 = rewardResponse.jsonPath().getString("id[1]").replace("[", "").replace("]", "");

		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"),dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponseCoupon1.getStatusCode(),"Status code 200 did not matched for authListDiscountBasketAdded");
		utils.logPass(couponCode1 + " couponCode 1 is added to the basket ");

		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"),dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponseCoupon2.getStatusCode(),"Status code 200 did not matched for authListDiscountBasketAdded");
		utils.logPass(couponCode2 + " couponCode 2 is added to the basket ");

		utils.logit("discountBasketResponseCoupon3 -=== " + discountBasketResponseCoupon2.asPrettyString());
		utils.logit(rewardID1 + " & " + rewardID2 + "is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"),dataSet.get("client"), dataSet.get("secret"), "reward", rewardID1);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID1 + " rewardid is added to the basket ");

		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"),dataSet.get("client"), dataSet.get("secret"), "reward", rewardID2);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass(rewardID2 + " rewardid is added to the basket ");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "1");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), userInfo.get("token"), userInfo.get("userID"),"101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isAuthBatchRedemptionProcessLimitReachedSchemaValidated = Utilities.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, batchRedemptionProcessResponse.asString());
		Assert.assertTrue(isAuthBatchRedemptionProcessLimitReachedSchemaValidated,"Auth API Batch Redemption Process Schema Validation failed");
		String actualErrorMessage = batchRedemptionProcessResponse.jsonPath().getString("error").replace("[", "").replace("]", "");
		String expectedErrorMessage = dataSet.get("expectedFailureMessage");

		Assert.assertEquals(actualErrorMessage, expectedErrorMessage);
		utils.logPass("Verifed the actual error message " + actualErrorMessage);

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	@Test(description = "SQ-T7477 - Step 4:Verify functionality validations for Multiple Redemptions configuration in Batch Redemption Process API", priority=3)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T129_Step4() throws Exception {
		
		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmailUser1, dataSet.get("client"), dataSet.get("secret"));

		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("coupanCampaignName == " + coupanCampaignName);

		// =====================  Create LIS =====================
		String lisName = "Automation_BatchRedmp_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "Automation_BatchRedmp_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts").setStackDiscounting(true).setReuseQualifyingItems(true)
				.addLineItemFilter(lisExternalID, "max_price", 2)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);


				
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");

		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Earned Rewards", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Pre-Purchased Discount", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Coupons & Promos", "2");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "",qcname, GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		utils.waitTillCompletePageLoad();
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		String couponCode1 = codeNameList.get(0);
		utils.logit("Coupon Code 1: " + couponCode1);
		
		String couponCode2 = codeNameList.get(1);
		utils.logit("Coupon Code 2: " + couponCode2);

		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		Assert.assertEquals(discountBasketResponseCoupon1.getStatusCode(),ApiConstants.HTTP_STATUS_OK,
		        "Status code 200 did not match while adding coupon code: " + couponCode1);
		utils.logit(couponCode1 + " code is added to basket successfully");

		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		Assert.assertEquals(discountBasketResponseCoupon2.getStatusCode(),ApiConstants.HTTP_STATUS_OK,
		        "Status code 200 did not match while adding coupon code: " + couponCode2);
		utils.logit(couponCode2 + " code is added to basket successfully");


		int exp_FirstRDP_discount_basket_item_id = discountBasketResponseCoupon1.jsonPath().getInt("discount_basket_items[0].discount_basket_item_id");
		utils.logit("Expected First RDP Discount Basket Item ID (Coupon 1): " + exp_FirstRDP_discount_basket_item_id);
		String exp_FirstRDP_discount_id = discountBasketResponseCoupon1.jsonPath().getString("discount_basket_items[0].discount_id");
		utils.logit("Expected First RDP Discount ID (Coupon 1): " + exp_FirstRDP_discount_id);

		int exp_SecondRDP_discount_basket_item_id = discountBasketResponseCoupon2.jsonPath().getInt("discount_basket_items[1].discount_basket_item_id");
		utils.logit("Expected Second RDP Discount Basket Item ID (Coupon 2): " + exp_SecondRDP_discount_basket_item_id);
		String exp_SecondRDP_discount_id = discountBasketResponseCoupon2.jsonPath().getString("discount_basket_items[1].discount_id");
		utils.logit("Expected Second RDP Discount ID (Coupon 2): " + exp_SecondRDP_discount_id);

		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), userInfo.get("token"), userInfo.get("userID"),"101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		utils.logPass("Verified the success message");
		
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	@Test(description = "SQ-T7477 - Step 3:Verify functionality validations for Multiple Redemptions configuration in Batch Redemption Process API", priority=3)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T129_Step3() throws Exception {
		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmailUser1, dataSet.get("client"), dataSet.get("secret"));

		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("coupanCampaignName == " + coupanCampaignName);
		
		// =====================  Create LIS =====================
		String lisName = "Automation_BatchRedmp_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "Automation_BatchRedmp_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts").setStackDiscounting(true).setReuseQualifyingItems(true)
				.addLineItemFilter(lisExternalID, "max_price", 2)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Offers", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Earned Rewards", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Pre-Purchased Discount", "2");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Coupons & Promos", "2");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");
		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "",
				qcname, GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();

		utils.waitTillCompletePageLoad();
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		String couponCode1 = codeNameList.get(0);
		utils.logit("Coupon Code 1: " + couponCode1);
		String couponCode2 = codeNameList.get(1);
		utils.logit("Coupon Code 2: " + couponCode2);

		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponseCoupon1.getStatusCode(),"Status code 200 did not matched for authListDiscountBasketAdded");
		utils.logPass(couponCode1 + " couponCode 1 is added to the basket ");

		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponseCoupon2.getStatusCode(),"Status code 200 did not matched for authListDiscountBasketAdded");
		utils.logPass(couponCode2 + " couponCode 2 is added to the basket ");

		int exp_FirstRDP_discount_basket_item_id = discountBasketResponseCoupon1.jsonPath().getInt("discount_basket_items[0].discount_basket_item_id");
		utils.logit("Expected First RDP Discount Basket Item ID: " + exp_FirstRDP_discount_basket_item_id);
		String exp_FirstRDP_discount_id = discountBasketResponseCoupon1.jsonPath().getString("discount_basket_items[0].discount_id");
		utils.logit("Expected First RDP Discount ID: " + exp_FirstRDP_discount_id);
		
		int exp_SecondRDP_discount_basket_item_id = discountBasketResponseCoupon2.jsonPath().getInt("discount_basket_items[1].discount_basket_item_id");
		utils.logit("Expected Second RDP Discount Basket Item ID: " + exp_SecondRDP_discount_basket_item_id);
		String exp_SecondRDP_discount_id = discountBasketResponseCoupon2.jsonPath().getString("discount_basket_items[1].discount_id");
		utils.logit("Expected Second RDP Discount ID: " + exp_SecondRDP_discount_id);
		
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().setAcquisitionType("Coupons & Promos", "1");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		Response batchRedemptionProcessResponse = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), userInfo.get("token"), userInfo.get("userID"),
				"101");
		Assert.assertEquals(batchRedemptionProcessResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isAuthBatchRedemptionProcessLimitReachedSchemaValidated = Utilities.validateJsonAgainstSchema(ApiResponseJsonSchema.apiErrorObjectSchema, batchRedemptionProcessResponse.asString());
		Assert.assertTrue(isAuthBatchRedemptionProcessLimitReachedSchemaValidated,"Auth API Batch Redemption Process Schema Validation failed");
		String actualErrorMessage = batchRedemptionProcessResponse.jsonPath().getString("error").replace("[", "").replace("]", "");
		utils.logit("Actual Error Message: " + actualErrorMessage);

		String expectedErrorMessage = dataSet.get("expectedFailureMessage");
		utils.logit("Expected Error Message: " + expectedErrorMessage);

		Assert.assertEquals(actualErrorMessage, expectedErrorMessage);
		utils.logPass("Verifed the actual error message " + actualErrorMessage);

		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Offers");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Earned Rewards");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Pre-Purchased Discount");
		pageObj.cockpitRedemptionsPage().clearAcquisitionTypeData("Coupons & Promos");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	@Test(description = "SQ-T7477 - Step 1:Verify functionality validations for Multiple Redemptions configuration in Batch Redemption Process API", priority=3)
	@Owner(name = "Shashank Sharma")
	public void validateOMM297_T129_Step1() throws Exception {
	
		String userEmailUser1 = pageObj.iframeSingUpPage().generateEmail();
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Map<String, String> userInfo = offerUtils.signUpUser(userEmailUser1, dataSet.get("client"), dataSet.get("secret"));

		String coupanCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		utils.logit("coupanCampaignName == " + coupanCampaignName);

		
		// =====================  Create LIS =====================
		String lisName = "Automation_BatchRedmp_LIS_" + Utilities.getTimestamp();
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName).setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", dataSet.get("item_id")).build();
		Response response = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		Assert.assertEquals(response.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");
		lisExternalID = response.jsonPath().getString("results[0].external_id");
		utils.logit(" LIS created with External ID: " + lisExternalID);

		// =====================️ Create QC =====================
		String qcname = "Automation_BatchRedmp_QC_" + Utilities.getTimestamp();
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setPercentageOfProcessedAmount(10).setQCProcessingFunction("sum_amounts").setStackDiscounting(true).setReuseQualifyingItems(true)
				.addLineItemFilter(lisExternalID, "max_price", 2)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		Assert.assertEquals(qcResponse.getStatusCode(),ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for createLIS API");

		// Get QC External ID
		qcExternalID = qcResponse.jsonPath().getString("results[0].external_id");
		utils.logit(qcname + "QC is created with External ID: " + qcExternalID);
		
         // Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(coupanCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "",
				qcname, GlobalBenefitRedemptionThrottlingToggle);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		utils.waitTillCompletePageLoad();
		pageObj.campaignspage().searchCampaign(coupanCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

		String couponCode1 = codeNameList.get(0);
		utils.logit("Coupon Code 1: " + couponCode1);

		String couponCode2 = codeNameList.get(1);
		utils.logit("Coupon Code 2: " + couponCode2);

		Response discountBasketResponseCoupon1 = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode1);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponseCoupon1.getStatusCode(),"Status code 200 did not matched for authListDiscountBasketAdded");
		utils.logPass(couponCode1 + " couponCode 1 is added to the basket ");


		Response discountBasketResponseCoupon2 = pageObj.endpoints().authListDiscountBasketAdded(userInfo.get("token"),
				dataSet.get("client"), dataSet.get("secret"), "redemption_code", couponCode2);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponseCoupon2.getStatusCode(),"Status code 200 did not matched for authListDiscountBasketAdded");
		utils.logPass(couponCode2 + " couponCode 2 is added to the basket ");


		int exp_FirstRDP_discount_basket_item_id = discountBasketResponseCoupon1.jsonPath().getInt("discount_basket_items[0].discount_basket_item_id");
		utils.logit("Expected First RDP Discount Basket Item ID (Coupon 2): " + exp_FirstRDP_discount_basket_item_id);
		String exp_FirstRDP_discount_id = discountBasketResponseCoupon1.jsonPath().getString("discount_basket_items[0].discount_id");
		utils.logit("Expected First RDP Discount ID (Coupon 2): " + exp_FirstRDP_discount_id);

		int exp_SecondRDP_discount_basket_item_id = discountBasketResponseCoupon2.jsonPath().getInt("discount_basket_items[1].discount_basket_item_id");
		utils.logit("Expected Second RDP Discount Basket Item ID (Coupon 2): " + exp_SecondRDP_discount_basket_item_id);

		String exp_SecondRDP_discount_id = discountBasketResponseCoupon2.jsonPath().getString("discount_basket_items[1].discount_id");
		utils.logit("Expected Second RDP Discount ID (Coupon 2): " + exp_SecondRDP_discount_id);

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("1");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		Response batchRedemptionProcessResponse1 = pageObj.endpoints().processBatchRedemptionOfBasketAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), dataSet.get("locationkey"), userInfo.get("token"), userInfo.get("userID"),
				"101");
		Assert.assertEquals(batchRedemptionProcessResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		utils.logit("Batch Redemption Response Status Code: " + batchRedemptionProcessResponse1.getStatusCode());
		
		String actualErrorMessage = batchRedemptionProcessResponse1.jsonPath().getString("error").replace("[", "").replace("]", "");
		utils.logit("Actual Error Message: " + actualErrorMessage);

		String expectedErrorMessage = dataSet.get("expectedFailureMessage");
		utils.logit("Expected Error Message: " + expectedErrorMessage);

		Assert.assertEquals(actualErrorMessage, expectedErrorMessage);
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.cockpitRedemptionsPage().updateTotalNumberOfRedemptionsAllowed("10");
		pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		utils.deleteLISQCRedeemable(env, lisExternalID, qcExternalID, redeemableExternalID);
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		utils.logit("Browser closed");
	}

} // END of class