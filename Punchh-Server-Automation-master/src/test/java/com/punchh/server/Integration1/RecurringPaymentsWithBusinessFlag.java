package com.punchh.server.Integration1;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.mongodb.client.model.Sorts;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
//import com.punchh.server.utilities.TestData;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class RecurringPaymentsWithBusinessFlag {
	static Logger logger = LogManager.getLogger(RecurringPaymentsWithBusinessFlag.class);
	public WebDriver driver;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String timeStamp;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private String userEmail;
	private String mockPaymenToken = "30920042429541310767000000246338";
	private String mockParPayURL = "https://c18f05d3-17bb-49e6-a0b8-c4119887e683.mock.pstmn.io";

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		utils = pageObj.utils();
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
		// Instance move to all business page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

	@Test(description = "SQ_T5514 | HL-T1117-T1198 - Recurring Payments With Enable Saved Payment flag in the business ", priority = 0)

	public void T5514_VerifyRecurringPaymentWithBusinessAndServiceFlag() throws Exception {

		// Select test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Gift Card page and select the gift card adapter as "Givex
		// (without PIN verify)"
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Givex (without PIN verify)");
		pageObj.giftcardsPage().setGiftcardSeriesLength("603628", "21");
		pageObj.giftcardsPage().enableGiftCardAutoReloadFlag(true);
		pageObj.giftcardsPage().clickOnUpdateButton();

		// Navigate to Payment page and select the payment adapter as "PAR Payments"
		pageObj.giftcardsPage().selectPaymentAdapter("PAR Payments");

		// Navigate to Subscriptions page and set the payment adapter as "PAR Payment"
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("PAR Payment");
		pageObj.dashboardpage().clickOnUpdateButton();

		// Navigate to Integration Services, set original Par Payment URL and enable the
		// recurring payment flag
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.giftcardsPage().parPaymentCredential(true, mockParPayURL);

		// Ensure that enable_recurring_payment is set to True in the business
		// preferences. (
		String bizPrefGetQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, bizPrefGetQuery, "preferences");
		String flag1 = DBUtils.businessesPreferenceFlag(expColValue1, "enable_recurring_payment");
		Assert.assertEquals(flag1, "true", "enable_recurring_payment flag is False");
		logger.info("enable_recurring_payment flag is True in the business preferences");
		pageObj.utils().logPass("enable_recurring_payment flag is True in the business preferences");

		// navigate to Cockpit -> Dashboard
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Saved Payment Optimisation Flag?", "uncheck");
		utils.getLocator("dashboardPage.updateBtn").click();

		// Create the user and get the user token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		String userToken = signUpResponse.jsonPath().get("auth_token.token");
		String userId = signUpResponse.jsonPath().getString("id");
		pageObj.utils().logPass(userEmail + " Api1 user signup is successful");

		// === Payment via Card, SSF Token, and Gift Card when both
		// vault_flag_optimisation and business_enable_recurring_payment_flag are True
		// ===
		// Generating the Saved Payment Card
		Response paymentCardResponse = pageObj.endpoints().createPaymentCard(dataSet.get("client"),
				dataSet.get("secret"), userToken, "par_payment", mockPaymenToken, "Rohit", true);
		String paycardUUID = paymentCardResponse.jsonPath().getString("uuid");
		Assert.assertEquals(paymentCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed POS payment api");
		String uuidPaymentCard = paymentCardResponse.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		logger.info("Payment card has created successfully with uuid: " + uuidPaymentCard);
		pageObj.utils().logPass("Payment card has created successfully with uuid: " + uuidPaymentCard);

		// Purchase Gift card with saved payment card
		Response giftCardPurcahseResp = pageObj.endpoints().Api1PurchaseGiftCardWithRecurring(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("non_wallet_gc_design_id"), "15", userToken, paycardUUID);
		Assert.assertEquals(giftCardPurcahseResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to purchase the gift card");
		String sourceGCUUID = giftCardPurcahseResp.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		int gcPurAmt = (int) Double.parseDouble(
				giftCardPurcahseResp.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(gcPurAmt, 15, "GC purchase amount is not matched with the expected amount");
		logger.info("Gift card has purchased successfully with uuid: " + sourceGCUUID);
		pageObj.utils().logPass("Gift card has purchased successfully with uuid: " + sourceGCUUID);

		// Autoreload Giftcard Config Enable with saved payment card
		Response giftCardUpdateResp = pageObj.endpoints().api1UpdateGiftCardwithAutoreloadConfig(dataSet.get("client"),
				dataSet.get("secret"), userToken, sourceGCUUID, true, true, dataSet.get("userGcThresholdAmt"),
				dataSet.get("userGcDefaultAmt"), paycardUUID);
		Assert.assertEquals(giftCardUpdateResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to add gift card autoreload config");
		boolean actAutorelodStatus = giftCardUpdateResp.jsonPath().getBoolean("auto_reload_enabled");
		Assert.assertTrue(actAutorelodStatus, "Autoreload is not enable on wallet GC");
		logger.info("Autoreload is enabled on giftcard with uuid: " + sourceGCUUID);
		pageObj.utils().logit("Autoreload is enabled on giftcard with uuid: " + sourceGCUUID);

		// SSF Token Generation Using a Saved Payment Card
		Response singleScanTokenResp = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), userToken, "recurring", "transaction_token", paycardUUID);
		Assert.assertEquals(singleScanTokenResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for the Single Scan Code API.");
		String singleScanCode = singleScanTokenResp.jsonPath().getString("single_scan_code");
		logger.info("Newly generated SSF code is " + singleScanCode);
		pageObj.utils().logPass("Newly generated SSF code is " + singleScanCode);

		// Location-based payment at a payment-configured location when the 'Allow
		// Business Config' flag is set to False.
		Response posPaymentResp = pageObj.endpoints().POSPayment("singleScan", userEmail, singleScanCode, "recurring",
				dataSet.get("payment_location_key"));
		Assert.assertEquals(posPaymentResp.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String payment_reference_id = posPaymentResp.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");
		logger.info("Payment has been done successfully with payment reference id : " + payment_reference_id);
		pageObj.utils().logPass("Payment has been done successfully with payment reference id : " + payment_reference_id);

		// Subscription purchase using a saved payment card
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(userToken,
				dataSet.get("subscription_plan_id"), dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("subscription_amount"), paycardUUID, "Heartland");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id = purchaseSubscriptionresponse.jsonPath().getString("subscription_id");

		// Get the subscription details from DB and validate
		String[] db_columns = { "type", "paid_for_type", "source" };
		String sQuery = "SELECT type, paid_for_type, payment_details, source " + "FROM payments "
				+ "WHERE payments.paid_for_id = '" + subscription_id + "' " + "AND payments.user_id = '" + userId + "' "
				+ "ORDER BY id ASC LIMIT 1;";
		List<Map<String, String>> coldata = DBUtils.executeQueryAndGetMultipleColumns(env, sQuery, db_columns);
		Assert.assertEquals(coldata.get(0).get("type"), "Payment",
				"Subscription Payment Type is not matched with Payment");
		Assert.assertEquals(coldata.get(0).get("paid_for_type"), "UserSubscription",
				"Subscription Paid for type is not matched with UserSubscription");
		Assert.assertEquals(coldata.get(0).get("source"), "recurring",
				"Subscription Source is not matched with recurring");
		logger.info("Subscription has purchased successfully with payment reference id: " + subscription_id);
		pageObj.utils().logPass("Subscription has purchased successfully with payment reference id: " + subscription_id);

		// === Payment via Card, SSF Token, and Gift Card when both
		// vault_flag_optimisation and enable_recurring_payment flags are True in the
		// business settings ====
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Saved Payment Optimisation Flag?", "check");
		utils.getLocator("dashboardPage.updateBtn").click();

		// Validate the payment cards when vault_flag_optimisation &
		// enable_recurring_payment are true in business
		Response paymentCardResp1 = pageObj.endpoints().api2FetchPaymentCard(userToken, "par_payment",
				dataSet.get("client"), dataSet.get("secret"));
		String uuid = paymentCardResp1.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		Assert.assertEquals(uuid, paycardUUID, "Payment Card is not present in UI");
		logger.info("Payment Card is present in api response with uuid: " + uuid);

		// Reload Gift card with saved payment card when vault_flag_optimisation &
		// enable_recurring_payment are true in business
		Response reloadGiftCardResp = pageObj.endpoints().Api1ReloadGiftCardRecurring(userEmail, dataSet.get("client"),
				dataSet.get("secret"), "15", userToken, sourceGCUUID, uuidPaymentCard);
		Assert.assertEquals(reloadGiftCardResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to reload gift card");
		int gcreloadAmt = (int) Double.parseDouble(
				reloadGiftCardResp.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(gcreloadAmt, 30, "Amount is not matched with the expected amount");
		pageObj.utils().logPass("Gift Card reloaded successfully with saved payment card with amount: " + gcreloadAmt);

		// Autoreload Giftcard Config Enable with saved payment card
		Response giftCardUpdateResp1 = pageObj.endpoints().api1UpdateGiftCardwithAutoreloadConfig(dataSet.get("client"),
				dataSet.get("secret"), userToken, sourceGCUUID, true, true, dataSet.get("userGcThresholdAmt"),
				dataSet.get("userGcDefaultAmt"), paycardUUID);
		Assert.assertEquals(giftCardUpdateResp1.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to add gift card autoreload config");
		boolean actAutorelodStatus1 = giftCardUpdateResp1.jsonPath().getBoolean("auto_reload_enabled");
		Assert.assertTrue(actAutorelodStatus1, "Autoreload is not enable on wallet GC");
		logger.info("Autoreload is enabled on giftcard with uuid: " + sourceGCUUID);
		pageObj.utils().logit("Autoreload is enabled on giftcard with uuid: " + sourceGCUUID);

		// SSF Token Generation Using a Saved Payment Card
		Response singleScanTokenResp1 = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), userToken, "recurring", "transaction_token", paycardUUID);
		Assert.assertEquals(singleScanTokenResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for the Single Scan Code API.");
		String singleScanCode1 = singleScanTokenResp1.jsonPath().getString("single_scan_code");
		logger.info("Newly generated SSF code is " + singleScanCode1);
		pageObj.utils().logPass("Newly generated SSF code is " + singleScanCode1);

		// POS Payment with SSF Token
		Response posPaymentResp1 = pageObj.endpoints().POSPayment("singleScan", userEmail, singleScanCode, "recurring",
				dataSet.get("payment_location_key"));
		Assert.assertEquals(posPaymentResp.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String payment_reference_id1 = posPaymentResp1.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");
		logger.info("Payment has been done successfully with payment reference id : " + payment_reference_id1);
		pageObj.utils().logPass("Payment has been done successfully with payment reference id : " + payment_reference_id1);

		// Subscription purchase using a saved payment card when
		// enable_recurring_payment is False in the business settings.
		Response purchaseSubscriptionresponse1 = pageObj.endpoints().Api2SubscriptionPurchase(userToken,
				dataSet.get("subscription_plan_id"), dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("subscription_amount"), paycardUUID, "Heartland");
		Assert.assertEquals(purchaseSubscriptionresponse1.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		String subscription_id1 = purchaseSubscriptionresponse1.jsonPath().getString("subscription_id");
		logger.info("Subscription has purchased successfully with payment reference id: " + subscription_id1);
		pageObj.utils().logit("Subscription has purchased successfully with payment reference id: " + subscription_id1);

		// Set the value of vault_flag_optimisation to true &
		// business_enable_recurring_payment_flag to false
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Saved Payment Optimisation Flag?", "check");
		utils.getLocator("dashboardPage.updateBtn").click();

		// ============== Payment via Card, SSF Token, and Gift Card when
		// vault_flag_optimisation is True, but enable_recurring_payment_flag is False
		// in the business settings ==============
		// Set value of enable_returning_recurring_payment_token (business) to false
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, bizPrefGetQuery, "preferences");
		DBUtils.updateBusinessesPreference(env, expColValue, "false", "enable_recurring_payment",
				dataSet.get("business_id"));

		// Validate the payment cards when enable_recurring_payment is False in the
		// business settings
		Response paymentCardResp2 = pageObj.endpoints().api2FetchPaymentCard(userToken, "par_payment",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(paymentCardResp2.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Payment Card is present in response");
		String actErrMsg = paymentCardResp2.jsonPath().getString("errors").replace("[", "").replace("]", "");
		Assert.assertEquals(actErrMsg, "Saved Payment hasn't been enabled for this payment adapter yet.",
				"Error message is not matched for the Payment Card");
		logger.info(
				"Payment Card is not present in the api response when enable_recurring_payment is false in business");
		pageObj.utils().logPass(
				"Payment Card is not present in the api response when enable_recurring_payment is false in business");

		// Reload a gift card using a saved payment card when vault_flag_optimisation is
		// True, but enable_recurring_payment is False in the business settings
		Response reloadGiftCardResp1 = pageObj.endpoints().Api1ReloadGiftCardRecurring(userEmail, dataSet.get("client"),
				dataSet.get("secret"), "15", userToken, sourceGCUUID, uuidPaymentCard);
		Assert.assertEquals(reloadGiftCardResp1.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Failed to reload gift card");
		String actErrMsg2 = reloadGiftCardResp1.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(actErrMsg2, "Saved Payment hasn't been enabled for this payment adapter yet.",
				"Error message is not matched");
		logger.info("Gift Card has not reloaded when enable_recurring_payment is false in business");
		pageObj.utils().logPass("Gift Card has not reloaded when enable_recurring_payment is false in business");

		// Autoreload Giftcard Config Enable with saved payment card
		Response giftCardUpdateResp2 = pageObj.endpoints().api1UpdateGiftCardwithAutoreloadConfig(dataSet.get("client"),
				dataSet.get("secret"), userToken, sourceGCUUID, true, true, dataSet.get("userGcThresholdAmt"),
				dataSet.get("userGcDefaultAmt"), paycardUUID);
		Assert.assertEquals(giftCardUpdateResp2.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code is not 422 for Gift Card Autoreload Config");
		String actErrMsg4 = giftCardUpdateResp2.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(actErrMsg4, "The payment card is not valid", "Error message is not matched");
		logger.info("Gift Card Autoreload Config has not updated when enable_recurring_payment is false in business");
		pageObj.utils().logPass("Gift Card Autoreload Config has not updated when enable_recurring_payment is false in business");

		// SSF Token Generation Using a Saved Payment Card when enable_recurring_payment
		// is False in the business settings.
		Response singleScanTokenResp3 = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), userToken, "recurring", "transaction_token", paycardUUID);
		Assert.assertEquals(singleScanTokenResp3.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for the Single Scan Code API.");
		String actErrMsg3 = singleScanTokenResp3.asString();
		Assert.assertEquals(actErrMsg3, "Saved Payment hasn't been enabled for this payment adapter yet.",
				"Error message is not matched");
		logger.info("SSF Token has not generated when enable_recurring_payment is false in business");
		pageObj.utils().logPass("SSF Token has not generated when enable_recurring_payment is false in business");

		// Subscription purchase using a saved payment card when
		// enable_recurring_payment is False in the business settings.
		Response purchaseSubscriptionresponse2 = pageObj.endpoints().Api2SubscriptionPurchase(userToken,
				dataSet.get("subscription_plan_id"), dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("subscription_amount"), paycardUUID, "Heartland");
		Assert.assertEquals(purchaseSubscriptionresponse2.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for the Subscription Purchase API.");
		String subscriptionPurchaseErrorMsg = purchaseSubscriptionresponse2.getBody().jsonPath()
				.getString("errors.recurring_payment_off_for_adapter").replace("[", "").replace("]", "");
		Assert.assertEquals(subscriptionPurchaseErrorMsg, "Payment Service not available.",
				"Error message is not matched for the Subscription Purchase API");
		logger.info("Subscription Purchase is not successful when enable_recurring_payment is false in business");
		pageObj.utils().logit("Subscription Purchase is not successful when enable_recurring_payment is false in business");

		// Set Enable Saved Payment Card to False and confirm that
		// business_enable_recurring_payment is also False
		// Navigate to Gift Card page and select the gift card adapter as "Givex
		// (without PIN verify)"
		// Navigate to Payment page and select the payment adapter as "PAR Payments"
		String expColValue2 = DBUtils.executeQueryAndGetColumnValue(env, bizPrefGetQuery, "preferences");
		DBUtils.updateBusinessesPreference(env, expColValue2, "false", "enable_gift_card_auto_reload",
				dataSet.get("business_id"));

		String expColValue3 = DBUtils.executeQueryAndGetColumnValue(env, bizPrefGetQuery, "preferences");
		DBUtils.updateBusinessesPreference(env, expColValue3, "true", "enable_recurring_payment",
				dataSet.get("business_id"));

		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.giftcardsPage().parPaymentCredential(false, "mockParPayURL");
		String expColValue4 = DBUtils.executeQueryAndGetColumnValue(env, bizPrefGetQuery, "preferences");
		String flag2 = DBUtils.businessesPreferenceFlag(expColValue4, "enable_recurring_payment");
		Assert.assertEquals(flag2, "false", "enable_recurring_payment flag is True");
		logger.info("enable_recurring_payment flag is False in the business preferences");
		pageObj.utils().logPass("enable_recurring_payment flag is False in the business preferences");
	}

	@Test(description = "SQ-T5512 | HL-T828-T829 cardholder_id presense in payment card api response based on flag  ", priority = 1)

	public void T5512_VerifyCardHolderIdPresenseInPaymentCardApi2Response() throws Exception {
		// Select test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Integration Services, set original Par Payment URL and enable the
		// recurring payment flag
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.giftcardsPage().parPaymentCredential(true, mockParPayURL);

		// Set value of enable_returning_recurring_payment_token (business) to true
		String bizPrefGetQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, bizPrefGetQuery, "preferences");
		boolean flag = DBUtils.updateBusinessesPreference(env, expColValue, "true",
				"enable_returning_recurring_payment_token", dataSet.get("business_id"));

		// Create the user and get the user token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		String userToken = signUpResponse.jsonPath().get("auth_token.token");
		String userId = signUpResponse.jsonPath().getString("id");
		pageObj.utils().logPass(userEmail + " Api1 user signup is successful");

		// Generating the Saved Payment Card
		Response paymentCardResponse = pageObj.endpoints().createPaymentCard(dataSet.get("client"),
				dataSet.get("secret"), userToken, "par_payment", mockPaymenToken, "Rohit", true);
		String paycardUUID = paymentCardResponse.jsonPath().getString("uuid");
		Assert.assertEquals(paymentCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed POS payment api");
		String uuidPaymentCard = paymentCardResponse.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		logger.info("Payment card has created successfully with uuid: " + uuidPaymentCard);
		pageObj.utils().logPass("Payment card has created successfully with uuid: " + uuidPaymentCard);

		// Validate the payment cards when enable_returning_recurring_payment_token is
		// True in the business settings
		Response paymentCardResp = pageObj.endpoints().api2FetchPaymentCard(userToken, "par_payment",
				dataSet.get("client"), dataSet.get("secret"));
		List<Map<String, Object>> pcard = paymentCardResp.jsonPath().getList("");
		boolean isCardHolderIdPresent = pcard.stream().anyMatch(map -> map.containsKey("cardholder_id"));
		Assert.assertTrue(isCardHolderIdPresent, "cardholder_id is not present");
		logger.info(
				"cardholder_id is present in the payment card api response when enable_returning_recurring_payment_token is true");
		pageObj.utils().logPass(
				"cardholder_id is present in the payment card api response when enable_returning_recurring_payment_token is true");

		// Set value of enable_returning_recurring_payment_token (business) to false
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, bizPrefGetQuery, "preferences");
		boolean flag1 = DBUtils.updateBusinessesPreference(env, expColValue1, "false",
				"enable_returning_recurring_payment_token", dataSet.get("business_id"));
		Response paymentCardResp1 = pageObj.endpoints().api2FetchPaymentCard(userToken, "par_payment",
				dataSet.get("client"), dataSet.get("secret"));
		List<Map<String, Object>> pcard1 = paymentCardResp1.jsonPath().getList("");
		boolean isCardHolderIdPresent1 = pcard1.stream().anyMatch(map -> map.containsKey("cardholder_id"));
		Assert.assertFalse(isCardHolderIdPresent1, "cardholder_id is present");
		logger.info(
				"cardholder_id is not present in the payment card api response when enable_returning_recurring_payment_token is true");
		pageObj.utils().logPass(
				"cardholder_id is not present in the payment card api response when enable_returning_recurring_payment_token is true");
	}

	@Test(description = "SQ-T5513 | HL-T1168_T1361 SSF & Payment with Expired Payment Card", priority = 2)

	public void T5513_VerifyPaymentWithExpiredPaymentCard() throws Exception {
		// Create the user and get the user token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		String userToken = signUpResponse.jsonPath().get("auth_token.token");
		String userId = signUpResponse.jsonPath().getString("id");
		pageObj.utils().logPass(userEmail + " Api1 user signup is successful");

		// Generating the Saved Payment Card
		Response paymentCardResponse = pageObj.endpoints().createPaymentCard(dataSet.get("client"),
				dataSet.get("secret"), userToken, "par_payment", mockPaymenToken, "Rohit", true);
		String paycardUUID = paymentCardResponse.jsonPath().getString("uuid");
		Assert.assertEquals(paymentCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed POS payment api");
		String uuidPaymentCard = paymentCardResponse.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		logger.info("Payment card has created successfully with uuid: " + uuidPaymentCard);
		pageObj.utils().logPass("Payment card has created successfully with uuid: " + uuidPaymentCard);

		// Set Payment Card status to 2 (Expired)
		String query = "UPDATE payment_cards SET `card_status` = '" + "2" + "' WHERE uuid = '" + paycardUUID + "';";
		DBUtils.executeUpdateQuery(env, query);

		// Purchase Gift card with expired saved payment card
		Response giftCardPurcahseResp = pageObj.endpoints().Api1PurchaseGiftCardWithRecurring(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("non_wallet_gc_design_id"), "15", userToken, paycardUUID);
		Assert.assertEquals(giftCardPurcahseResp.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Gift card purchase is successful with expired card");
		String errMsg = giftCardPurcahseResp.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(errMsg, "Your saved payment card has expired. Please update your payment information.",
				"Payment Card is not expired");
		logger.info("Gift card purchase is not successful with expired card");
		pageObj.utils().logPass("Gift card purchase is not successful with expired card");

		// SSF Token Generation Using a Saved Payment Card when enable_recurring_payment
		// is False in the business settings.
		Response singleScanTokenResp = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), userToken, "recurring", "transaction_token", paycardUUID);
		Assert.assertEquals(singleScanTokenResp.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not match for the Single Scan Code API.");
		String errMsg1 = singleScanTokenResp.jsonPath().get().toString().replace("[", "").replace("]", "");
		Assert.assertEquals(errMsg1, "Your saved payment card has expired. Please update your payment information.",
				"SSF Token is generated with expired Payment Card");
		logger.info("SSF Token has not generated when enable_recurring_payment is false in business");
		pageObj.utils().logPass("SSF Token has not generated when enable_recurring_payment is false in business");

		// Update the card status to active
		String query1 = "UPDATE payment_cards SET `card_status` = '" + "0" + "' WHERE uuid = '" + paycardUUID + "';";
		DBUtils.executeUpdateQuery(env, query1);
		pageObj.utils().logPass("Payment card status has updated to active");

	}

	@Test(description = "SQ-5884 | HL-T1298 Verify the Visibility of Payment Card Tab based on flag", priority = 3)

	public void T5884_VerifyPaymentCardTabVisibilityOnDashboard() throws Exception {

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Create the user and get the user token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		String userToken = signUpResponse.jsonPath().get("auth_token.token");
		pageObj.utils().logPass(userEmail + " Api1 user signup is successful");

		// Generating the Saved Payment Card
		Response paymentCardResponse = pageObj.endpoints().createPaymentCard(dataSet.get("client"),
				dataSet.get("secret"), userToken, "par_payment", mockPaymenToken, "Rohit", true); // String paycardUUID
																									// =
																									// paymentCarResponse.jsonPath().getString("uuid");
		Assert.assertEquals(paymentCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed POS payment api");
		String uuidPaymentCard = paymentCardResponse.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		logger.info("Payment card has created successfully with uuid: " + uuidPaymentCard);
		pageObj.utils().logPass("Payment card has created successfully with uuid: " + uuidPaymentCard);

		// Navigate to Payment page and select the payment adapter as "PAR Payments"
		String bizPrefGetQuery = "SELECT `businesses`.preferences FROM `businesses` WHERE `businesses`.id='"
				+ dataSet.get("business_id") + "';";
		String expColValue = DBUtils.executeQueryAndGetColumnValue(env, bizPrefGetQuery, "preferences");
		DBUtils.updateBusinessesPreference(env, expColValue, "false", "enable_recurring_payment",
				dataSet.get("business_id"));
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean paymentCardPresent = pageObj.guestTimelinePage().isPaymentCardTabPresent();
		Assert.assertFalse(paymentCardPresent, "Payment Card Tab is present");
		logger.info("Payment Card Tab is not present");
		pageObj.utils().logPass("Payment Card Tab is not present");

		// Navigate to Payment page and select the payment adapter as "PAR Payments"
		String expColValue1 = DBUtils.executeQueryAndGetColumnValue(env, bizPrefGetQuery, "preferences");
		DBUtils.updateBusinessesPreference(env, expColValue1, "true", "enable_recurring_payment",
				dataSet.get("business_id"));

		pageObj.giftcardsPage().selectPaymentAdapter("PAR Payments");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		boolean paymentCardPresent1 = pageObj.guestTimelinePage().isPaymentCardTabPresent();
		Assert.assertTrue(paymentCardPresent1, "Payment Card Tab is not present");
		logger.info("Payment Card Tab is present");
		pageObj.utils().logPass("Payment Card Tab is present");
	}

	@Test(description = "SQ-T5652 | HL-T1253-1252-1250-1251 POS Location Based Transactions with Par Payment Saved Cards", priority = 4)
	public void T3965_T5652_VerifyLocationBasedPaymentWithSavedPaymentCard() throws Exception {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to the Payment page, select the payment adapter as 'PAR Payments,' and
		// enable location-based payments without using business payment configurations.
		pageObj.giftcardsPage().enableLocationBasedPayment("PAR Payments", true, false);

		// Create the user and get the user token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		String userToken = signUpResponse.jsonPath().get("auth_token.token");
		String userId = signUpResponse.jsonPath().getString("id");
		pageObj.utils().logPass(userEmail + " Api1 user signup is successful");

		// Generating the Saved Payment Card
		Response paymentCardResponse = pageObj.endpoints().createPaymentCard(dataSet.get("client"),
				dataSet.get("secret"), userToken, "par_payment", mockPaymenToken, "Rohit", true);
		String paycardUUID = paymentCardResponse.jsonPath().getString("uuid");
		Assert.assertEquals(paymentCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed POS payment api");
		String uuidPaymentCard = paymentCardResponse.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		logger.info("Payment card has created successfully with uuid: " + uuidPaymentCard);
		pageObj.utils().logPass("Payment card has created successfully with uuid: " + uuidPaymentCard);

		// SSF Token Generation Using a Saved Payment Card
		Response singleScanTokenResp = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), userToken, "recurring", "transaction_token", paycardUUID);
		Assert.assertEquals(singleScanTokenResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for the Single Scan Code API.");
		String singleScanCode = singleScanTokenResp.jsonPath().getString("single_scan_code");
		logger.info("Newly generated SSF code is " + singleScanCode);
		pageObj.utils().logPass("Newly generated SSF code is " + singleScanCode);

		// Location-based payment at a non-payment-configured location when the 'Allow
		// Business Config' flag is set to False.
		Response posPaymentResp1 = pageObj.endpoints().POSPayment("singleScan", userEmail, singleScanCode, "recurring",
				dataSet.get("non_payment_location_key"));
		String errMsg = posPaymentResp1.jsonPath().getString("error").replace("[", "").replace("]", "");
		Assert.assertEquals(errMsg, "Payment couldn't be completed. Please check your settings and try again.");
		pageObj.utils().logPass(
				"Location based payment is not allowed on non payment config location when allow business config flag is false");

		// Location-based payment at a payment-configured location when the 'Allow
		// Business Config' flag is set to False.
		Response posPaymentResp2 = pageObj.endpoints().POSPayment("singleScan", userEmail, singleScanCode, "recurring",
				dataSet.get("payment_location_key"));

		Assert.assertEquals(posPaymentResp2.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String payment_reference_id2 = posPaymentResp2.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");
		String sQuery = "SELECT type,paid_for_type,payment_details,source FROM `payments` WHERE `payments`.id='"
				+ payment_reference_id2 + "' order by id asc limit 1;";
		String[] db_columns = { "type", "paid_for_type", "payment_details", "source" };
		List<Map<String, String>> coldata1 = DBUtils.executeQueryAndGetMultipleColumns(env, sQuery, db_columns);
		String flag2 = DBUtils.businessesPreferenceFlag(coldata1.get(0).get("payment_details"),
				"location_based_payment");
		Assert.assertEquals(coldata1.get(0).get("type"), "Payment", "Payment Type is not matched with Payment");
		Assert.assertEquals(coldata1.get(0).get("paid_for_type"), "SingleScanToken",
				"Payment is not performed for SingleScanToken");
		Assert.assertEquals(flag2, "true", "Value of Location based payment is false for non payment location");
		pageObj.utils().logPass(
				"POS Payment has successfuly performed on valid payment config location with payment reference id: "
						+ payment_reference_id2);
		pageObj.utils().logPass("POS Payment is successfuly performed with Location based payment");

		// Go to the Payment page, select 'PAR Payments' as the payment adapter, and
		// enable location-based payments with business payment configurations.
		pageObj.giftcardsPage().enableLocationBasedPayment("PAR Payments", true, true);

		// Location-based payment at a non-payment-configured location when the 'Allow
		// Business Config' flag is set to True.
		Response posPaymentResp3 = pageObj.endpoints().POSPayment("singleScan", userEmail, singleScanCode, "recurring",
				dataSet.get("non_payment_location_key"));
		Assert.assertEquals(posPaymentResp3.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String payment_reference_id3 = posPaymentResp3.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");
		sQuery = "SELECT type,paid_for_type,payment_details,source FROM `payments` WHERE `payments`.id='"
				+ payment_reference_id3 + "' order by id asc limit 1;";
		List<Map<String, String>> coldata3 = DBUtils.executeQueryAndGetMultipleColumns(env, sQuery, db_columns);
		String flag3 = DBUtils.businessesPreferenceFlag(coldata3.get(0).get("payment_details"),
				"location_based_payment");
		Assert.assertEquals(flag3, "false", "Value of Location based payment is true on non payment location");
		pageObj.utils().logPass("POS Payment is successfuly performed on non payment config location with payment reference id: "
						+ payment_reference_id3);

		// Location-based payment at a payment-configured location when the 'Allow
		// Business Config' flag is set to True.
		Response posPaymentResp4 = pageObj.endpoints().POSPayment("singleScan", userEmail, singleScanCode, "recurring",
				dataSet.get("payment_location_key"));

		Assert.assertEquals(posPaymentResp4.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String payment_reference_id4 = posPaymentResp4.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");
		sQuery = "SELECT type,paid_for_type,payment_details,source FROM `payments` WHERE `payments`.id='"
				+ payment_reference_id4 + "' order by id asc limit 1;";
		List<Map<String, String>> db_data4 = DBUtils.executeQueryAndGetMultipleColumns(env, sQuery, db_columns);
		String flag4 = DBUtils.businessesPreferenceFlag(db_data4.get(0).get("payment_details"),
				"location_based_payment");
		Assert.assertEquals(flag4, "true", "Value of Location based payment is false for non payment location");
		pageObj.utils().logPass(
				"POS Payment has successfuly performed on valid payment config location with payment reference id: "
						+ payment_reference_id4);
		pageObj.utils().logPass("POS Payment is successfuly performed with Location based payment");

		// Go to the Payment page, select 'PAR Payments' as the payment adapter, and
		// enable location-based payments with business payment configurations.
		pageObj.giftcardsPage().enableLocationBasedPayment("PAR Payments", false, true);
		pageObj.utils().logPass("Location based payment is disabled");
	}

	@Test(description = "SQ-T7045 | HL-T1539 SubTransType verification in Saved Payment for Par Payment", priority = 4)
	public void T7045_T1539_SubTransTypeVerificationForSavedPaymentInParPayment() throws Exception {

		Bson delFilter = and(eq("business_slug", dataSet.get("slug")));
		long deletedCount = pageObj.mongoDBUtils().deleteDocuments(env, "other", "helios_payments", delFilter);
		logger.info("Deleted " + deletedCount + " documents from helios_payments collection for business_slug: "
				+ dataSet.get("slug"));
		pageObj.utils().logit("Deleted " + deletedCount + " documents.");
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to the Payment page, select the payment adapter as 'PAR Payments,' and
		// enable location-based payments without using business payment configurations.
		pageObj.giftcardsPage().enableLocationBasedPayment("PAR Payments", true, true);

		// Create the user and get the user token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		String userToken = signUpResponse.jsonPath().get("auth_token.token");
		String userId = signUpResponse.jsonPath().getString("id");
		pageObj.utils().logPass(userEmail + " Api1 user signup is successful");

		// Generating the Saved Payment Card
		Response paymentCardResponse = pageObj.endpoints().createPaymentCard(dataSet.get("client"),
				dataSet.get("secret"), userToken, "par_payment", mockPaymenToken, "Rohit", true);
		String paycardUUID = paymentCardResponse.jsonPath().getString("uuid");
		Assert.assertEquals(paymentCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to create payment card\"");
		String uuidPaymentCard = paymentCardResponse.jsonPath().getString("uuid").replace("[", "").replace("]", "");
		logger.info("Payment card has created successfully with uuid: " + paycardUUID);
		pageObj.utils().logPass("Payment card has created successfully with uuid: " + paycardUUID);

		// SSF Token Generation Using a Saved Payment Card
		Response singleScanTokenResp = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), userToken, "recurring", "transaction_token", paycardUUID);
		Assert.assertEquals(singleScanTokenResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for the Single Scan Code API.");
		String singleScanCode = singleScanTokenResp.jsonPath().getString("single_scan_code");
		logger.info("Newly generated SSF code is " + singleScanCode);
		pageObj.utils().logPass("Newly generated SSF code is " + singleScanCode);

		// SSF Token based Payment with Saved Payment Card "recurring"
		Response posPaymentResp = pageObj.endpoints().POSPayment("singleScan", userEmail, singleScanCode, "recurring",
				dataSet.get("payment_location_key"));
		Assert.assertEquals(posPaymentResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "POS Payment failed at payment config location");
		Bson filter = and(eq("business_slug", dataSet.get("slug")), eq("method", "sale"), eq("status", "success"),
				eq("request_params.payment_by_vault", true));
		Bson sort = Sorts.descending("created_at");

		Document result = pageObj.mongoDBUtils().getSingleDocumentWithPolling(env, "other", "helios_payments", filter,
				sort, 1, 5, 3);
		Assert.assertNotNull(result, "No document found matching the criteria in the database.");
		Assert.assertEquals(utils.getFieldValue(result, "vendor_request_params.SubTransType", String.class), "30");
		logger.info("SubTransType is verified as 30 for saved card payment.");
		pageObj.utils().logPass("SubTransType is verified as 30 for saved card payment.");

		// SSF Token Generation Using a Credit Card
		Response singleScanTokenResp1 = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), userToken, "CreditCard", "transaction_token", "24617245462354237574527354");
		Assert.assertEquals(singleScanTokenResp1.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for the Single Scan Code API.");
		String singleScanCode1 = singleScanTokenResp1.jsonPath().getString("single_scan_code");
		logger.info("Newly generated SSF code is " + singleScanCode1);
		pageObj.utils().logPass("Newly generated SSF code is " + singleScanCode1);

		// SSF Token based Payment with Credit Card
		Response posPaymentResp1 = pageObj.endpoints().POSPayment("singleScan", userEmail, singleScanCode1,
				"CreditCard", dataSet.get("payment_location_key"));
		Assert.assertEquals(posPaymentResp1.statusCode(), ApiConstants.HTTP_STATUS_OK, "POS Payment failed at payment config location");
		Bson filter1 = and(eq("business_slug", dataSet.get("slug")), eq("method", "sale"), eq("status", "success"),
				eq("request_params.payment_by_vault", false));
		Bson sort1 = Sorts.descending("created_at");

		Document result1 = pageObj.mongoDBUtils().getSingleDocumentWithPolling(env, "other", "helios_payments", filter1,
				sort1, 1, 5, 3);
		Assert.assertNotNull(result1, "No document found matching the criteria in the database.");
		Document vendorParams = (Document) result1.get("vendor_request_params");
		Assert.assertFalse(vendorParams.containsKey("SubTransType"),
				"SubTransType key should NOT be present in vendor_request_params");
		logger.info("SubTransType key is not present for non saved card payment.");
		pageObj.utils().logPass("SubTransType key is not present for non saved card payment.");
	}

	@Test(description = "SQ-T5516 | HL-T889 Par Payment Token generation through get client token api", priority = 5)

	public void T5516_VerifyParPaymentTokenGeneration() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Integration Services, set original Par Payment URL and enable the
		// recurring payment flag
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.giftcardsPage().parPaymentCredential(true, "https://UATps42.aurusepay.com");

		// Create the user and get the user token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		String userToken = signUpResponse.jsonPath().get("auth_token.token");
		pageObj.utils().logPass(userEmail + " Api1 user signup is successful");

		// Payment Token Generation through Payment URL Page
		Response parPaymentURL = pageObj.endpoints().Api1ParPaymentGetClientToken(dataSet.get("client"),
				dataSet.get("secret"), userToken);
		String parpayUrl = parPaymentURL.jsonPath().get("url");
		String trToken = pageObj.parPaymentTokenGenPage().submitCardDetails(parpayUrl, dataSet.get("card_number"),
				dataSet.get("exp_date"), dataSet.get("cvv"), dataSet.get("zip_code"));
		Assert.assertNotNull(trToken);
		logger.info("Recurring Payment Token generated successfully : " + trToken);
		pageObj.utils().logPass("Recurring Payment Token has generated successfully : " + trToken);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() throws Exception {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}

}
