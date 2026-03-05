package com.punchh.server.Test;

import java.lang.reflect.Method;
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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

// Author - Shashank sharma 
@Listeners(TestListeners.class)
public class GiftCardApiOrderIDValidation {
	static Logger logger = LogManager.getLogger(GiftCardApiOrderIDValidation.class);
	public WebDriver driver;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	// private String timeStamp;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	// 
	// private Utilities utils;
	Properties prop;
	private String receiptsDBName;
	boolean expValue;
	String userEmail, first_name, last_name, authentication_token, access_token, user_id, checkinID, externalUID, query,
			transaction_no, punchh_key, orderID;
	String password = Utilities.getApiConfigProperty("password");
	String birthday = Utilities.getApiConfigProperty("birthday");
	String anniversary = Utilities.getApiConfigProperty("anniversary");
	Response signUpResponse, checkinResponse, closeOrderResponse;
	public static BrowserUtilities brw  = null ; 

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		prop = Utilities.loadPropertiesFile("segmentBeta.properties");

		driver = new BrowserUtilities().launchBrowser();
		apiUtils = new ApiUtils();
		// timeStamp = CreateDateTime.getTimeDateString();
		pageObj = new PageObj(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		// 
		// utils = new Utilities(driver);
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		brw = new BrowserUtilities();
		String runType = brw.getRunType();
		String envType = Utilities.getInstance(env)+"."+runType;
		
		receiptsDBName = Utilities.getDBConfigProperty(envType, "receiptsDBName");
	}

	// Right now order_id is not supported in the gift card api till then making
	// this as disabled
	// @Test(description = "SQ-T3593 (1.0) - CCA2-540 | Gift A Card API with
	// additional parameter named “order_id“ instead of “transaction_token“"
	// + "SQ-T3591 (1.0) - CCA2-540 | Purchase Gift card API with additional
	// parameter named “order_id“ instead of “transaction_token“"
	// + "SQ-T3592 (1.0) - CCA2-540 | Reload API with additional parameter named
	// “order_id“ instead of “transaction_token“")
	@Owner(name = "Shashank Sharma")
	public void T3593_GiftACardWithAdditionalParam_OrderID() throws Exception {
		String orderID = dataSet.get("orderID");
		String query = "update payments set status = 'unused' where transaction_id = '" + orderID
				+ "' and status = 'processed'";

		pageObj.singletonDBUtilsObj();
		// dbUtils.executeUpdateQuery(env, query);
		DBUtils.executeQuery(env, query);

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().clickOnUpdateButton();
		pageObj.giftcardsPage().selectPaymentAdapter("Olo Pay");

		// User register/signup using API2 Signup
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api2 signup");
		TestListeners.extentTest.get().pass("Api2 user signup is successful");

		// Gift a card
		Response giftaCardResponse = pageObj.endpoints().Api2GiftaCardWithOrderID(userEmail, dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("designID"), "20", "0999", "Punchh", orderID);

		String actualSuccessMessage = giftaCardResponse.asString();
		Assert.assertTrue(actualSuccessMessage.contains(dataSet.get("expectedSuccessMessage")));

		Assert.assertEquals(giftaCardResponse.getStatusCode(), 201,
				"Status code 201 did not matched for api2  gift a card");
		TestListeners.extentTest.get().pass("Api2 Gift a Card is successful ");

		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, query);

		Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCardWithOrderID(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("designID"), "20", "0999", "Punchh", orderID);
		Assert.assertEquals(signUpResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api2 purchase gift card");
		TestListeners.extentTest.get().pass("Api2 Purchase Gift Card is successful ");
		String uuid = purchaseGiftCardResponse.jsonPath().getString("uuid");

		pageObj.singletonDBUtilsObj();
		DBUtils.executeQuery(env, query);
		Response reloadGiftCardResponse = pageObj.endpoints().Api2ReloadGiftCardWithOrderID(dataSet.get("client"),
				dataSet.get("secret"), token, uuid, "20", "Punchh", "0999", orderID);
		Assert.assertEquals(reloadGiftCardResponse.getStatusCode(), 200,
				"Status code 422 did not matched for api2 reload gift card");
		TestListeners.extentTest.get().pass("Api2 Reload Gift Card is unsuccessful (expected) ");
	}

	@Test(description = "SQ-T2997 - Double Earning | Authenticated OLO - Same and Different guest")
	@Owner(name = "Nipun Jain")
	public void T2997_DoubleEarningAuthenticatedOLOSameAndDifferentGuest() throws Exception {

		String client = dataSet.get("client");
		String secret = dataSet.get("secret");
		String slug = dataSet.get("slug");
		String locationStoreNumber = dataSet.get("locationStoreNumber");
		String locationName = dataSet.get("locationName");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(slug);

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable OLO Webhooks?", "check");
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.dashboardpage().navigateToTabs("OLO");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable to stop Double Earning", "check");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable Transaction Profile Match", "uncheck");
		pageObj.dashboardpage().clickOnUpdateOloButton();

		// Auth api signup
		String userEmail = "double_earning_auto_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@partech.com";
		Response response = pageObj.endpoints().authApiSignUp(userEmail, client, secret);
		Assert.assertEquals(response.getStatusCode(), 201);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		String authToken = response.jsonPath().get("authentication_token");
		String token = response.jsonPath().get("access_token").toString();
		String userID = response.jsonPath().getString("user_id");

		Response checkinResponse = pageObj.endpoints().onlineOrderCheckin(authToken, "25", client, secret,
				locationStoreNumber);
		logger.info("checkinResponse : " + checkinResponse.asString());
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		String checkinID = checkinResponse.jsonPath().getString("checkin.checkin_id");
		String externalUID = checkinResponse.jsonPath().getString("checkin.external_uid");

		// Validate entiry in checkin
		String query = "select id from checkins where user_id=" + userID + " and checkin_type='OnlineCheckin' limit 1";
		DBUtils.verifyValueFromDBUsingPolling(env, query, "id", checkinID);

		// Validate entry in receipt_details
		query = "select transaction_no from receipt_details where user_id=" + userID + " limit 1";
		pageObj.singletonDBUtilsObj();
		String transactionNumber = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"transaction_no", 1);
		Assert.assertNotNull(transactionNumber,
				"No entry found in receipt_details, got transaction number as : " + transactionNumber);
		TestListeners.extentTest.get()
				.info("Entry found in receipt_details, got transaction number as : " + transactionNumber);

		// Validate no Entry in receipts table
		query = "select id from " + receiptsDBName + ".receipts where transaction_no=" + transactionNumber + " limit 1";
		pageObj.singletonDBUtilsObj();
		String receipt_id = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query, "id", 1);
		Assert.assertEquals("", receipt_id, "Entry found in receipts table, got receipt_id as : " + receipt_id);
		TestListeners.extentTest.get().pass("No entry found in receipts table, got receipt_id as : " + receipt_id);

		// Close Order
		orderID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		response = pageObj.endpoints().closeOrderOnline(client, secret, slug, externalUID, token, userEmail,
				locationStoreNumber, transactionNumber, "Punchh", userID, orderID);
		Assert.assertEquals(response.getStatusCode(), 200, "Status code 200 did not matched for close order online");

		// Get punchh_key from checkins table
		query = "select punchh_key from checkins where user_id=" + userID + " and checkin_type='OnlineCheckin' limit 1";
		pageObj.singletonDBUtilsObj();
		String punchh_key = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query,
				"punchh_key", 1);

		// Validate Entry in receipts table
		query = "select id from " + receiptsDBName + ".receipts where punchh_key='" + punchh_key + "' limit 1";
		pageObj.singletonDBUtilsObj();
		receipt_id = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, query, "id", 10);
		Assert.assertNotEquals("", receipt_id, "No entry found in receipts table, got receipt_id as : " + receipt_id);
		Assert.assertNotNull(receipt_id, "No entry found in receipts table, got receipt_id as : " + receipt_id);
		TestListeners.extentTest.get().pass("Entry found in receipts table, got receipt_id as : " + receipt_id);

		// generateBarcode
		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcodeWithTransactionNumber(locationName, transactionNumber, "15",
				"30");
		pageObj.instanceDashboardPage().clickOnGenerateBarcodeButton();
		String barcode = pageObj.instanceDashboardPage().captureBarcodeWithCurrentDateAndTime();

		TestListeners.extentTest.get().pass("barcode generated - " + barcode);

		Response barcodeCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinBarCode(client, secret, token, barcode);
		boolean isApi2BarcodeCheckinUsageLimitSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, barcodeCheckinResponse.asString());
		Assert.assertTrue(isApi2BarcodeCheckinUsageLimitSchemaValidated,
				"API v2 Create Loyalty Checkin by Barcode Schema Validation failed");
		String errorMessage = barcodeCheckinResponse.jsonPath().getString("errors.base");
		Assert.assertEquals(barcodeCheckinResponse.statusCode(), 422);

		Assert.assertEquals(errorMessage, "[You cannot use the same receipt more than once]");
		TestListeners.extentTest.get()
				.pass("Verified the expected error message [You cannot use the same receipt more than once]");

		// Create another user for checkin
		userEmail = "double_earning_auto_" + CreateDateTime.getTimeDateAsneed("yyyyMMddHHmmss") + "@partech.com";

		// Auth api signup
		response = pageObj.endpoints().authApiSignUp(userEmail, client, secret);
		Assert.assertEquals(response.getStatusCode(), 201);
		Assert.assertEquals(response.jsonPath().get("email").toString(), userEmail.toLowerCase());
		token = response.jsonPath().get("access_token").toString();

		barcodeCheckinResponse = pageObj.endpoints().Api2LoyaltyCheckinBarCode(client, secret, token, barcode);
		boolean isApi2BarcodeCheckinAlreadyScannedSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, barcodeCheckinResponse.asString());
		Assert.assertTrue(isApi2BarcodeCheckinAlreadyScannedSchemaValidated,
				"API v2 Create Loyalty Checkin by Barcode Schema Validation failed");
		errorMessage = barcodeCheckinResponse.jsonPath().getString("errors.base");
		Assert.assertEquals(barcodeCheckinResponse.statusCode(), 422);

		Assert.assertEquals(errorMessage, "[This receipt has already been scanned by someone else.]");
		TestListeners.extentTest.get()
				.pass("Verified the expected error message [This receipt has already been scanned by someone else.]");
	}

	@Test(description = "SQ-T5576 - INT2-1765 | INT2-1766 | Save Non-Loyalty Transactions from OLO Webhook Based on Configuration of 'Save Non-Loyalty Transaction' Flag")
	@Owner(name = "Shaleen Gupta")
	public void T5576_verifySaveNonLoyaltyTransactionFlag() throws Exception {

		// open business
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Scenario 1
		updateFlags("check", "uncheck");
		setGuestCreationStrategy("Loyalty");
		createLoyaltyUser();
		createCheckin();
		validateEntryInCheckinTable("entry found");
		validateEntryInReceiptDetailsTable("entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		closeOrder(dataSet.get("loginProviderSlug"));
		getPunchh_key();
		validateEntryInReceiptsTable("entry found by punchh_key");
		/*---*/
		updateFlags("check", "check");
		createLoyaltyUser();
		createCheckin();
		validateEntryInCheckinTable("entry found");
		validateEntryInReceiptDetailsTable("entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		closeOrder(dataSet.get("loginProviderSlug"));
		getPunchh_key();
		validateEntryInReceiptsTable("entry found by punchh_key");

		// Scenario 2
		updateFlags("check", "uncheck");
		createLoyaltyUser();
		createCheckin();
		validateEntryInCheckinTable("entry found");
		validateEntryInReceiptDetailsTable("entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		closeOrder("Pun6789chh");
		getPunchh_key();
		validateEntryInReceiptsTable("no entry found by punchh_key");
		/*---*/
		updateFlags("check", "check");
		createLoyaltyUser();
		createCheckin();
		validateEntryInCheckinTable("entry found");
		validateEntryInReceiptDetailsTable("entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		closeOrder("Pun6459chh");
		getPunchh_key();
		validateEntryInReceiptsTable("entry found by punchh_key");

		// Scenario 3
		updateFlags("check", "uncheck");
		createLoyaltyUser();
		externalUID = CreateDateTime.getUniqueString("7");
		transaction_no = CreateDateTime.getUniqueString("8");
		closeOrder(dataSet.get("loginProviderSlug"));
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		/*---*/
		updateFlags("check", "check");
		createLoyaltyUser();
		externalUID = CreateDateTime.getUniqueString("4");
		transaction_no = CreateDateTime.getUniqueString("3");
		closeOrder(dataSet.get("loginProviderSlug"));
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("entry found by orderID");

		// Scenario 4
		updateFlags("check", "uncheck");
		createLoyaltyUser();
		externalUID = CreateDateTime.getUniqueString("1");
		transaction_no = CreateDateTime.getUniqueString("2");
		closeOrder("Pun6129chh");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		/*---*/
		updateFlags("check", "check");
		createLoyaltyUser();
		externalUID = CreateDateTime.getUniqueString("3");
		transaction_no = CreateDateTime.getUniqueString("4");
		closeOrder("Pun6189chh");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("entry found by orderID");

		// Scenario 5
		updateFlags("check", "uncheck");
		userEmail = CreateDateTime.getUniqueString("randomUserOLO_") + "@partech.com";
		access_token = CreateDateTime.getUniqueString("jo");
		user_id = Integer.toString(Utilities.getRandomNoFromRange(500, 10000));
		externalUID = CreateDateTime.getUniqueString("6");
		transaction_no = CreateDateTime.getUniqueString("31");
		closeOrder("Pun6129chh");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		/*---*/
		updateFlags("check", "check");
		userEmail = CreateDateTime.getUniqueString("randomUserOLO_") + "@partech.com";
		access_token = CreateDateTime.getUniqueString("gr");
		user_id = Integer.toString(Utilities.getRandomNoFromRange(500, 10000));
		externalUID = CreateDateTime.getUniqueString("72");
		transaction_no = CreateDateTime.getUniqueString("49");
		closeOrder("Pun6429chh");
		validateEntryInUsersTable("entry found");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("entry found by orderID");

		// Scenario 6
		updateFlags("check", "uncheck");
		setGuestCreationStrategy("no selection");
		userEmail = CreateDateTime.getUniqueString("randomUserOLO_") + "@partech.com";
		access_token = CreateDateTime.getUniqueString("ix");
		user_id = Integer.toString(Utilities.getRandomNoFromRange(500, 10000));
		externalUID = CreateDateTime.getUniqueString("98");
		transaction_no = CreateDateTime.getUniqueString("37");
		closeOrder("Pun7169chh");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		validateEntryInUsersTable("no entry found");
		/*---*/
		updateFlags("check", "check");
		userEmail = CreateDateTime.getUniqueString("randomUserOLO_") + "@partech.com";
		access_token = CreateDateTime.getUniqueString("yu");
		user_id = Integer.toString(Utilities.getRandomNoFromRange(500, 10000));
		externalUID = CreateDateTime.getUniqueString("74");
		transaction_no = CreateDateTime.getUniqueString("29");
		closeOrder("Pun8169chh");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInUsersTable("no entry found");
		validateEntryInReceiptsTable("entry found by orderID");

		// Scenario 7
		updateFlags("uncheck", "uncheck");
		setGuestCreationStrategy("Loyalty");
		createLoyaltyUser();
		externalUID = CreateDateTime.getUniqueString("73");
		transaction_no = CreateDateTime.getUniqueString("41");
		closeOrder(dataSet.get("loginProviderSlug"));
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		/*---*/
		updateFlags("uncheck", "check");
		createLoyaltyUser();
		externalUID = CreateDateTime.getUniqueString("89");
		transaction_no = CreateDateTime.getUniqueString("46");
		closeOrder(dataSet.get("loginProviderSlug"));
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("entry found by orderID");

		// Scenario 8
		updateFlags("uncheck", "uncheck");
		createLoyaltyUser();
		externalUID = CreateDateTime.getUniqueString("52");
		transaction_no = CreateDateTime.getUniqueString("75");
		closeOrder("Pun9129chh");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		/*---*/
		updateFlags("uncheck", "check");
		createLoyaltyUser();
		externalUID = CreateDateTime.getUniqueString("67");
		transaction_no = CreateDateTime.getUniqueString("95");
		closeOrder("Pun9189chh");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("entry found by orderID");

		// Scenario 9
		updateFlags("uncheck", "uncheck");
		createLoyaltyUser();
		createCheckin();
		validateEntryInCheckinTable("entry found");
		validateEntryInReceiptDetailsTable("entry found");
		getPunchh_key();
		validateEntryInReceiptsTable("entry found by punchh_key");
		closeOrder(dataSet.get("loginProviderSlug"));
		/*---*/
		updateFlags("uncheck", "check");
		createLoyaltyUser();
		createCheckin();
		validateEntryInCheckinTable("entry found");
		validateEntryInReceiptDetailsTable("entry found");
		getPunchh_key();
		validateEntryInReceiptsTable("entry found by punchh_key");
		closeOrder(dataSet.get("loginProviderSlug"));
		validateEntryInReceiptsTable("entry found by orderID");

		// Scenario 10
		updateFlags("uncheck", "uncheck");
		createLoyaltyUser();
		createCheckin();
		validateEntryInCheckinTable("entry found");
		validateEntryInReceiptDetailsTable("entry found");
		getPunchh_key();
		validateEntryInReceiptsTable("entry found by punchh_key");
		closeOrder("Pun6122chh");
		validateEntryInReceiptsTable("entry found by orderID");
		/*---*/
		updateFlags("uncheck", "check");
		createLoyaltyUser();
		createCheckin();
		validateEntryInCheckinTable("entry found");
		validateEntryInReceiptDetailsTable("entry found");
		getPunchh_key();
		validateEntryInReceiptsTable("entry found by punchh_key");
		closeOrder("Pun6882chh");
		validateEntryInReceiptsTable("entry found by orderID");

		// Scenario 11
		updateFlags("uncheck", "uncheck");
		userEmail = CreateDateTime.getUniqueString("randomUserOLO_") + "@partech.com";
		access_token = CreateDateTime.getUniqueString("su");
		user_id = Integer.toString(Utilities.getRandomNoFromRange(500, 10000));
		externalUID = CreateDateTime.getUniqueString("71");
		transaction_no = CreateDateTime.getUniqueString("92");
		closeOrder("Pun3289chh");
		validateEntryInUsersTable("entry found");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		/*---*/
		updateFlags("uncheck", "check");
		userEmail = CreateDateTime.getUniqueString("randomUserOLO_") + "@partech.com";
		access_token = CreateDateTime.getUniqueString("gq");
		user_id = Integer.toString(Utilities.getRandomNoFromRange(500, 10000));
		externalUID = CreateDateTime.getUniqueString("28");
		transaction_no = CreateDateTime.getUniqueString("99");
		closeOrder("Pun3284chh");
		validateEntryInUsersTable("entry found");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("entry found by orderID");

		// Scenario 12
		updateFlags("uncheck", "uncheck");
		setGuestCreationStrategy("no selection");
		userEmail = CreateDateTime.getUniqueString("randomUserOLO_") + "@partech.com";
		access_token = CreateDateTime.getUniqueString("hq");
		user_id = Integer.toString(Utilities.getRandomNoFromRange(500, 10000));
		externalUID = CreateDateTime.getUniqueString("91");
		transaction_no = CreateDateTime.getUniqueString("32");
		closeOrder("Pun3669chh");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInReceiptsTable("no entry found by transaction_no");
		validateEntryInUsersTable("no entry found");
		/*---*/
		updateFlags("uncheck", "check");
		userEmail = CreateDateTime.getUniqueString("randomUserOLO_") + "@partech.com";
		access_token = CreateDateTime.getUniqueString("hvu");
		user_id = Integer.toString(Utilities.getRandomNoFromRange(500, 10000));
		externalUID = CreateDateTime.getUniqueString("73");
		transaction_no = CreateDateTime.getUniqueString("82");
		closeOrder("Pun3666chh");
		validateEntryInCheckinTable("no entry found");
		validateEntryInReceiptDetailsTable("no entry found");
		validateEntryInUsersTable("no entry found");
		validateEntryInReceiptsTable("entry found by orderID");

	}
	/*-Helper methods-*/

	public void updateFlags(String enableStopDoubleEarning, String saveNonLoyaltyTransaction)
			throws InterruptedException {
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.dashboardpage().navigateToTabs("OLO");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage("Enable to stop Double Earning",
				enableStopDoubleEarning);
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboardOLOPage(
				"Save non-loyalty transaction based on Order Close Webhook", saveNonLoyaltyTransaction);
		pageObj.dashboardpage().clickOnUpdateOloButton();

	}
	@Owner(name = "Shaleen Gupta")
	public void setGuestCreationStrategy(String strategy) throws InterruptedException {
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Guests");
		pageObj.dashboardpage().navigateToTabs("Miscellaneous Config");
		if (strategy.equalsIgnoreCase("no selection")) {
			pageObj.cockpitGuestPage().clearDropdownValue("Guest Creation Strategy");
		} else {
			pageObj.posIntegrationPage().selectDrpDownValue("Guest Creation Strategy", strategy);
		}
		pageObj.dashboardpage().clickOnUpdateButton();

	}
	@Owner(name = "Shaleen Gupta")
	public void createLoyaltyUser() {
		userEmail = CreateDateTime.getUniqueString("testUserOLO_") + "@partech.com";
		first_name = "first_name" + CreateDateTime.getTimeDateString();
		last_name = "last_name" + CreateDateTime.getTimeDateString();
		signUpResponse = pageObj.endpoints().authApiSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"),
				password, password, birthday, anniversary, "", "", first_name, last_name);
		Assert.assertEquals(signUpResponse.getStatusCode(), 201,
				"Status code 201 did not matched for auth user signup api");
		authentication_token = signUpResponse.jsonPath().get("authentication_token").toString();
		access_token = signUpResponse.jsonPath().get("access_token").toString();
		user_id = signUpResponse.jsonPath().getString("user_id");
		logger.info("AUTH Api user signup is successful");
		TestListeners.extentTest.get().pass("AUTH Api user signup is successful");

	}
	@Owner(name = "Shaleen Gupta")
	public void createCheckin() {
		checkinResponse = pageObj.endpoints().onlineOrderCheckin(authentication_token, "25", dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("locationStoreNumber"));
		logger.info("checkinResponse : " + checkinResponse.asString());
		Assert.assertEquals(checkinResponse.getStatusCode(), 200);
		checkinID = checkinResponse.jsonPath().getString("checkin.checkin_id");
		externalUID = checkinResponse.jsonPath().getString("checkin.external_uid");

	}

	public void validateEntryInCheckinTable(String condition) throws Exception {
		if (condition.equalsIgnoreCase("entry found")) {
			String q = "select id from checkins where user_id=" + user_id + " and checkin_type='OnlineCheckin' limit 1";
			DBUtils.verifyValueFromDBUsingPolling(env, q, "id", checkinID);
		} else if (condition.equalsIgnoreCase("no entry found")) {
			String q = "select COUNT(*) as count from checkins where user_id=" + user_id + " limit 1";
			expValue = DBUtils.verifyValueFromDBUsingPolling(env, q, "count", "0");
			Assert.assertTrue(expValue, "Entry found in checkins table");
			logger.info("No entry found in checkins table");
			TestListeners.extentTest.get().pass("No entry found in checkins table");
		}

	}

	public void validateEntryInReceiptDetailsTable(String condition) throws Exception {
		if (condition.equalsIgnoreCase("entry found")) {
			String q = "select transaction_no from receipt_details where user_id=" + user_id + " limit 1";
			pageObj.singletonDBUtilsObj();
			transaction_no = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, q,
					"transaction_no", 2);
			Assert.assertNotNull(transaction_no, "No entry found in receipt_details");
			logger.info("Entry found in receipt_details table");
			TestListeners.extentTest.get().info("Entry found in receipt_details table");
		} else if (condition.equalsIgnoreCase("no entry found")) {
			String q = "select COUNT(*) as count from receipt_details where user_id=" + user_id + " limit 1";
			expValue = DBUtils.verifyValueFromDBUsingPolling(env, q, "count", "0");
			Assert.assertTrue(expValue, "Entry found in receipt_details table");
			logger.info("No entry found in receipt_details table");
			TestListeners.extentTest.get().pass("No entry found in receipt_details table");
		}

	}

	public void validateEntryInReceiptsTable(String condition) throws Exception {
		if (condition.equalsIgnoreCase("entry found by punchh_key")) {
			String q = "select COUNT(*) as count from " + receiptsDBName + ".receipts where punchh_key='" + punchh_key
					+ "' limit 1";
			expValue = DBUtils.verifyValueFromDBUsingPolling(env, q, "count", "1");
			Assert.assertTrue(expValue, "No entry found in receipts table");
			logger.info("Entry found in receipts table");
			TestListeners.extentTest.get().pass("Entry found in receipts table");
		} else if (condition.equalsIgnoreCase("no entry found by punchh_key")) {
			String q = "select COUNT(*) as count from " + receiptsDBName + ".receipts where punchh_key='" + punchh_key
					+ "' limit 1";
			expValue = DBUtils.verifyValueFromDBUsingPolling(env, q, "count", "0");
			Assert.assertTrue(expValue, "Entry found in receipts table");
			logger.info("No entry found in receipts table");
			TestListeners.extentTest.get().pass("No entry found in receipts table");
		} else if (condition.equalsIgnoreCase("entry found by orderID")) {
			String q = "select COUNT(*) as count from " + receiptsDBName + ".receipts where transaction_no='" + "OID"
					+ orderID + "' limit 1";
			expValue = DBUtils.verifyValueFromDBUsingPolling(env, q, "count", "1");
			Assert.assertTrue(expValue, "No entry found in receipts table");
			logger.info("Entry found in receipts table");
			TestListeners.extentTest.get().pass("Entry found in receipts table");
		} else if (condition.equalsIgnoreCase("no entry found by transaction_no")) {
			String q = "select COUNT(*) as count from " + receiptsDBName + ".receipts where transaction_no="
					+ transaction_no + " limit 1";
			expValue = DBUtils.verifyValueFromDBUsingPolling(env, q, "count", "0");
			Assert.assertTrue(expValue, "Entry found in receipts table");
			logger.info("No entry found in receipts table");
			TestListeners.extentTest.get().pass("No entry found in receipts table");
		}

	}

	public void closeOrder(String loginProviderSlug) throws Exception {
		orderID = Integer.toString(Utilities.getRandomNoFromRange(50000, 100000));
		closeOrderResponse = pageObj.endpoints().closeOrderOnline(dataSet.get("client"), dataSet.get("secret"),
				dataSet.get("slug"), externalUID, access_token, userEmail, dataSet.get("locationStoreNumber"),
				transaction_no, loginProviderSlug, user_id, orderID);
		Assert.assertEquals(closeOrderResponse.getStatusCode(), 200,
				"Status code 200 did not matched for close order online");
		logger.info("Close order online is successful");
		TestListeners.extentTest.get().pass("Close order online is successful");

	}

	public void getPunchh_key() throws Exception {
		String q = "select punchh_key from checkins where user_id=" + user_id
				+ " and checkin_type='OnlineCheckin' limit 1";
		pageObj.singletonDBUtilsObj();
		punchh_key = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, q, "punchh_key", 1);

	}

	public void validateEntryInUsersTable(String condition) throws Exception {
		if (condition.equalsIgnoreCase("entry found")) {
			String q = "select id from users where email='" + userEmail + "' limit 1";
			pageObj.singletonDBUtilsObj();
			user_id = DBUtils.executeQueryAndGetColumnValuePollingUsed(env, q, "id", 2);
			Assert.assertNotNull(user_id, "No entry found in users table");
			logger.info("Entry found in users table");
			TestListeners.extentTest.get().pass("Entry found in users table");
		} else if (condition.equalsIgnoreCase("no entry found")) {
			String q = "select COUNT(*) as count from users where email='" + userEmail + "' limit 1";
			expValue = DBUtils.verifyValueFromDBUsingPolling(env, q, "count", "0");
			Assert.assertTrue(expValue, "Entry found in users table");
			logger.info("No entry found in users table");
			TestListeners.extentTest.get().pass("No entry found in users table");
		}

	}

	@AfterMethod()
	public void afterClass() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}

}
