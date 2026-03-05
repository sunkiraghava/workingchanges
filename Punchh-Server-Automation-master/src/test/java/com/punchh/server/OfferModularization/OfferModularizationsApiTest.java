package com.punchh.server.OfferModularization;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.LisCreationUtilityClasses.BaseItemClauses;
import com.punchh.server.LisCreationUtilityClasses.ModifiersItemsClauses;
import com.punchh.server.OfferModularizationUtilityClass.OrderPayloadBuilder;
import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apimodel.lineitemselector.LineItemSelectorFilterItemSet;
import com.punchh.server.pages.ApiPayloadObj;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBManager;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

/*
 * @Author : Shashank Sharma 
 * Auth Online ordering Offer Modularization Test Class with Coupon Code without QC
 */

@Listeners(TestListeners.class)
public class OfferModularizationsApiTest {

	public WebDriver driver;
	private String userEmail;
	private ApiUtils apiUtils;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> DataSet;
	public List<BaseItemClauses> listBaseItemClauses = new ArrayList();
	public List<ModifiersItemsClauses> listModifiresItemClauses = new ArrayList();
	String lisDeleteBaseQuery = "Delete from line_item_selectors where external_id = '${externalID}' and business_id='${businessID}'";
	String getQC_idString = "select id from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCFromQualification_criteriaQuery = "delete from qualification_criteria where external_id = '$qcExternalID' and business_id = '${businessID}'";
	String deleteQCQueryFromQualifying_expressionsQuery = "delete from qualifying_expressions where qualification_criterion_id ='$qcID'";
	String deleteRedeemableQuery = "delete from redeemables where uuid ='$redeemableExternalID' and business_id ='${businessID}'";
	private static Map<String, String> dataSet;
	private static List<String> codeNameList;
	private ApiPayloadObj apipayloadObj;

	// MENU_ITEM_NAME, 1, 2.86, "M", 290, "800", "152", "1.0"
	public static final String MENU_ITEM_NAME = "White rice";
	public static final int MENU_ITEM_QTY = 1;
	public static final double MENU_ITEM_AMOUNT = 2.86;
	public static final String MENU_ITEM_TYPE = "M";
	public static final int MENU_ITEM_ID = 290;
	public static final String MENU_ITEM_FAMILY = "800";
	public static final String MENU_ITEM_MAJOR_GROUP = "152";
	public static final String MENU_ITEM_SERIAL = "1.0";

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		sTCName = method.getName();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		apiUtils = new ApiUtils();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		pageObj.utils().logit(sTCName + " ==>" + dataSet);
		codeNameList = new LinkedList<String>();
		apipayloadObj = new ApiPayloadObj();
	}

	public void prepareDataSetForCouponCampaign(String campName, Map<String, String> datasetVal)
			throws ParseException, InterruptedException {
		String couponCampaignName = campName;
		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(datasetVal.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(datasetVal.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(datasetVal.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(datasetVal.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(datasetVal.get("noOfGuests"),
				datasetVal.get("usagePerGuest"), datasetVal.get("giftType"), datasetVal.get("amount"), "", "", false);

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		pageObj.utils().logPass("Coupon campaign created successfully " + couponCampaignName);

		// Thread.sleep(8000);
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();

	}

	@Test(description = "SQ-T7233 | PPT-T1 PPT OMM | Verify that Online Redemption (1.0) for Coupon Code without QC should be redemption with valid details"
			+ "SQ-T7241 PPT-T9 PPT OMM | AUTH | Verify that void redemption should work with valid details"
			+ "SQ-T7239 PPT-T7 PPT OMM | Online Redemption (1.0) for Coupon Code without QC | Verify that redeem a coupon code that has already been redeemed should give the proper message"
			+ "SQ-T7240 PPT-T8 PPT OMM | Online Redemption (1.0) for Coupon Code without QC | Verify that redeem a coupon code that has already been redeemed should get the previous_amount and category= processed"
			+ "SQ-T7242 PPT-T10 AUTH Redemption | Verify that redemption of coupon should be pass and validate the data into database", priority = 0, groups = {
					"OfferIngestionApiTest", "Regression", "nonNightly" })
	@Owner(name = "Shashank Sharma")
	public void verifyOnlineOrderRedemptionTest() throws Exception {

		String secretKey = dataSet.get("secret");
		String clientKey = dataSet.get("client");

		long transactionNumber = Long.parseLong(CreateDateTime.getTimeDateString());
		String couponCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), "", "", false);

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		pageObj.utils().logPass("Coupon campaign created successfully " + couponCampaignName);

		// Thread.sleep(8000);
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		codeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();
		int numberOfGuestExpected = Integer.parseInt(dataSet.get("noOfGuests"));
		Assert.assertEquals(codeNameList.size(), numberOfGuestExpected);
		pageObj.utils().logPass("Expected no of coupon code i.e. " + codeNameList + "is equal to actual no of coupon code i.e. "
				+ dataSet.get("noOfGuests"));

		String generatedCodeName = codeNameList.get(0).toString();// pageObj.campaignspage().getPreGeneratedCuponCode();
		pageObj.utils().logit("selected coupon code is " + generatedCodeName);

//		String generatedCodeName = "XCJ5Z41";

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("access_token").toString();

		String json = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();

		Response responseOnlinOrder = pageObj.endpoints().authOnlineOrderForOfferModularization(env, token, clientKey,
				secretKey, json);

		Assert.assertEquals(responseOnlinOrder.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");

		String acutalCampName = responseOnlinOrder.jsonPath().getString("campaign_name").replace("[", "").replace("]",
				"");
		Assert.assertEquals(acutalCampName, couponCampaignName,
				acutalCampName + " actual Coupon campaign name did not matched with expected " + couponCampaignName);

		pageObj.utils().logPass("Coupon campaign name " + acutalCampName + " matched with expected " + couponCampaignName);

		String actualCategory = responseOnlinOrder.jsonPath().getString("category").replace("[", "").replace("]", "");
		Assert.assertEquals(actualCategory, dataSet.get("expCategory"),
				actualCategory + " actual category did not matched with expected redeemable");

		pageObj.utils().logPass("Category " + actualCategory + " matched with expected redeemable");

		double actualRedemptionAmount = responseOnlinOrder.jsonPath().getDouble("redemption_amount");
		Assert.assertEquals(actualRedemptionAmount, Double.parseDouble(dataSet.get("expAmount")), actualRedemptionAmount
				+ " actual redemption amount did not matched with expected " + dataSet.get("amount"));

		pageObj.utils().logPass("Redemption amount " + actualRedemptionAmount + " matched with expected " + dataSet.get("amount"));

		String actualRedemptionCode = responseOnlinOrder.jsonPath().getString("redemption_code").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualRedemptionCode, generatedCodeName, actualRedemptionCode
				+ " redemption code is not matched with expected coupon code " + generatedCodeName);

		String actualRedemptionID = responseOnlinOrder.jsonPath().getString("redemption_id").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualRedemptionID, generatedCodeName,
				actualRedemptionID + " redemption id is not matched with expected coupon code " + generatedCodeName);

		String statusActual = responseOnlinOrder.jsonPath().getString("status").replace("[", "").replace("]", "");
		Assert.assertTrue(statusActual.contains("Please HONOR it"), " Status is not coming in response ");

		// verify data in user_coupon_redemptions table
		// get the coupon ID from coupons table
		String getCouponIDQuery = "SELECT id FROM coupons WHERE code = '" + generatedCodeName + "' AND business_id = '"
				+ dataSet.get("business_id") + "' ;";
		String rsCouponID = DBManager.executeQueryAndGetColumnValue(env, getCouponIDQuery, "id");

		// get the record from user_coupon_redemptions table
		String getUserCouponRedemptionsQuery = "SELECT count(*) as count FROM user_coupon_redemptions WHERE coupon_id = '"
				+ rsCouponID + "' AND business_id = '" + dataSet.get("business_id") + "' ;";
		String rsCount = DBManager.executeQueryAndGetColumnValue(env, getUserCouponRedemptionsQuery, "count");
		int rsCountInt = Integer.parseInt(rsCount);
		// Assert.assertEquals(rsCount, "1", "Record is not present in
		// user_coupon_redemptions table after redemption");
		Assert.assertTrue(rsCountInt >= 1, "Record is not present in user_coupon_redemptions table after redemption");

		// PPT-T7 - Verify that if Coupon is already processed with same transaction
		// number then giving proper message
		Response responseOnlinOrder1 = pageObj.endpoints().authOnlineOrderForOfferModularization(env, token, clientKey,
				secretKey, json);

		Assert.assertEquals(responseOnlinOrder1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");

		String statusActual1 = responseOnlinOrder1.jsonPath().getString("status").replace("[", "").replace("]", "");
		Assert.assertTrue(statusActual1.contains("Already processed at"), " Status is not coming in response ");
		pageObj.utils().logPass("Status message " + statusActual1 + " matched with expected Already processed at");

		double actualprevious_amount = responseOnlinOrder1.jsonPath().getDouble("previous_amount");
		Assert.assertEquals(actualprevious_amount, actualRedemptionAmount, actualprevious_amount
				+ " actual previous amount did not matched with expected " + actualRedemptionAmount);
		pageObj.utils().logPass("Previous amount " + actualprevious_amount + " matched with expected " + actualRedemptionAmount);

		String actualCategoryProcessed = responseOnlinOrder1.jsonPath().getString("category").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualCategoryProcessed, dataSet.get("expCategoryProcessed"), actualCategoryProcessed
				+ " actual category did not matched with expected " + dataSet.get("expCategoryProcessed"));
		pageObj.utils().logPass("Category " + actualCategoryProcessed + " matched with expected "
				+ dataSet.get("expCategoryProcessed"));

		// Void Redemption
		String voidJson = new OrderPayloadBuilder().startNewOrder().setClient(clientKey).excludeMenuItems()
				.setRedemptionCode(generatedCodeName).setTransactionNo(transactionNumber + "").build();

		Response voidRedemptionResponse = pageObj.endpoints().authOnlineVoidRedemptionOfferModularization(env, token,
				clientKey, secretKey, voidJson);
		Assert.assertEquals(voidRedemptionResponse.getStatusCode(), 202,
				"Status code 202 did not matched for auth online void redemption api");
		pageObj.utils().logPass("Void redemption api executed successfully with status code 202");

		// invalid use case :- verify the error message when token is invalid : -
		// invalid token and discount_type != redemption_code then user will get the
		// error message "error": "You need to sign in or sign up before continuing."
//		String invalidToken = token;
//		Response voidRedemptionResponseInvalidToken = pageObj.endpoints().authOnlineVoidRedemptionOfferModularization(invalidToken,
//				clientKey, secretKey, voidJson);
//		Assert.assertEquals(voidRedemptionResponseInvalidToken.getStatusCode(), 401,
//				"Status code 401 did not matched for auth online void redemption api with invalid token");tt

		// PPT-T14 Verify that if receipt_amount is 0.0 in payload then redemption
		// should not happen and should get the error message with QC , then check QC
		// amount
		pageObj.utils().logit(
				"************************* PPT-T14 Started without QC then check error message************************");
		String jsonReceiptAmountZero = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(0)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setStoreNumber("").setClient(clientKey).build();
		Response responseOnlinOrderReceiptAmountZero = pageObj.endpoints().authOnlineOrderForOfferModularization(env,
				token, clientKey, secretKey, jsonReceiptAmountZero);

		Assert.assertEquals(responseOnlinOrderReceiptAmountZero.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");
		verifyErrorMessage(responseOnlinOrderReceiptAmountZero, ApiConstants.HTTP_STATUS_OK, "status",
				"Redemption not possible since amount is 0.");

	}

	// https://punchhdev.atlassian.net/browse/PPT-588
	@Test(description = "SQ-T7234 | PPT-T2 PPT OMM | Online Redemption (1.0) for Coupon Code without QC | Verify that if passing the invalid redemption_code then should get the error message"
			+ "SQ-T7235 PPT-T3 PPT OMM | Online Redemption (1.0) for Coupon Code without QC | Verify that if passing the invalid redemption_code then should get the error message", groups = {
					"OfferIngestionApiTest", "Regression", "nonNightly" })
	@Owner(name = "Shashank Sharma")
	public void PPT_T2_VerifyAuthRedemptionWithInvalidDetails() throws ParseException, InterruptedException {
		String clientKey = dataSet.get("client");
		String secretKey = dataSet.get("secret");
		String generatedCodeNameInvalid = "MIPY6AB22AA";
		String invalidDiscountType = "redemption_codeInvalid";

		long transactionNumber = Long.parseLong(CreateDateTime.getTimeDateString());
		String couponCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("authentication_token").toString();

		String json = new OrderPayloadBuilder().startNewOrder().setDiscountType(invalidDiscountType)
				.setRedemptionCode(generatedCodeNameInvalid).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();

		Response responseOnlinOrder = pageObj.endpoints().authOnlineOrderForOfferModularization(env, token, clientKey,
				secretKey, json);

		Assert.assertEquals(responseOnlinOrder.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED,
				"Status code 200 did not matched for auth online order api");

		String actualErrorMessage = responseOnlinOrder.jsonPath().getString("error").replace("[", "").replace("]", "");
		Assert.assertEquals(actualErrorMessage, dataSet.get("expectedErrorMessage"), actualErrorMessage
				+ " actual error message did not matched with expected " + dataSet.get("expectedErrorMessage"));

		String json1 = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeNameInvalid).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();

		String access_token = signUpResponse.jsonPath().get("access_token").toString();

		Response responseOnlinOrder1 = pageObj.endpoints().authOnlineOrderForOfferModularization(env, access_token,
				clientKey, secretKey, json1);

		Assert.assertEquals(responseOnlinOrder1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");

//		"status": "Code MIPY6AB22 not found",
		String actualStatusMessage = responseOnlinOrder1.jsonPath().getString("status").replace("[", "").replace("]",
				"");
		String expectedStatusMessage = "Code " + generatedCodeNameInvalid + " not found";
		Assert.assertEquals(actualStatusMessage, expectedStatusMessage,
				actualStatusMessage + " actual status message did not matched with expected " + expectedStatusMessage);
		pageObj.utils().logPass("Status message " + actualStatusMessage + " matched with expected " + expectedStatusMessage);

//		  "redemption_amount": 0.0,
		double actualredemption_amount = responseOnlinOrder1.jsonPath().getDouble("redemption_amount");
		Assert.assertEquals(actualredemption_amount, 0.0,
				actualredemption_amount + " actual redemption amount did not matched with expected 0.0");
		pageObj.utils().logPass("Redemption amount " + actualredemption_amount + " matched with expected 0.0");

		// "category": "invalid",
		String actualCategory = responseOnlinOrder1.jsonPath().getString("category").replace("[", "").replace("]", "");
		Assert.assertEquals(actualCategory, "invalid",
				actualCategory + " actual category did not matched with expected invalid");
		pageObj.utils().logPass("Category " + actualCategory + " matched with expected invalid");

		// "redemption_code": null
		String actualRedemptionCode = responseOnlinOrder1.jsonPath().getString("redemption_code");
		Assert.assertNull(actualRedemptionCode, " redemption code is not null ");
		pageObj.utils().logPass("Redemption code is null as expected ");

	}

	@Test(description = "SQ-T7236 PPT-T4 PPT OMM | Online Redemption (1.0) for Coupon Code without QC | Verify structured error response for invalid requests with invalid token", groups = {
			"OfferIngestionApiTest", "Regression", "nonNightly" })
	@Owner(name = "Shashank Sharma")
	public void PPT_T4_VerifyAuthRedemptionWithInvalidDetails() throws ParseException, InterruptedException {
		String clientKey = dataSet.get("client");
		String secretKey = dataSet.get("secret");
		String generatedCodeNameInvalid = "MIPY6AB22AA";
		String invalidDiscountType = "redemption_codeInvalid";

		long transactionNumber = Long.parseLong(CreateDateTime.getTimeDateString());
		String couponCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("access_token").toString();

		// Remove discount_type field from json payload
		String json = new OrderPayloadBuilder().startNewOrder().setRedemptionCode(generatedCodeNameInvalid)
				.setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();

		Response responseOnlinOrder = pageObj.endpoints().authOnlineOrderForOfferModularization(env, token, clientKey,
				secretKey, json);

		Assert.assertEquals(responseOnlinOrder.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 200 did not matched for auth online order api");

		String actualErrorMessage = responseOnlinOrder.jsonPath().getString("error").replace("[", "").replace("]", "");
		Assert.assertEquals(actualErrorMessage, dataSet.get("expectedErrorMessage"), actualErrorMessage
				+ " actual error message did not matched with expected " + dataSet.get("expectedErrorMessage"));
		pageObj.utils().logPass("Error message " + actualErrorMessage + " matched with expected "
				+ dataSet.get("expectedErrorMessage"));

		// remove redemption_code field from json payload

		String jsonWithoutRedemptionCode = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
//				.setRedemptionCode(	generatedCodeNameInvalid)
				.setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();

		Response responseOnlinOrder1 = pageObj.endpoints().authOnlineOrderForOfferModularization(env, token, clientKey,
				secretKey, jsonWithoutRedemptionCode);

		Assert.assertEquals(responseOnlinOrder1.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST,
				"Status code 200 did not matched for auth online order api");

		String actualErrorMessage1 = responseOnlinOrder1.jsonPath().getString("error").replace("[", "").replace("]",
				"");
		Assert.assertEquals(actualErrorMessage1, dataSet.get("expectedErrorMessageForDiscountCode"), actualErrorMessage1
				+ " actual error message did not matched with expected " + dataSet.get("expectedErrorMessage"));
		pageObj.utils().logPass("Error message " + actualErrorMessage1 + " matched with expected "
				+ dataSet.get("expectedErrorMessage"));

		// remove client field from json payload
		String jsonWithoutClient = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeNameInvalid).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber)
				// .setClient(clientKey)
				.build();

		Response responseOnlinOrder2 = pageObj.endpoints().authOnlineOrderForOfferModularization(env, token, clientKey,
				secretKey, jsonWithoutClient);

		Assert.assertEquals(responseOnlinOrder2.getStatusCode(), 412,
				"Status code 200 did not matched for auth online order api");

		String actualErrorMessage2 = responseOnlinOrder2.jsonPath().getString("").replace("[", "").replace("]", "");
		Assert.assertEquals(actualErrorMessage2, dataSet.get("expectedErrorMessageForClient"), actualErrorMessage2
				+ " actual error message did not matched with expected " + dataSet.get("expectedErrorMessage"));
		pageObj.utils().logPass("Error message " + actualErrorMessage2 + " matched with expected "
				+ dataSet.get("expectedErrorMessage"));

	}

	@Test(description = "SQ-T7237 PPT-T5 PPT OMM | Online Redemption (1.0) for Coupon Code without QC | Verify that redemption with an expired coupon code should give the error message"
			+ "PPT-T6 PPT OMM | Online Redemption (1.0) for Coupon Code without QC | Verify that redemption with a deactivated coupon camp should give the error message"
			+ "SQ-T7238	PPT OMM | Online Redemption (1.0) for Coupon Code without QC |  Verify that  redemption with a deactivated coupon camp should give the error message", groups = {
					"OfferIngestionApiTest", "Regression", "nonNightly" })
	@Owner(name = "Shashank Sharma")
	public void PPT_T5_VerifyAuthRedemptionErrorMessageForExpiredCoupon() throws Exception {
		String campName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		if (codeNameList.size() == 0) {
			prepareDataSetForCouponCampaign(campName, dataSet);
		}

		// Assert.assertTrue(codeNameList.size()>1, "No pre generated coupon codes
		// available to test expired coupon code scenario");
		String clientKey = dataSet.get("client");
		String secretKey = dataSet.get("secret");
		String setCampaignActivatedQuery = "UPDATE campaigns SET deactivated_at = null where name='" + campName
				+ "' and `business_id`='" + dataSet.get("business_id") + "' ;";

		DBUtils.executeUpdateQuery(env, setCampaignActivatedQuery);

		long transactionNumber = Long.parseLong(CreateDateTime.getTimeDateString());

		String generatedCodeName = codeNameList.get(3).toString();
		String updateExpiryDateQuery = "UPDATE coupons SET expiry_date = '2024-01-10' WHERE code = '"
				+ generatedCodeName + "'  AND business_id = '" + dataSet.get("business_id") + "';";
		int rs = DBUtils.executeUpdateQuery(env, updateExpiryDateQuery);
		Assert.assertTrue(rs > 0, "Coupon expiry date is not updated to past date in DB");

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("authentication_token").toString();

		String json = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();

		Response responseOnlinOrder = pageObj.endpoints().authOnlineOrderForOfferModularization(env, null, clientKey,
				secretKey, json);

		Assert.assertEquals(responseOnlinOrder.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");

		String expErrorMessage = "This code has been expired.";

		verifyErrorMessage(responseOnlinOrder, ApiConstants.HTTP_STATUS_OK, "status", expErrorMessage);

		String updateExpiryDateQueryAsActive = "UPDATE coupons SET expiry_date = null WHERE code = '"
				+ generatedCodeName + "'  AND business_id = '" + dataSet.get("business_id") + "';";
		int rs1 = DBUtils.executeUpdateQuery(env, updateExpiryDateQueryAsActive);

		long transactionNumber1 = Long.parseLong(CreateDateTime.getTimeDateString());

		Response responseOnlinOrderActiveCode = pageObj.endpoints().authOnlineOrderForOfferModularization(env, null,
				clientKey, secretKey, json);

		Assert.assertEquals(responseOnlinOrderActiveCode.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");

		String expStatusMessage = "Please HONOR it";
		String actualStatusMessage = responseOnlinOrderActiveCode.jsonPath().getString("status").replace("[", "")
				.replace("]", "");
		Assert.assertTrue(actualStatusMessage.contains(expStatusMessage), " Status is not coming in response ");

		// For Dactivated Coupon campaign

		String couponCodeCamp = generatedCodeName;
		long transactionNumber2 = Long.parseLong(CreateDateTime.getTimeDateString());
		String setCampaignDeactivatedQuery = "UPDATE campaigns SET deactivated_at = '2024-10-09' where name='"
				+ campName + "' and `business_id`='" + dataSet.get("business_id") + "' ;";

		int rs2 = DBUtils.executeUpdateQuery(env, setCampaignDeactivatedQuery);

		String json2 = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(couponCodeCamp).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber2).setClient(clientKey).build();

		Response responseOnlinOrderDactivateCamp = pageObj.endpoints().authOnlineOrderForOfferModularization(env, null,
				clientKey, secretKey, json2);

		Assert.assertEquals(responseOnlinOrderDactivateCamp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");

		int rs4 = DBUtils.executeUpdateQuery(env, setCampaignActivatedQuery);
		Assert.assertTrue(rs4 > 0, "Coupon expiry date is not updated to past date in DB");
	}

	@Test(description = "SQ-T7243 PPT-T11 Auth Redemption | Verify the error message with invalid combination for store_number"
			+ "SQ-T7246 PPT-T14 Verify that if receipt_amount is 0.0 in payload then redemption should not happen and should get the error message with QC , then check QC amount", groups = "nonNightly")
	@Owner(name = "Shashank Sharma")
	public void PPT_T11_VerifyErrorMessageWithInvalidStoreNumberCombination() throws Exception {
		String locationName = dataSet.get("locationName");
		String clientKey = dataSet.get("client");
		String secretKey = dataSet.get("secret");
		String couponCampaignName = "Auto_CuponCampaign" + CreateDateTime.getTimeDateString();
		String lisName = "AutoLIS_PPT11_Test" + CreateDateTime.getTimeDateString();
		String qcName = "AutoQC_PPT11_Test" + CreateDateTime.getTimeDateString();
		String invalidStoreNumber = "9999";

		// Create LIS and QC
		createLisAndQC(lisName, qcName);

		long transactionNumber = Long.parseLong(CreateDateTime.getTimeDateString());

		// Login to instance
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Campaigns", "Campaigns");

		pageObj.campaignspage().selectOfferdrpValue(dataSet.get("OfferdrpValue"));
		pageObj.campaignspage().clickNewCampaignBtn();
		pageObj.signupcampaignpage().createWhatDetailsCoupanCampaign(couponCampaignName);
		pageObj.signupcampaignpage().setCouponCampaignUsageType(dataSet.get("usageType"));
		pageObj.signupcampaignpage().setCouponCampaignCodeGenerationType(dataSet.get("codeType"));
		pageObj.signupcampaignpage().setCouponCampaignGuestUsagewithGiftAmount(dataSet.get("noOfGuests"),
				dataSet.get("usagePerGuest"), dataSet.get("giftType"), dataSet.get("amount"), locationName, qcName,
				false);

		pageObj.signupcampaignpage().setStartDate();
		pageObj.signupcampaignpage().setEndDateforCouponCampaign(1);

		pageObj.signupcampaignpage().createWhenDetailsCampaign();
		boolean status = pageObj.campaignspage().validateSuccessMessage();
		Assert.assertTrue(status, "Campaign created success message did not displayed....");
		pageObj.utils().logPass("Coupon campaign created successfully " + couponCampaignName);

		// Thread.sleep(8000);
		pageObj.campaignspage().searchCampaign(couponCampaignName);
		List<String> couponCodeNameList = pageObj.campaignspage().getPreGeneratedCuponCodeList();
		String generatedCodeName = couponCodeNameList.get(0).toString();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("authentication_token").toString();

		// no store_number given in payload

		String jsonNoStoreNumber = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();

		Response responseOnlinOrder = pageObj.endpoints().authOnlineOrderForOfferModularization(env, token, clientKey,
				secretKey, jsonNoStoreNumber);

		Assert.assertEquals(responseOnlinOrder.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");

		verifyErrorMessage(responseOnlinOrder, ApiConstants.HTTP_STATUS_OK, "status", "Provided Redeemable location is not correct.");

		// invalid store_number given in payload

		String jsonInvalidStorenumber = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setStoreNumber(invalidStoreNumber).setClient(clientKey).build();

		Response responseOnlinOrderInvalidStoreNum = pageObj.endpoints().authOnlineOrderForOfferModularization(env,
				token, clientKey, secretKey, jsonInvalidStorenumber);

		Assert.assertEquals(responseOnlinOrderInvalidStoreNum.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");

		verifyErrorMessage(responseOnlinOrderInvalidStoreNum, ApiConstants.HTTP_STATUS_OK, "status",
				"Provided Redeemable location is not correct.");

		// invalid store_number as blank given in payload

		String jsonInvalidStoreBlank = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setStoreNumber("").setClient(clientKey).build();

		Response responseOnlinOrderInvalidStoreBlank = pageObj.endpoints().authOnlineOrderForOfferModularization(env,
				token, clientKey, secretKey, jsonInvalidStoreBlank);

		Assert.assertEquals(responseOnlinOrderInvalidStoreBlank.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");

		verifyErrorMessage(responseOnlinOrderInvalidStoreNum, ApiConstants.HTTP_STATUS_OK, "status",
				"Provided Redeemable location is not correct.");

		// PPT-T14 Verify that if receipt_amount is 0.0 in payload then redemption
		// should not happen and should get the error message with QC , then check QC
		// amount

		pageObj.utils().logit(
				"************************* PPT-T14 Started without QC then check error message************************");
		String jsonReceiptAmountZero = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(0)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, 0.0, MENU_ITEM_TYPE, MENU_ITEM_ID, MENU_ITEM_FAMILY,
						MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setStoreNumber(dataSet.get("validStoreNumber"))
				.setClient(clientKey).build();
		Response responseOnlinOrderReceiptAmountZero = pageObj.endpoints().authOnlineOrderForOfferModularization(env,
				token, clientKey, secretKey, jsonReceiptAmountZero);

		Assert.assertEquals(responseOnlinOrderReceiptAmountZero.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for auth online order api");
		verifyErrorMessage(responseOnlinOrderReceiptAmountZero, ApiConstants.HTTP_STATUS_OK, "status",
				"Redemption not possible since amount is 0.");

	}// end of PPT_T11_VerifyErrorMessageWithInvalidStoreNumberCombination method

	@Test(description = "SQ-T7244 PPT-T12 Auth Redemption | Verify the error message with invalid combination for discount_type"
			+ "SQ-T7245 PPT-T13 Auth Redemption | Verify the error message with invalid combination for redemption_code"
			+ "SQ-T7247 PPT-T15 Auth Redemption | Verify the error message with invalid combination for client id", groups = "nonNightly")
	@Owner(name = "Shashank Sharma")
	public void PPT_T12_VerifyErrorMessageWithInvalidDiscountType() throws Exception {
		String clientKey = dataSet.get("client");
		String secretKey = dataSet.get("secret");
		long transactionNumber = Long.parseLong(CreateDateTime.getTimeDateString());
		String generatedCodeName = "INVALIDCODE";
		String invalidDiscountType = "invalid_discount_type";

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Status code 201 did not matched for auth user signup api");
		String token = signUpResponse.jsonPath().get("access_token").toString();

		// invalid discount_type given in payload

		String jsonInvalidDiscountType = new OrderPayloadBuilder().startNewOrder().setDiscountType(invalidDiscountType)
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();

		Response responseOnlinOrder = pageObj.endpoints().authOnlineOrderForOfferModularization(env, token, clientKey,
				secretKey, jsonInvalidDiscountType);

		verifyErrorMessage(responseOnlinOrder, ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "",
				"Discount type should be card_completion, discount_amount, redemption_code, reward, fuel_reward, subscription");

		// discount_type as blank given in payload
		String jsonBlankDiscountType = new OrderPayloadBuilder().startNewOrder().setDiscountType("")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();
		Response responseOnlinOrderBlankDiscountType = pageObj.endpoints().authOnlineOrderForOfferModularization(env,
				token, clientKey, secretKey, jsonBlankDiscountType);
		verifyErrorMessage(responseOnlinOrderBlankDiscountType, ApiConstants.HTTP_STATUS_BAD_REQUEST, "error",
				"Required parameter missing or the value is empty: discount_type");

		// discount_type fileld removed from payload
		String jsonWithoutDiscountType = new OrderPayloadBuilder().startNewOrder().setRedemptionCode(generatedCodeName)
				.setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();
		Response responseOnlinOrderWithoutDiscountType = pageObj.endpoints().authOnlineOrderForOfferModularization(env,
				token, clientKey, secretKey, jsonWithoutDiscountType);
		verifyErrorMessage(responseOnlinOrderWithoutDiscountType, ApiConstants.HTTP_STATUS_BAD_REQUEST, "error",
				"Required parameter missing or the value is empty: discount_type");

		// invalid redemption_code given in payload
		// PPT-T13 Auth Redemption | Verify the error message with invalid combination
		// for redemption_code

		String jsonInvalidRedemptionCode = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();

		Response responseOnlinOrderInvalidCouponCode = pageObj.endpoints().authOnlineOrderForOfferModularization(env,
				token, clientKey, secretKey, jsonInvalidRedemptionCode);

		verifyErrorMessage(responseOnlinOrderInvalidCouponCode, ApiConstants.HTTP_STATUS_OK, "status",
				"Code " + generatedCodeName + " not found");

		// redemption_code as blank given in payload
		String jsonBlankRedemptionCode = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode("").setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();
		Response responseOnlinOrderBlankRedemptionCode = pageObj.endpoints().authOnlineOrderForOfferModularization(env,
				token, clientKey, secretKey, jsonBlankRedemptionCode);
		verifyErrorMessage(responseOnlinOrderBlankRedemptionCode, ApiConstants.HTTP_STATUS_BAD_REQUEST, "error",
				"Required parameter missing or the value is empty: redemption_code");

		// redemption_code field removed from payload
		String jsonWithoutRedemptionCode = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient(clientKey).build();
		Response responseOnlinOrderWithoutRedemptionCode = pageObj.endpoints()
				.authOnlineOrderForOfferModularization(env, token, clientKey, secretKey, jsonWithoutRedemptionCode);
		verifyErrorMessage(responseOnlinOrderWithoutRedemptionCode, ApiConstants.HTTP_STATUS_BAD_REQUEST, "error",
				"Required parameter missing or the value is empty: redemption_code");

		// invalid client id combination
		// PPT-T15 Auth Redemption | Verify the error message with invalid combination
		// for client id

		String jsonInvalidClientID = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient("INVALIDCLIENTID").build();

		Response responseOnlinOrderInvalidClientID = pageObj.endpoints().authOnlineOrderForOfferModularization(env,
				token, clientKey, secretKey, jsonInvalidClientID);
		verifyErrorMessage(responseOnlinOrderInvalidClientID, 412, "", "Invalid Signature");

		// client id is blank
		String jsonBlankClientID = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).setClient("").build();

		Response responseOnlinOrderBlankClientID = pageObj.endpoints().authOnlineOrderForOfferModularization(env, token,
				clientKey, secretKey, jsonBlankClientID);
		verifyErrorMessage(responseOnlinOrderBlankClientID, 412, "", "Invalid Signature");

		// client id field removed from payload
		String jsonWithoutClientID = new OrderPayloadBuilder().startNewOrder().setDiscountType("redemption_code")
				.setRedemptionCode(generatedCodeName).setReceiptAmount(10)
				.addMenuItem(MENU_ITEM_NAME, MENU_ITEM_QTY, MENU_ITEM_AMOUNT, MENU_ITEM_TYPE, MENU_ITEM_ID,
						MENU_ITEM_FAMILY, MENU_ITEM_MAJOR_GROUP, MENU_ITEM_SERIAL)
				.setSubtotalAmount(1).setReceiptDatetime("2015-04-03T18:05:01+05:30")
				.setTransactionNo(transactionNumber).build();

		Response responseOnlinOrderWithoutClientID = pageObj.endpoints().authOnlineOrderForOfferModularization(env,
				token, clientKey, secretKey, jsonWithoutClientID);
		verifyErrorMessage(responseOnlinOrderWithoutClientID, 412, "", "Invalid Signature");

	}// end of PPT_T12_VerifyErrorMessageWithInvalidDiscountType method

	public void verifyErrorMessage(Response response, int expStatus, String keyPath, String expectedResult) {
		Assert.assertEquals(response.getStatusCode(), expStatus,
				"Status code 200 did not matched for auth online order api");

		String actualErrorMessage2 = response.jsonPath().getString(keyPath).replace("[", "").replace("]", "");

		Assert.assertEquals(actualErrorMessage2, expectedResult,
				actualErrorMessage2 + " actual error message did not matched with expected " + expectedResult);
		pageObj.utils().logPass("Error message " + actualErrorMessage2 + " matched with expected " + expectedResult);

	}

	public void createLisAndQC(String lisName, String qcname) throws Exception {
		// Create LIS with base item 101,102,103,104
		String lisPayload = apipayloadObj.lineItemSelectorBuilder().setName(lisName)
				.setFilterItemSet(LineItemSelectorFilterItemSet.BASE_ONLY)
				.addBaseItemClause("item_id", "in", MENU_ITEM_ID + "").build();
		// Create LIS
		Response lisResponse = pageObj.endpoints().createLIS(lisPayload, dataSet.get("apiKey"));
		pageObj.utils().logit("API response: " + lisResponse.prettyPrint());

		// Get LIS External ID
		String lisExternalID = lisResponse.jsonPath().getString("results[0].external_id");
		pageObj.utils().logPass(lisName + " LIS is Created with External ID: " + lisExternalID);

		// Create QC-payload with 100% off on total amount of items matching LIS
		String qcPayload = apipayloadObj.qualificationCriteriaBuilder().setName(qcname)
				.setQCProcessingFunction("sum_amounts").addLineItemFilter(lisExternalID, "", 0)
				.addItemQualifier("line_item_exists", lisExternalID, 0.0, 0).build();

		// Create QC
		Response qcResponse = pageObj.endpoints().createQC(qcPayload, dataSet.get("apiKey"));
		pageObj.utils().logit("API response: " + qcResponse.prettyPrint());

	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		pageObj.utils().logit("Browser closed");
	}
}