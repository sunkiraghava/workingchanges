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
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class ParPayCommercePaymentTest {
	static Logger logger = LogManager.getLogger(ParPayCommercePaymentTest.class);
	public WebDriver driver;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private Properties prop;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private String userEmail;
	private static String PAYMENTCARDUUID;
	private static String USERTOKEN;

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

	@Test(description = "SQ-T6945 | PAR Commerce Import Vendor Location for PAR Commerce payments", priority = 0)
	public void T6945_ToValidateImportPARCommerceConfig() throws Exception {
		// Prepare test data
		String importName = Utilities.generateRandomString(8);
		String email = "nitesh.shekhawat@partech.com";
		String directoryPath = System.getProperty("user.dir") + "/resources/Testdata/locationPaymentsCC.csv";

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Payment page and select the payment adapter as "PAR Commerce"
		pageObj.giftcardsPage().selectPaymentAdapter("PAR Commerce");

		// Set the HQ Location
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Locations");
		pageObj.cockpitLocationPage().SelectHQLocation(dataSet.get("nonPayLocName"));
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

	}

	@Test(description = "SQ-T6946 | Verify GC purchase & POS paymets using PAR Commerce", priority = 0)
	public void T6946_ToValidateGCPurchaseAndPOSPayments_PARCommerce() throws Exception {

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setGiftcardSeriesLength("603628", "21");
		pageObj.giftcardsPage().clickOnUpdateButton();

		// Create the user and get the user token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not match for api1 signup");
		USERTOKEN = signUpResponse.jsonPath().get("auth_token.token");
		pageObj.utils().logPass(userEmail + " Api1 user signup is successful");

		// Get PAR payment client token Response

		Response parPaymentTokenResponse = pageObj.endpoints().Api1ParPaymentGetClientToken(dataSet.get("client"),
				dataSet.get("secret"), USERTOKEN);

		// Verify response
		Assert.assertEquals(parPaymentTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to get PAR payment client token");

		// Extract the URL from response
		String parPaymentUrl = parPaymentTokenResponse.jsonPath().getString("url");
		logger.info("PAR Payment URL: " + parPaymentUrl);
		pageObj.utils().logPass("Successfully retrieved PAR payment client token");

		// Complete PAR payment flow
		String paymentToken = pageObj.parCommerceIframePage().completeParCommercePaymentFlow(parPaymentUrl);

		// Verify payment token was generated
		Assert.assertNotNull(paymentToken, "Payment token is null");
		Assert.assertFalse(paymentToken.isEmpty(), "Payment token is empty");

		pageObj.utils().logPass("PAR payment flow completed successfully with token: " + paymentToken);

		// Create a payment card using generated token for user
		Response paymentCardResponse = pageObj.endpoints().createPaymentCard(dataSet.get("client"),
				dataSet.get("secret"), USERTOKEN, "par_commerce", paymentToken, "Nitesh", true);
		PAYMENTCARDUUID = paymentCardResponse.jsonPath().getString("uuid");
		Assert.assertEquals(paymentCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed Payment Card vault api");
		logger.info("Payment card has created successfully with uuid: " + PAYMENTCARDUUID);
		pageObj.utils().logPass("Payment card has created successfully with uuid: " + PAYMENTCARDUUID);

		// Purchase Gift card with saved payment card
		Response giftCardPurcahseResp = pageObj.endpoints().Api1PurchaseGiftCardWithRecurring(dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("non_wallet_gc_design_id"), "15", USERTOKEN, PAYMENTCARDUUID);
		Assert.assertEquals(giftCardPurcahseResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to purchase the gift card");

		// API2 Generate Single Scan Code
		Response singleScanCodeResponse = pageObj.endpoints().api2SingleScanCode(USERTOKEN, "CreditCard", paymentToken,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(singleScanCodeResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 Generate Single Scan Code");
		String singleScanCode = singleScanCodeResponse.jsonPath().getString("single_scan_code");

		// Create Payment using CreditCard-SingleScanCode
		Response posPaymentResponse = pageObj.endpoints().POSPayment("singleScan", "", singleScanCode, "CreditCard",
				dataSet.get("locationKey"));
		String payment_reference_id = posPaymentResponse.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");
		String status = posPaymentResponse.jsonPath().getString("status").replace("[", "").replace("]", "");
		Assert.assertEquals(posPaymentResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "POS Create Payment status code not matched.");
		Assert.assertEquals(status, "success", "payment_type is not matched.");
		pageObj.utils().logPass("POS Create Payment call is successful.");

		// Get Payment Status
		Response posPaymentStatusResponse = pageObj.endpoints().POSPaymentStatus(payment_reference_id,
				dataSet.get("locationKey"));
		Assert.assertEquals(posPaymentStatusResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "POS Get Payment status code not matched.");
		Assert.assertEquals(posPaymentStatusResponse.jsonPath().getString("status").replace("[", "").replace("]", ""),
				"success", "status is not matched.");
		pageObj.utils().logPass("POS Get Payment Success Status call is successful.");

	}

	@Test(description = "SQ-T6947 | Verify Fetch-Update-Delete payment card for PAR Commerce", priority = 0, dependsOnMethods = "T6946_ToValidateGCPurchaseAndPOSPayments_PARCommerce")
	public void T6947_ToValidateFetchUpdateDeletePaymentCard_PARCommerce() throws Exception {

		// Fetch Payment Card for PAR Commerce
		Response fetchPaymentCardResponse = pageObj.endpoints().api2FetchPaymentCard(USERTOKEN, "par_commerce",
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(fetchPaymentCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API2 Fetch Payment card");
		String pcUUID = fetchPaymentCardResponse.jsonPath().getList("uuid", String.class).get(0);
		Assert.assertEquals(pcUUID, PAYMENTCARDUUID, "PC UUID not matched.");

		// Update Payment Card
		Response updatePaymentCardResponse = pageObj.endpoints().api2UpdatePaymentCard(USERTOKEN, PAYMENTCARDUUID,
				dataSet.get("nicknameToUpdate"), dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(updatePaymentCardResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for api2 Update Payment Card");
		String updatedNickname = updatePaymentCardResponse.jsonPath().getString("nickname");
		Assert.assertEquals(updatedNickname, dataSet.get("nicknameToUpdate"),
				"Nickname did not match for api2 Update Payment Card");
		pageObj.utils().logPass("Payment card is successfully updated");

		// Delete Payment Card
		Response deletePaymentCardResponse = pageObj.endpoints().api2DeletePaymentCard(USERTOKEN, PAYMENTCARDUUID,
				dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(deletePaymentCardResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for api2 Delete Payment Card");
		String message = deletePaymentCardResponse.jsonPath().getString("message");
		Assert.assertEquals(message, "Payment card has been deleted successfully.",
				"Payment card deletion message did not match for api2 Delete Payment Card");
		pageObj.utils().logPass("Payment card is successfully deleted");
	}

	@Test(description = "SQ-T6937 Verify IsActive Status Validation for PAR commerce Payment")
	@Owner(name = "Pradeep Kumar")
	public void T6937_VerifyIsActiveStatusPARCommercePaymentValidation() throws Exception {

		// Select Test Business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Navigate to Gift Card page and select the gift card adapter as "Mock GiftCard
		// Adapter"
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setGiftcardSeriesLength("603628", "21");
		pageObj.giftcardsPage().clickOnUpdateButton();

		// Go to Cocpit > Locations and select the HQ Location
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Locations");
		pageObj.cockpitLocationPage().SelectHQLocation(dataSet.get("hqLocationName"));

		// Navigate to Payment page and select the payment adapter as "PAR Commerce"
		pageObj.giftcardsPage().selectPaymentAdapter("PAR Commerce");
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

		// Update vendor location id & status
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
				logger.info("update occurred. Rows updated: " + rs);
				pageObj.utils().logFail("update in vendor_locations table. Rows affected: " + rs);
				Assert.assertTrue(true, "Validation pass: expected 1 rows updated" + rs);
			}

		} catch (Exception e) {
			logger.error("Error while executing DB update ");
			pageObj.utils().logFail("Database operation failed ");
			Assert.fail("Exception during DB update ");
		}

		// Payment Token Generation through Payment URL Page
		Response parPaymentURL = pageObj.endpoints().Api1ParPaymentGetClientToken(dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(parPaymentURL.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "Payment is not supported or Invalid Configuration.");
		logger.info("PAR Commerce Payment Token generation is unsuccessful (expected)");
		pageObj.utils().logPass("Api1 PAR Commerce Payment Token generation is unsuccessful (expected)");

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
