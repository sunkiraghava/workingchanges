package com.punchh.server.OMMTest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Vansham Mishra
 */
@Listeners(TestListeners.class)

public class OffersPosRedemption {

	static Logger logger = LogManager.getLogger(OffersPosRedemption.class);
	public WebDriver driver;
	private String userEmail;
	private ApiUtils apiUtils;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private boolean GlobalBenefitRedemptionThrottlingToggle;
	private String endDateTime;
	private Utilities utils;
	public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
	public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
	String lisDeleteBaseQuery = "Delete from line_item_selectors where external_id = '${externalID}' and business_id='${businessID}'";
	String QCDeleteBaseQuery = "Delete from qualification_criteria where external_id = '${externalID2}' and business_id='${businessID}'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";
	String getQC_idString = "select id from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCFromQualification_criteriaQuery = "delete from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCQueryFromQualifying_expressionsQuery = "delete from qualifying_expressions where qualification_criterion_id ='$qcID'";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
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
		logger.info(sTCName + " ==>" + dataSet);
		GlobalBenefitRedemptionThrottlingToggle = false;
		endDateTime = CreateDateTime.getTomorrowDate() + " 10:00 AM";
		utils = new Utilities(driver);
	}

	@Test(description = "SQ-T5740 Validate that if user is performing POS redemption using Redemption 2.0 then channel field is displayed as POS in redemptions table", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5740_AddRewardDiscountToBasketPOSPartOne() throws Exception {
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "",
				dataSet.get("redeemable_id"), "", "");

		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().pass("Api2  send reward amount to user is successful");

		TestListeners.extentTest.get().info("== Auth API List Available Rewards ==");
		logger.info("== Auth API List Available Rewards ==");
		Response rewardResponse = pageObj.endpoints().authListAvailableRewardsNew(token, dataSet.get("client"),
				dataSet.get("secret"));
		boolean isAuthListAvailableRewardsSchemaValidated = Utilities.validateJsonArrayAgainstSchema(
				ApiResponseJsonSchema.authListAvailableRewardsSchema, rewardResponse.asString());
		Assert.assertTrue(isAuthListAvailableRewardsSchemaValidated,
				"Auth List Available Rewards Schema Validation failed");
		String rewardID = rewardResponse.jsonPath().getString("id").replace("[", "").replace("]", "");

		utils.logPass("Reward id " + rewardID + " is generated successfully ");

		Response discountBasketResponse = pageObj.endpoints()
				.authListDiscountBasketAddedForPOSAPI(dataSet.get("locationkey"), userID, "reward", rewardID);

		String actualDiscountType_InBasketAddResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_type");
		String actualDiscountID_InBasketAddResponse = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountType_InBasketAddResponse, "[reward]");
		Assert.assertEquals(actualDiscountID_InBasketAddResponse, rewardID);

		utils.logPass("Verified the discount type and ID in Basket add response");

		String expdiscount_basket_item_id = discountBasketResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");

		String discount_id = discountBasketResponse.jsonPath().getString("discount_basket_items.discount_id")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(discount_id, rewardID);

		Response basketDiscountDetailsResponse = pageObj.endpoints().getUserDiscountBasketDetailsUsingPOS(userID,
				dataSet.get("locationkey"));

		String actualDiscountBasketItemIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_basket_item_id").replace("[", "").replace("]", "");
		String actualDiscountIDFromBasket = basketDiscountDetailsResponse.jsonPath()
				.getString("discount_basket_items.discount_id").replace("[", "").replace("]", "");

		Assert.assertEquals(actualDiscountIDFromBasket, rewardID);
		Assert.assertEquals(expdiscount_basket_item_id, actualDiscountBasketItemIDFromBasket);
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionPosApiPayload(dataSet.get("locationkey"), userID, "5", "1", "101", externalUID);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		TestListeners.extentTest.get().pass("POS Process Batch Redemption Api is successful");
		logger.info("POS Process Batch Redemption Api is successful");

		// check redemption channel on user timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String redemptionChannel = pageObj.guestTimelinePage().getRedemptionChannel();
		Assert.assertEquals(redemptionChannel, "POS", "Redemption channel on the guest timeline does not match");
		logger.info(redemptionChannel + " : is the redemption channel displayed on the guest timeline");
		TestListeners.extentTest.get()
				.pass(redemptionChannel + " : is the redemption channel displayed on the guest timeline");
		String getRedemptionChannel = "select channel from redemptions where user_id = '$userid'";
		String getQC_idStringQuery = getRedemptionChannel.replace("$userid", userID);
		String getRedemptionChannelDb = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery,
				"channel");
		Assert.assertEquals(getRedemptionChannelDb, "POS", "Redemption channel in DB does not match");
		logger.info(
				"Verified that Channel-POS is getting displayed in redemptions table and on user timeline as well for POS redemption of reward");
		TestListeners.extentTest.get().pass(
				"Verified that Channel-POS is getting displayed in redemptions table and on user timeline as well for POS redemption of reward");

	}

	@Test(description = "SQ-T5740 Validate that if user is performing POS redemption using Redemption 2.0 then channel field is displayed as POS in redemptions table", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5740_subscriptionToAddInDiscountBucketPOSPartTwo() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Reward Locking", "uncheck");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable Auto-redemption", "check");

		// create subscription plan
		String spName = "OMM SubcriptionPlan";
//		String QCname = "QcSubscription_" + CreateDateTime.getTimeDateString();
//		String amountcap = Integer.toString(Utilities.getRandomNoFromRange(10, 200));
//		String unitDiscount = Integer.toString(Utilities.getRandomNoFromRange(1, 10));
		String spPrice = Integer.toString(Utilities.getRandomNoFromRange(300, 500));
//		int expSpPrice = Integer.parseInt(spPrice);
		String startDate = CreateDateTime.getYesterdayDays(2); // "2024-01-10T19:30:00Z";
		String endDate = CreateDateTime.getFutureDate(1);// "2024-01-13T04:30:00Z";

		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		// subscription purchase api 2
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token,
				dataSet.get("PlanID"), dataSet.get("client"), dataSet.get("secret"), spPrice, endDateTime);
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		int subscription_id = Integer
				.parseInt(purchaseSubscriptionresponse.jsonPath().get("subscription_id").toString());
		logger.info(userEmail + " purchased " + subscription_id + " Plan id = " + dataSet.get("PlanID"));

		// Adding subscription into discount basket

		Response addDiscountIntoBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationkey"), userID, "subscription", subscription_id + "");
		Assert.assertEquals(addDiscountIntoBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionPosApiPayload(dataSet.get("locationkey"), userID, "5", "1", "101", externalUID);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		TestListeners.extentTest.get().pass("POS Process Batch Redemption Api is successful");
		logger.info("POS Process Batch Redemption Api is successful");

		// check redemption channel on user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String redemptionChannel = pageObj.guestTimelinePage().getRedemptionChannel();
		Assert.assertEquals(redemptionChannel, "POS", "Redemption channel on the guest timeline does not match");
		logger.info(redemptionChannel + " : is the redemption channel displayed on the guest timeline");
		TestListeners.extentTest.get()
				.pass(redemptionChannel + " : is the redemption channel displayed on the guest timeline");
		String getRedemptionChannel = "select channel from redemptions where user_id = '$userid'";
		String getQC_idStringQuery = getRedemptionChannel.replace("$userid", userID);
		String getRedemptionChannelDb = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery,
				"channel");
		Assert.assertEquals(getRedemptionChannelDb, "POS", "Redemption channel in DB does not match");
		logger.info(
				"Verified that Channel-POS is getting displayed in redemptions table and on user timeline as well for POS redemption of Subscription");
		TestListeners.extentTest.get().pass(
				"Verified that Channel-POS is getting displayed in redemptions table and on user timeline as well for POS redemption of Subscription");

	}

	@Test(description = "SQ-T5740 Validate that if user is performing POS redemption using Redemption 2.0 then channel field is displayed as POS in redemptions table", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5740_addDiscountAmountToDiscountBasketPOSPartThree() throws Exception {
		String discountAmount = dataSet.get("discountAmount");
		// create user
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		String userID = signUpResponse.jsonPath().get("user.user_id").toString();

		pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), dataSet.get("discountAmount"), "", "", "");

		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAddedForPOSAPI(
				dataSet.get("locationkey"), userID, "discount_amount", discountAmount);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints()
				.processBatchRedemptionPosApiPayload(dataSet.get("locationkey"), userID, "5", "1", "101", externalUID);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		TestListeners.extentTest.get().pass("POS Process Batch Redemption Api is successful");
		logger.info("POS Process Batch Redemption Api is successful");

		// check redemption channel on user timeline
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String redemptionChannel = pageObj.guestTimelinePage().getRedemptionChannel();
		Assert.assertEquals(redemptionChannel, "POS", "Redemption channel on the guest timeline does not match");
		logger.info(redemptionChannel + " : is the redemption channel displayed on the guest timeline");
		TestListeners.extentTest.get()
				.pass(redemptionChannel + " : is the redemption channel displayed on the guest timeline");
		String getRedemptionChannel = "select channel from redemptions where user_id = '$userid'";
		String getQC_idStringQuery = getRedemptionChannel.replace("$userid", userID);
		String getRedemptionChannelDb = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery,
				"channel");
		Assert.assertEquals(getRedemptionChannelDb, "POS", "Redemption channel in DB does not match");
		logger.info(
				"Verified that Channel-POS is getting displayed in redemptions table and on user timeline as well for POS redemption of Discount Amount");
		TestListeners.extentTest.get().pass(
				"Verified that Channel-POS is getting displayed in redemptions table and on user timeline as well for POS redemption of Discount Amount");
	}

	@Test(description = "SQ-T5715 Validate Handling of Receipt Qualifiers"
			+ "SQ-T5754 (1.0) Create a QC using the API with a invalid location group ID i.e. does not belong to any business", groups = {
					"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5715_ValidateHandlingOfReceiptQualifiers() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		String businessID = dataSet.get("business_id");
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		Map<String, String> map = new HashMap<String, String>();
		map.put("receipt_qualifiers", "{\"attribute\":\"total_amount\",\"operator\":\">=\",\"value\":-10}");

		// filter_item_set == base_only, modifiers_only and base_and_modifiers
//  	// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id,
				"base_and_modifiers", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");

		// qc creation where value in receipt_qualifiers is a negative number.
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "10", dataSet.get("locationID"), map);
		;
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[]", "Error in creating QC");
		Response getQCDetailsResponse = pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName, "1", "5");
		Assert.assertEquals(getQCDetailsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Unable to fetch the list of LIS");
		String receiptQualifierValue = getQCDetailsResponse.jsonPath().getString("data[0].receipt_qualifiers[0].value");
		Assert.assertEquals(receiptQualifierValue, "-10", "receipt_qualifiers value key's value doesn't match");
		logger.info(
				"QC should get created without any error message, where value in receipt_qualifiers is a negative number.");
		TestListeners.extentTest.get().pass(
				"QC should get created without any error message, where value in receipt_qualifiers is a negative number.");

		// qc creation with invalid attributes in receipt qualifiers
		map.put("receipt_qualifiers", "{\"attribute\":\"total_ammount\",\"operator\":\">=\",\"value\":10}");
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "10", dataSet.get("locationID"), map);
		;
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]", "Error in creating QC with invalid attributes in receipt qualifiers");
		String warningMsg2 = qcCreateResponse2.jsonPath()
				.getString("results[0].warnings.receipt_qualifiers[0].message");
		Assert.assertEquals(warningMsg2, dataSet.get("invalidAttributeWarning"),
				"Warning is not generated on creating QC with invalid attributes in receipt qualifiers");
		Response getQCDetailsResponse2 = pageObj.endpoints().getQualificationListUsingAPI(adminKey, QCName2, "1", "5");
		Assert.assertEquals(getQCDetailsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Unable to fetch the list of LIS");
		String receiptQualifierValue2 = getQCDetailsResponse2.jsonPath().getString("data[0].receipt_qualifiers");
		Assert.assertEquals(receiptQualifierValue2, "[]", "receipt_qualifiers value key value doesn't match");
		logger.info(
				"Verified that the system has ignored Receipt Qualifiers with invalid attributes and only create those with valid attributes.");
		TestListeners.extentTest.get().pass(
				"Verified that the system has ignored Receipt Qualifiers with invalid attributes and only create those with valid attributes.");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName1 + " LIS is deleted successfully");
		logger.info(lisName1 + " LIS is deleted successfully");

		// Delete QC1
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");

		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		// Not required to delete from qualifying_expressions as it is not created in
		// this test case
//        boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
//                deleteQCFromQualifying_expressions);
//        Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
//        TestListeners.extentTest.get().pass(QCName + " QC is deleted successfully");
//        logger.info(QCName + " QC is deleted successfully");

		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName + " QC is deleted successfully");
		logger.info(QCName + " QC is deleted successfully ");

		// Delete QC2
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");

		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);

		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName2 + " QC is deleted successfully");
		logger.info(QCName2 + " QC is deleted successfully");

		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));

		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName2 + " QC is deleted successfully");
		logger.info(QCName2 + " QC is deleted successfully ");

	}

	@Test(description = "SQ-T5763 Verify Interoperability Settings are Retained when Updating POS Integration/Earning Tab in Cockpit", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T5763_verifyInteroperabilitySettingsRetainedOnUpdatingPosIntegrationTabInCockpit() throws Exception {
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// taking Initial count of Interoperability Exclusions before updating PPCC
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		int exclusions1 = pageObj.redemptionsPage().verifyInteroperabilityExclusions();
		logger.info("Count of Interoperability Exclusions before updating PPCC is:" + exclusions1);
		TestListeners.extentTest.get()
				.info("Count of Interoperability Exclusions before updating PPCC is:" + exclusions1);

		// updating ppcc to premium from basic
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_pos_control_center_subscription_type_premium",
				"check");
		pageObj.posIntegrationPage().clickUpdateBtn();
		utils.logPass("PPCC has been updated to Premium");

		// taking updated count of Interoperability Exclusions after updating PPCC
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		int exclusions2 = pageObj.redemptionsPage().verifyInteroperabilityExclusions();
		logger.info("Count of Interoperability Exclusions after updating PPCC is:" + exclusions2);
		TestListeners.extentTest.get()
				.info("Count of Interoperability Exclusions before updating PPCC is:" + exclusions1);
		Assert.assertEquals(exclusions2, exclusions1,
				"Count of Interoperability Exclusions after updating PPCC doesn't match");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");

		// setting back PPCC to Basic
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_pos_control_center_subscription_type_basic",
				"check");
		pageObj.posIntegrationPage().clickUpdateBtn();
		utils.logPass("PPCC has been set back to Basic");
	}

	@Test(description = "SQ-T5769 Auth Batch Redemption>Verify max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T5769_ValidateMaxApplicableQuantityReturnedForProcessingFunction() throws Exception {

		Map<String, String> map = new HashMap<String, String>();
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS1_SQ_T5769_" + CreateDateTime.getTimeDateString();
		String lisName2 = "AutomationLIS2_SQ_T5769_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_SQ_T5769_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC1_SQ_T5769_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_SQ_T5769_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_SQ_T5769_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bundle_price_target_advanced";
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");
		String baseItemClauses1 = dataSet.get("baseItemClauses1");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		String businessID = dataSet.get("businessId");

		// create redeemable having QC with processing
		// function-bundle_price_target_advanced having 5 valid line item filters and 5
		// item qualifiers
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers",
				"[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\"" + actualExternalIdLIS + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "10", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bundle_price_target_advanced");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName20 = "AutoRedeemableExistingQC20_SQ_T5769_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName20);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		Response response20 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response20.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage20 = response20.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage20, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName20 + " redeemable is created successfully");
		String redeemableExternalId = response20.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='1043'";
		String getRedeemableIdQuery1 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1,
				"id");
		utils.logPass("Lis_id for the first redeemable is: " + redeemable_ID);

		// create redeemable having QC with processing function-Target Price for Bundle
		// having 5 valid line item filters and 5 item qualifiers
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
				+ actualExternalIdLIS2 + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS2
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction2, "10", "", map);
		System.out.println("qcCreateResponse2--**** " + qcCreateResponse2.asPrettyString());
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[Processing Function is not included in the list]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC2, "QC success message is not True");
		logger.info(
				"User is unable to create QC with processing function-Bundle target price for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is unable to create QC with processing function-Bundle target price for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("qc_processing_function", "bundle_price_target");
		map.put("line_item_selector_id", actualExternalIdLIS2);
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		String redeemableName21 = "AutoRedeemableExistingQC21_SQ_T5769_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery2 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId2);
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2,
				"id");
		utils.logPass("Lis_id for the first redeemable is: " + redeemable_ID2);
		// create redeemable having QC with processing function-rate rollback having 5
		// valid line item filters and 5 item qualifiers
		Response createLISResponse3 = pageObj.endpoints().createLISUsingApi(adminKey, lisName3, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage3 = createLISResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage3, "STEP-1  Success message is not True");
		String actualExternalIdLIS3 = createLISResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
				+ actualExternalIdLIS3 + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS3
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", "", map);
		Assert.assertEquals(qcCreateResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName3 + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("qc_processing_function", "bundle_price_target");
		map.put("line_item_selector_id", actualExternalIdLIS3);
		map.put("expQCName", QCName3);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC3);
		String redeemableName22 = "AutoRedeemableExistingQC22_SQ_T5769_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName3);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId3 = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId3);
		String redeemable_ID3 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3,
				"id");
		utils.logPass("Lis_id for the first redeemable is: " + redeemable_ID3);
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		utils.logPass("User is signed up successfully");
		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
// commenting below code as the fix according to - OMM -1265
//        Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
//                "", redeemable_ID2, "", "", "");
//        Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
//                "Status code 201 did not matched for api2 send message to user");
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID3, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		utils.logPass("Send redeemable to the user successfully");
		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);
		utils.logPass("Reward id " + rewardId + " is generated successfully ");
// commenting below code as the fix according to - OMM -1265
//        String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
//                redeemable_ID2);
//        logger.info("Reward id " + rewardId2 + " is generated successfully ");
//        TestListeners.extentTest.get().pass("Reward id " + rewardId2 + " is generated successfully ");
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID3);
		logger.info("Reward id " + rewardId3 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId3 + " is generated successfully ");
		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();
		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1", "123456");
		parentMap.put("Pizza1", detailsMap1);
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// Add reward in basket
		Response discountBasketResponse = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId);
		Assert.assertEquals(discountBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");
		// commenting below code as the fix according to - OMM -1265
//        Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
//                dataSet.get("secret"), "reward", rewardId2);
//        Assert.assertEquals(discountBasketResponse2.getStatusCode(),200,
//                "Status code 200 did not match with add discount to basket ");

		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAdded(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId3);
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");
		// Auth Process Batch Redemption
		Response batchRedemptionProcessResponse2 = pageObj.endpoints().processBatchRedemptionAUTHAPI(
				dataSet.get("client"), dataSet.get("secret"), "", token, userID, "123456", externalUID);
		Assert.assertEquals(batchRedemptionProcessResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		// Verifying the response contains the max_applicable_quantity field inside the
		// discount_details object.
		String actualRedeemable1 = batchRedemptionProcessResponse2.jsonPath().get("success[0].discount_details.name")
				.toString();
		Assert.assertEquals(actualRedeemable1, redeemableName20,
				"Redeemable Name " + actualRedeemable1 + " does not match in discount basket items");
		// commenting below code as the fix according to - OMM -1265
//        String actualRedeemable2 = batchRedemptionProcessResponse2.jsonPath().get("failures[0].discount_details.name").toString();
//        Assert.assertEquals(actualRedeemable2,redeemableName21,"Redeemable Name "+actualRedeemable2+" does not match in discount basket items");
		String actualRedeemable3 = batchRedemptionProcessResponse2.jsonPath().get("failures[0].discount_details.name")
				.toString();
		Assert.assertEquals(actualRedeemable3, redeemableName22,
				"Redeemable Name " + actualRedeemable3 + " does not match in discount basket items");
		String actual_maq1 = batchRedemptionProcessResponse2.jsonPath()
				.get("success[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(actual_maq1, dataSet.get("exceptedMaq"),
				"Max applicable quantity of " + actualRedeemable1 + " does not match in discount basket items");
//       commenting below code as the fix according to - OMM -1265
//        String actual_maq2 = batchRedemptionProcessResponse2.jsonPath().get("failures[1].discount_details.max_applicable_quantity").toString();
//        Assert.assertEquals(actual_maq2,dataSet.get("exceptedMaq"),"Max applicable quantity of "+actualRedeemable2+" does not match in discount basket items");
		String actual_maq3 = batchRedemptionProcessResponse2.jsonPath()
				.get("failures[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(actual_maq3, dataSet.get("exceptedMaq"),
				"Max applicable quantity of " + actualRedeemable3 + " does not match in discount basket items");

		logger.info(
				"Verified that max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced");
		TestListeners.extentTest.get().pass(
				"Verified that max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced");
		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName1 + " LIS is deleted successfully");
		logger.info(lisName1 + " LIS is deleted successfully");
		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName2 + " LIS is deleted successfully");
		logger.info(lisName2 + " LIS is deleted successfully");
		String deleteLISQuery3 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS3)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus3 = DBUtils.executeQuery(env, deleteLISQuery3);
		Assert.assertTrue(deleteLisStatus3, lisName3 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName3 + " LIS is deleted successfully");
		logger.info(lisName3 + " LIS is deleted successfully");
		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName + " QC is deleted successfully");
		logger.info(QCName + " QC is deleted successfully");
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertFalse(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName2 + " QC is not deleted successfully, expected");
		logger.info(QCName2 + " QC is not deleted successfully, expected");
		String getQC_idStringQuery3 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB3 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery3, "id");
		String deleteQCFromQualifying_expressions3 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB3);
		boolean statusQualifying_expressionsQuery3 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions3);
		Assert.assertTrue(statusQualifying_expressionsQuery3, QCName3 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC3).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName3 + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName3 + " QC is deleted successfully");
		logger.info(QCName3 + " QC is deleted successfully");
		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		logger.info(redeemableExternalId + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId + " external  redeemable is deleted successfully");
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		logger.info(redeemableExternalId2 + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId2 + " external  redeemable is deleted successfully");
		String deleteRedeemableQuery3 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId3)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery3);
		logger.info(redeemableExternalId3 + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId3 + " external  redeemable is deleted successfully");

	}

	@Test(description = "SQ-T5774 POS Auto select>Verify max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced", groups = {
			"regression", "dailyrun" }, priority = 2)
	@Owner(name = "Vansham Mishra")
	public void T5774_ValidateMaxApplicableQuantityReturnedForProcessingFunction() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS1_SQ_T5774_" + CreateDateTime.getTimeDateString();
		String lisName2 = "AutomationLIS2_SQ_T5774_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_SQ_T5774_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC1_SQ_T5774_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_SQ_T5774_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_SQ_T5774_" + CreateDateTime.getTimeDateString();
		String processingFunction = "bundle_price_target_advanced";
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");
		String baseItemClauses1 = dataSet.get("baseItemClauses1");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);
		// Add Modifiers clauses
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		String businessID = dataSet.get("businessId");
		// create redeemable having QC with processing
		// function-bundle_price_target_advanced having 5 valid line item filters and 5
		// item qualifiers
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers",
				"[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\"" + actualExternalIdLIS + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "10", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "bundle_price_target_advanced");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName20 = "AutoRedeemableExistingQC20_SQ_T5774_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName20);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");
		Response response20 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response20.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage20 = response20.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage20, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName20 + " redeemable is created successfully");
		String redeemableExternalId = response20.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='1043'";
		String getRedeemableIdQuery1 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1,
				"id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);
		// create redeemable having QC with processing function-Target Price for Bundle
		// having 5 valid line item filters and 5 item qualifiers
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
				+ actualExternalIdLIS2 + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS2
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction2, "10", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[Processing Function is not included in the list]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertFalse(actualSuccessMessageQC2, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-bundle_price_target for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-bundle_price_target for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("qc_processing_function", "bundle_price_target");
		map.put("line_item_selector_id", actualExternalIdLIS2);
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		String redeemableName21 = "AutoRedeemableExistingQC21_SQ_T5774_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, false,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery2 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId2);
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2,
				"id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID2);

		// create redeemable having QC with processing function-rate rollback having 5
		// valid line item filters and 5 item qualifiers
		Response createLISResponse3 = pageObj.endpoints().createLISUsingApi(adminKey, lisName3, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "max_price", "");
		Assert.assertEquals(createLISResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage3 = createLISResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage3, "STEP-1  Success message is not True");
		String actualExternalIdLIS3 = createLISResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		map.put("item_qualifiers", "[{\"quantity\":1,\"processing_method\":\"\",\"line_item_selector\":\""
				+ actualExternalIdLIS3 + "\"}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS3
				+ "\",\"processing_method\":\"\",\"quantity\":\"1\"}]");
		Response qcCreateResponse3 = pageObj.endpoints().createQCUsingApi(adminKey, QCName3, "", actualExternalIdLIS3,
				"actualExternalIdLIS2", processingFunction3, "10", "", map);
		Assert.assertEquals(qcCreateResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName3 + " QC is not created and status code is not 200");
		String actualExternalIdQC3 = qcCreateResponse3.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg3 = qcCreateResponse3.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg3, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC3 = qcCreateResponse3.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC3, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		map.put("qc_processing_function", "bundle_price_target");
		map.put("line_item_selector_id", actualExternalIdLIS3);
		map.put("expQCName", QCName3);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC3);
		String redeemableName22 = "AutoRedeemableExistingQC22_SQ_T5774_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName3);
		map.put("redeemableName", redeemableName22);
		Response response22 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response22.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage22 = response22.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage22, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName22 + " redeemable is created successfully");
		String redeemableExternalId3 = response22.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery3 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId3);
		String redeemable_ID3 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery3,
				"id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID3);

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

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		// commenting below code as the fix according to - OMM -1265
//        Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
//                "", redeemable_ID2, "", "", "");
//        Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
//                "Status code 201 did not matched for api2 send message to user");
		Response sendRewardResponse3 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID3, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse3.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		utils.logPass("Send redeemable to the user successfully");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);
		utils.logPass("Reward id " + rewardId + " is generated successfully ");
		// commenting below code as the fix according to - OMM -1265
//        String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
//                redeemable_ID2);
//        logger.info("Reward id " + rewardId2 + " is generated successfully ");
//        TestListeners.extentTest.get().pass("Reward id " + rewardId2 + " is generated successfully ");
		String rewardId3 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID3);
		logger.info("Reward id " + rewardId3 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId3 + " is generated successfully ");

		Map<String, Map<String, String>> parentMap = new LinkedHashMap<String, Map<String, String>>();
		Map<String, String> detailsMap1 = new HashMap<String, String>();

		detailsMap1 = pageObj.endpoints().getRecieptDetailsMap("Pizza1", "1", "10", "M", "10", "999", "1", "123456");
		parentMap.put("Pizza1", detailsMap1);
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));

		// POS Auto Unlock
		Response autoUnlockResponse1 = pageObj.endpoints().autoSelectPosApi(userID, "30", "1", "123456", externalUID,
				dataSet.get("locationKey"));
		Assert.assertEquals(autoUnlockResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Auto Unlock ");
		TestListeners.extentTest.get().pass("POS Auto Unlock Api is successful");

		// Verifying the response contains the max_applicable_quantity field inside the
		// discount_details object.
		String actualRedeemable1 = autoUnlockResponse1.jsonPath().get("discount_basket_items[0].discount_details.name")
				.toString();
		Assert.assertEquals(actualRedeemable1, redeemableName20,
				"Redeemable Name does not match in discount basket items");
		// commenting below code as the fix according to - OMM -1265
//        String actualRedeemable2 = autoUnlockResponse1.jsonPath().get("discount_basket_items[1].discount_details.name")
//                .toString();
//        Assert.assertEquals(actualRedeemable2, redeemableName21,
//                "Redeemable Name does not match in discount basket items");
		String actualRedeemable3 = autoUnlockResponse1.jsonPath().get("discount_basket_items[1].discount_details.name")
				.toString();
		Assert.assertEquals(actualRedeemable3, redeemableName22,
				"Redeemable Name does not match in discount basket items");
		String actual_maq1 = autoUnlockResponse1.jsonPath()
				.get("discount_basket_items[0].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(actual_maq1, dataSet.get("exceptedMaq"),
				"Max applicable quantity does not match in discount basket items");
		// commenting below code as the fix according to - OMM -1265
//        String actual_maq2 = autoUnlockResponse1.jsonPath()
//                .get("discount_basket_items[1].discount_details.max_applicable_quantity").toString();
//        Assert.assertEquals(actual_maq2, dataSet.get("exceptedMaq"),
//                "Max applicable quantity does not match in discount basket items");
		String actual_maq3 = autoUnlockResponse1.jsonPath()
				.get("discount_basket_items[1].discount_details.max_applicable_quantity").toString();
		Assert.assertEquals(actual_maq3, dataSet.get("exceptedMaq"),
				"Max applicable quantity does not match in discount basket items");

		logger.info(
				"Verified that max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced on hitting POS auto select API");
		TestListeners.extentTest.get().pass(
				"Verified that max_applicable_quantity is returned for processing function-Target Price for Bundle,rate rollback and Target Price for Bundle advanced on hitting POS auto select API");

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName1 + " LIS is deleted successfully");
		logger.info(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName2 + " LIS is deleted successfully");
		logger.info(lisName2 + " LIS is deleted successfully");

		String deleteLISQuery3 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS3)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus3 = DBUtils.executeQuery(env, deleteLISQuery3);
		Assert.assertTrue(deleteLisStatus3, lisName3 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName3 + " LIS is deleted successfully");
		logger.info(lisName3 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria);
		Assert.assertTrue(statusQualification_criteriaQuery, QCName + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName + " QC is deleted successfully");
		logger.info(QCName + " QC is deleted successfully");

		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertFalse(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName2 + " QC is deleted successfully");
		logger.info(QCName2 + " QC is deleted successfully");

		String getQC_idStringQuery3 = getQC_idString.replace("$qcExternalID", actualExternalIdQC3)
				.replace("${businessID}", businessID);
		String qcID_DB3 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery3, "id");
		String deleteQCFromQualifying_expressions3 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB3);
		boolean statusQualifying_expressionsQuery3 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions3);
		Assert.assertTrue(statusQualifying_expressionsQuery3, QCName3 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC3).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName3 + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName3 + " QC is deleted successfully");
		logger.info(QCName3 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		logger.info(redeemableExternalId + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId + " external  redeemable is deleted successfully");

		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		logger.info(redeemableExternalId2 + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId2 + " external  redeemable is deleted successfully");

		String deleteRedeemableQuery3 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId3)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery3);
		logger.info(redeemableExternalId3 + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId3 + " external  redeemable is deleted successfully");
	}

	@Test(description = "SQ-T6030 Verify the POS Discount lookup API- /api/pos/discounts/lookup (POST) for Discount basket unlock duration functionality & verify sidekiq for job"
			+ "SQ-T6031 Verify the Batch Redemption API-/api/pos/batch_redemptions (POST) for Discount basket unlock duration functionality & verify sidekiq for job"
			+ "SQ-T6011 POS discount lookup API>Validate that user is unable to get the details of selected and unselected discounts of locked discount_basket_item if reward locking is ON but external_uid is not invalid/empty. "
			+ "SQ-T6012 POS Batch Redemption API>Validate that user is able to commit the locked discount_basket_item if reward locking is ON and external_uid is correct."
			+ "SQ-T6013 POS Batch Redemption API>Validate that user is unable to commit the locked discount_basket_item if reward locking is ON and external_uid is incorrect/empty.", groups = {
					"regression", "dailyrun" }, priority = 1)
	@Owner(name = "Vansham Mishra")
	public void T6030_verifyPosDiscountLookupApiForUnlockDurationAndSidekiqJob() throws Exception {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");

		pageObj.sidekiqPage().sidekiqCheck(baseUrl);
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reward Locking", "check");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Auto Unlock", "check");
        pageObj.cockpitRedemptionsPage().setAutoUnlockPeriod("present", "1");
        pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), dataSet.get("redeemableId"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Reward amount is successfully sent to the user");
		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableId"));
		logger.info("Reward id " + rewardId2 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId2 + " is generated successfully ");
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Api Adding Reward into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding Reward into discount basket is successful");
		logger.info("POS Api Adding Reward into discount basket is successful");

		Map<String, String> map = new HashMap<String, String>();
		map.put("item_qty", "5");
		Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
				userID, dataSet.get("itemId"), dataSet.get("amount2"), externalUID, map);
		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Assert value of locked key is true in posDiscountLookupResponse response
		boolean locked = posDiscountLookupResponse.jsonPath().get("locked");
		Assert.assertTrue(locked, "Locked value is not true");

		logger.info("Verified that value of locked key is true in pos discount lookup response");
		TestListeners.extentTest.get()
				.pass("Verified that value of locked key is true in pos discount lookup response");

		// Check if the selected_discounts array is present
		Assert.assertNotNull(posDiscountLookupResponse.jsonPath().get("selected_discounts"),
				"selected_discounts array is not present in the response");
		// Check if the selected_discounts array is not empty
		Assert.assertFalse(posDiscountLookupResponse.jsonPath().getList("selected_discounts").isEmpty(),
				"selected_discounts array is empty");
		logger.info("Verified that selected_discounts array is present and not empty in pos discount lookup response");
		TestListeners.extentTest.get().pass(
				"Verified that selected_discounts array is present and not empty in pos discount lookup response");
		// hit pos batch redemption api
		Response batchRedemptionProcessResponseUser2 = pageObj.endpoints().processBatchRedemptionPosApiPayload(
				dataSet.get("locationkey"), userID, dataSet.get("amount2"), "1", dataSet.get("itemId"), externalUID);
		Assert.assertEquals(batchRedemptionProcessResponseUser2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for POS API Process Batch Redemption.");
		TestListeners.extentTest.get().pass("POS API Process Batch Redemption call is successful");
		logger.info("POS API Process Batch Redemption call is successful");
		// create the query to fetch internal_tracking_code from redemptions table where
		// user_id=userID
		String query = "select internal_tracking_code from redemptions where user_id = '" + userID + "'";
		String internal_tracking_code = DBUtils.executeQueryAndGetColumnValue(env, query,
				"internal_tracking_code");
		// verify that redemption code is displayed on the guest timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean isRedemptionCodeDisplayed = pageObj.guestTimelinePage().verifyRedemptionCode(internal_tracking_code);
		Assert.assertTrue(isRedemptionCodeDisplayed,
				"Redemption code is not displayed on the guest timeline: " + internal_tracking_code);
		logger.info("Verified that redemption code is displayed on the guest timeline: " + internal_tracking_code);
		TestListeners.extentTest.get()
				.pass("Verified that redemption code is displayed on the guest timeline: " + internal_tracking_code);

		// hit the query to fetch locked at from discount_baskets table where user_id =
		// userID
		String getLockedAtQuery = "select locked_at from discount_baskets where user_id = '${userID}'";
		getLockedAtQuery = getLockedAtQuery.replace("${userID}", userID);
		String lockedAt = DBUtils.executeQueryAndGetColumnValue(env, getLockedAtQuery,
				"locked_at");
		// assert that locked_at is not null
		Assert.assertNotNull(lockedAt, "Locked at value is null");
		logger.info("Locked at value is: " + lockedAt + " Before unlocking");
		TestListeners.extentTest.get().pass("Locked at value is: " + lockedAt + " Before unlocking");

		// hit the query to fetch external_uid from discount_baskets table where
		// external_uid = externalUID
		String getExternalUidQuery = "select external_uid from discount_baskets where user_id = '${userID}'";
		getExternalUidQuery = getExternalUidQuery.replace("${externalUID}", externalUID);
		String ExternalUid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUidQuery,
				"external_uid");
		// assert that locked_at is not null
		Assert.assertNotNull(ExternalUid, "external_uid value is null");
		logger.info("external_uid value is: " + ExternalUid + " Before unlocking");
		TestListeners.extentTest.get().pass("external_uid value is: " + ExternalUid + " Before unlocking");
		// navigate to sidekiq page
		pageObj.sidekiqPage().navigateToSidekiqScheduled(baseUrl);
		// verify that DiscountBasketAutoUnlockWorker job is visible in sidekiq schedule
		// jobs
		int num = pageObj.sidekiqPage().checkSidekiqJobWithPolling(baseUrl, "DiscountBasketAutoUnlockWorker");
		Assert.assertTrue(num > 0, "DiscountBasketAutoUnlockWorker job is not visible in sidekiq schedule jobs");
		logger.info("Verified that DiscountBasketAutoUnlockWorker job is visible in sidekiq schedule jobs");
		TestListeners.extentTest.get()
				.pass("Verified that DiscountBasketAutoUnlockWorker job is visible in sidekiq schedule jobs");
		utils.longWaitInSeconds(150);
		// hit the query to fetch locked at from discount_baskets table where user_id =
		// userID
		String getLockedAtQuery2 = "select locked_at from discount_baskets where user_id = '${userID}'";
		getLockedAtQuery2 = getLockedAtQuery2.replace("${userID}", userID);
		String lockedAt2 = DBUtils.executeQueryAndGetColumnValue(env, getLockedAtQuery2, "locked_at");
		// assert that locked_at is null
		Assert.assertNull(lockedAt2, "Locked at value is not null");
		logger.info("Locked at value is: " + lockedAt2 + " After unlocking");
		TestListeners.extentTest.get().pass("Locked at value is: " + lockedAt2 + " After unlocking");

		// hit the query to fetch external_uid from discount_baskets table where
		// external_uid = externalUID
		String getExternalUidQuery2 = "select external_uid from discount_baskets where user_id = '${userID}'";
		getExternalUidQuery2 = getExternalUidQuery2.replace("${userID}", userID);
		String ExternalUid2 = DBUtils.executeQueryAndGetColumnValue(env, getExternalUidQuery2,
				"external_uid");
		// assert that locked_at is null
		Assert.assertNull(ExternalUid2, "external_uid is not null");
		logger.info("external_uid value is: " + lockedAt2 + " After unlocking");
		TestListeners.extentTest.get().pass("external_uid value is: " + lockedAt2 + " After unlocking");
	}

	@Test(description = "SQ-T6034 Verify the Meta API's for - Mobile & POS for auto_unlock_duration key", groups = {
			"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T6034verifyMetaApiForMobileAndPosAutoUnlockDurationKey() throws Exception {

		// Meta v2 api response validations
		Response cardsResponse1 = pageObj.endpoints().Api2Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardsResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 cards");
		TestListeners.extentTest.get()
				.pass("Api1 cards is successful with response code : " + cardsResponse1.statusCode());
		logger.info("Api1 cards is successful with response code : " + cardsResponse1.statusCode());
		String mobileAutoUnlockDuration = cardsResponse1.jsonPath().get("multiple_redemptions.auto_unlock_duration")
				.toString();
		Assert.assertEquals(mobileAutoUnlockDuration, dataSet.get("mobileAutoUnlockDuration"),
				"Mobile auto unlock duration is not matching");
		logger.info("Mobile auto unlock duration is: " + mobileAutoUnlockDuration);
		TestListeners.extentTest.get().pass("Mobile auto unlock duration is: " + mobileAutoUnlockDuration);

		Response response2 = pageObj.endpoints().posProgramMeta(dataSet.get("locationkey"));
		response2.then().log().all();
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for POS Program Meta API");
		String posAutoUnlockDuration = response2.jsonPath().get("multiple_redemptions.auto_unlock_duration").toString();
		Assert.assertEquals(posAutoUnlockDuration, dataSet.get("posAutoUnlockDuration"),
				"POS auto unlock duration is not matching");
		logger.info("POS auto unlock duration is: " + posAutoUnlockDuration);
		TestListeners.extentTest.get().pass("POS auto unlock duration is: " + posAutoUnlockDuration);

	}

	@Test(description = "SQ-T6002 Verify that If user is passing correct external uid in {{path}}/api/auth/discounts/unselect API, then only user is able to delete the locked basket items."
			+ "SQ-T6010 DELETE API>Validate that user is able to delete the discount_basket_item of locked discount_basket_item if reward locking is ON and external_uid is correct. "
			+ "SQ-T6014 Verify that the API endpoint /api/auth/discounts/unlock is functional and unlocks the discount basket successfully."
			+ "SQ-T6144/SQ-T6216 SELECT API>Validate that user is able to add discount basket item with incorrect external uid even if reward locking is ON", groups = {
					"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T6002verifyDiscountUnselectApiWithCorrectExternalUid() throws Exception {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");
		String userEmail2 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse2 = pageObj.endpoints().Api2SignUp(userEmail2, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse2, "API 2 user signup");
		String userID2 = signUpResponse2.jsonPath().get("user.user_id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), dataSet.get("redeemableId"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Reward amount is successfully sent to the user");
		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableId"));
		logger.info("Reward id " + rewardId2 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId2 + " is generated successfully ");
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Api Adding Reward into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Response discountBasketResponse4 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, "externalUID");
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding Reward into discount basket is successful");
		logger.info("POS Api Adding Reward into discount basket is successful");
		String discount_basket_item_id = discountBasketResponse1.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), dataSet.get("redeemableId"), "", "");
		Assert.assertEquals(sendRewardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Reward amount is successfully sent to the user");
		Response deleteBasketResponse = pageObj.endpoints().deleteDiscountBasketForUserWithExt_UidAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id, "externalUID");
		Assert.assertEquals(deleteBasketResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");
		TestListeners.extentTest.get().pass("Remove discount from basket is successful");
		logger.info("Remove discount from basket is successful");
		// again adding the reward into discount basket
		Response discountBasketResponse2 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, externalUID);
		Assert.assertEquals(discountBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket after delete");
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableId"));
		utils.logPass("Reward id " + rewardId + " is generated successfully ");
		// POS Api Adding Reward into discount basket
		Response discountBasketResponse3 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId, "");
		Assert.assertEquals(discountBasketResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get()
				.pass("POS Api Adding Reward into discount basket is successful with invalid external id");
		logger.info("POS Api Adding Reward into discount basket is successful with invalid external id");
		String discount_basket_item_id2 = discountBasketResponse3.jsonPath()
				.get("discount_basket_items[0].discount_basket_item_id").toString();
		Response deleteBasketResponse2 = pageObj.endpoints().deleteDiscountBasketForUserWithExt_UidAUTH(token,
				dataSet.get("client"), dataSet.get("secret"), discount_basket_item_id2, "externalUID");
		Assert.assertEquals(deleteBasketResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with remove discount from basket ");
		TestListeners.extentTest.get().pass(
				"Verified that the API endpoint /api/auth/discounts/unlock is functional and unlocks the discount basket successfully");
		logger.info(
				"Verified that the API endpoint /api/auth/discounts/unlock is functional and unlocks the discount basket successfully");
	}

	@Test(description = "SQ-T6033 Verify that in Locked Accounts tab, user profile is visible and unlock option is present"
			+ "SQ-T6008 SELECT API>Validate that user is able to add discount basket item with Empty External_uid or not passing external_uid even if reward locking is ON."
			+ "SQ-T6006 Verify that user is unable to lock discount_basket using external_uid and email using {{path}}/api/pos/users/find API."
			+ "SQ-T6009 GET ACTIVE API>Validate that user is able to get the details of locked discount_basket_item if reward locking is ON and external_uid is passed correctly in GET API."
			+ "SQ-T6217 GET ACTIVE API>Validate that user is unable to get the details of locked discount_basket_item if reward locking is ON but external_uid is not passed", groups = {
					"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T6033_verifyLockedAccountsTabDisplaysUserProfileWithUnlockOption() throws Exception {
		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String userID = signUpResponse.jsonPath().get("user.user_id").toString();
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("API2 Signup is successful");
		logger.info("API2 Signup is successful");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reward Locking", "check");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Multiple Redemptions");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Auto Unlock", "check");
//        pageObj.cockpitRedemptionsPage().setAutoUnlockPeriod("present", "1");
//        pageObj.cockpitRedemptionsPage().clickOnUpdateButton();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"),
				dataSet.get("amount"), dataSet.get("redeemableId"), "", "");
		Assert.assertEquals(sendRewardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED, "Reward amount is successfully sent to the user");
		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("redeemableId"));
		logger.info("Reward id " + rewardId2 + " is generated successfully ");
		TestListeners.extentTest.get().pass("Reward id " + rewardId2 + " is generated successfully ");
		String externalUID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		// POS Api Adding Reward into discount basket
		Response discountBasketResponse1 = pageObj.endpoints().authListDiscountBasketAddedForPOSAPIWithExt_Uid(
				dataSet.get("locationkey"), userID, "reward", rewardId2, "");
		Assert.assertEquals(discountBasketResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for POS add reward completion to basket");
		TestListeners.extentTest.get().pass("POS Api Adding Reward into discount basket is successful");
		logger.info("POS Api Adding Reward into discount basket is successful");
		Map<String, String> map = new HashMap<String, String>();
		map.put("item_qty", "5");
		Response userLookupResponse1 = pageObj.endpoints().userLookupPosApi("email", userEmail,
				dataSet.get("locationkey"), externalUID);
		Assert.assertEquals(userLookupResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with POS user lookUp ");
		boolean userLookupResponseLocked = userLookupResponse1.jsonPath().get("locked");
		Assert.assertFalse(userLookupResponseLocked, "User lookup response locked value is not false");
		logger.info(
				"Verified that user is unable to lock discount_basket using external_uid and email using POS user lookup API");
		TestListeners.extentTest.get().pass(
				"Verified that user is unable to lock discount_basket using external_uid and email using POS user lookup API");
		Response posDiscountLookupResponse = pageObj.endpoints().discountLookUpApiPos(dataSet.get("locationkey"),
				userID, dataSet.get("itemId"), dataSet.get("amount2"), externalUID, map);
		Assert.assertEquals(posDiscountLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		// Assert value of locked key is true in posDiscountLookupResponse response
		boolean locked = posDiscountLookupResponse.jsonPath().get("locked");
		Assert.assertTrue(locked, "Locked value is not true");

		logger.info("Verified that value of locked key is true in pos discount lookup response");
		TestListeners.extentTest.get()
				.pass("Verified that value of locked key is true in pos discount lookup response");
		Response basketDiscountDetailsResponse3 = pageObj.endpoints().fetchActiveBasketPOSAPI(userID,
				dataSet.get("locationkey"), externalUID);
		Assert.assertEquals(basketDiscountDetailsResponse3.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with fetch active basket details ");
		logger.info(
				"Verified that user is able to get the details of locked discount_basket_item if reward locking is ON and external_uid is passed correctly in GET API.");
		TestListeners.extentTest.get().pass(
				"Verified that user is able to get the details of locked discount_basket_item if reward locking is ON and external_uid is passed correctly in GET API.");
		// hit the query to fetch locked at from discount_baskets table where user_id =
		// userID
		String getLockedAtQuery = "select locked_at from discount_baskets where user_id = '${userID}'";
		getLockedAtQuery = getLockedAtQuery.replace("${userID}", userID);
		String lockedAt = DBUtils.executeQueryAndGetColumnValue(env, getLockedAtQuery,
				"locked_at");
		// assert that locked_at is not null
		Assert.assertNotNull(lockedAt, "Locked at value is null");
		logger.info("Locked at value is: " + lockedAt + " Before unlocking");
		TestListeners.extentTest.get().pass("Locked at value is: " + lockedAt + " Before unlocking");

		// hit the query to fetch external_uid from discount_baskets table where
		// external_uid = externalUID
		String getExternalUidQuery = "select external_uid from discount_baskets where user_id = '${userID}'";
		getExternalUidQuery = getExternalUidQuery.replace("${userID}", userID);
		String ExternalUid = DBUtils.executeQueryAndGetColumnValue(env, getExternalUidQuery,
				"external_uid");
		// assert that locked_at is not null
		Assert.assertNotNull(ExternalUid, "external_uid value is null");
		logger.info("external_uid value is: " + ExternalUid + " Before unlocking");
		TestListeners.extentTest.get().pass("external_uid value is: " + ExternalUid + " Before unlocking");
		pageObj.menupage().navigateToSubMenuItem("Guests", "Guests");
		pageObj.guestTimelinePage().navigateToLockedAccountTab();
		pageObj.guestTimelinePage().unlockOrCancelLockedAccount(userEmail, "ok");
		utils.longWaitInSeconds(60);
		// hit the query to fetch locked at from discount_baskets table where user_id =
		// userID
		String getLockedAtQuery2 = "select locked_at from discount_baskets where user_id = '${userID}'";
		getLockedAtQuery2 = getLockedAtQuery2.replace("${userID}", userID);
		String lockedAt2 = DBUtils.executeQueryAndGetColumnValue(env, getLockedAtQuery2,
				"locked_at");
		// assert that locked_at is null
		Assert.assertNull(lockedAt2, "Locked at value is not null");
		logger.info("Locked at value is: " + lockedAt2 + " After unlocking");
		TestListeners.extentTest.get().pass("Locked at value is: " + lockedAt2 + " After unlocking");

		// hit the query to fetch external_uid from discount_baskets table where
		// external_uid = externalUID
		String getExternalUidQuery2 = "select external_uid from discount_baskets where user_id = '${userID}'";
		getExternalUidQuery2 = getExternalUidQuery2.replace("${userID}", userID);
		String ExternalUid2 = DBUtils.executeQueryAndGetColumnValue(env, getExternalUidQuery2,
				"external_uid");
		// assert that locked_at is null
		Assert.assertNull(ExternalUid2, "external_uid is not null");
		logger.info("external_uid value is: " + lockedAt2 + " After unlocking");
		TestListeners.extentTest.get().pass("external_uid value is: " + lockedAt2 + " After unlocking");
	}

	@Test(description = "SQ-T6016 Verify that the default limit for returning deals is 10 and sorted by 'name'."
			+ "SQ-T6004 Validate that If user is passing external_uid in {{path}}/api/pos/discounts/lookup API, then only user is able to get the discount_value in selected discounts and unselected discounts."
			+ "SQ-T6007 Validate that parameter validations for unlock API if reward locking is ON"
			+ "SQ-T6017 Set the per_page value to 100 and retrieve the deals.", priority = 2, groups = { "regression",
					"dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T6016_verifyDefaultDealLimitAndSortingByName() throws Exception {
        pageObj.sidekiqPage().sidekiqCheck(baseUrl);

		// Signup using mobile api
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString(); // to be used in non auth api
		String authToken = signUpResponse.jsonPath().get("authentication_token").toString();
		String userID = signUpResponse.jsonPath().get("id").toString();

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUser(userID, dataSet.get("apiKey"), "20",
				dataSet.get("deal_id_Off"), "", "");

		logger.info("Send redeemable to the user successfully");
		TestListeners.extentTest.get().info("Send redeemable to the user successfully");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");
		TestListeners.extentTest.get().info("Api2  send reward amount to user is successful");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("deal_id_Off"));
		logger.info("Reward id " + rewardId + " is generated successfully ");
		TestListeners.extentTest.get().info("Reward id " + rewardId + " is generated successfully ");

		// Mobile Api -> List all deals
		Response listdealsResponse = pageObj.endpoints().Api2ListAllDeals(dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(listdealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 list all deals");
		logger.info("List all deals of Mobile Api is successful");
		TestListeners.extentTest.get().info("List all deals of Mobile Api is successful");

		// get count of records from redeemables table where `distributable`='1' and
		// business_id='1087'
		String getCountQuery = "select id from redeemables where distributable = '1' and business_id = '${businessId}' and deactivated_at IS NULL";
		getCountQuery = getCountQuery.replace("${businessId}", dataSet.get("business_id"));
		List<String> countId = DBUtils.getValueFromColumnInList(env, getCountQuery, "id");
		Integer count = countId.size();
		// Mobile deals -> per page
		Response listDealsResponse2 = pageObj.endpoints().Api2ListAllDeals2(dataSet.get("client"),
				dataSet.get("secret"), token, "20", "1");
		Assert.assertEquals(listDealsResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 list all deals with per page");
		// verify the count of deals in listDealsResponse2
		String count2 = listDealsResponse2.jsonPath().get("redeemables.size()").toString();
		Assert.assertEquals(Integer.parseInt(count2), count,
				"Count of deals in response is not matching with per page value");
		logger.info("Verified that the count of deals in response is matching with per page value");
		TestListeners.extentTest.get()
				.pass("Verified that the count of deals in response is matching with per page value");

		pageObj.guestTimelinePage().verifySortedRedeemableNamesFromResponse(listdealsResponse);
		logger.info("Verified that the names are sorted in ascending order for mobile list all deals");
		TestListeners.extentTest.get()
				.pass("Verified that the names are sorted in ascending order for mobile list all deals");

		// Auth Api -> list all deals
		Response listAuthDealsResponse = pageObj.endpoints().authListAllDeals(authToken, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(listAuthDealsResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 list all deals");
		pageObj.guestTimelinePage().verifySortedRedeemableNamesFromResponse(listAuthDealsResponse);
		logger.info("Verified that the names are sorted in ascending order for auth list all deals");
		TestListeners.extentTest.get()
				.pass("Verified that the names are sorted in ascending order for auth list all deals");

		// Secure Api -> list all deals
		Response listApi1DealsResponse = pageObj.endpoints().Api1ListAllDeals(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, listApi1DealsResponse.getStatusCode(),
				"Status code 200 did not matched for api1 list all deals");
		pageObj.guestTimelinePage().verifySortedRedeemableNamesFromResponse(listApi1DealsResponse);
		logger.info("Verified that the names are sorted in ascending order for api1 list all deals");
		TestListeners.extentTest.get()
				.pass("Verified that the names are sorted in ascending order for api1 list all deals");
	}

	@Test(description = "SQ-T6242 Verify that for redeemables business timezone get saved in the DB and timezone field is not visible on UI when redeemable is allowed to run indefinitely."
			+ "SQ-T6274 Verify that for Deals business timezone get saved in the DB and timezone field is not visible on UI when redeemable is allowed to run indefinitely", groups = {
					"regression", "dailyrun" }, priority = 3)
	@Owner(name = "Vansham Mishra")
	public void T6242_verifyBusinessTimezoneSavedForIndefiniteRedeemable() throws Exception {

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		String redeemableName = "AutoRedeemable" + CreateDateTime.getTimeDateString();
		pageObj.redeemablePage().createRedeemableV5redemption(redeemableName);
		boolean isPresent = pageObj.redeemablePage().isFieldPresent(dataSet.get("field1"));
		Assert.assertFalse(isPresent, "End time field is present on validity page while creating redeemable");
		logger.info(
				"End time field is not present in redeemable creation page when Allow this redeemable to run indefinitely toggle is ON");
		TestListeners.extentTest.get().pass(
				"End time field is not present in redeemable creation page when Allow this redeemable to run indefinitely toggle is ON");
		boolean isPresent2 = pageObj.redeemablePage().isFieldPresent(dataSet.get("field2"));
		Assert.assertFalse(isPresent2, "Timezone field is present on validity page while creating redeemable");
		logger.info(
				"Timezone field is not present in redeemable creation page when Allow this redeemable to run indefinitely toggle is ON");
		TestListeners.extentTest.get().pass(
				"Timezone field is not present in redeemable creation page when Allow this redeemable to run indefinitely toggle is ON");

		pageObj.redeemablePage().createRedeemableClickFinishButton();

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		String redeemableId = pageObj.redeemablePage().getRedeemableID(redeemableName);
		logger.info("Redeemable ID is: " + redeemableId);
		TestListeners.extentTest.get().pass("Redeemable ID is: " + redeemableId);
		// fetch timezone from redeemables table where redeemable_id = redeemableId and
		// business_id = businessID
		String getTimezoneQuery = "select timezone from redeemables where id = '${redeemableId}'";
		getTimezoneQuery = getTimezoneQuery.replace("${redeemableId}", redeemableId);
		pageObj.singletonDBUtilsObj();
		String redeemableTimezone = DBUtils.executeQueryAndGetColumnValue(env, getTimezoneQuery, "timezone");
		// assert redeemableTimezone is not null
		Assert.assertNotNull(redeemableTimezone, "Redeemable timezone is null");
		logger.info("Redeemable timezone is: " + redeemableTimezone);
		TestListeners.extentTest.get().pass("Redeemable timezone is: " + redeemableTimezone);

		pageObj.menupage().navigateToSubMenuItem("Settings", "Redeemables");
		pageObj.redeemablePage().deleteRedeemable(redeemableName);

	}

	@Test(description = "SQ-T6392 [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with LIF has exclude function with LIQ as line item does not exists.", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T6392_verifyDiscountCalculationForStackingReusabilityWithExcludeFunctionAndNonExistentLineItem()
			throws Exception {

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
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "exclude", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().pass("LIS has been created successfully");
		logger.info("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector\": \""
				+ actualExternalIdLIS + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "exclude", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().pass("LIS has been created successfully");
		logger.info("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"line_item_exists\",\"line_item_selector\": \""
				+ actualExternalIdLIS2 + "\",\"net_value\":1}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS2
				+ "\",\"processing_method\":\"exclude\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS,
				actualExternalIdLIS2, processingFunction, "50", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
		pageObj.qualificationcriteriapage().setItemQualifiers(0, "Line Item Does Not Exist", lisName1);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS2);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2,
				"id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID2);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);

		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");

		map.put("line_items",
				"[\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 2,\n"
						+ "            \"amount\": 14,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 113,\n" + "            \"item_family\": \"1001\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.0\"\n"
						+ "        },\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 2,\n" + "            \"amount\": 10,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 114,\n"
						+ "            \"item_family\": \"1001\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"2.0\"\n" + "        }]");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying menu items", "check");
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationkey"),
				userID, "101", "24", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		// assert discount amount and item of the qualified items
		String discountAmount1 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[0].qualified_items[1].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmount1, "-5.0", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		logger.info("Discount amount for the item is matched with -5.0");
		TestListeners.extentTest.get().pass("Discount amount for the item is matched with -5.0");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(itemId1, "114", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		logger.info("Item id for the item is matched with 114");
		TestListeners.extentTest.get().pass("Item id for the item is matched with 114");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionPosApiPayloadWithMap(
				dataSet.get("locationkey"), userID, "28", "1", "101", external_id, map);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		TestListeners.extentTest.get().pass("POS Process Batch Redemption Api is successful");
		logger.info("POS Process Batch Redemption Api is successful");

		// assert discount amount and item of the qualified items
		String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmountLookupResponse1, "-5.0", "Discount amount for the first item is not matched");
		logger.info("Discount amount for the item is matched with -5.0");
		TestListeners.extentTest.get().pass("Discount amount for the item is matched with -5.0");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse1, "114", "Item id for the first item is not matched");
		logger.info("Item id for the item is matched with 114");
		TestListeners.extentTest.get().pass("Item id for the item is matched with 114");
		// verify the two entries in redemptions table should be created for the user
		String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
		getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}", businessID);
		List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
				"redemption_code_id");
		Assert.assertEquals(redemptionIds.size(), 1,
				"Redemption code id is not created for the user in redemptions table");
		logger.info("Redemption code id is created for the user in redemptions table");
		TestListeners.extentTest.get().pass("Redemption code id is created for the user in redemptions table");
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
				.replace("${businessID}", businessID);
		String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery,
				"status");
		Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
		logger.info(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(0));
		TestListeners.extentTest.get().pass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(0));

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName1 + " LIS is deleted successfully");
		logger.info(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName2 + " LIS is deleted successfully");
		logger.info(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		String deleteQCFromQualification_criteria2 = deleteQCFromQualification_criteriaQuery
				.replace("$qcExternalID", actualExternalIdQC2).replace("${businessID}", dataSet.get("business_id"));
		boolean statusQualification_criteriaQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualification_criteria2);
		Assert.assertTrue(statusQualification_criteriaQuery2, QCName2 + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName2 + " QC is deleted successfully");
		logger.info(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		logger.info(redeemableExternalId2 + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId2 + " external  redeemable is deleted successfully");

	}

	@Test(description = "SQ-T6393 [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with LIQ as net quantity is greater than equal to 4 for offer 1 and net quantity is equal to 2 for offer 2. from the above description suggest me a methosd name", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T6393_verifyDiscountCalculationForStackingReusabilityWithNetQuantityConditions() throws Exception {

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
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "min_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().pass("LIS has been created successfully");
		logger.info("LIS has been created successfully");
		map.put("item_qualifiers",
				"[{\"expression_type\": \"net_quantity_greater_than_or_equal_to\",\"line_item_selector_id\": \""
						+ actualExternalIdLIS + "\",\"net_value\":4}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"min_price\",\"quantity\":\"2\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "50", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// login to punchh
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName20 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName20);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response20 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response20.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage20 = response20.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage20, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName20 + " redeemable is created successfully");
		String redeemableExternalId = response20.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery1 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1,
				"id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "min_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().pass("LIS has been created successfully");
		logger.info("LIS has been created successfully");
		map.put("item_qualifiers", "[{\"expression_type\": \"net_quantity_equal_to\",\"line_item_selector_id\": \""
				+ actualExternalIdLIS2 + "\",\"net_value\":2}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS2
				+ "\",\"processing_method\":\"min_price\",\"quantity\":\"2\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction, "10", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS2);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2,
				"id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID2);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying menu items", "check");

		// AUTH Add Discount to Basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");
		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");

		map.put("line_items",
				"[\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 2,\n"
						+ "            \"amount\": 10,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 101,\n" + "            \"item_family\": \"1001\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.0\"\n"
						+ "        },\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 2,\n" + "            \"amount\": 5,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 102,\n"
						+ "            \"item_family\": \"1001\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"2.0\"\n" + "        },\n" + "        { \n"
						+ "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 1,\n"
						+ "            \"amount\": 8,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 201,\n" + "            \"item_family\": \"1001\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"3.0\"\n"
						+ "        },\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 1,\n" + "            \"amount\": 6,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 202,\n"
						+ "            \"item_family\": \"1001\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"4.0\"\n" + "        }\n" + "    ]");

		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationkey"),
				userID, "101", "29", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		// assert discount amount and item of the qualified items
		String discountAmount1 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[0].qualified_items[1].amount").toString();
		String discountAmount2 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[1].qualified_items[2].amount").toString();
		String discountAmount3 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[1].qualified_items[3].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmount1, "-2.5", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		Assert.assertEquals(discountAmount2, "-0.8", "Discount amount for the second item is not matched");
		Assert.assertEquals(discountAmount3, "-0.6", "Discount amount for the second item is not matched");
		logger.info("Discount amount for the first item is matched with -4.0 and second item is matched with -2.0");
		TestListeners.extentTest.get().pass(
				"Discount amount for the first item is matched with -4.0 and second item is matched with -0.8 and -0.6");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(itemId1, "102", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[2].item_id")
				.toString();
		Assert.assertEquals(itemId2, "201", "Item id for the second item is not matched");
		String itemId3 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[3].item_id")
				.toString();
		Assert.assertEquals(itemId3, "202", "Item id for the second item is not matched");
		logger.info("Item id for the first item is matched with 102 and second item is matched with 201 and 202");
		TestListeners.extentTest.get()
				.pass("Item id for the first item is matched with 102 and second item is matched with 201 and 202");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionPosApiPayloadWithMap(
				dataSet.get("locationkey"), userID, "29", "1", "101", external_id, map);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		TestListeners.extentTest.get().pass("POS Process Batch Redemption Api is successful");
		logger.info("POS Process Batch Redemption Api is successful");

		// assert discount amount and item of the qualified items
		String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].amount").toString();
		String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[2].amount").toString();
		String discountAmountLookupResponse3 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[3].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmountLookupResponse1, "-2.5", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		Assert.assertEquals(discountAmountLookupResponse2, "-0.8",
				"Discount amount for the second item is not matched");
		Assert.assertEquals(discountAmountLookupResponse3, "-0.6",
				"Discount amount for the second item is not matched");
		logger.info(
				"Discount amount for the first item is matched with -2.5 and second item is matched with -0.8 and -0.6 in discount lookup response");
		TestListeners.extentTest.get().pass(
				"Discount amount for the first item is matched with -2.5 and second item is matched with -0.8 and -0.6 in discount lookup response");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse1, "102", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[2].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse2, "201", "Item id for the second item is not matched");
		String itemIdLookUpResponse3 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[3].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse3, "202", "Item id for the second item is not matched");
		logger.info("Item id for the first item is matched with 102 and second item is matched with 201 and 202");
		TestListeners.extentTest.get()
				.pass("Item id for the first item is matched with 102 and second item is matched with 201 and 202");
		// verify the two entries in redemptions table should be created for the user
		String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
		getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}", businessID);
		List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
				"redemption_code_id");
		Assert.assertEquals(redemptionIds.size(), 2,
				"Redemption code id is not created for the user in redemptions table");
		logger.info("Redemption code id is created for the user in redemptions table");
		TestListeners.extentTest.get().pass("Redemption code id is created for the user in redemptions table");
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
				.replace("${businessID}", businessID);
		String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery,
				"status");
		Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
		logger.info(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(0));
		TestListeners.extentTest.get().pass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(0));
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
				.replace("${businessID}", businessID);
		String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2,
				"status");
		Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
		logger.info(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(1));
		TestListeners.extentTest.get().pass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(1));

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName1 + " LIS is deleted successfully");
		logger.info(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName2 + " LIS is deleted successfully");
		logger.info(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName + " QC is deleted successfully");
		logger.info(QCName + " QC is deleted successfully");

		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName2 + " QC is deleted successfully");
		logger.info(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		logger.info(redeemableExternalId + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId + " external  redeemable is deleted successfully");

		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		logger.info(redeemableExternalId2 + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId2 + " external  redeemable is deleted successfully");

	}

	@Test(description = "SQ-T6394 [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with LIF as max price, LIQ as net amount is greater than equal to 10 for both offers.", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T6394_verifyDiscountCalculationForStackingReusabilityWithMaxPriceAndNetAmountConditions()
			throws Exception {

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
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "min_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().pass("LIS has been created successfully");
		logger.info("LIS has been created successfully");
		map.put("item_qualifiers",
				"[{\"expression_type\": \"net_amount_greater_than_or_equal_to\",\"line_item_selector_id\": \""
						+ actualExternalIdLIS + "\",\"net_value\":10}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"max_price\",\"quantity\":\"2\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "50", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// login to punchh
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName20 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName20);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response20 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response20.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage20 = response20.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage20, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName20 + " redeemable is created successfully");
		String redeemableExternalId = response20.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery1 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1,
				"id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "min_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().pass("LIS has been created successfully");
		logger.info("LIS has been created successfully");
		map.put("item_qualifiers",
				"[{\"expression_type\": \"net_amount_greater_than_or_equal_to\",\"line_item_selector_id\": \""
						+ actualExternalIdLIS2 + "\",\"net_value\":10}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS2
				+ "\",\"processing_method\":\"max_price\",\"quantity\":\"2\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction, "10", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS2);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2,
				"id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID2);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);

		// AUTH Add Discount to Basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");
		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");

		map.put("line_items",
				"[\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 3,\n"
						+ "            \"amount\": 15,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 101,\n" + "            \"item_family\": \"1001\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.0\"\n"
						+ "        },\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 3,\n" + "            \"amount\": 12,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 201,\n"
						+ "            \"item_family\": \"1001\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"2.0\"\n" + "        }]");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying menu items", "check");
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationkey"),
				userID, "101", "27", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		// assert discount amount and item of the qualified items
		String discountAmount1 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[0].qualified_items[1].amount").toString();
		String discountAmount2 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[1].qualified_items[1].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmount1, "-5.0", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		Assert.assertEquals(discountAmount2, "-0.8", "Discount amount for the second item is not matched");
		logger.info("Discount amount for the first item is matched with -5.0 and second item is matched with -0.8");
		TestListeners.extentTest.get()
				.pass("Discount amount for the first item is matched with -5 and second item is matched with -0.8");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(itemId1, "101", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(itemId2, "201", "Item id for the second item is not matched");
		logger.info("Item id for the first item is matched with 102 and second item is matched with 201");
		TestListeners.extentTest.get()
				.pass("Item id for the first item is matched with 102 and second item is matched with 201");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionPosApiPayloadWithMap(
				dataSet.get("locationkey"), userID, "29", "1", "101", external_id, map);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		TestListeners.extentTest.get().pass("POS Process Batch Redemption Api is successful");
		logger.info("POS Process Batch Redemption Api is successful");

		// assert discount amount and item of the qualified items
		String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].amount").toString();
		String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[1].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmountLookupResponse1, "-5.0", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		Assert.assertEquals(discountAmountLookupResponse2, "-0.8",
				"Discount amount for the second item is not matched");
		logger.info(
				"Discount amount for the first item is matched with -5.0 and second item is matched with -0.8 in discount lookup response");
		TestListeners.extentTest.get().pass(
				"Discount amount for the first item is matched with -2.5 and second item is matched with -0.8 in discount lookup response");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse1, "101", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[0].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse2, "201", "Item id for the second item is not matched");
		logger.info("Item id for the first item is matched with 102 and second item is matched with 201");
		TestListeners.extentTest.get()
				.pass("Item id for the first item is matched with 102 and second item is matched with 201");
		// verify the two entries in redemptions table should be created for the user
		String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
		getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}", businessID);
		List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
				"redemption_code_id");
		Assert.assertEquals(redemptionIds.size(), 2,
				"Redemption code id is not created for the user in redemptions table");
		logger.info("Redemption code id is created for the user in redemptions table");
		TestListeners.extentTest.get().pass("Redemption code id is created for the user in redemptions table");
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
				.replace("${businessID}", businessID);
		String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery,
				"status");
		Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
		logger.info(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(0));
		TestListeners.extentTest.get().pass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(0));
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
				.replace("${businessID}", businessID);
		String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2,
				"status");
		Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
		logger.info(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(1));
		TestListeners.extentTest.get().pass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(1));

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName1 + " LIS is deleted successfully");
		logger.info(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName2 + " LIS is deleted successfully");
		logger.info(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName + " QC is deleted successfully");
		logger.info(QCName + " QC is deleted successfully");

		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName2 + " QC is deleted successfully");
		logger.info(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		logger.info(redeemableExternalId + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId + " external  redeemable is deleted successfully");

		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		logger.info(redeemableExternalId2 + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId2 + " external  redeemable is deleted successfully");

	}

	@Test(description = "SQ-T6395 [Stacking ON, Reusability ON]Verify discount calculation for Offer 1 and Offer 2 with LIF as min price, LIQ as Net Amount Excluding Min Priced Item Equal To Or More Than 5 for both offers.", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Vansham Mishra")
	public void T6395_verifyDiscountCalculationForStackingReusabilityWithMinPriceAndNetAmountExcludingMinPricedItemConditions()
			throws Exception {

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
		Map<String, String> map = new HashMap<String, String>();
		String lisName2 = "AutomationLIS2_API_" + CreateDateTime.getTimeDateString();
		String lisName3 = "AutomationLIS3_API_" + CreateDateTime.getTimeDateString();
		String QCName2 = "AutomationQC2_API_" + CreateDateTime.getTimeDateString();
		String QCName3 = "AutomationQC3_API_" + CreateDateTime.getTimeDateString();
		String processingFunction2 = "bundle_price_target";
		String processingFunction3 = "rate_rollback";
		map.put("receipt_qualifiers", "[{\"attribute\":\"amount\",\"operator\":\"==\",\"value\":\"10\"}]");

		String external_id = "";
		String adminKey = dataSet.get("apiKey");
		String lisName1 = "AutomationLIS_API_" + CreateDateTime.getTimeDateString();
		String QCName = "AutomationQC_API_" + CreateDateTime.getTimeDateString();
		String processingFunction = "sum_amounts";
		String businessID = dataSet.get("business_id");
		double subtotal_amount = Double.parseDouble(dataSet.get("subtotal_amount"));
		double receipt_amount = Double.parseDouble(dataSet.get("receipt_amount"));
		// filter_item_set == base_only, modifiers_only and base_and_modifiers
		// Add base item cluases

		String baseItemClauses1 = dataSet.get("baseItemClauses");
		String baseItemClauses2 = dataSet.get("baseItemClauses2");
		String modifiersItemClauses1 = dataSet.get("modifierItemClauses");

		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses1);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		Response createLISResponse = pageObj.endpoints().createLISUsingApi(adminKey, lisName1, external_id, "base_only",
				listBaseItemClauses, listModifiresItemClauses, 2, "min_price", "");
		Assert.assertEquals(createLISResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage = createLISResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage, "STEP-1  Success message is not True");
		String actualExternalIdLIS = createLISResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().pass("LIS has been created successfully");
		logger.info("LIS has been created successfully");
		map.put("item_qualifiers",
				"[{\"expression_type\": \"net_amount_excluding_min_priced_item_equal_to_or_more_than\",\"line_item_selector_id\": \""
						+ actualExternalIdLIS + "\",\"net_value\":5}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS
				+ "\",\"processing_method\":\"min_price\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse = pageObj.endpoints().createQCUsingApi(adminKey, QCName, "", actualExternalIdLIS,
				"actualExternalIdLIS2", processingFunction, "50", "", map);
		Assert.assertEquals(qcCreateResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC = qcCreateResponse.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg = qcCreateResponse.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC = qcCreateResponse.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// login to punchh
		// login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName20 = "AutomationRedeemableExistingQC20_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName);
		map.put("redeemableName", redeemableName20);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response20 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response20.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage20 = response20.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage20, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName20 + " redeemable is created successfully");
		String redeemableExternalId = response20.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery = "select id from redeemables where uuid = '${uuid}' and business_id='" + businessID
				+ "'";
		String getRedeemableIdQuery1 = getRedeemableIdQuery.replace("${uuid}", redeemableExternalId);
		String redeemable_ID = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery1,
				"id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID);

		// send reward amount to user Reedemable
		Response sendRewardResponse = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID);

		// Step1 - If pass blank external_id, LIS will be created and system will
		// automatically generate the external_id and assigned to that LIS.
		listBaseItemClauses = pageObj.lineItemSelectorsJsonCreation().getListOfBaseItemClauses(baseItemClauses2);

		// Add Modifiers clauses// filter_item_set == base_only, modifiers_only and
		// base_and_modifiers
		listModifiresItemClauses = pageObj.lineItemSelectorsJsonCreation()
				.getListOfModifiersItemClauses(modifiersItemClauses1);
		Response createLISResponse2 = pageObj.endpoints().createLISUsingApi(adminKey, lisName2, external_id,
				"base_only", listBaseItemClauses, listModifiresItemClauses, 2, "min_price", "");
		Assert.assertEquals(createLISResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"STEP-1 Create LIS response status code is not 200");
		boolean actualSuccessMessage2 = createLISResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessage2, "STEP-1  Success message is not True");
		String actualExternalIdLIS2 = createLISResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		TestListeners.extentTest.get().pass("LIS has been created successfully");
		logger.info("LIS has been created successfully");
		map.put("item_qualifiers",
				"[{\"expression_type\": \"net_amount_excluding_min_priced_item_equal_to_or_more_than\",\"line_item_selector_id\": \""
						+ actualExternalIdLIS2 + "\",\"net_value\":5}]");
		map.put("line_item_filters", "[{\"line_item_selector_id\":\"" + actualExternalIdLIS2
				+ "\",\"processing_method\":\"min_price\",\"quantity\":\"1\"}]");
		map.put("amount_cap", "0");

		// create QC and verified error message
		Response qcCreateResponse2 = pageObj.endpoints().createQCUsingApi(adminKey, QCName2, "", actualExternalIdLIS2,
				"actualExternalIdLIS2", processingFunction, "10", "", map);
		Assert.assertEquals(qcCreateResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				QCName + " QC is not created and status code is not 200");
		String actualExternalIdQC2 = qcCreateResponse2.jsonPath().getString("results[0].external_id").replace("[", "")
				.replace("]", "");
		String errorMsg2 = qcCreateResponse2.jsonPath().getString("results[0].errors");
		Assert.assertEquals(errorMsg2, "[]",
				"Error message for qc creation with invalid line item filter id doesn't match");
		boolean actualSuccessMessageQC2 = qcCreateResponse2.jsonPath().getBoolean("results[0].success");
		Assert.assertTrue(actualSuccessMessageQC2, "QC success message is not True");
		logger.info(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");
		TestListeners.extentTest.get().pass(
				"User is able to create QC with processing function-Hit Target Menu for Maximum Price unit having 5 valid line item filters and 5 item qualifiers");

		// Navigate to dashbaord -> misc. -> reward sequence flag
		pageObj.menupage().navigateToSubMenuItem("Settings", "Qualification Criteria");
		pageObj.qualificationcriteriapage().SearchQC(QCName2);
		pageObj.dashboardpage().checkUncheckAnyFlag("Turn On Discount Stacking?", "check");
		pageObj.dashboardpage().checkUncheckAnyFlag("Enable Reuse of qualifying items?", "check");
		pageObj.qualificationcriteriapage().updateButton();

		map.put("external_id", "");
		map.put("amount_cap", "10.0");
		map.put("percentage_of_processed_amount", "1");
		map.put("qc_processing_function", "sum_amounts");
		map.put("line_item_selector_id", actualExternalIdLIS2);
		map.put("locationID", null);
		map.put("external_id_redeemable", "");
		map.put("redeemableProcessingFunction", "Sum Of Amount");
		map.put("qualifier_type", "existing");
		map.put("amount_cap", "10.0");
		map.put("expQCName", QCName2);
		map.put("lineitemSelector", dataSet.get("lineItemSelector"));
		map.put("redeeming_criterion_id", actualExternalIdQC2);
		map.put("distributable", "true");
		map.put("segment_definition_id", dataSet.get("segmentID"));
		map.put("indefinetely", "false");
		map.put("active_now", "true");
		map.put("expiry_days", null);
		// leap year start date > 30 for feb month
		String redeemableName21 = "AutomationRedeemableExistingQC21_API_" + CreateDateTime.getTimeDateString();
		map.put("name", QCName2);
		map.put("redeemableName", redeemableName21);
		map.put("start_time", "2027-12-12T00:00:00");
		map.put("end_time", "2027-12-13T23:59:59");

		// create redeemable
		Response response21 = pageObj.endpoints().createRedeemablesUsingAPI(dataSet.get("apiKey"), map);
		Assert.assertEquals(response21.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean successMessage21 = response21.jsonPath().getBoolean("results[0].success");
		Assert.assertEquals(successMessage21, true,
				"Redeeming criterion processing method is not included in the list error message is not matched");
		logger.info(redeemableName21 + " redeemable is created successfully");
		String redeemableExternalId2 = response21.jsonPath().getString("results[0].external_id");
		String getRedeemableIdQuery22 = "select id from redeemables where uuid = '${uuid}' and business_id='"
				+ businessID + "'";
		String getRedeemableIdQuery2 = getRedeemableIdQuery22.replace("${uuid}", redeemableExternalId2);
		String redeemable_ID2 = DBUtils.executeQueryAndGetColumnValue(env, getRedeemableIdQuery2,
				"id");
		logger.info("Lis_id for the first redeemable is: " + redeemable_ID2);
		// send reward amount to user Reedemable
		Response sendRewardResponse2 = pageObj.endpoints().sendMessageToUserWithEndDate(userID, dataSet.get("apiKey"),
				"", redeemable_ID2, "", "", "");
		Assert.assertEquals(ApiConstants.HTTP_STATUS_CREATED, sendRewardResponse2.getStatusCode(),
				"Status code 201 did not matched for api2 send message to user");

		// get reward id
		String rewardId2 = pageObj.redeemablesPage().getRewardId(token, dataSet.get("client"), dataSet.get("secret"),
				redeemable_ID2);

		// AUTH Add Discount to Basket
		Response discountBasketResponse2 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse2.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");
		// AUTH Add Discount to Basket
		Response discountBasketResponse3 = pageObj.endpoints().addDiscountToBasketAUTH(token, dataSet.get("client"),
				dataSet.get("secret"), "reward", rewardId2, external_id);
		Assert.assertEquals(ApiConstants.HTTP_STATUS_OK, discountBasketResponse3.getStatusCode(),
				"Status code 200 did not match with add discount to basket ");
		TestListeners.extentTest.get().pass("AUTH add discount to basket is successful");

		map.put("line_items",
				"[\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 1,\n"
						+ "            \"amount\": 2,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 101,\n" + "            \"item_family\": \"1001\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"1.0\"\n"
						+ "        },\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 1,\n" + "            \"amount\": 1,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 102,\n"
						+ "            \"item_family\": \"1001\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"2.0\"\n" + "        },\n" + "        { \n"
						+ "            \"item_name\": \"Sandwich\",\n" + "            \"item_qty\": 1,\n"
						+ "            \"amount\": 3,\n" + "            \"item_type\": \"M\",\n"
						+ "            \"item_id\": 201,\n" + "            \"item_family\": \"1001\",\n"
						+ "            \"item_group\": \"23\",\n" + "            \"serial_number\": \"3.0\"\n"
						+ "        },\n" + "        { \n" + "            \"item_name\": \"Sandwich\",\n"
						+ "            \"item_qty\": 1,\n" + "            \"amount\": 2,\n"
						+ "            \"item_type\": \"M\",\n" + "            \"item_id\": 202,\n"
						+ "            \"item_family\": \"1001\",\n" + "            \"item_group\": \"23\",\n"
						+ "            \"serial_number\": \"4.0\"\n" + "        }\n" + "    ]");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "POS Integration");
		pageObj.posIntegrationPage().checkUncheckAnyFlag("Return qualifying menu items", "check");
		// POS Discount Lookup Api
		Response discountLookupResponse0 = pageObj.endpoints().discountLookUpPosApiWithMap(dataSet.get("locationkey"),
				userID, "101", "8", external_id, map);
		Assert.assertEquals(discountLookupResponse0.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Discount Lookup Api ");

		// assert discount amount and item of the qualified items
		String discountAmount1 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[0].qualified_items[1].amount").toString();
		String discountAmount2 = discountLookupResponse0.jsonPath()
				.get("selected_discounts[1].qualified_items[1].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmount1, "-0.5", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		Assert.assertEquals(discountAmount2, "-0.2", "Discount amount for the second item is not matched");
		logger.info("Discount amount for the first item is matched with -0.5 and second item is matched with -0.2");
		TestListeners.extentTest.get()
				.pass("Discount amount for the first item is matched with -0.5 and second item is matched with -0.2");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemId1 = discountLookupResponse0.jsonPath().get("selected_discounts[0].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(itemId1, "102", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		String itemId2 = discountLookupResponse0.jsonPath().get("selected_discounts[1].qualified_items[1].item_id")
				.toString();
		Assert.assertEquals(itemId2, "202", "Item id for the second item is not matched");
		logger.info("Item id for the first item is matched with 102 and second item is matched with 202");
		TestListeners.extentTest.get()
				.pass("Item id for the first item is matched with 102 and second item is matched with 202");

		// POS Process Batch Redemption
		Response batchRedemptionProcessResponseUser1 = pageObj.endpoints().processBatchRedemptionPosApiPayloadWithMap(
				dataSet.get("locationkey"), userID, "29", "1", "101", external_id, map);
		Assert.assertEquals(batchRedemptionProcessResponseUser1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match with Process Batch Redemption ");
		TestListeners.extentTest.get().pass("POS Process Batch Redemption Api is successful");
		logger.info("POS Process Batch Redemption Api is successful");

		// assert discount amount and item of the qualified items
		String discountAmountLookupResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].amount").toString();
		String discountAmountLookupResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[1].amount").toString();
		// assert discountAmount1 should be -4
		Assert.assertEquals(discountAmountLookupResponse1, "-0.5", "Discount amount for the first item is not matched");
		// assert discountAmount2 should be -2
		Assert.assertEquals(discountAmountLookupResponse2, "-0.2",
				"Discount amount for the second item is not matched");
		logger.info(
				"Discount amount for the first item is matched with -0.5 and second item is matched with -0.2 in discount lookup response");
		TestListeners.extentTest.get().pass(
				"Discount amount for the first item is matched with -0.5 and second item is matched with -0.1 in discount lookup response");
		// assert selected_discounts[0].qualified_items[0].item_id should be 102
		String itemIdLookUpResponse1 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[0].qualified_items[1].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse1, "102", "Item id for the first item is not matched");
		// assert selected_discounts[1].qualified_items[0].item_id should be 201
		String itemIdLookUpResponse2 = batchRedemptionProcessResponseUser1.jsonPath()
				.get("success[1].qualified_items[1].item_id").toString();
		Assert.assertEquals(itemIdLookUpResponse2, "202", "Item id for the second item is not matched");
		logger.info("Item id for the first item is matched with 102 and second item is matched with 202");
		TestListeners.extentTest.get()
				.pass("Item id for the first item is matched with 102 and second item is matched with 202");
		// verify the two entries in redemptions table should be created for the user
		String getRedemptionIdQuery = "select redemption_code_id from redemptions where user_id = '${userID}' and business_id='${businessID}'";
		getRedemptionIdQuery = getRedemptionIdQuery.replace("${userID}", userID).replace("${businessID}", businessID);
		List<String> redemptionIds = DBUtils.getValueFromColumnInList(env, getRedemptionIdQuery,
				"redemption_code_id");
		Assert.assertEquals(redemptionIds.size(), 2,
				"Redemption code id is not created for the user in redemptions table");
		logger.info("Redemption code id is created for the user in redemptions table");
		TestListeners.extentTest.get().pass("Redemption code id is created for the user in redemptions table");
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery = getRedemptionCodeIdQuery.replace("${redemptionCodeId}", redemptionIds.get(0))
				.replace("${businessID}", businessID);
		String status = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery,
				"status");
		Assert.assertEquals(status, "processed", "Status is not processed in redemption_codes table");
		logger.info(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(0));
		TestListeners.extentTest.get().pass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(0));
		// verify the status should be processed in redemption_codes table by taking
		// redemption_code_id from redemtions table
		String getRedemptionCodeIdQuery2 = "select status from redemption_codes where id = '${redemptionCodeId}' and business_id='${businessID}'";
		getRedemptionCodeIdQuery2 = getRedemptionCodeIdQuery2.replace("${redemptionCodeId}", redemptionIds.get(1))
				.replace("${businessID}", businessID);
		String status2 = DBUtils.executeQueryAndGetColumnValue(env, getRedemptionCodeIdQuery2,
				"status");
		Assert.assertEquals(status2, "processed", "Status is not processed in redemption_codes table");
		logger.info(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(1));
		TestListeners.extentTest.get().pass(
				"Status is processed in redemption_codes table for the redemption code id: " + redemptionIds.get(1));

		// Delete LIS 1
		String deleteLISQuery1 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus1 = DBUtils.executeQuery(env, deleteLISQuery1);
		Assert.assertTrue(deleteLisStatus1, lisName1 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName1 + " LIS is deleted successfully");
		logger.info(lisName1 + " LIS is deleted successfully");

		String deleteLISQuery2 = lisDeleteBaseQuery.replace("${externalID}", actualExternalIdLIS2)
				.replace("${businessID}", businessID);
		boolean deleteLisStatus2 = DBUtils.executeQuery(env, deleteLISQuery2);
		Assert.assertTrue(deleteLisStatus2, lisName2 + " LIS is not deleted successfully");
		TestListeners.extentTest.get().pass(lisName2 + " LIS is deleted successfully");
		logger.info(lisName2 + " LIS is deleted successfully");

		// Delete QC
		String getQC_idStringQuery = getQC_idString.replace("$qcExternalID", actualExternalIdQC)
				.replace("${businessID}", businessID);
		String qcID_DB = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery, "id");
		String deleteQCFromQualifying_expressions = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB);
		boolean statusQualifying_expressionsQuery = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions);
		Assert.assertTrue(statusQualifying_expressionsQuery, QCName + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName + " QC is deleted successfully");
		logger.info(QCName + " QC is deleted successfully");

		String getQC_idStringQuery2 = getQC_idString.replace("$qcExternalID", actualExternalIdQC2)
				.replace("${businessID}", businessID);
		String qcID_DB2 = DBUtils.executeQueryAndGetColumnValue(env, getQC_idStringQuery2, "id");
		String deleteQCFromQualifying_expressions2 = deleteQCQueryFromQualifying_expressionsQuery.replace("$qcID",
				qcID_DB2);
		boolean statusQualifying_expressionsQuery2 = DBUtils.executeQuery(env,
				deleteQCFromQualifying_expressions2);
		Assert.assertTrue(statusQualifying_expressionsQuery2, QCName2 + " QC is not deleted successfully");
		TestListeners.extentTest.get().pass(QCName2 + " QC is deleted successfully");
		logger.info(QCName2 + " QC is deleted successfully");

		// deleting the created redeemables
		String deleteRedeemableQuery1 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery1);
		logger.info(redeemableExternalId + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId + " external  redeemable is deleted successfully");

		String deleteRedeemableQuery2 = deleteRedeemableQuery.replace("$redeemableExternalID", redeemableExternalId2)
				.replace("${businessID}", businessID);
		DBUtils.executeQuery(env, deleteRedeemableQuery2);
		logger.info(redeemableExternalId2 + " external redeemable is deleted successfully");
		TestListeners.extentTest.get().pass(redeemableExternalId2 + " external  redeemable is deleted successfully");

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}