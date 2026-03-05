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

import com.punchh.server.annotations.Owner;
import com.punchh.server.apiConfig.ApiConstants;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)

public class GiftCardIntegrationServicesPaymentTest {

	private static Logger logger = LogManager.getLogger(GiftCardIntegrationServicesPaymentTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String sTCName;
	private String baseUrl;
	private String env, run = "ui";
	private static Map<String, String> dataSet;
	Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// single Login to instance
		baseUrl = pageObj.getEnvDetails().setBaseUrl();
		pageObj.instanceDashboardPage().instanceLogin(baseUrl);
	}

	@BeforeMethod(alwaysRun = true)
	public void setUp(Method method) {
		Utilities.loadPropertiesFile("config.properties");
		sTCName = method.getName();
		dataSet = new ConcurrentHashMap<>();
		pageObj.readData().ReadDataFromJsonFile(pageObj.readData().getJsonFilePath(run , env), sTCName);
		dataSet = pageObj.readData().readTestData;
		utils = new Utilities();
		logger.info(sTCName + " ==>" + dataSet);
		pageObj.readData().ReadDataFromJsonFileForClientSecretKey(
				pageObj.readData().getJsonFilePath(run , env , "Secrets"), dataSet.get("slug"));
		dataSet.putAll(pageObj.readData().readTestData);
		logger.info(sTCName + " ==>" + dataSet);
		utils = new Utilities(driver);
	}

    @Test(
        description = "SQ-T2786 INTD-396 | Failure Gift Card/Payment logs | Integration services payment",
        groups = {"regression", "dailyrun"}, priority = 0)
	@Owner(name = "Hardik Bhardwaj")
	public void T2786_failureGiftCardOrPaymentLogs_IntegrationServicesPayment_PartOne_ReloadGiftCard()
			throws InterruptedException {
		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String firstName = signUpResponse.jsonPath().get("user.first_name").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit -> redemptions
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Redemptions");
		pageObj.cockpitRedemptionsPage().navigateToRedemptionsTabs("Redemption Codes");
		pageObj.dashboardpage().checkBoxFlagOnOffAndClick("business_make_uuid_for_online_redemption_code", "check");
		pageObj.dashboardpage().updateCheckBox();

		// Click Cockpit -> gift card
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("0", "0");
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree v2.0");
		// whitelabel->service->Braintree->valid key
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.giftcardsPage().braintreeV2Credentials(dataSet.get("validPublicKey"));
		// random amount
		String amount = utils.twoDigitDecimalNumberUnderFiveHundred();
		// Gift card purchase API
		// Purchase Gift Card API2
		Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("design_id"), amount, dataSet.get("expDate"), firstName);
		Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api2 purchase gift card");
		utils.logPass("Api2 Purchase Gift Card is successful ");
		String uuid = purchaseGiftCardResponse.jsonPath().getString("uuid").toString();
		// whitelabel->service->Braintree->invalid key
		pageObj.giftcardsPage().braintreeV2Credentials(dataSet.get("invalidPublicKey"));
		// Reload Gift Card API2
		String amount1 = utils.twoDigitDecimalNumberUnderFiveHundred();
		Response reloadGiftCardResponse = pageObj.endpoints().Api2ReloadGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, uuid, amount1, firstName, dataSet.get("expDate"));
		Assert.assertEquals(reloadGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 reload gift card");
		utils.logPass("Api2 Reload Gift Card is unsuccessful (expected) ");
		// check gift card logs
		pageObj.menupage().navigateToSubMenuItem("Support", "Integration Services Logs");
//		pageObj.instanceDashboardPage().goToIntegrationServiceLogs();
		pageObj.instanceDashboardPage().serviceTypeAndTime("Payments");
		pageObj.giftcardsPage().checkIntegrationGiftCardLogs(amount1);
	}

    @Test(
        description = "SQ-T2786 INTD-396 | Failure Gift Card/Payment logs | Integration services payment",
        groups = {"regression", "dailyrun"}, priority = 1)
	@Owner(name = "Hardik Bhardwaj")
	public void T2786_failureGiftCardOrPaymentLogs_IntegrationServicesPayment_PartTwo_GiftCardPurchased()
			throws InterruptedException {
		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		String firstName = signUpResponse.jsonPath().get("user.first_name").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("0", "0");
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree v2.0");
		// whitelabel->service->Braintree->valid key
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.giftcardsPage().braintreeV2Credentials(dataSet.get("invalidPublicKey"));
		// random amount
		String amount = utils.twoDigitDecimalNumber();
		// Gift card purchase API
		// Purchase Gift Card API2
		Response purchaseGiftCardResponse = pageObj.endpoints().Api2PurchaseGiftCard(dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("design_id"), amount, dataSet.get("expDate"), firstName);
		Assert.assertEquals(purchaseGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2 purchase gift card");
		utils.logPass("Api2 Purchase Gift Card is unsuccessful (expected) ");
		// check gift card logs
		pageObj.menupage().navigateToSubMenuItem("Support", "Integration Services Logs");
		pageObj.instanceDashboardPage().serviceTypeAndTime("Payments");
		pageObj.giftcardsPage().checkIntegrationGiftCardLogs(amount);
	}

    @Test(
        description = "SQ-T2786 INTD-396 | Failure Gift Card/Payment logs | Integration services payment",
        groups = {"regression", "dailyrun"}, priority = 2)
	@Owner(name = "Hardik Bhardwaj")
	public void T2786_failureGiftCardOrPaymentLogs_IntegrationServicesPayment_PartThree_GiftAGiftCard()
			throws InterruptedException {
		// User SignUp using API
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api2SignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 2 user signup");
		String token = signUpResponse.jsonPath().get("access_token.token").toString();
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");
		// User SignUp using API
		String userEmail1 = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse1 = pageObj.endpoints().Api2SignUp(userEmail1, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse1, "API 2 user signup");
		String firstName1 = signUpResponse1.jsonPath().get("user.first_name").toString();
		Assert.assertEquals(signUpResponse1.getStatusCode(), ApiConstants.HTTP_STATUS_OK, "Status code 200 did not matched for api2 signup");
		utils.logPass("API2 Signup is successful");
		logger.info("API2 Signup is successful");
		// login to instance
//		pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
//		pageObj.instanceDashboardPage().loginToInstance();

		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		// Click Cockpit
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("0", "0");
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree v2.0");
		// whitelabel->service->Braintree->invalid key
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.giftcardsPage().braintreeV2Credentials(dataSet.get("invalidPublicKey"));
		// random amount
		String amount = utils.twoDigitDecimalNumber();
		// Gift a card
		Response giftaCardResponse = pageObj.endpoints().Api2GiftaCard(userEmail1, dataSet.get("client"),
				dataSet.get("secret"), token, dataSet.get("design_id"), amount, dataSet.get("expDate"), firstName1);
		Assert.assertEquals(giftaCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api2  gift a card");
		utils.logPass("Api2 Gift a Card is unsuccessful (expected) ");
		// check gift card logs
		pageObj.menupage().navigateToSubMenuItem("Support", "Integration Services Logs");
		pageObj.instanceDashboardPage().serviceTypeAndTime("Payments");
		pageObj.giftcardsPage().checkIntegrationGiftCardLogs(amount);
	}

	@AfterMethod(alwaysRun = true)
	public void tearDown() {
		pageObj.utils().clearDataSet(dataSet);
		logger.info("Data set cleared");
	}

	@AfterClass(alwaysRun = true)
	public void closeBrowser() {
		driver.quit();
		logger.info("Browser closed");
	}
}