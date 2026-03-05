package com.punchh.server.Integration1;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

// @Author = Shashank sharma
@Listeners(TestListeners.class)
public class Integration1 {
	static Logger logger = LogManager.getLogger(Integration1.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	private String timeStamp;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		utils = new Utilities(driver);
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

	// Author:- Shashank Sharma
	@Test(description = "SQ-T2936	[Heartland 2.0] Payment with Credit Card / transaction token", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T2936_VerifyCreditCardPaymentWithGiftCardPOSPayment() throws InterruptedException {
		int purchaseAmount = 15;
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");

		pageObj.giftcardsPage().selectGiftCardAdapter("Givex Gift Card Adapter");
		pageObj.giftcardsPage().clickOnUpdateButton();
		pageObj.giftcardsPage().selectPaymentAdapter("Heartland v2.0");

		Response singleTokenResponse_1 = pageObj.endpoints()
				.generateHeartlandPaymentToken(dataSet.get("authorizationKey"), dataSet.get("api_key"));
		String finalesingleScanToken_1 = singleTokenResponse_1.jsonPath().getString("token_value");
		logger.info("Heartland token One -- " + singleTokenResponse_1);
		TestListeners.extentTest.get().pass("Heartland token One -- " + singleTokenResponse_1);

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass(userEmail + " Api1 user signup is successful");

		String authToken = signUpResponse.jsonPath().get("auth_token.token");

		Response singleScanTokenResponse = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), authToken, "CreditCard", "transaction_token", finalesingleScanToken_1);

		Assert.assertEquals(singleScanTokenResponse.getStatusCode(), 200,
				"singleScanTokenResponse API response status code not matched");

		String single_scan_code = singleScanTokenResponse.jsonPath().getString("single_scan_code").replace("[", "")
				.replace("]", "");

		logger.info(single_scan_code + " single scan code generated ");
		TestListeners.extentTest.get().pass(single_scan_code + " single scan code generated ");

		Response userLookupResponse = pageObj.endpoints().posUserLookupFetchDetails(single_scan_code,
				dataSet.get("buregerMongerLocationKey"));

		Assert.assertEquals(userLookupResponse.getStatusCode(), 200, "userLookupResponse status code not matched");

		String actualPayment_mode = userLookupResponse.jsonPath().getString("payment_mode").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualPayment_mode, "CreditCard",
				actualPayment_mode + " payment mode not matched with CreditCard");
		logger.info(actualPayment_mode + " actual payment mode is matched in user lookup API ");
		TestListeners.extentTest.get().pass(actualPayment_mode + " actual payment mode is matched in user lookup API ");

		Response posPaymentResponse = pageObj.endpoints().POSPayment("singleScan", "", single_scan_code, "CreditCard",
				dataSet.get("buregerMongerLocationKey"));
		String payment_reference_id = posPaymentResponse.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");

		Response posPaymentRefundResponse = pageObj.endpoints().POSPaymentRefund(payment_reference_id,
				dataSet.get("buregerMongerLocationKey"));
		Assert.assertEquals(posPaymentRefundResponse.statusCode(), 200);

		System.out.println("posPaymentRefundResponse--" + posPaymentRefundResponse.asPrettyString());

		String posPaymentRefundStatus = posPaymentRefundResponse.jsonPath().getString("status").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(posPaymentRefundStatus, "refunded", "posPaymentRefundStatus not matched with refunded ");

		Response singleTokenResponse_2 = pageObj.endpoints()
				.generateHeartlandPaymentToken(dataSet.get("authorizationKey"), dataSet.get("api_key"));
		String finalesingleScanToken_2 = singleTokenResponse_2.jsonPath().getString("token_value");
		System.out.println("finalesingleScanToken1--" + finalesingleScanToken_2);

		Response giftCardPurcahseResponse = pageObj.endpoints().Api1PurchaseGiftCardWithSingleScanToken(
				dataSet.get("client"), dataSet.get("secret"), purchaseAmount + "", authToken, finalesingleScanToken_2);

		String UUID = giftCardPurcahseResponse.jsonPath().getString("uuid");
		// Step 1 - verified the amount

		int actualPurchaseAmount = (int) Double.parseDouble(
				giftCardPurcahseResponse.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));

		Assert.assertEquals(actualPurchaseAmount, purchaseAmount, "Actual purcahse amount " + actualPurchaseAmount
				+ " is not with expected purchase amount " + purchaseAmount);

	}

	// Author :- Shashank Sharma moved to SubscriptionWithHeartlandAdaptorTest class
	/*
	 * @Test(description =
	 * "SQ-T3584 [Recurring Payment] Recurring Payment with Gift Card and Pos Payments"
	 * )
	 * 
	 * public void T3584_VerifyRecurringPaymentWithGiftCardPosPayment() throws
	 * InterruptedException {
	 * 
	 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	 * 
	 * pageObj.instanceDashboardPage().loginToInstance();
	 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
	 * 
	 * pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
	 * 
	 * pageObj.giftcardsPage().selectGiftCardAdapter("Givex Gift Card Adapter");
	 * pageObj.giftcardsPage().clickOnUpdateButton();
	 * pageObj.giftcardsPage().selectPaymentAdapter("Heartland v2.0");
	 * 
	 * Response singleTokenResponse_1 = pageObj.endpoints()
	 * .generateHeartlandPaymentTokenPolling(dataSet.get("authorizationKey"),
	 * dataSet.get("api_key"));
	 * Assert.assertEquals(singleTokenResponse_1.statusCode(), 201,
	 * "Not able to generate single Token for heartland adaptor"); String
	 * finalesingleScanToken =
	 * singleTokenResponse_1.jsonPath().getString("token_value");
	 * logger.info("Heartland token One -- " + finalesingleScanToken);
	 * TestListeners.extentTest.get().pass("Heartland token One -- " +
	 * finalesingleScanToken); if (finalesingleScanToken.contains("io.restassured"))
	 * {
	 * 
	 * logger.warn("Heartland token is not generated");
	 * TestListeners.extentTest.get().warning("Heartland token is not generated"); }
	 * 
	 * userEmail = pageObj.iframeSingUpPage().generateEmail(); Response
	 * signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail,
	 * dataSet.get("client"), dataSet.get("secret"));
	 * Assert.assertEquals(signUpResponse.getStatusCode(), 200,
	 * "Status code 200 did not matched for api1 signup");
	 * TestListeners.extentTest.get().pass(userEmail +
	 * " Api1 user signup is successful"); String authToken =
	 * signUpResponse.jsonPath().get("auth_token.token");
	 * 
	 * Response paymentCardResponse =
	 * pageObj.endpoints().POSPaymentCard(dataSet.get("client"),
	 * dataSet.get("secret"), authToken, finalesingleScanToken);
	 * Assert.assertEquals(paymentCardResponse.getStatusCode(), 200,
	 * "Failed POS payment api");
	 * 
	 * String uuidPaymentCard =
	 * paymentCardResponse.jsonPath().getString("uuid").replace("[",
	 * "").replace("]", "");
	 * 
	 * Response giftCardPurcahseResponse =
	 * pageObj.endpoints().Api1PurchaseGiftCardWithTransactionID(
	 * dataSet.get("client"), dataSet.get("secret"), "15", authToken,
	 * uuidPaymentCard);
	 * 
	 * Assert.assertEquals(giftCardPurcahseResponse.statusCode(), 200,
	 * "Failed to purchase gift card giftCardPurcahseResponse"); String
	 * uuidGiftCardPurcahse =
	 * giftCardPurcahseResponse.jsonPath().getString("uuid").replace("[", "")
	 * .replace("]", "");
	 * 
	 * Response reloadGiftCardResp =
	 * pageObj.endpoints().Api1ReloadGiftCardRecurring(userEmail,
	 * dataSet.get("client"), dataSet.get("secret"), "15", authToken,
	 * uuidGiftCardPurcahse, uuidPaymentCard);
	 * Assert.assertEquals(reloadGiftCardResp.statusCode(), 200,
	 * "Failed to relaod gift card");
	 * 
	 * // Single scan Token Response singleScanTokenResponse =
	 * pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
	 * dataSet.get("secret"), authToken, "recurring", "transaction_token",
	 * uuidPaymentCard);
	 * Assert.assertEquals(singleScanTokenResponse.getStatusCode(), 200,
	 * "Status code 200 did not matched for the single scan code API");
	 * TestListeners.extentTest.get().pass("single Scan Code successful");
	 * logger.info("single Scan Code is successful"); String singleScanCode1 =
	 * singleScanTokenResponse.jsonPath().getString("single_scan_code");
	 * logger.info("Mobile single scan code is " + singleScanCode1);
	 * TestListeners.extentTest.get().pass("Mobile single scan code is " +
	 * singleScanCode1);
	 * 
	 * Response posPaymentResponse = pageObj.endpoints().POSPayment("singleScan",
	 * userEmail, singleScanCode1, "recurring", dataSet.get("locationKey"));
	 * Assert.assertEquals(posPaymentResponse.statusCode(), 200);
	 * 
	 * String payment_reference_id =
	 * posPaymentResponse.jsonPath().getString("payment_reference_id").replace("[",
	 * "") .replace("]", "");
	 * 
	 * Response posPaymentRefundResponse =
	 * pageObj.endpoints().POSPaymentRefund(payment_reference_id,
	 * dataSet.get("locationKey"));
	 * Assert.assertEquals(posPaymentRefundResponse.statusCode(), 200);
	 * 
	 * String actualStatusRefund =
	 * posPaymentRefundResponse.jsonPath().getString("status").replace("[", "")
	 * .replace("]", ""); Assert.assertEquals(actualStatusRefund, "refunded",
	 * actualStatusRefund + " not matched with expected status refunded");
	 * 
	 * }
	 */

	// Author - Shashank Sharma //As discussed with @Rohit, paypal credential has
	// been chagned , and this will remove in future .
	// @Test(description = "SQ-T2943 - [PaypalBA] Verify PaypalBA payment with POS
	// API")
	public void T2943_VerifyPaypalBAPaymentWithPOSAPI() throws InterruptedException {

		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Paypal?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass(userEmail + " Api1 user signup is successful");

		String authToken = signUpResponse.jsonPath().get("auth_token.token");

		Response paymentAgreementTokenResponse = pageObj.endpoints().paymentAgreementTokenAPI(dataSet.get("client"),
				dataSet.get("secret"), authToken);

		String paypallink = paymentAgreementTokenResponse.jsonPath().getString("links[0].href").replace("[", "")
				.replace("]", "").toString();
		TestListeners.extentTest.get().pass(paypallink + " link is captured from paymentAgreementTokenAPI");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(paypallink);

		pageObj.payWithPayPalPage().loginToPayPalPage("rohit+pal@punchh.com", "Punchh@123");

		pageObj.payWithPayPalPage().clickOnSaveAndContinueButton();

		pageObj.payWithPayPalPage().verifySlugNameIsVisible("Burger Monger");

		Response posPaymentResponse = pageObj.endpoints().POSPayment("email", userEmail, "", "PaypalBA",
				dataSet.get("burgerMongerLocationKey"));
		Assert.assertEquals(posPaymentResponse.statusCode(), 200);

		String actualStatus = posPaymentResponse.jsonPath().getString("status").replace("[", "").replace("]", "");
		Assert.assertEquals(actualStatus, "success", "POS payment status not matched ");

		String actualPosPaymentType = posPaymentResponse.jsonPath().getString("payment_type").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualPosPaymentType, "PaypalBA", "PaypalBA payment type is not matched in response");

		String payment_reference_id = posPaymentResponse.jsonPath().getString("payment_reference_id").replace("[", "")
				.replace("]", "");

		Response responsePOSPaymentStatus = pageObj.endpoints().POSPaymentStatus(payment_reference_id,
				dataSet.get("burgerMongerLocationKey"));

		Assert.assertEquals(responsePOSPaymentStatus.getStatusCode(), 200);

		String posPaymentStatus_Actual = responsePOSPaymentStatus.jsonPath().getString("status").replace("[", "")
				.replace("]", "");

		Assert.assertEquals(posPaymentStatus_Actual, "success", "Sucess status not coming ");

		Response posPaymentUpdateRepsonse = pageObj.endpoints().POSPaymentPUT(payment_reference_id,
				dataSet.get("burgerMongerLocationKey"), dataSet.get("statusToUpdate"));
		Assert.assertEquals(posPaymentUpdateRepsonse.getStatusCode(), 200);

		String actualStatusposPaymentUpdateRepsonse = posPaymentUpdateRepsonse.jsonPath().getString("status")
				.replace("[", "").replace("]", "");
		Assert.assertEquals(actualStatusposPaymentUpdateRepsonse, "processed",
				actualStatusposPaymentUpdateRepsonse + " not matched with expected status processed");

		Response posPaymentRefundResponse = pageObj.endpoints().POSPaymentRefund(payment_reference_id,
				dataSet.get("burgerMongerLocationKey"));
		Assert.assertEquals(posPaymentRefundResponse.statusCode(), 200);

		String actualStatusRefund = posPaymentRefundResponse.jsonPath().getString("status").replace("[", "")
				.replace("]", "");
		Assert.assertEquals(actualStatusRefund, "refunded",
				actualStatusRefund + " not matched with expected status refunded");

	}

	// @Author = Shashank Sharma
	@Test(description = "SQ-T3547 - [E2E - ISL Receipt] Mobile Checkin Flow with ISL Receipt and Post Checkin Campaign", groups = {"nonNightly"})
	@Owner(name = "Shashank Sharma")
	public void T3547_ISL_MobileCheckinWithISLReceiptAndPostCheckin() throws Exception {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Support", "Test Barcodes");
		pageObj.instanceDashboardPage().generateBarcode(dataSet.get("location"));
		String barcode = pageObj.instanceDashboardPage().captureBarcode();
		logger.info(barcode + " barcode is generated ");
		TestListeners.extentTest.get().pass(barcode + " barcode is generated ");
		String getPunchKeyQuery = "select code,punchh_key from receipt_store_production.receipts where code=\""
				+ barcode + "\";";

		String punchhKey = DBUtils.executeQueryAndGetColumnValue(env, getPunchKeyQuery, "punchh_key");
		// dbUtils.executeQueryForPunchhDatabase("receipt_store_production", env,
		// getPunchKeyQuery,"punchh_key");
		String deleteQuery = "DELETE FROM receipt_store_production.receipts WHERE code=\"" + barcode + "\";";
		DBUtils.executeUpdateQuery(env, deleteQuery);
		// dbUtils.executeDeleteForPunchhDatabase("receipt_store_production", env,
		// deleteQuery);

		Response ISLreceiptResponse = pageObj.endpoints().ISLReceiptDetailsAPI(dataSet.get("locationKey"), punchhKey);
		Assert.assertEquals(ISLreceiptResponse.statusCode(), 200, "ISL Receipt API not working ");

		pageObj.menupage().navigateToSubMenuItem("Support", "Barcode Lookup");

		String newPunchhBarcode = punchhKey + 1;
		pageObj.barcodelookup().selectLocation("Any");

		pageObj.barcodelookup().enterBarcode(newPunchhBarcode);

		pageObj.barcodelookup().clickOnSubmitButton();

		String actualReceiptBarcode = pageObj.barcodelookup().getLookupReceiptDetailsBarcode();
		actualReceiptBarcode = actualReceiptBarcode.substring(1, actualReceiptBarcode.length());

		Assert.assertEquals(actualReceiptBarcode, punchhKey,
				punchhKey + " is not coming in lookup receipt details page .");

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass(userEmail + " Api1 user signup is successful");

		String authToken = signUpResponse.jsonPath().get("auth_token.token");
		String userId = signUpResponse.jsonPath().get("user_id").toString();

		Response barcodeCheckinResponse = pageObj.endpoints().Api1LoyaltyCheckinBarCode(dataSet.get("client"),
				dataSet.get("secret"), authToken, newPunchhBarcode);
		TestListeners.extentTest.get().info("barcodeCheckinResponse--" + barcodeCheckinResponse.asPrettyString());

		Assert.assertEquals(barcodeCheckinResponse.getStatusCode(), 201,
				"Status code 201 did not matched for api2 Loyalty Checkin by bar code");

		pageObj.menupage().navigateToSubMenuItem("Support", "Barcode Lookup");

		// again do the barcode lookup
		pageObj.barcodelookup().selectLocation("Any");

		pageObj.barcodelookup().enterBarcode(newPunchhBarcode);

		pageObj.barcodelookup().clickOnSubmitButton();

		boolean isGreenColourBGVisible = pageObj.barcodelookup().verifyTheColour(userId, "green-background");
		Assert.assertTrue(isGreenColourBGVisible, " Expected Green colour background NOT matched");

		boolean item1 = pageObj.barcodelookup().verifyReceiptItems("Crispy Taco2");
		Assert.assertTrue(item1, "Crispy Taco2 not coming in receipt");

		boolean item2 = pageObj.barcodelookup().verifyReceiptItems("SoftShell Chk");
		Assert.assertTrue(item2, "SoftShell Chk not coming in receipt");

		boolean item3 = pageObj.barcodelookup().verifyReceiptItems("Quesadilla Chk");
		Assert.assertTrue(item3, "Quesadilla Chk not coming in receipt");

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);

		String approvedLoayltyText = pageObj.guestTimelinePage().getApprovedLoyalty();

		Assert.assertTrue(approvedLoayltyText.contains(newPunchhBarcode));

	}

	// Author : Shashank Sharma
	@Test(description = "SQ-T2946 [Attentive] Verify the Text to Join Flow with Prefilled Iframe URL", groups = {
			"regression", "unstable", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T2946_VerifyTextToJoinFlowWithPrefilledIframeURL() throws InterruptedException {

		String phoneNumber = Integer.toString(Utilities.getRandomNoFromRange(1, 999999999)) + 1;
		String fName = "iframeuserfname" + CreateDateTime.getTimeDateString();
		String emailID = fName + "@punchh.com";
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable SMS?", "check");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Text to Join?", "check");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Custom Profile Fields?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Campaigns");
		pageObj.dashboardpage().smsAdapter("Attentive");
		pageObj.dashboardpage().clickOnUpdateButton();

		utils.longWaitInSeconds(4);
		Response response = pageObj.endpoints().WebHookAttentiveAPI(dataSet.get("client"), dataSet.get("secret"),
				phoneNumber, fName);
		Assert.assertEquals(response.getStatusCode(), 202, "Having some problem in Web hook API response code");

		Response iframePrifilledURLResponse = pageObj.endpoints().iFramePrefilledURL(dataSet.get("adminKey"),
				phoneNumber, fName);

		String prefilledURL = iframePrifilledURLResponse.jsonPath().getString("url");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(prefilledURL);

		pageObj.iframeSingUpPage().verifyUserPrefilledDetails(phoneNumber, fName);
		int currentPoints = pageObj.iframeSingUpPage().getCurrentPointAfterSignupForPrefilledURL(emailID);
		Assert.assertEquals(currentPoints, 2, currentPoints + " Current points did not matched with expected 2");
		logger.info("Current Point 2 is matched ");
		TestListeners.extentTest.get().pass("Current Point 2 is matched ");

	}

	// Author : Shashank Sharma - on hold
	// @Test(description = "SQ-T2946 [Attentive] Verify the Text to Join Flow with
	// Prefilled Iframe URL")
	public void T2946_VerifyTextToJoinFlowWithPrefilledIframeURL_MobivityAPI() throws InterruptedException {

		String phoneNumber = Integer.toString(Utilities.getRandomNoFromRange(1, 999999999)) + 1;
		String fName = "iframeuserfname" + CreateDateTime.getTimeDateString();
		String emailID = fName + "@punchh.com";
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable SMS?", "check");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Text to Join?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();

		Response response = pageObj.endpoints().WebHookAttentiveAPI(dataSet.get("client"), dataSet.get("secret"),
				phoneNumber, fName);
		Assert.assertEquals(response.getStatusCode(), 202, "Having some problem in Web hook API response code");

		Response iframePrifilledURLResponse = pageObj.endpoints().iFramePrefilledURL(dataSet.get("adminKey"),
				phoneNumber, fName);

		String prefilledURL = iframePrifilledURLResponse.jsonPath().getString("url");

		pageObj.instanceDashboardPage().navigateToPunchhInstance(prefilledURL);

		pageObj.iframeSingUpPage().verifyUserPrefilledDetails(phoneNumber, fName);
		int currentPoints = pageObj.iframeSingUpPage().getCurrentPointAfterSignupForPrefilledURL(emailID);
		Assert.assertEquals(currentPoints, 2, currentPoints + " Current points did not matched with expected 2");
		logger.info("Current Point 2 is matched ");
		TestListeners.extentTest.get().pass("Current Point 2 is matched ");

	}

	@Test(description = "SQ-T4518 Verify that the active subscription schedule should not run when the subscriptions for business is deactivated", groups = {
			"regression", "dailyrun" })
	@Owner(name = "Shashank Sharma")
	public void T4518_VerifyActiveSubscriptionScheduleNotVisibleBasedOnSubscriptionFlag() throws InterruptedException {
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Subscriptions?", "uncheck");
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		boolean schedulerIsExist = pageObj.schedulespage().isScheduleTypeExist("Active Subscriber Count Schedule");
		Assert.assertFalse(schedulerIsExist, "Active Subscriber Count Schedule is  displayed");
		logger.info("Verified that  if Enable Subscriptions flag is Unchecked");
		TestListeners.extentTest.get().pass("Verified that subscription renewal not happen for deactivated user");

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Subscriptions?", "check");
		pageObj.dashboardpage().clickOnUpdateButton();
		pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
		schedulerIsExist = pageObj.schedulespage().isScheduleTypeExist("Active Subscriber Count Schedule");
		Assert.assertTrue(schedulerIsExist, "Active Subscriber Count Schedule is not displayed");
		logger.info("Verified that subscription renewal not happen for deactivated user");
		TestListeners.extentTest.get().pass("Verified that subscription renewal not happen for deactivated user");

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
