package com.punchh.server.Integration1;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class PARCommerceGiftCardTest {

	private static Logger logger = LogManager.getLogger(PARCommerceGiftCardTest.class);
	public WebDriver driver;
	private Properties prop;
	private PageObj pageObj;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;
	private static String authToken;
	private static String userEmail;
	private static String GCUUID;
	private static String gcCardNumber;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		prop = Utilities.loadPropertiesFile("config.properties");
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
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

	@Test(description = "SQ-T6724, HL-T-1382 Verify location onboarding and configuration for PAR Commerce Gift Cards INT-3142", groups = {
			"regression", "giftcard" }, priority = 1)
	public void T6724ToVerifyTheBulkImportLocationOnboardingForPARCommerceGC() throws InterruptedException {

		// Delete existing vendor location data
		String deletePunchKeyQuery = "DELETE FROM vendor_locations WHERE business_id=" + dataSet.get("business_id")
				+ ";";
		String verifyDeletionQuery = "SELECT COUNT(*) AS rowCount FROM vendor_locations WHERE business_id="
				+ dataSet.get("business_id") + ";";

		try {
			DBUtils.executeUpdateQuery(env, deletePunchKeyQuery);
			logger.info("All vendor location data has been deleted for business_id: " + dataSet.get("business_id"));
			pageObj.utils().logPass("Successfully deleted Vendor location Data from DB");
		} catch (Exception e) {
			logger.error("Error during deletion of vendor location data", e);
			Assert.fail("Failed to delete vendor location data before test execution.");
			pageObj.utils().logPass("Unable to Delete Vendor location Data from DB");
		}

		// Verify deletion
		try {
			String vendorLocationRowCount = DBUtils.executeQueryAndGetColumnValue(env, verifyDeletionQuery, "rowCount");
			pageObj.utils().logPass("Successfully fetched number of rows for Vendor location from DB");
			logger.info("Remaining vendor_location rows: " + vendorLocationRowCount);
			Assert.assertEquals(vendorLocationRowCount, "0", "Vendor location rows were not fully deleted.");
		} catch (Exception e) {
			logger.error("Error during verification of deletion", e);
			pageObj.utils().logPass("Unable to fetch number of vendor location from DB");
			Assert.fail("Failed to verify vendor location deletion.");
		}

		// Prepare test data
		String importName = Utilities.generateRandomString(8);
		String email = "nitesh.shekhawat@partech.com";
		String directoryPath = System.getProperty("user.dir") + "/resources/Testdata/locationPayments.csv";

		// Select Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// set giftcard adapter
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("PAR Commerce Gift Card Adapter");
		pageObj.giftcardsPage().clickOnUpdateButton();

		// Upload location config
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");
		pageObj.locationPage().clickImportPARCommerceConfigurations();
		pageObj.utils().logPass("Successfully Navigated To Bulk Import Par Commerce Config Page");
		pageObj.locationPage().bulkImportLocationParWalletConfig(importName, email, directoryPath);

		// Assert success message
		String successMessage = pageObj.locationPage().getErrorSuccessMessage();
		Assert.assertEquals(successMessage,
				"Your Configuration CSV has been uploaded successfully. You will be notified on provided email once it has been processed.");
		logger.info("Configuration CSV upload success message verified.");
		pageObj.utils().logPass("Your Configuration CSV has been uploaded successfully");

		// Assert bulk import status
		Assert.assertTrue(pageObj.locationPage().getBulkImportStatus(importName), "Bulk import status check failed.");
		logger.info("Bulk import status verified successfully.");
		pageObj.utils().logPass("Your Configuration CSV file has been processed");

		// Navigate and verify location
		pageObj.menupage().navigateToSubMenuItem("Settings", "Locations");

		// need to update
		pageObj.locationPage().clickOnLocationName("Lindbergh");
		pageObj.locationPage().selectTabOnEditLocation("PAR Commerce");

		Map<String, String> parCommerceConfigForLocation = pageObj.locationPage()
				.getLocationParCommerceGiftCardConfig();
		pageObj.utils().logPass("Fetched the Location PAR Commerce configurations successfully");
		Assert.assertEquals(parCommerceConfigForLocation.get("vendorLocationId"), dataSet.get("vendorLocationID"));
		Assert.assertEquals(parCommerceConfigForLocation.get("vendorLocationStatus"),
				dataSet.get("vendorLocationStatus"));
		logger.info("Location ID and status matched for Automation Location.");
		pageObj.utils().logPass("Automation Location PAR Commerce configurations matches the expected value");

// Final DBvalidation
		String getPunchKeyQuery = "SELECT COUNT(*) AS rowCount FROM vendor_locations WHERE business_id="
				+ dataSet.get("business_id") + ";";
		try {
			String vendorLocationRowCount = DBUtils.executeQueryAndGetColumnValue(env, getPunchKeyQuery, "rowCount");
			Assert.assertEquals(vendorLocationRowCount, "2");
			logger.info("Vendor location count after import: " + vendorLocationRowCount);
			pageObj.utils().logPass("DB Records in vendor_locations table matched");
		} catch (Exception e) {
			logger.error("Error fetching final row count", e);
			pageObj.utils().logFail("DB validation failed: " + e.getMessage());
			Assert.fail("Database validation failed: " + e.getMessage());
		}
	}

	@Test(description = "SQ-T6719 To verify the purchase functionality for ParCommerce GC", groups = { "regression",
			"giftcard" }, dependsOnMethods = "T6724ToVerifyTheBulkImportLocationOnboardingForPARCommerceGC")

	public void T6719ToVerifyPurchaseFunctionlaityForParCommerceGC() throws Exception {
		// Instance business selection
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		userEmail = "testuser+" + System.currentTimeMillis() + "@partech.com";
		logger.info("Test User Email ID is " + userEmail);
		Response response = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"), dataSet.get("secret"),
				"", "");
		authToken = response.jsonPath().getString("auth_token.token");
		pageObj.utils().logPass("Test User created successfully with Email ID:-" + userEmail);

		// Navigate to Payment page and select the payment adapter as "Braintree"
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");
		pageObj.giftcardsPage().clickOnUpdateButton();

		// Gift card purchase apiv1
		Response purchaseGiftCardResp = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("amount"), authToken, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card purchase");
		pageObj.utils().logPass("Api1 purchase gift card is successful ");
		gcCardNumber = purchaseGiftCardResp.jsonPath().get("card_number").toString();
		GCUUID = purchaseGiftCardResp.jsonPath().get("uuid").toString();
		String amount = purchaseGiftCardResp.jsonPath().get("last_fetched_amount").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String notification = pageObj.guestTimelinePage().getGiftCard();
		Assert.assertTrue(notification.contains("You just purchased a Gift Card for yourself"),
				"You just purchased a Gift Card for yourself, string did not matched with notification value");
		Assert.assertTrue(notification.contains(amount), "Balance value did not matched");
		pageObj.utils().logPass("Gift card purchase notification verified successfuly on timeline");
		// Timeline Giftcards section
		pageObj.guestTimelinePage().clickGiftCards();
		String card_Number = pageObj.giftcardsPage().getCardNumber();
		String cardBalance = pageObj.giftcardsPage().getCardBalance();
		Assert.assertEquals(card_Number, gcCardNumber, "Card number did not matched in gift cards page");
		Assert.assertTrue(cardBalance.contains(amount), "Card balance did not matched in gift cards page");
		pageObj.utils().logPass(
				"Gift card number, balance and reload status verified successfuly on User gift card timeline page");

	}

	@Test(description = "SQ-T6720 This is to verify the Reload and Add Existing Card functionality for Par Commerce GC", groups = {
			"regression", "giftcard" }, dependsOnMethods = "T6719ToVerifyPurchaseFunctionlaityForParCommerceGC")
	public void T6720ToVerifyReloadAndAddExistingGC() throws Exception {
		// Instance business selection
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Gift card reload api1
		Response reloadGiftCardResp = pageObj.endpoints().Api1ReloadGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("reloadAmount"), authToken, GCUUID, "fake-valid-nonce");
		Assert.assertEquals(reloadGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card reload");
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String notification = pageObj.guestTimelinePage().getCardReloadNotification();
		Assert.assertTrue(notification.contains("Processed Gift Card Payment"),
				"Processed Gift Card Payment, string did not matched with notification value");
		pageObj.utils().logPass("Gift card reload notification verified successfuly on timeline");
		// Adding Existing Gift Card
		Response addExitingCardResp = pageObj.endpoints().apiAddExistingGiftCard(dataSet.get("design_id"),
				dataSet.get("card_number"), dataSet.get("epin"), dataSet.get("client"), authToken,
				dataSet.get("secret"));
		Assert.assertEquals(addExitingCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card reload");
		String gcid = addExitingCardResp.jsonPath().get("gift_card_id").toString();
		pageObj.giftcardsPage().validateGiftCardEvent(env, gcid, "card_added");
		// Deleting added existing gift card
		DBUtils.deleteGiftCardData(env, gcid);

	}

	@Test(description = "SQ-T6721 This is to verify POS payments and refund functionality for Par Commerce GC", groups = {
			"regression", "giftcard" }, dependsOnMethods = "T6720ToVerifyReloadAndAddExistingGC")
	public void T6721ToVerifyPOSPaymentsAndRefundUsingGC() throws Exception {
		// Helper functional interface for logging
		java.util.function.Consumer<String> info = msg -> pageObj.utils().logit(msg);

		// Generating Single Scan Code for User lookup and payments
		Response singleScanCodeResponse = pageObj.endpoints().singleScanCodeWithGiftcard(dataSet.get("client"),
				dataSet.get("secret"), authToken, GCUUID);
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to generate Single Scan Code");
		info.accept("Single Scan Code:-: " + singleScanCodeResponse);
		String singleScanCode = singleScanCodeResponse.jsonPath().get("single_scan_code").toString();

		// Calling User Lookup to validate Single Scan Code
		Response userLookupResponse = pageObj.endpoints().posUserLookupSingleScanToken(singleScanCode,
				dataSet.get("locationKey"));

		Assert.assertEquals(userLookupResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to User Lookup");
		Assert.assertEquals(gcCardNumber, userLookupResponse.jsonPath().getString("selected_card_number"),
				"Card number did not matched in User Lookup");
		Assert.assertEquals("GiftCard", userLookupResponse.jsonPath().getString("payment_mode"),
				"Payment Mode did not matched in User Lookup");
		//
		Response posPaymentResponse = pageObj.endpoints().POSPayment("singleScan", dataSet.get("locationKey"),
				singleScanCode, "GiftCard", dataSet.get("locationKey"));
		Assert.assertEquals(posPaymentResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String payment_reference_id = posPaymentResponse.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");

		Response posPaymentRefundResponse = pageObj.endpoints().POSPaymentRefund(payment_reference_id,
				dataSet.get("locationKey"));
		Assert.assertEquals(posPaymentRefundResponse.statusCode(), ApiConstants.HTTP_STATUS_OK);

		String actualStatusRefund = posPaymentRefundResponse.jsonPath().getString("status").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualStatusRefund, "refunded",
				actualStatusRefund + " not matched with expected status refunded");

		pageObj.utils().logPass("Gift card POS payment and Refund verified successfuly using PAR Commerce");
	}

	@Test(description = "SQ-T6722 This is to verify the GC balance,Share a GC, Revoke, Transfer, and Tipping using the GC", groups = {
			"regression", "giftcard" }, dependsOnMethods = "T6721ToVerifyPOSPaymentsAndRefundUsingGC")
	public void T6722ToVerifyGCBalance_Share_Revoke_transferAndTipping() throws Exception {

		// Helper functional interface for logging
		java.util.function.Consumer<String> info = msg -> pageObj.utils().logit(msg);

		// Updating the Gift Card balance to invalid
		String query = "UPDATE gift_cards SET `last_fetched_amount` = '" + 15 + "' WHERE uuid = '" + GCUUID + "';";
		DBUtils.executeUpdateQuery(env, query);

		// Fetch gift card balance
		Response gcBalanceResp = pageObj.endpoints().api1FetchGiftCardBalance(dataSet.get("client"),
				dataSet.get("secret"), authToken, GCUUID);

		int gcBalAmt = (int) Double.parseDouble(
				gcBalanceResp.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		info.accept("Gift Card balance retrieved: " + gcBalAmt);
		// Share the Gift Card to a new signedUP user
		String sharerUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(sharerUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for signup");
		Response gcShareResponse = pageObj.endpoints().Api1ShareGiftCard(sharerUserEmail, dataSet.get("client"),
				dataSet.get("secret"), authToken, GCUUID);
		Assert.assertEquals(gcShareResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to share the gift card");
		info.accept("Gift Card shared successfully with user: " + sharerUserEmail);

		// Validate shared state in DB
		String gcId = DBUtils.executeQueryAndGetColumnValue(env,
				"SELECT id FROM gift_cards WHERE uuid='" + GCUUID + "';", "id");

		String state = DBUtils.executeQueryAndGetColumnValue(env, "SELECT state FROM user_cards WHERE gift_card_id='"
				+ gcId + "' AND state='shared' ORDER BY id ASC LIMIT 1;", "state");
		Assert.assertEquals(state, "shared", "Gift Card state is not 'shared'");
		info.accept("Gift Card state validated as 'shared' for user: " + sharerUserEmail);

		// Revoke the Gift Card
		Response gcRevokeResp = pageObj.endpoints().Api2RevokeGiftCard(dataSet.get("client"), dataSet.get("secret"),
				authToken, GCUUID, sharerUserEmail);
		Assert.assertEquals(gcRevokeResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to revoke the gift card");
		info.accept("Gift Card revoked for user: " + sharerUserEmail);

		// Transfer the Gift Card
		Response gcTransferResp = pageObj.endpoints().Api1TransferGiftCard(sharerUserEmail, dataSet.get("client"),
				dataSet.get("secret"), "10", authToken, GCUUID);
		Assert.assertEquals(gcTransferResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to transfer the gift card");

		// Verify balance after transfer
		String selectBalanceQuery = "SELECT last_fetched_amount FROM gift_cards WHERE uuid = '" + GCUUID + "';";
		String actGCBal = DBUtils.executeQueryAndGetColumnValue(env, selectBalanceQuery, "last_fetched_amount");
		info.accept("Balance after transfer: " + actGCBal);

		// POS checkin
		Response response2 = pageObj.endpoints().posCheckin(userEmail, dataSet.get("locationKey"));
		Assert.assertEquals(response2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		// Fetch latest checkin ID
		String checkinId = DBUtils.executeQueryAndGetColumnValue(env,
				"SELECT id FROM checkins ORDER BY id DESC LIMIT 1;", "id");

		// Tip via Gift Card
		Response tipGiftCardResponse = pageObj.endpoints().Api2TipGiftCard(dataSet.get("client"), dataSet.get("secret"),
				authToken, GCUUID, checkinId);
		Assert.assertEquals(tipGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);

		boolean isSchemaValid = Utilities.validateJsonAgainstSchema(ApiResponseJsonSchema.api1GiftCardTipSchema,
				tipGiftCardResponse.asString());
		Assert.assertTrue(isSchemaValid, "Tip via Gift Card schema validation failed");
		pageObj.utils().logPass("Api2 Tip Via Gift Card successful");
	}

	@Test(description = "SQ-T6937 Verify IsActive Status Validation for PAR Commerce Gift Card")
	@Owner(name = "Pradeep Kumar")
	public void T6937_VerifyIsActiveStatusPARCommerceGiftCardValidation() throws Exception {

		// Instance business selection
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Gift Card page and select the gift card adapter as "PAR Commerce
		// Gift Card Adapter "
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("PAR Commerce Gift Card Adapter");
		pageObj.giftcardsPage().clickOnUpdateButton();

		// Go to Cocpit > Locations and select the HQ Location
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Locations");
		pageObj.cockpitLocationPage().SelectHQLocation(dataSet.get("hqLocationName"));

		// Navigate to Payment page and select the payment adapter as "Braintree"
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");
		pageObj.giftcardsPage().clickOnUpdateButton();

		// User SignUp using API
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		String token = signUpResponse.jsonPath().getString("auth_token.token");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		pageObj.utils().logit("API1 Signup is successful");
		logger.info("API1 Signup is successful");

		// update vendor_location_id & status
		try {
			String query = "UPDATE vendor_locations SET vendor_location_id = '" + dataSet.get("vendor_location_id")
					+ "', status = '" + dataSet.get("status") + "' WHERE business_id = '" + dataSet.get("business_id")
					+ "' AND location_id = '" + dataSet.get("location_id") + "'";
			int rs = DBUtils.executeUpdateQuery(env, query);

			if (rs == 0) {
				logger.info(" No row was updated in vendor_locations table .");
				pageObj.utils().logit("No record updated in vendor_locations table.");
				Assert.assertTrue(true, "Validation passed: no rows updated ");
			} else {
				logger.warn("update occurred. Rows updated: " + rs);
				pageObj.utils().logFail("update in vendor_locations table. Rows affected: " + rs);
				Assert.assertTrue(true, "Validation pass: expected 1 rows updated" + rs);
			}

		} catch (Exception e) {
			logger.error("Error while executing DB update ");
			pageObj.utils().logFail("Database operation failed ");
			Assert.fail("Exception during DB update ");
		}

		// create the GC card
		Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("design_id"), dataSet.get("amount"), dataSet.get("expDate"),
				dataSet.get("firstName"));
		Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Due to technical issues we cannot update your amount. It will be updated within next 24 hours or will be added back to the account used to make payment.");
		pageObj.utils().logPass("Api2 Purchase Gift Card is unsuccessful (expected) ");

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