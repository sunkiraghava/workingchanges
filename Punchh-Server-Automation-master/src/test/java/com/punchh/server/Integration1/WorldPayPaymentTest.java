package com.punchh.server.Integration1;

import java.lang.reflect.Method;
import java.util.Map;
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

import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.ApiUtils;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class WorldPayPaymentTest {
	static Logger logger = LogManager.getLogger(WorldPayPaymentTest.class);
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
	private String cardNum[] = { "5555555555554444", "4444333322221111", "5454545454545454" };

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

	@Test(description = "SQ-T2938 Payment with WorldPay")
	public void T2938_VerifyPaymentWithWorldPay() throws Exception {

		// Select the business
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock gift Card Adapter");
		pageObj.giftcardsPage().clickOnUpdateButton();
		pageObj.giftcardsPage().selectPaymentAdapter("World Pay");

		// Set Captcha Flag to true in Services to true in services
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Services");
		pageObj.giftcardsPage().enableWorldPayCaptchaFlag(false);
		String currentURL = driver.getCurrentUrl();

		// Verify Captcha flag "wp_enable_captcha" value in cards API response (false)
		Response cardResp = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardResp.getStatusCode(), 200, "Status code 200 did not matched for api1 cards");
		Assert.assertFalse(Boolean.parseBoolean(cardResp.jsonPath().getString("[0].wp_enable_captcha")),
				"WorldPay Captcha Flag is not set to false");
		logger.info("wp_enable_captcha is set to false in API response");
		TestListeners.extentTest.get().pass("wp_enable_captcha is set to false in API response");

		// Create user and get auth token
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), 200, "Status code 200 did not matched for api1 signup");
		TestListeners.extentTest.get().pass(userEmail + " Api1 user signup is successful");
		String authToken = signUpResponse.jsonPath().get("auth_token.token");

		// Get Transaction Setup ID
		Response transactionSetupIdResp = pageObj.endpoints().getWorldpayTransactionSetupId(
				dataSet.get("certTransactionUrl"), utils.decrypt(dataSet.get("accToken")), false);
		Assert.assertEquals(transactionSetupIdResp.getStatusCode(), 200, "Failed to get Worldpaypayment Token");
		String transactionSetupID = transactionSetupIdResp.getBody().xmlPath()
				.get("TransactionSetupResponse.Response.Transaction.TransactionSetupID").toString();
		logger.info("Generated Transaction Setup ID is : " + transactionSetupID);
		TestListeners.extentTest.get().info("Generated Transaction Setup ID is : " + transactionSetupID);

		// Generate WorldPay Payment Token
		String paymentUrl = dataSet.get("paymentUrl") + transactionSetupID;
		pageObj.worldPayPaymentPage().submitCardDetails(paymentUrl, cardNum[0], dataSet.get("expMonth"),
				dataSet.get("expYear"), dataSet.get("cvv"));
		String cardToken = pageObj.worldPayPaymentPage().getPaymentToken();
		logger.info("WorldPay Card Token is : " + cardToken);
		TestListeners.extentTest.get().info("WorldPay Card Token is : " + cardToken);

		// Get Transaction Setup ID
		Response paymentAccountIdResp = pageObj.endpoints().getPaymentToken(dataSet.get("certServiceUrl"),
				utils.decrypt(dataSet.get("accToken")), cardToken);
		Assert.assertEquals(paymentAccountIdResp.getStatusCode(), 200, "Failed to get Worldpaypayment Token");
		String transactionToken = paymentAccountIdResp.getBody().xmlPath()
				.get("PaymentAccountCreateWithTransIDResponse.Response.PaymentAccount.PaymentAccountID").toString();
		logger.info("generated Transaction Token is : " + transactionToken);
		TestListeners.extentTest.get().pass("Generated Transaction Token is : " + transactionToken);

		// Gift Card Purchase with WorldPay Payment Token
		Response giftCardPurcahseResponse = pageObj.endpoints().Api1PurchaseGiftCardWithSingleScanToken(
				dataSet.get("client"), dataSet.get("secret"), 5 + "", authToken, transactionToken);
		Assert.assertEquals(giftCardPurcahseResponse.getStatusCode(), 200,
				"Status code 200 did not matched for api1 purchase gift card");
		String UUID = giftCardPurcahseResponse.jsonPath().getString("uuid");
		int actualPurchaseAmount = (int) Double.parseDouble(
				giftCardPurcahseResponse.jsonPath().getString("last_fetched_amount").replace("[", "").replace("]", ""));
		Assert.assertEquals(actualPurchaseAmount, 5,
				"Actual purcahse amount " + actualPurchaseAmount + " is not with expected purchase amount " + 5);
		logger.info("Gift card purchase is successful with UUID: " + UUID);
		TestListeners.extentTest.get().pass("Gift card purchase is successful with UUID: " + UUID);

		// Set Captcha Flag to true in Services
		driver.navigate().to(currentURL);
		logger.info("Navigated to " + currentURL);
		pageObj.giftcardsPage().enableWorldPayCaptchaFlag(true);

		// Verify Captcha flag "wp_enable_captcha" value in cards API response (true)
		Response cardResp1 = pageObj.endpoints().Api1Cards(dataSet.get("client"), dataSet.get("secret"));
		Assert.assertEquals(cardResp.getStatusCode(), 200, "Status code 200 did not matched for api1 cards");
		Assert.assertTrue(Boolean.parseBoolean(cardResp1.jsonPath().getString("[0].wp_enable_captcha")),
				"WorldPay Captcha Flag is not set to true");
		logger.info("wp_enable_captcha is set to true in API response");
		TestListeners.extentTest.get().pass("wp_enable_captcha is set to true in API response");

		// Get Transaction Setup ID
		Response transactionSetupIdResp1 = pageObj.endpoints().getWorldpayTransactionSetupId(
				dataSet.get("certTransactionUrl"), utils.decrypt(dataSet.get("accToken")), true);
		Assert.assertEquals(transactionSetupIdResp1.getStatusCode(), 200, "Failed to get Worldpaypayment Token");
		String transactionSetupID1 = transactionSetupIdResp1.getBody().xmlPath()
				.get("TransactionSetupResponse.Response.Transaction.TransactionSetupID").toString();
		logger.info("Generated Transaction Setup ID is : " + transactionSetupID1);
		TestListeners.extentTest.get().info("Generated Transaction Setup ID is : " + transactionSetupID1);

		// Verification of Captcha field on UI
		paymentUrl = dataSet.get("paymentUrl") + transactionSetupID1;
		boolean pageObjectCaptcha = pageObj.worldPayPaymentPage().isCaptchaPresent(paymentUrl);
		Assert.assertTrue(pageObjectCaptcha, "Captcha is not present on the page");
		logger.info("Captcha is present on the WorldPay payment page");
		TestListeners.extentTest.get().pass("Captcha is present on the WorldPay payment page");
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
