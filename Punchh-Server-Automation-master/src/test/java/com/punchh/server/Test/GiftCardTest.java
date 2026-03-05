package com.punchh.server.Test;

import java.lang.reflect.Method;
import java.util.List;
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
import com.punchh.server.apiConfig.ApiResponseJsonSchema;
import com.punchh.server.pages.PageObj;
import com.punchh.server.utilities.BrowserUtilities;
import com.punchh.server.utilities.CreateDateTime;
import com.punchh.server.utilities.DBUtils;
import com.punchh.server.utilities.TestListeners;
import com.punchh.server.utilities.Utilities;

import io.restassured.response.Response;

@Listeners(TestListeners.class)
public class GiftCardTest {

	private static Logger logger = LogManager.getLogger(GiftCardTest.class);
	public WebDriver driver;
	private PageObj pageObj;
	private String userEmail;
	private String sTCName;
	private String env, run = "ui";
	private String baseUrl;
	private static Map<String, String> dataSet;
	private Utilities utils;

	@BeforeClass(alwaysRun = true)
	public void openBrowser() {
		driver = new BrowserUtilities().launchBrowser();
		pageObj = new PageObj(driver);
		env = pageObj.getEnvDetails().setEnv();
		// Single login to instance
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
		logger.info(sTCName + " ==> " + dataSet);
		utils = new Utilities(driver);
		// Move to All Businesses page
		pageObj.instanceDashboardPage().moveToAllBusinessPage(baseUrl);
	}

//@author = amit kumar
	@SuppressWarnings("unused")
	@Test(description = "SQ-T2230 Verify user is able to Purchase Gift Card", groups = {"regression", "dailyrun"}, priority = 0)
	@Owner(name = "Amit Kumar")
	public void T2230_verifyUserisabletoPurchaseGiftCard() throws InterruptedException {

		// Instance login and goto timeline
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit set giftcard adapter/payment adapter/ min max amount
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("10", "500");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable PassCode For Gift Cards?", "uncheck");
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable PassCode For Payments?", "uncheck");

		// User creation using api1
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		TestListeners.extentTest.get().pass("Api1 user signup is successful ");
		// Gift card purchase apiv1
		Response purchaseGiftCardResp = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card purchase");
		TestListeners.extentTest.get().pass("Api1 purchase goft card is successful ");
		String cardNumber = purchaseGiftCardResp.jsonPath().get("card_number").toString();
		String uuidNumber = purchaseGiftCardResp.jsonPath().get("uuid").toString();
		String amount = purchaseGiftCardResp.jsonPath().get("last_fetched_amount").toString();

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String notification = pageObj.guestTimelinePage().getGiftCard();
		Assert.assertTrue(notification.contains("You just purchased a Gift Card for yourself"),
				"You just purchased a Gift Card for yourself, string did not matched with notification value");
		Assert.assertTrue(notification.contains(amount), "Balance value did not matched");
		TestListeners.extentTest.get().pass("Gift card purchase notification verified successfuly on timeline");
		// Timeline Giftcards section
		pageObj.guestTimelinePage().clickGiftCards();
		String card_Number = pageObj.giftcardsPage().getCardNumber();
		String cardBalance = pageObj.giftcardsPage().getCardBalance();
		// String cardDetails = pageObj.giftcardsPage().getGiftCardPurchaseDetails();
		Assert.assertEquals(card_Number, cardNumber, "Card number did not matched in gift cards page");
		Assert.assertTrue(cardBalance.contains(amount), "Card balance did not matched in gift cards page");
		// Assert.assertTrue(cardDetails.contains("Reloaded"), "card reloaded value did
		// not matched");
		TestListeners.extentTest.get()
				.pass("Gift card number, balance and reload status verified successfuly on gift cards page");

		// goto guests menu and giftcards
		pageObj.menupage().navigateToSubMenuItem("Guests", "Gift Cards");
		pageObj.giftcardsPage().searchbyCard(cardNumber);
		List<String> data = pageObj.giftcardsPage().getCardDetailsGuestSection();
		Assert.assertEquals(data.get(2), cardNumber, "Card number did not matched in guests menu gift cards section");
		Assert.assertTrue(data.get(4).contains(amount),
				"Card balance did not matched in guests menu gift cards section");
		TestListeners.extentTest.get().pass("Gift card details verified successfuly in guests menu gift cards section");

		// goto reports menu and payment reports
		/*
		 * pageObj.menupage().clickReportsMenu();
		 * pageObj.menupage().clickPaymentReportLink();
		 * pageObj.paymentreportPage().searchbyCardorTxn(cardNumber); List<String>
		 * Carddata = pageObj.paymentreportPage().getCardDetailsPaymentReportSection();
		 * Assert.assertEquals(Carddata.get(2), cardNumber,
		 * "Card number did not matched in reports menu payment report section");
		 * Assert.assertTrue(Carddata.get(1).contains(amount),
		 * "Card balance did not matched in reports menu payment report section");
		 * TestListeners.extentTest.get()
		 * .pass("Gift card details verified successfuly in reports menu payment report section"
		 * );
		 */

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2178 Verify user is able to Reload Gift Card", groups = {"regression", "dailyrun"}, priority = 1)
	@Owner(name = "Amit Kumar")
	public void T2178_verifyUserisabletoReloadGiftCard() throws InterruptedException {
		// Instance login and goto timeline
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit set giftcard adapter/payment adapter/ min max amount
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("10", "500");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable PassCode For Gift Cards?", "uncheck");
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable PassCode For Payments?", "uncheck");

		// User creation using api1
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		TestListeners.extentTest.get().pass("Api1 user signup is successful ");

		// Gift card purchase api1
		Response purchaseGiftCardResp = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card purchase");
		TestListeners.extentTest.get().pass("Api1 purchase goft card is successful ");
		String cardNumber = purchaseGiftCardResp.jsonPath().get("card_number").toString();
		String uuidNumber = purchaseGiftCardResp.jsonPath().get("uuid").toString();
		String amount = purchaseGiftCardResp.jsonPath().get("last_fetched_amount").toString();

		// Gift card reload api1
		Response reloadGiftCardResp = pageObj.endpoints().Api1ReloadGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("reloadAmount"), token, uuidNumber, "fake-valid-nonce");
		Assert.assertEquals(reloadGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card reload");
		String card = reloadGiftCardResp.jsonPath().get("card_number").toString();
		String last_fetched_amount = reloadGiftCardResp.jsonPath().get("last_fetched_amount").toString();

		// Instance login and goto timeline
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		 */

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String notification = pageObj.guestTimelinePage().getCardReloadNotification();
		String Txn_no = pageObj.guestTimelinePage().getCardTransactionnumber();
		Assert.assertTrue(notification.contains("Processed Gift Card Payment"),
				"Processed Gift Card Payment, string did not matched with notification value");
		TestListeners.extentTest.get().pass("Gift card reload notification verified successfuly on timeline");

		// Timeline Giftcards section
		pageObj.guestTimelinePage().clickGiftCards();
		String card_Number = pageObj.giftcardsPage().getCardNumber();
		String cardBalance = pageObj.giftcardsPage().getCardBalance();
		// String cardDetails = pageObj.giftcardsPage().getGiftCardReloadDetails();
		Assert.assertEquals(card_Number, card, "Card number did not matched in gift cards page");
		Assert.assertTrue(cardBalance.contains(last_fetched_amount),
				"Card total balance did not matched in gift cards page"); // total balance
		/*
		 * Assert.assertTrue(cardDetails.contains("$20.00"),
		 * "Card reloaded balance did not matched in gift cards page"); /// reloaded //
		 * balance Assert.assertTrue(cardDetails.contains("Reloaded"),
		 * "card reloaded value did not matched");
		 */
		TestListeners.extentTest.get()
				.pass("Gift card number, balance amount and reload status verified successfuly on gift cards page");

		// goto guests menu and giftcards
		pageObj.menupage().clickDashboardMenu();
		pageObj.menupage().navigateToSubMenuItem("Guests", "Gift Cards");
		pageObj.giftcardsPage().searchbyCard(card_Number);
		List<String> data = pageObj.giftcardsPage().getCardDetailsGuestSection();
		System.out.println(data);
		Assert.assertEquals(data.get(2), card_Number, "Card number did not matched in guests menu gift cards section");
		Assert.assertTrue(data.get(4).contains(last_fetched_amount),
				"Card balance did not matched in guests menu gift cards section");
		TestListeners.extentTest.get().pass("Gift card details verified successfuly in guests menu gift cards section");

		// goto reports menu and payment reports
		/*
		 * pageObj.menupage().clickDashboardMenu();
		 * pageObj.menupage().clickReportsMenu();
		 * pageObj.menupage().clickPaymentReportLink();
		 * pageObj.paymentreportPage().searchbyCardorTxn(Txn_no); List<String> Carddata
		 * = pageObj.paymentreportPage().getCardDetailsPaymentReportSection();
		 * System.out.println(Carddata); Assert.assertEquals(Carddata.get(2),
		 * card_Number,
		 * "Card number did not matched in reports menu payment report section");
		 * Assert.assertTrue(Carddata.get(1).contains("$20.00"),
		 * "Card balance did not matched in reports menu payment report section");
		 * TestListeners.extentTest.get()
		 * .pass("Gift card details verified successfuly in reports menu payment report section"
		 * );
		 */

	}

	@Test(description = "SQ-T2264 Verify user is able to Share Gift Card", groups = {"regression", "dailyrun"}, priority = 2)
	@Owner(name = "Amit Kumar")
	public void T2264_verifyUserisabletoShareGiftCard() throws InterruptedException {
		// Instance login and goto timeline
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit set giftcard adapter/payment adapter/ min max amount
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("10", "500");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable PassCode For Gift Cards?", "uncheck");
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable PassCode For Payments?", "uncheck");

		// User creation using api1
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		TestListeners.extentTest.get().pass("Api1 user signup is successful ");

		// Gift card purchase api1
		Response purchaseGiftCardResp = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card purchase");
		TestListeners.extentTest.get().pass("Api1 purchase goft card is successful ");
		String cardNumber = purchaseGiftCardResp.jsonPath().get("card_number").toString();
		String uuidNumber = purchaseGiftCardResp.jsonPath().get("uuid").toString();
		String amount = purchaseGiftCardResp.jsonPath().get("last_fetched_amount").toString();

		// User creation using api1
		String newUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signInResponse = pageObj.endpoints().Api1UserSignUp(newUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signInResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 user signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful ");

		// Gift card share api1
		Response shareGiftCardResp = pageObj.endpoints().Api1ShareGiftCard(newUserEmail, dataSet.get("client"),
				dataSet.get("secret"), token, uuidNumber);
		Assert.assertEquals(shareGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card share");

		// Instance login and goto giftcards
		/*
		 * pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		 * pageObj.instanceDashboardPage().loginToInstance();
		 * pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));
		 */

		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		pageObj.guestTimelinePage().clickGiftCards();

		List<String> ownerData = pageObj.giftcardsPage().getCardOwnerDetails();
		Assert.assertTrue(ownerData.get(1).contains(userEmail.toLowerCase()),
				"User email and owner email did not matched in timline gift cards section");
		Assert.assertTrue(ownerData.get(2).contains("owner"),
				"owner string did not matched in timline gift cards section");
		List<String> sharedData = pageObj.giftcardsPage().getCardSharedDetails();

		Assert.assertTrue(sharedData.get(1).contains(newUserEmail.toLowerCase()),
				"New user email and shared email did not matched in timline gift cards section");
		Assert.assertTrue(sharedData.get(2).contains("shared"),
				"shared string did not matched in timline gift cards section");
		TestListeners.extentTest.get()
				.pass("Gift card owner and shared details verified successfuly on timeline gift cards");
		// validate new user timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(newUserEmail);
		String notification = pageObj.guestTimelinePage().getGiftCard();
		Assert.assertTrue(notification.contains("shared a Gift Card with you"),
				"shared a Gift Card with you, string did not matched with notification value");
		TestListeners.extentTest.get().pass("shared gift card  notification verified successfuly on new user timeline");

		pageObj.guestTimelinePage().clickGiftCards();
		List<String> newuserOwnerData = pageObj.giftcardsPage().getCardOwnerDetails();
		Assert.assertTrue(newuserOwnerData.get(1).contains(userEmail.toLowerCase()),
				"User email and owner email did not matched in timline gift cards section");
		Assert.assertTrue(newuserOwnerData.get(2).contains("owner"),
				"owner string did not matched in timline gift cards section");
		List<String> newUserSharedData = pageObj.giftcardsPage().getCardSharedDetails();
		Assert.assertTrue(newUserSharedData.get(1).contains(newUserEmail.toLowerCase()),
				"New user email and shared email did not matched in timline gift cards section");
		Assert.assertTrue(newUserSharedData.get(2).contains("shared"),
				"shared string did not matched in timline gift cards section");
		TestListeners.extentTest.get()
				.pass("Gift card owner and shared details verified successfuly on timeline gift cards");

		String card_Number = pageObj.giftcardsPage().getCardNumber();
		String cardBalance = pageObj.giftcardsPage().getCardBalance();
		// String cardDetails = pageObj.giftcardsPage().getGiftCardPurchaseDetails();
		Assert.assertEquals(card_Number, cardNumber, "Card number did not matched in gift cards page");
		Assert.assertTrue(cardBalance.contains(amount), "Card balance did not matched in gift cards page");
		// Assert.assertTrue(cardDetails.contains("Reloaded"), "card reloaded value did
		// not matched");
		TestListeners.extentTest.get().pass(
				"Gift card number, balance, reload status and owner shared details verified successfuly on gift cards page for new user");
	}

	@Test(description = "SQ-T2211 Verify User is able to Transfer Gift Card", groups = {"regression", "dailyrun"}, priority = 3)
	@Owner(name = "Amit Kumar")
	public void T2211_verifyUserisabletoTransferGiftCard() throws InterruptedException {
		// Instance login and goto timeline
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Click Cockpit set giftcard adapter/payment adapter/ min max amount
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("Mock Gift Card Adapter");
		pageObj.giftcardsPage().setMinMaxAmountGiftCard("10", "500");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable PassCode For Gift Cards?", "uncheck");
		pageObj.giftcardsPage().selectPaymentAdapter("Braintree");
		pageObj.dashboardpage().checkUncheckFlagOnCockpitDasboard("Enable PassCode For Payments?", "uncheck");

		// User creation using api1
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		TestListeners.extentTest.get().pass("Api1 user signup is successful ");

		// Gift card purchase api1
		Response purchaseGiftCardResp = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card purchase");
		TestListeners.extentTest.get().pass("Api1 purchase goft card is successful ");
		String cardNumber = purchaseGiftCardResp.jsonPath().get("card_number").toString();
		String uuidNumber = purchaseGiftCardResp.jsonPath().get("uuid").toString();
		String amount = purchaseGiftCardResp.jsonPath().get("last_fetched_amount").toString();

		// User creation using api1
		String newUserEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signInResponse = pageObj.endpoints().Api1UserSignUp(newUserEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signInResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 user signup");
		TestListeners.extentTest.get().pass("Api1 user signup is successful ");

		// Gift card transfer api1
		Response shareGiftCardResp = pageObj.endpoints().Api1TransferGiftCard(newUserEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("amount"), token, uuidNumber);
		Assert.assertEquals(shareGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card transfer");

		// validate owner timeline
		pageObj.instanceDashboardPage().navigateToGuestTimeline(userEmail);
		String giftCardTransferNotification = pageObj.guestTimelinePage()
				.getSystemNotificatioGiftCard("gift_card_transferred");
		Assert.assertEquals(giftCardTransferNotification, "gift_card_transferred",
				"System Notification gift card transferred not appeared");
		String notification = pageObj.guestTimelinePage().getGiftCard();
		String transferGiftcard = pageObj.guestTimelinePage().giftCardTransfer();
		Assert.assertTrue(notification.contains("You just transferred $20.00 from Gift Card"),
				"You just transferred $20.00 from Gift Card, string did not matched with notification value");
		Assert.assertEquals(transferGiftcard, "Gift Card Transferred From");

		// balance deduction validation after card transfer for owner
		pageObj.guestTimelinePage().clickGiftCards();
		String ownerCardBalance = pageObj.giftcardsPage().getCardBalance();
		Assert.assertTrue(ownerCardBalance.contains("$0.00"), "Card balance did not matched in gift cards page");
		TestListeners.extentTest.get().pass("Gift card balance after transfer is verified :" + ownerCardBalance);
		// goto guests menu and giftcards
		pageObj.menupage().navigateToSubMenuItem("Guests", "Gift Cards");
		pageObj.giftcardsPage().searchbyCard(cardNumber);
		List<String> ownerCarddata = pageObj.giftcardsPage().getCardDetailsGuestSection();
		Assert.assertEquals(ownerCarddata.get(2), cardNumber,
				"Card number did not matched in guests menu gift cards section");
		Assert.assertTrue(ownerCarddata.get(4).contains("$0.00"),
				"Card balance did not matched in guests menu gift cards section");
		TestListeners.extentTest.get().pass("Gift card details verified successfuly in guests menu gift cards section");

		// validate reciver timeline and giftcard section
		pageObj.instanceDashboardPage().navigateToGuestTimeline(newUserEmail);
		String reciverNotification = pageObj.guestTimelinePage().getGiftCard();
		Assert.assertTrue(reciverNotification.contains("just sent you a Gift Card"),
				"just sent you a Gift Card, string did not matched with notification value");

		// Timeline Giftcards section
		pageObj.guestTimelinePage().clickGiftCards();
		String card_Number = pageObj.giftcardsPage().getCardNumber();
		String cardBalance = pageObj.giftcardsPage().getCardBalance();
		Assert.assertTrue(cardBalance.contains(amount), "Card balance did not matched in gift cards page");
		TestListeners.extentTest.get().pass("card balance after transfer verified successfuly on gift cards page");

		// goto guests menu and giftcards
		pageObj.menupage().navigateToSubMenuItem("Guests", "Gift Cards");
		pageObj.giftcardsPage().searchbyCard(card_Number);
		List<String> data = pageObj.giftcardsPage().getCardDetailsGuestSection();
		System.out.println(data);
		Assert.assertEquals(data.get(2), card_Number, "Card number did not matched in guests menu gift cards section");
		Assert.assertTrue(data.get(4).contains(amount),
				"Card balance did not matched in guests menu gift cards section");
		TestListeners.extentTest.get().pass("Gift card details verified successfuly in guests menu gift cards section");

	}

	@Test(description = "SQ-T2164 Verify User is not able to purchase card beyond given limit", groups = {"regression", "dailyrun"}, priority = 4)
	@Owner(name = "Amit Kumar")
	public void T2164_verifyUserisnotabletopurchasecardbeyondgivenlimit() throws InterruptedException {

		// User creation using api1
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		TestListeners.extentTest.get().pass("Api1 user signup is successful ");

		// Gift card purchase api1 with more than max amount limit
		Response purchaseGiftCardResp = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("maxAmount"), token, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api1 gift card purchase");
		Assert.assertTrue(purchaseGiftCardResp.asString().contains("Maximum balance exceeded"),
				"Maximum balance exceeded, string did not matched in response");
		TestListeners.extentTest.get().pass("Api1 purchase gift card beyond max amount limit  is successful ");

		// Gift card purchase api1 with less than min amount limit
		Response purchaseGiftCardRespo = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("minAmount"), token, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardRespo.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api1 gift card purchase");
		Assert.assertTrue(purchaseGiftCardRespo.asString().contains("Transaction amount can not be less than $10.00"),
				"Transaction amount can not be less than $10.00, string did not matched in response");
		TestListeners.extentTest.get().pass("Api1 purchase gift card with less than min amount limit  is successful ");

	}

	@SuppressWarnings("unused")
	@Test(description = "SQ-T2247 Verify User is not able to relod card beyond given limit", groups = {"regression", "dailyrun"}, priority = 5)
	@Owner(name = "Amit Kumar")
	public void T2247_verifyUserisnotabletoreloadcardbeyondgivenlimit() throws InterruptedException {

		// User creation using api1
		userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		pageObj.apiUtils().verifyResponse(signUpResponse, "API 1 user signup");
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		TestListeners.extentTest.get().pass("Api1 user signup is successful ");

		// Gift card purchase apiv1
		Response purchaseGiftCardResp = pageObj.endpoints().Api1PurchaseGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("amount"), token, dataSet.get("cardId"));
		Assert.assertEquals(purchaseGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not matched for api1 gift card purchase");
		TestListeners.extentTest.get().pass("Api1 purchase goft card is successful ");
		String cardNumber = purchaseGiftCardResp.jsonPath().get("card_number").toString();
		String uuidNumber = purchaseGiftCardResp.jsonPath().get("uuid").toString();
		String amount = purchaseGiftCardResp.jsonPath().get("last_fetched_amount").toString();

		// Gift card reload with more than max amount api1
		Response reloadGiftCardResp = pageObj.endpoints().Api1ReloadGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("maxAmount"), token, uuidNumber, "fake-valid-nonce");
		Assert.assertEquals(reloadGiftCardResp.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api1 gift card reload");
		Assert.assertTrue(reloadGiftCardResp.asString().contains("Maximum balance exceeded"),
				"Maximum balance exceeded, string did not matched in response");
		TestListeners.extentTest.get().pass("Api1 purchase gift card beyond max amount limit  is successful ");

		// Gift card reload with less than min amount api1
		Response reloadGiftCardRespo = pageObj.endpoints().Api1ReloadGiftCard(userEmail, dataSet.get("client"),
				dataSet.get("secret"), dataSet.get("minAmount"), token, uuidNumber, "fake-valid-nonce");
		Assert.assertEquals(reloadGiftCardRespo.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY,
				"Status code 422 did not matched for api1 gift reload purchase");
		Assert.assertTrue(reloadGiftCardRespo.asString().contains("Transaction amount can not be less than $10.00"),
				"Transaction amount can not be less than $10.00, string did not matched in response");
		TestListeners.extentTest.get().pass("Api1 purchase gift card with less than min amount limit  is successful ");

	}

	@Test(description = "SQ-T5081: Mobile API1 and API2 - Verify Import Gift Card", groups = {"regression", "dailyrun"}, priority = 6)
	@Owner(name = "Vaibhav Agnihotri")
	public void T5081_verifyAPIv1ImportGiftCard() throws Exception {

		// Login to instance. Select the business
		//pageObj.instanceDashboardPage().navigateToPunchhInstance(baseUrl);
		//pageObj.instanceDashboardPage().loginToInstance();
		pageObj.instanceDashboardPage().selectBusiness(dataSet.get("slug"));

		// Go to Cockpit > Gift Cards, ensure the required gift card adapter is selected
		pageObj.menupage().navigateToSubMenuItem("Cockpit", "Gift Cards");
		pageObj.giftcardsPage().selectGiftCardAdapter("World Gift Card Adapter");

		// Go to Whitelabel > Integration Services, ensure valid World Gift Card
		// username is available
		pageObj.menupage().navigateToSubMenuItem("Whitelabel", "Integration Services");
		pageObj.giftcardsPage().worldGiftCardCredentialsIntegration(dataSet.get("worldGiftCardUsername"));
		utils.logit("World Gift Card Adapter is selected and valid credentials are present.");

		// Mobile API v1: User sign-up
		logger.info("== Mobile API v1: User sign-up ==");
		String userEmail = pageObj.iframeSingUpPage().generateEmail();
		Response signUpResponse = pageObj.endpoints().Api1UserSignUp(userEmail, dataSet.get("client"),
				dataSet.get("secret"));
		Assert.assertEquals(signUpResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 user signup");
		String token = signUpResponse.jsonPath().get("auth_token.token").toString();
		utils.logPass("API v1 User signup call is successful");

		/*
		 * Execute SELECT query to check if the gift card record for given epin is
		 * already present in the table. If yes, delete it.
		 */
		String checkGCQuery = "SELECT uuid FROM gift_cards WHERE business_id = $business_id AND epin = '$epin';";
		checkGCQuery = checkGCQuery.replace("$business_id", dataSet.get("businessId"))
				.replace("$epin", dataSet.get("epinEncrypted"));
		String existingGCUuid = DBUtils.executeQueryAndGetColumnValue(env, checkGCQuery, "uuid");
		if (!existingGCUuid.isEmpty()) {
			// Execute DELETE query to delete the existing gift card record
			String deleteExistingGCQuery = "DELETE FROM gift_cards WHERE business_id = $business_id AND uuid = '$uuid';";
			deleteExistingGCQuery = deleteExistingGCQuery.replace("$business_id", dataSet.get("businessId"))
					.replace("$uuid", existingGCUuid);
			int deleteExistingGCQueryResult = DBUtils.executeUpdateQuery(env, deleteExistingGCQuery);
			Assert.assertEquals(deleteExistingGCQueryResult, 1, "Unable to delete the record.");
			utils.logit("Gift Card UUID: " + existingGCUuid + " is successfully deleted from the gift_cards table.");
		} else {
			utils.logPass("There is no GC present with the given epin.");
		}

		// Mobile API v1: Import Gift Card using valid credentials
		utils.logit("== Mobile API v1: Import Gift Card ==");
		Response importGiftCardResponse = pageObj.endpoints().api1ImportGiftCard(dataSet.get("designId"),
				dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(importGiftCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_OK,
				"Status code 200 did not match for API v1 Import Gift Card call");
		boolean isApi1ImportGiftCardSchemaValidated = Utilities
				.validateJsonAgainstSchema(ApiResponseJsonSchema.api1GiftCardSchema, importGiftCardResponse.asString());
		Assert.assertTrue(isApi1ImportGiftCardSchemaValidated, "API v1 Import Gift Card Schema Validation failed");
		String giftCardUuid = importGiftCardResponse.jsonPath().get("uuid").toString();
		utils.logPass("API v1 Import Gift Card call is successful and Gift Card having UUID '"
				+ giftCardUuid + "' is imported.");

		// Execute SELECT query to ensure the gift card record is added to the table
		String selectQuery = "SELECT * FROM gift_cards WHERE business_id = '" + dataSet.get("businessId")
				+ "' AND uuid = '" + giftCardUuid + "'";
		String expectedUuidValueAfterSelect = DBUtils.executeQueryAndGetColumnValue(env,
				selectQuery, "uuid");
		Assert.assertEquals(expectedUuidValueAfterSelect, giftCardUuid,
				"Value is not present in uuid column in gift_cards table.");

		// Execute DELETE query to ensure the gift card record is deleted from the table
		String deleteQuery = "DELETE FROM gift_cards WHERE business_id = '" + dataSet.get("businessId")
				+ "' AND uuid = '" + expectedUuidValueAfterSelect + "'";
		int deleteQueryResult = DBUtils.executeUpdateQuery(env, deleteQuery);
		Assert.assertEquals(deleteQueryResult, 1, "Value is still present in uuid column in gift_cards table.");
		utils.logPass("Gift Card UUID: " + giftCardUuid + " is successfully deleted from the gift_cards table.");

		// Mobile API2 Import Gift Card using valid credentials
		utils.logit("== Mobile API2: Import Gift Card ==");
		String giftCardName = CreateDateTime.getUniqueString("AutomationGiftCard");
		Response importGiftCardResponse2 = pageObj.endpoints().api2ImportGiftCard(giftCardName, dataSet.get("designId"),
				dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(importGiftCardResponse2.getStatusCode(), ApiConstants.HTTP_STATUS_OK);
		boolean isApi2ImportGiftCardSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2GiftCardSchema, importGiftCardResponse2.asString());
		Assert.assertTrue(isApi2ImportGiftCardSchemaValidated, "API2 Import Gift Card Schema Validation failed");
		String giftCardUuid2 = importGiftCardResponse2.jsonPath().get("uuid").toString();
		utils.logPass("API2 Import Gift Card call is successful and Gift Card having UUID '"
				+ giftCardUuid2 + "' is imported.");

		// Negative Case: Mobile API2 Import Gift Card using already used card number
		utils.logit("== Mobile API2: Import Gift Card using already used card number ==");
		Response importGiftCardAlreadyUsedCardResponse = pageObj.endpoints().api2ImportGiftCard(giftCardName,
				dataSet.get("designId"), dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"),
				dataSet.get("secret"), token);
		Assert.assertEquals(importGiftCardAlreadyUsedCardResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2ImportGiftCardAlreadyUsedCardSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, importGiftCardAlreadyUsedCardResponse.asString());
		Assert.assertTrue(isApi2ImportGiftCardAlreadyUsedCardSchemaValidated,
				"API2 Import Gift Card with already used card number Schema Validation failed");
		String importGiftCardAlreadyUsedCardMsg = importGiftCardAlreadyUsedCardResponse.jsonPath().get("errors.base[0]")
				.toString();
		Assert.assertEquals(importGiftCardAlreadyUsedCardMsg, "Card Number has already been used before.");
		utils.logPass("API2 Import Gift Card call with already used card number is unsuccessful");

		// Execute SELECT query to ensure the gift card record is added to the table
		String selectQuery2 = "SELECT * FROM gift_cards WHERE business_id = '" + dataSet.get("businessId")
				+ "' AND uuid = '" + giftCardUuid2 + "'";
		String expectedUuidValueAfterSelect2 = DBUtils.executeQueryAndGetColumnValue(env,
				selectQuery2, "uuid");
		Assert.assertEquals(expectedUuidValueAfterSelect2, giftCardUuid2,
				"Value is not present in uuid column in gift_cards table.");

		// Execute DELETE query to ensure the gift card record is deleted from the table
		String deleteQuery2 = "DELETE FROM gift_cards WHERE business_id = '" + dataSet.get("businessId")
				+ "' AND uuid = '" + expectedUuidValueAfterSelect2 + "'";
		int deleteQueryResult2 = DBUtils.executeUpdateQuery(env, deleteQuery2);
		Assert.assertEquals(deleteQueryResult2, 1, "Value is still present in uuid column in gift_cards table.");
		utils.logPass("Gift Card UUID: " + giftCardUuid2 + " is successfully deleted from the gift_cards table.");

		// Negative Case: Mobile API2 Import Gift Card with invalid client
		utils.logit("== Mobile API2: Import Gift Card with invalid client ==");
		Response importGiftCardInvalidClientResponse = pageObj.endpoints().api2ImportGiftCard(giftCardName,
				dataSet.get("designId"), dataSet.get("cardNumber"), dataSet.get("epin"), "1", dataSet.get("secret"),
				token);
		Assert.assertEquals(importGiftCardInvalidClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApi2ImportGiftCardInvalidClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnknownClientSchema, importGiftCardInvalidClientResponse.asString());
		Assert.assertTrue(isApi2ImportGiftCardInvalidClientSchemaValidated,
				"API2 Import Gift Card with invalid client Schema Validation failed");
		String importGiftCardInvalidClientMsg = importGiftCardInvalidClientResponse.jsonPath()
				.get("errors.unknown_client[0]");
		Assert.assertEquals(importGiftCardInvalidClientMsg,
				"Client ID is incorrect. Please check client param or contact us");
		utils.logPass("API2 Import Gift Card call with invalid client is unsuccessful");

		// Negative Case: Mobile API2 Import Gift Card with missing client
		utils.logit("== Mobile API2: Import Gift Card with missing client ==");
		Response importGiftCardMissingClientResponse = pageObj.endpoints().api2ImportGiftCard(giftCardName,
				dataSet.get("designId"), dataSet.get("cardNumber"), dataSet.get("epin"), "", dataSet.get("secret"),
				token);
		Assert.assertEquals(importGiftCardMissingClientResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isApi2ImportGiftCardMissingClientSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2MissingClientSchema, importGiftCardMissingClientResponse.asString());
		Assert.assertTrue(isApi2ImportGiftCardMissingClientSchemaValidated,
				"API2 Import Gift Card with missing client Schema Validation failed");
		String importGiftCardMissingClientMsg = importGiftCardMissingClientResponse.jsonPath().get("errors.client");
		Assert.assertEquals(importGiftCardMissingClientMsg, "Required parameter missing or the value is empty.");
		utils.logPass("API2 Import Gift Card call with missing client is unsuccessful");

		// Negative Case: Mobile API2 Import Gift Card with invalid secret
		utils.logit("== Mobile API2: Import Gift Card with invalid secret ==");
		Response importGiftCardInvalidSecretResponse = pageObj.endpoints().api2ImportGiftCard(giftCardName,
				dataSet.get("designId"), dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"), "1",
				token);
		Assert.assertEquals(importGiftCardInvalidSecretResponse.getStatusCode(), 412);
		boolean isApi2ImportGiftCardInvalidSecretSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2InvalidSignatureSchema, importGiftCardInvalidSecretResponse.asString());
		Assert.assertTrue(isApi2ImportGiftCardInvalidSecretSchemaValidated,
				"API2 Import Gift Card with invalid secret Schema Validation failed");
		String importGiftCardInvalidSecretMsg = importGiftCardInvalidSecretResponse.jsonPath()
				.get("errors.invalid_signature[0]");
		Assert.assertEquals(importGiftCardInvalidSecretMsg,
				"Signature doesn't match. For information about generating the x-pch-digest header, see https://developers.punchh.com.");
		utils.logPass("API2 Import Gift Card call with invalid secret is unsuccessful");

		// Negative Case: Mobile API2 Import Gift Card with invalid design ID
		utils.logit("== Mobile API2: Import Gift Card with invalid design ID ==");
		Response importGiftCardInvalidDesignIdResponse = pageObj.endpoints().api2ImportGiftCard(giftCardName, "1",
				dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(importGiftCardInvalidDesignIdResponse.getStatusCode(), 406);
		boolean isApi2ImportGiftCardInvalidDesignIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema2, importGiftCardInvalidDesignIdResponse.asString());
		Assert.assertTrue(isApi2ImportGiftCardInvalidDesignIdSchemaValidated,
				"API2 Import Gift Card with invalid design ID Schema Validation failed");
		String importGiftCardInvalidDesignIdMsg = importGiftCardInvalidDesignIdResponse.jsonPath().get("errors[0]");
		Assert.assertEquals(importGiftCardInvalidDesignIdMsg,
				"Card design unavailable! Please restart application to refresh gift card design data.");
		utils.logPass("API2 Import Gift Card call with invalid design ID is unsuccessful");

		// Negative Case: Mobile API2 Import Gift Card with missing design ID
		utils.logit("== Mobile API2: Import Gift Card with missing design ID ==");
		Response importGiftCardMissingDesignIdResponse = pageObj.endpoints().api2ImportGiftCard(giftCardName, "",
				dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(importGiftCardMissingDesignIdResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isApi2ImportGiftCardMissingDesignIdSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2DesignIdErrorSchema, importGiftCardMissingDesignIdResponse.asString());
		Assert.assertTrue(isApi2ImportGiftCardMissingDesignIdSchemaValidated,
				"API2 Import Gift Card with missing design ID Schema Validation failed");
		String importGiftCardMissingDesignIdMsg = importGiftCardMissingDesignIdResponse.jsonPath()
				.get("errors.design_id");
		Assert.assertEquals(importGiftCardMissingDesignIdMsg, "Required parameter missing or the value is empty.");
		utils.logPass("API2 Import Gift Card call with missing design ID is unsuccessful");

		// Negative Case: Mobile API2 Import Gift Card with invalid card number
		utils.logit("== Mobile API2: Import Gift Card with invalid card number ==");
		Response importGiftCardInvalidCardNumberResponse = pageObj.endpoints().api2ImportGiftCard(giftCardName,
				dataSet.get("designId"), "a", dataSet.get("epin"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(importGiftCardInvalidCardNumberResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNPROCESSABLE_ENTITY);
		boolean isApi2ImportGiftCardInvalidCardNumberSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.apiErrorsObjectSchema3, importGiftCardInvalidCardNumberResponse.asString());
		Assert.assertTrue(isApi2ImportGiftCardInvalidCardNumberSchemaValidated,
				"API2 Import Gift Card with invalid card number Schema Validation failed");
		String importGiftCardInvalidCardNumberMsg = importGiftCardInvalidCardNumberResponse.jsonPath()
				.get("errors.base[0]");
		Assert.assertEquals(importGiftCardInvalidCardNumberMsg, "Card no or epin is not valid.");
		utils.logPass("API2 Import Gift Card call with invalid card number is unsuccessful");

		// Negative Case: Mobile API2 Import Gift Card with missing card number
		utils.logit("== Mobile API2: Import Gift Card with missing card number ==");
		Response importGiftCardMissingCardNumberResponse = pageObj.endpoints().api2ImportGiftCard(giftCardName,
				dataSet.get("designId"), "", dataSet.get("epin"), dataSet.get("client"), dataSet.get("secret"), token);
		Assert.assertEquals(importGiftCardMissingCardNumberResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isApi2ImportGiftCardMissingCardNumberSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2CardNumberErrorSchema, importGiftCardMissingCardNumberResponse.asString());
		Assert.assertTrue(isApi2ImportGiftCardMissingCardNumberSchemaValidated,
				"API2 Import Gift Card with missing card number Schema Validation failed");
		String importGiftCardMissingCardNumberMsg = importGiftCardMissingCardNumberResponse.jsonPath()
				.get("errors.card_number");
		Assert.assertEquals(importGiftCardMissingCardNumberMsg, "Required parameter missing or the value is empty.");
		utils.logPass("API2 Import Gift Card call with missing card number is unsuccessful");

		// Negative Case: Mobile API2 Import Gift Card with missing epin
		utils.logit("== Mobile API2: Import Gift Card with missing epin ==");
		Response importGiftCardMissingEpinResponse = pageObj.endpoints().api2ImportGiftCard(giftCardName,
				dataSet.get("designId"), dataSet.get("cardNumber"), "", dataSet.get("client"), dataSet.get("secret"),
				token);
		Assert.assertEquals(importGiftCardMissingEpinResponse.getStatusCode(), ApiConstants.HTTP_STATUS_BAD_REQUEST);
		boolean isApi2ImportGiftCardMissingEpinSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2EpinErrorSchema, importGiftCardMissingEpinResponse.asString());
		Assert.assertTrue(isApi2ImportGiftCardMissingEpinSchemaValidated,
				"API2 Import Gift Card with missing epin Schema Validation failed");
		String importGiftCardMissingEpinMsg = importGiftCardMissingEpinResponse.jsonPath().get("errors.epin");
		Assert.assertEquals(importGiftCardMissingEpinMsg, "Required parameter missing or the value is empty.");
		utils.logPass("API2 Import Gift Card call with missing epin is unsuccessful");

		// Negative Case: Mobile API2 Import Gift Card with invalid token
		utils.logit("== Mobile API2: Import Gift Card with invalid token ==");
		Response importGiftCardInvalidTokenResponse = pageObj.endpoints().api2ImportGiftCard(giftCardName,
				dataSet.get("designId"), dataSet.get("cardNumber"), dataSet.get("epin"), dataSet.get("client"),
				dataSet.get("secret"), "1");
		Assert.assertEquals(importGiftCardInvalidTokenResponse.getStatusCode(), ApiConstants.HTTP_STATUS_UNAUTHORIZED);
		boolean isApi2ImportGiftCardInvalidTokenSchemaValidated = Utilities.validateJsonAgainstSchema(
				ApiResponseJsonSchema.api2UnauthorizedErrorSchema, importGiftCardInvalidTokenResponse.asString());
		Assert.assertTrue(isApi2ImportGiftCardInvalidTokenSchemaValidated,
				"API2 Import Gift Card with invalid token Schema Validation failed");
		String importGiftCardInvalidTokenMsg = importGiftCardInvalidTokenResponse.jsonPath()
				.get("errors.unauthorized[0]");
		Assert.assertEquals(importGiftCardInvalidTokenMsg,
				"An active access token must be used to query information about the current user.");
		utils.logPass("API2 Import Gift Card call with invalid token is unsuccessful");

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
