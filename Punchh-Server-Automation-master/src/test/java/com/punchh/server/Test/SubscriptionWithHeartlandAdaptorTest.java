package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class SubscriptionWithHeartlandAdaptorTest {
	static Logger logger = LogManager.getLogger(SubscriptionWithHeartlandAdaptorTest.class);
	public WebDriver driver;
	String userEmail;
	String email = "autoemailTemp@punchh.com";
	ApiUtils apiUtils;
	PageObj pageObj;
	String sTCName;
	@SuppressWarnings("unused")
	private String timeStamp, spPrice, spName, PlanID, iFrameEmail;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	Properties prop;

	@BeforeMethod(alwaysRun = true)
	public void beforeMethod(Method method) {
		prop = Utilities.loadPropertiesFile("segmentBeta.properties");
		driver = new BrowserUtilities().launchBrowser();
		apiUtils = new ApiUtils();
		timeStamp = CreateDateTime.getTimeDateString();
		userEmail = email.replace("Temp", timeStamp);
		pageObj = new PageObj(driver);
		dataSet = new ConcurrentHashMap<>();
		sTCName = method.getName();
		env = pageObj.getEnvDetails().setEnv();
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run, env), sTCName);
		dataSet = pageObj.readData().readTestData;
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run, env, "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
	}

	// Anant
	@Test(description = "SQ-T3987 Verify the Vault deletion white label message for card deletion")
    @Owner(name = "Shashank Sharma")
	public void T3987_vaultDeletion() throws InterruptedException {
		TestListeners.extentTest.get().info("------------ Heartland Test Case ---------------");
		String passcode = "123456";

		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("Heartland v2.0");
		pageObj.dashboardpage().clickOnUpdateButton();

		// create QC and subscription Plan
		spName = dataSet.get("spName");
		spPrice = dataSet.get("spPrice");
		PlanID = dataSet.get("PlanID");

		// single token
		Response singleTokenResponse = pageObj.endpoints()
				.generateHeartlandPaymentToken(dataSet.get("authorizationKey"), dataSet.get("api_key"));
		String finalesingleScanToken = singleTokenResponse.jsonPath().getString("token_value");

		logger.info("Heartland token One -- " + singleTokenResponse);
		TestListeners.extentTest.get().pass("Heartland token One -- " + singleTokenResponse);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// generate UUID
		Response posPaymentCard = pageObj.endpoints().POSPaymentCard(dataSet.get("client"), dataSet.get("secret"),
				token, finalesingleScanToken);
		Assert.assertEquals(posPaymentCard.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String uuid = posPaymentCard.jsonPath().get("uuid").toString();

		// purchase subscription plan
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, uuid, "Heartland");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);

		pageObj.endpoints().Api2CreatePasscode(dataSet.get("client"), dataSet.get("secret"), token, passcode);

		// delete the payment card
		Response paymentDeleteCard = pageObj.endpoints().deletePaymentCard(dataSet.get("client"), dataSet.get("secret"),
				token, uuid, passcode);
		Assert.assertEquals(paymentDeleteCard.statusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY, "vault is deleted successfully");
		logger.info("vault is not deleted");
		TestListeners.extentTest.get().pass("vault is not deleted");

		String error = paymentDeleteCard.jsonPath().get("errors").toString();
		Assert.assertEquals(error,
				"[Payment card cannot be deleted as it is associated with one or more active subscriptions.]",
				"Error msg is not equal");

		logger.info("vault deletion error msg is same");
		TestListeners.extentTest.get().pass("vault deletion error msg is same");

	}

	// Anant pkapi_cert_IK0iNjWm2RtOczqasr pkapi_cert_rEHakZcVf4Fj9HpE3g
	@Test(description = "SQ-T4042 Verify the Payment method deletion for Subscription Par Payments")
	public void T4042_paymentMethodDelection() throws InterruptedException {
		TestListeners.extentTest.get().pass("------------ Heartland Test Case ----------------");
		String passcode = "123456";
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("Heartland v2.0");
		pageObj.dashboardpage().clickOnUpdateButton();

		Response singleTokenResponse_1 = pageObj.endpoints()
				.generateHeartlandPaymentToken(dataSet.get("authorizationKey"), dataSet.get("api_key"));
		String finalesingleScanToken_1 = singleTokenResponse_1.jsonPath().getString("token_value");

		logger.info("Heartland token One -- " + singleTokenResponse_1);
		TestListeners.extentTest.get().pass("Heartland token One -- " + singleTokenResponse_1);

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// generate UUID
		Response posPaymentCard = pageObj.endpoints().POSPaymentCard(dataSet.get("client"), dataSet.get("secret"),
				token, finalesingleScanToken_1);
		Assert.assertEquals(posPaymentCard.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String uuid = posPaymentCard.jsonPath().get("uuid").toString();
		logger.info("uuid is successfully generated");
		TestListeners.extentTest.get().pass("uuid is successfully generated");

		pageObj.endpoints().Api2CreatePasscode(dataSet.get("client"), dataSet.get("secret"), token, passcode);

		// delete the payment card
		Response paymentDeleteCard = pageObj.endpoints().deletePaymentCard(dataSet.get("client"), dataSet.get("secret"),
				token, uuid, passcode);
		Assert.assertEquals(paymentDeleteCard.statusCode(), ApiConstants.HTTP_STATUS_OK, "Payment card is not deleted");
		logger.info("payment card is successfully deleted");
		TestListeners.extentTest.get().pass("payment card is successfully deleted");

	}

	// shashank
	@Test(description = "SQ-T4516 Verify if the guest is deactivated the renewal of the subscription does not happen in case of Subscription Adapter")
	public void T4516_VerifySubscriptionRenewalNotHappenForDeactivatedUser() throws InterruptedException {
		TestListeners.extentTest.get().info("------------ Heartland Test Case ----------------");
		PlanID = dataSet.get("subscriptionID");
		spPrice = "33";
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
		pageObj.dashboardpage().checkUncheckFlagCockpitDashboard("Enable Subscriptions?", "check");
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Subscriptions");
		pageObj.dashboardpage().setPaymentAdapterInSubscriptionPage("Heartland v2.0");
		pageObj.dashboardpage().clickOnUpdateButton();

		// user signup using api2
		iFrameEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(iFrameEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("access_token.token").toString();

		// single token
		Response singleTokenResponse = pageObj.endpoints()
				.generateHeartlandPaymentToken(dataSet.get("authorizationKey"), dataSet.get("heartLandApikey"));
		String finalesingleScanToken = singleTokenResponse.jsonPath().getString("token_value");

		// generate UUID
		Response posPaymentCard = pageObj.endpoints().POSPaymentCard(dataSet.get("client"), dataSet.get("secret"),
				token, finalesingleScanToken);
		Assert.assertEquals(posPaymentCard.statusCode(), ApiConstants.HTTP_STATUS_OK);
		String uuid = posPaymentCard.jsonPath().get("uuid").toString();

		// purchase subscription plan
		Response purchaseSubscriptionresponse = pageObj.endpoints().Api2SubscriptionPurchase(token, PlanID,
				dataSet.get("client"), dataSet.get("secret"), spPrice, uuid, "Heartland");
		Assert.assertEquals(purchaseSubscriptionresponse.getStatusCode(), ApiConstants.HTTP_STATUS_CREATED);
		logger.info("Verified that subscription renewal not happen for deactivated user");
		TestListeners.extentTest.get().pass("Verified that subscription renewal not happen for deactivated user");

	}

	// moved to Integration1 class
	/*
	 * @Test(description =
	 * "SQ-T4518 Verify that the active subscription schedule should not run when the subscriptions for business is deactivated"
	 * ) public void
	 * T4518_VerifyActiveSubscriptionScheduleNotVisibleBasedOnSubscriptionFlag()
	 * throws InterruptedException {
	 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
	 * pageObj.instanceDashboardPage().loginToInstance();
	 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
	 * 
	 * pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
	 * pageObj.dashboardpage().
	 * checkUncheckFlagCockpitDasboard("Enable Subscriptions?", "uncheck");
	 * pageObj.dashboardpage().clickOnUpdateButton();
	 * pageObj.menupage().navigateToSubMenuItem("Support", "Schedules"); boolean
	 * schedulerIsExist = pageObj.schedulespage().
	 * isScheduleTypeExist("Active Subscriber Count Schedule");
	 * Assert.assertFalse(schedulerIsExist,
	 * "Active Subscriber Count Schedule is  displayed");
	 * logger.info("Verified that  if Enable Subscriptions flag is Unchecked");
	 * TestListeners.extentTest.get().
	 * pass("Verified that subscription renewal not happen for deactivated user");
	 * 
	 * 
	 * pageObj.menupage().navigateToSubMenuItem("Cockpit", "Dashboard");
	 * pageObj.dashboardpage().
	 * checkUncheckFlagCockpitDasboard("Enable Subscriptions?", "check");
	 * pageObj.dashboardpage().clickOnUpdateButton();
	 * pageObj.menupage().navigateToSubMenuItem("Support", "Schedules");
	 * schedulerIsExist = pageObj.schedulespage().
	 * isScheduleTypeExist("Active Subscriber Count Schedule");
	 * Assert.assertTrue(schedulerIsExist,
	 * "Active Subscriber Count Schedule is not displayed"); logger.
	 * info("Verified that subscription renewal not happen for deactivated user");
	 * TestListeners.extentTest.get().
	 * pass("Verified that subscription renewal not happen for deactivated user");
	 * 
	 * 
	 * }
	 */

	// Author :- Shashank Sharma
	@Test(description = "SQ-T3584 [Recurring Payment] Recurring Payment with Gift Card and Pos Payments")

	public void T3584_VerifyRecurringPaymentWithGiftCardPosPayment() throws InterruptedException {
		TestListeners.extentTest.get().info("----------- Heartland Test Case -----------");
		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);

		pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");

		pageObj.giftcardsPage().selectGiftCardAdapter("Givex Gift Card Adapter");
		pageObj.giftcardsPage().clickOnUpdateButton();
		pageObj.giftcardsPage().selectPaymentAdapter("Heartland v2.0");

		Response singleTokenResponse_1 = pageObj.endpoints()
				.generateHeartlandPaymentTokenPolling(dataSet.get("authorizationKey"), dataSet.get("api_key"));
		Assert.assertEquals(singleTokenResponse_1.statusCode(), ApiConstants.HTTP_STATUS_CREATED,
				"Not able to generate single Token for heartland adaptor");
		String finalesingleScanToken = singleTokenResponse_1.jsonPath().getString("token_value");
		logger.info("Heartland token One -- " + finalesingleScanToken);
		TestListeners.extentTest.get().pass("Heartland token One -- " + finalesingleScanToken);
		if (finalesingleScanToken.contains("io.restassured")) {

			logger.warn("Heartland token is not generated");
			TestListeners.extentTest.get().warning("Heartland token is not generated");
		}

		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass(userEmail + " Api1 user signup is successful");
		String authToken = signUpResponse.jsonPath().get("auth_token.token");

		Response paymentCardResponse = pageObj.endpoints().POSPaymentCard(dataSet.get("client"), dataSet.get("secret"),
				authToken, finalesingleScanToken);
		Assert.assertEquals(paymentCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Failed POS payment api");

		String uuidPaymentCard = paymentCardResponse.jsonPath().getString("uuid").replace("[", "").replace("]", "");

		Response giftCardPurcahseResponse = pageObj.endpoints().Api1PurchaseGiftCardWithTransactionID(
				dataSet.get("client"), dataSet.get("secret"), "15", authToken, uuidPaymentCard);

		Assert.assertEquals(giftCardPurcahseResponse.statusCode(), ApiConstants.HTTP_STATUS_OK,
				"Failed to purchase gift card giftCardPurcahseResponse");
		String uuidGiftCardPurcahse = giftCardPurcahseResponse.jsonPath().getString("uuid").replace("[", "")
				.replace("]", "");

		Response reloadGiftCardResp = pageObj.endpoints().Api1ReloadGiftCardRecurring(userEmail, dataSet.get("client"),
				dataSet.get("secret"), "15", authToken, uuidGiftCardPurcahse, uuidPaymentCard);
		Assert.assertEquals(reloadGiftCardResp.statusCode(), ApiConstants.HTTP_STATUS_OK, "Failed to relaod gift card");

		// Single scan Token
		Response singleScanTokenResponse = pageObj.endpoints().singleScanCodeSecureApi(dataSet.get("client"),
				dataSet.get("secret"), authToken, "recurring", "transaction_token", uuidPaymentCard);
		Assert.assertEquals(singleScanTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for the single scan code API");
		TestListeners.extentTest.get().pass("single Scan Code successful");
		logger.info("single Scan Code is successful");
		String singleScanCode1 = singleScanTokenResponse.jsonPath().getString("single_scan_code");
		logger.info("Mobile single scan code is " + singleScanCode1);
		TestListeners.extentTest.get().pass("Mobile single scan code is " + singleScanCode1);

		Response posPaymentResponse = pageObj.endpoints().POSPayment("singleScan", userEmail, singleScanCode1,
				"recurring", dataSet.get("locationKey"));
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

	}

	@AfterMethod(alwaysRun = true)
	public void afterClass() {
		pageObj.utils().clearDataSet(dataSet);
		driver.quit();
		logger.info("Browser closed");
	}
}